import { verifyAppCheck, json, b64url } from "../_shared/appcheck.ts";
import { db } from "../_shared/integrity.ts";

/**
 * POST /issueNonce
 *
 * Emite un nonce aleatorio, lo guarda con TTL de 5 minutos y un solo uso.
 *
 * Esta es la pieza que faltaba en el diseño anterior: el nonce lo generaba el
 * CLIENTE, así que el servidor no tenía forma de saber si un token era nuevo o
 * reenviado. Ahora el nonce lo emite el servidor y lo verifica al recibirlo.
 *
 * Cabecera obligatoria: X-Firebase-AppCheck
 */
Deno.serve(async (req) => {
  if (req.method !== "POST") return json({ error: "method_not_allowed" }, 405);

  const appId = await verifyAppCheck(req.headers.get("X-Firebase-AppCheck"));
  if (!appId) return json({ error: "unauthorized" }, 401);

  const bytes = new Uint8Array(32);
  crypto.getRandomValues(bytes);
  const nonce = b64url(bytes);

  const { error } = await db.from("integrity_nonces").insert({ nonce });
  if (error) {
    console.error("nonce insert failed:", error.message);
    return json({ error: "server_error" }, 500);
  }

  return json({ nonce, ttlSeconds: 300 });
});
