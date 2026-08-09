import { verifyAppCheck, json } from "../_shared/appcheck.ts";
import { readRequestedIcons } from "../_shared/google.ts";

/**
 * POST /iconStatus  → { requested[], totalRequested, freeRemaining }
 *
 * Devuelve el estado de solicitudes de una instalación.
 *
 * ── Por qué existe ──
 * Antes el cliente leía su documento de Firestore directamente. Eso resultó
 * frágil: si la lectura fallaba por cualquier motivo, el `catch` devolvía una
 * lista vacía y la app mostraba la cuota entera disponible aunque estuviera
 * agotada. El usuario veía "10 disponibles", seleccionaba iconos, y el servidor
 * los rechazaba. Confuso y con pinta de estar roto.
 *
 * Ahora la cuenta la da quien la lleva: el servidor. Una sola fuente de verdad.
 *
 * ── Por qué NO pide token de Play Integrity ──
 * Esto solo LEE, no concede nada. Exigir Play Integrity aquí gastaría una
 * llamada de cuota (10.000/día) cada vez que alguien abre la pantalla, para
 * proteger un dato que no vale nada por sí solo. App Check basta: garantiza
 * que la petición viene de la app genuina.
 *
 * La puerta que decide —/iconRequest— sí exige las siete validaciones.
 */

const COLLECTION = Deno.env.get("FIRESTORE_COLLECTION") ?? "icon_requests";
const FREE_LIMIT = Number(Deno.env.get("FREE_REQUEST_LIMIT") ?? "10");

Deno.serve(async (req) => {
  if (req.method !== "POST") return json({ error: "method_not_allowed" }, 405);

  const appId = await verifyAppCheck(req.headers.get("X-Firebase-AppCheck"));
  if (!appId) return json({ ok: false, reason: "unauthorized" }, 401);

  try {
    const { installId } = await req.json().catch(() => ({}));
    if (!installId || typeof installId !== "string") {
      return json({ ok: false, reason: "bad_request" }, 400);
    }

    const requested = await readRequestedIcons(COLLECTION, installId);
    const freeUsed = Math.min(requested.length, FREE_LIMIT);

    return json({
      ok: true,
      requested,
      totalRequested: requested.length,
      freeRemaining: Math.max(0, FREE_LIMIT - freeUsed),
      freeLimit: FREE_LIMIT,
    });
  } catch (e) {
    console.error("iconStatus failed:", e instanceof Error ? e.message : e);
    return json({ ok: false, reason: "server_error" }, 500);
  }
});
