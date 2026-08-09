import { verifyAppCheck, json, b64url } from "../_shared/appcheck.ts";
import { consumeNonce, validateIntegrity, db } from "../_shared/integrity.ts";
import {
  decodeIntegrityToken,
  readRequestedIcons,
  writeRequestedIcons,
} from "../_shared/google.ts";

/**
 * POST /iconRequest
 *
 * LA PUERTA. Un usuario ilegítimo no puede solicitar iconos porque no puede
 * pasar de aquí.
 *
 * Antes: el cliente escribía directo en Firestore, contaba su propia cuota y
 * el servidor no intervenía. Cualquiera con el APK pedía iconos ilimitados.
 *
 * Ahora: el cliente pide, el SERVIDOR decide y el servidor escribe. Fail-closed:
 * si no se puede confirmar que el usuario compró la app, no hay solicitud.
 * Bloquear el dominio para esquivar la comprobación ya no libera nada — deja al
 * pirata sin el servicio.
 *
 * Body: { nonce, integrityToken, installId, packages[], appVersion, premiumClaimed }
 */

const PACKAGE_NAME  = Deno.env.get("APP_PACKAGE_NAME") ?? "com.akustom15.glasswave";
const EXPECTED_CERT = Deno.env.get("EXPECTED_CERT_SHA256") ?? "";
const COLLECTION    = Deno.env.get("FIRESTORE_COLLECTION") ?? "icon_requests";
const FREE_LIMIT    = Number(Deno.env.get("FREE_REQUEST_LIMIT") ?? "10");
const SIGNING_SECRET = Deno.env.get("REQUEST_SIGNING_SECRET") ?? "";

/**
 * Integridad del DISPOSITIVO: desactivada por defecto, a propósito.
 *
 * MEETS_DEVICE_INTEGRITY falla en móviles rooteados y con ROM personalizada, y
 * el público de un pack de iconos (gente que cambia de launcher) está lleno de
 * esos. Exigirlo bloquearía a compradores legítimos, que es peor que dejar
 * pasar a algún pirata con root: la licencia ya la valida appLicensingVerdict.
 *
 * Ponlo a "true" solo si detectas abuso desde emuladores.
 */
const REQUIRE_DEVICE_INTEGRITY =
  (Deno.env.get("REQUIRE_DEVICE_INTEGRITY") ?? "false") === "true";

/** Máximo de paquetes por solicitud. Evita que una sola llamada infle el documento. */
const MAX_PER_REQUEST = 60;
const MAX_TOTAL = 1000;

/** Firma el identificador que viajará en el email, para que puedas verificarlo. */
async function signRequestId(installId: string, count: number): Promise<string> {
  const stamp = Date.now().toString(36).toUpperCase();
  const body = `${installId}|${count}|${stamp}`;

  if (!SIGNING_SECRET) return `GW-${stamp}-UNSIGNED`;

  const key = await crypto.subtle.importKey(
    "raw",
    new TextEncoder().encode(SIGNING_SECRET),
    { name: "HMAC", hash: "SHA-256" },
    false,
    ["sign"],
  );
  const sig = await crypto.subtle.sign("HMAC", key, new TextEncoder().encode(body));
  const short = b64url(new Uint8Array(sig)).slice(0, 10).toUpperCase();
  return `GW-${stamp}-${short}`;
}

Deno.serve(async (req) => {
  if (req.method !== "POST") return json({ error: "method_not_allowed" }, 405);

  // ── Puerta 1: App Check. Sin esto no se gasta cuota de Play Integrity.
  const appId = await verifyAppCheck(req.headers.get("X-Firebase-AppCheck"));
  if (!appId) return json({ ok: false, reason: "unauthorized" }, 401);

  // Fuera del try para que el catch pueda registrar de quién era la petición.
  let installIdForLog = "desconocido";

  try {
    const body = await req.json().catch(() => null);
    if (!body) return json({ ok: false, reason: "bad_request" }, 400);

    const { nonce, integrityToken, installId, appVersion, premiumClaimed } = body;
    const packages: string[] = Array.isArray(body.packages) ? body.packages : [];

    if (!nonce || !integrityToken || !installId || packages.length === 0) {
      return json({ ok: false, reason: "bad_request" }, 400);
    }
    installIdForLog = String(installId);
    if (packages.length > MAX_PER_REQUEST) {
      return json({ ok: false, reason: "too_many" }, 400);
    }

    // ── Puerta 2: el nonce lo emitimos nosotros, está fresco y sin usar.
    if (!await consumeNonce(nonce)) {
      console.warn("nonce rejected for install", installId);
      return json({ ok: false, reason: "unavailable" }, 403);
    }

    // ── Puerta 3: las siete validaciones del token, decodificado por Google.
    const payload = await decodeIntegrityToken(PACKAGE_NAME, integrityToken);
    const check = validateIntegrity(
      payload, nonce, PACKAGE_NAME, EXPECTED_CERT, REQUIRE_DEVICE_INTEGRITY,
    );

    if (!check.ok) {
      // Motivo genérico hacia el CLIENTE: no le decimos a un atacante qué
      // comprobación falló ni qué parchear.
      console.warn("rejected:", check.reason, "verdict:", check.verdict, "install:", installId);

      // Motivo detallado para TI, en la base de datos. Los logs de Supabase se
      // rotan y son incómodos de consultar; esto queda y se filtra con SQL.
      // Las filas de rechazo llevan request_id con prefijo REJ-.
      await db.from("icon_request_log").insert({
        request_id: `REJ-${crypto.randomUUID().slice(0, 18)}`,
        install_id: installId,
        packages: packages.slice(0, 5),
        verdict: check.reason === "nonce_mismatch"
          ? `RECHAZADO:nonce_mismatch | enviado=[${nonce}] recibido=[${check.tokenNonce ?? ""}]`
          : `RECHAZADO:${check.reason} | appLicensingVerdict=${check.verdict}`,
        cert_ok: check.certOk,
      });

      return json({ ok: false, reason: "unavailable" }, 403);
    }

    // ── Puerta 4: cuota, contada EN SERVIDOR sobre el documento real.
    const already = await readRequestedIcons(COLLECTION, installId);
    const alreadySet = new Set(already);
    const newPackages = packages.filter((p) => p && !alreadySet.has(p));

    if (newPackages.length === 0) {
      return json({
        ok: true, requestId: null, alreadyRequested: true,
        totalRequested: already.length,
        freeRemaining: Math.max(0, FREE_LIMIT - Math.min(already.length, FREE_LIMIT)),
      });
    }

    const freeUsed = Math.min(already.length, FREE_LIMIT);
    const freeAvailable = Math.max(0, FREE_LIMIT - freeUsed);

    // Los créditos premium siguen siendo del cliente (ver nota al final del
    // archivo). Solo se aceptan porque llegar hasta aquí ya exige LICENSED.
    const premium = Math.max(0, Math.min(Number(premiumClaimed) || 0, 500));
    const allowed = freeAvailable + premium;

    if (newPackages.length > allowed) {
      // También se registra: sin esto, un rechazo por cuota era invisible y no
      // se podía distinguir "el servidor bloqueó" de "el cliente ni lo intentó".
      await db.from("icon_request_log").insert({
        request_id: `CUOTA-${crypto.randomUUID().slice(0, 16)}`,
        install_id: installId,
        packages: newPackages.slice(0, 5),
        verdict: `CUOTA_AGOTADA | ya_tenia=${already.length} pedidos=${newPackages.length} ` +
                 `libres=${freeAvailable} premium_declarado=${premium}`,
        cert_ok: check.certOk,
      });

      return json({
        ok: false, reason: "quota_exceeded",
        freeRemaining: freeAvailable, allowed,
      }, 403);
    }

    const merged = [...already, ...newPackages];
    if (merged.length > MAX_TOTAL) {
      return json({ ok: false, reason: "quota_exceeded", allowed: 0 }, 403);
    }

    // ── Escritura: la hace el SERVIDOR. El cliente ya no escribe en Firestore.
    await writeRequestedIcons(COLLECTION, installId, merged, appVersion ?? "");

    const requestId = await signRequestId(installId, newPackages.length);

    // Traza para cruzar con los emails que te lleguen.
    const { error: logError } = await db.from("icon_request_log").insert({
      request_id: requestId,
      install_id: installId,
      packages: newPackages,
      verdict: `${check.verdict} | ya_tenia=${already.length} nuevos=${newPackages.length} ` +
               `libres_antes=${freeAvailable} premium_declarado=${premium}`,
      cert_ok: check.certOk,
    });
    if (logError) console.error("log insert:", logError.message);

    const newFreeUsed = Math.min(merged.length, FREE_LIMIT);
    return json({
      ok: true,
      requestId,
      accepted: newPackages,
      totalRequested: merged.length,
      freeRemaining: Math.max(0, FREE_LIMIT - newFreeUsed),
      premiumConsumed: Math.max(0, newPackages.length - freeAvailable),
    });

  } catch (e) {
    const msg = e instanceof Error ? e.message : String(e);
    console.error("iconRequest failed:", msg);

    // Los errores inesperados también quedan en la tabla, para poder
    // diagnosticarlos sin depender de los logs rotatorios de Supabase.
    try {
      await db.from("icon_request_log").insert({
        request_id: `ERR-${crypto.randomUUID().slice(0, 18)}`,
        install_id: installIdForLog,
        packages: [],
        verdict: `ERROR:${msg.slice(0, 300)}`,
        cert_ok: false,
      });
    } catch (_) { /* si ni esto funciona, queda el console.error */ }

    return json({ ok: false, reason: "server_error" }, 500);
  }
});

// ═══════════════════════════════════════════════════════════════════════════
// LO QUE ESTE ENDPOINT CIERRA
//
//   Un pirata pide iconos gratis        → appLicensingVerdict != LICENSED → 403
//   Resetea su cuota borrando el doc    → el cliente ya no escribe en Firestore
//   Reenvía un token capturado          → nonce de un solo uso + TTL 5 min
//   Usa un APK modificado/re-firmado    → appRecognitionVerdict + certificado
//   Llama al endpoint con un script     → App Check
//   Bloquea el dominio para esquivar    → sin respuesta = sin solicitud
//
// HUECO CONOCIDO (para una fase posterior):
//   `premiumClaimed` lo envía el cliente. Un COMPRADOR podría inflarlo para
//   darse créditos extra. No es la vía de piratería que te preocupa —para
//   llegar aquí hay que tener la app comprada— pero se cierra validando los
//   purchase tokens con la Google Play Developer API y guardándolos en la
//   tabla `entitlements`, que ya está creada esperando eso.
// ═══════════════════════════════════════════════════════════════════════════
