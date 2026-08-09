import { verifyAppCheck, json } from "../_shared/appcheck.ts";
import { consumeNonce, validateIntegrity } from "../_shared/integrity.ts";
import { decodeIntegrityToken } from "../_shared/google.ts";

/**
 * POST /verifyAppLicense — OBSOLETO
 *
 * Este endpoint existía para comprobar la licencia AL ARRANCAR la app. Ya no se
 * usa: la verificación se movió a /iconRequest, que es donde de verdad importa
 * (ver 03_PRIORIDAD_solicitudes_de_iconos.md). Dejar la comprobación en el
 * arranque gastaba ~4.000 llamadas diarias de una cuota de 10.000, sin proteger
 * nada, porque el cliente fallaba en abierto igualmente.
 *
 * Se mantiene por dos razones:
 *  1. Las versiones ya instaladas (librería 1.0.62) siguen llamándolo. Como
 *     hacen fail-open, un 401 no las rompe: la app abre igual.
 *  2. Cerrarlo elimina el hallazgo A-1 — hasta hoy era público y cualquiera
 *     podía agotar tu cuota de Play Integrity desde internet.
 *
 * Se puede borrar cuando ya no queden instalaciones de 1.0.62 o anteriores.
 */

const PACKAGE_NAME  = Deno.env.get("APP_PACKAGE_NAME") ?? "com.akustom15.glasswave";
const EXPECTED_CERT = Deno.env.get("EXPECTED_CERT_SHA256") ?? "";
const REQUIRE_DEVICE_INTEGRITY =
  (Deno.env.get("REQUIRE_DEVICE_INTEGRITY") ?? "false") === "true";

Deno.serve(async (req) => {
  if (req.method !== "POST") return json({ error: "method_not_allowed" }, 405);

  // Puerta que faltaba: sin App Check no se gasta cuota de Play Integrity.
  const appId = await verifyAppCheck(req.headers.get("X-Firebase-AppCheck"));
  if (!appId) return json({ licensed: false, verdict: "UNKNOWN" }, 401);

  try {
    const { integrityToken, nonce } = await req.json().catch(() => ({}));
    if (!integrityToken || !nonce) {
      return json({ licensed: false, verdict: "UNKNOWN" }, 400);
    }

    if (!await consumeNonce(nonce)) {
      return json({ licensed: false, verdict: "REJECTED" }, 403);
    }

    const payload = await decodeIntegrityToken(PACKAGE_NAME, integrityToken);
    const check = validateIntegrity(
      payload, nonce, PACKAGE_NAME, EXPECTED_CERT, REQUIRE_DEVICE_INTEGRITY,
    );

    if (!check.ok) {
      console.warn("verifyAppLicense rejected:", check.reason);
      return json({ licensed: false, verdict: check.verdict }, 403);
    }

    return json({ licensed: true, verdict: check.verdict });

  } catch (e) {
    console.error("verifyAppLicense failed:", e instanceof Error ? e.message : e);
    // Mensaje genérico: no filtrar internals al cliente (hallazgo B-2).
    return json({ licensed: false, verdict: "UNKNOWN" }, 500);
  }
});
