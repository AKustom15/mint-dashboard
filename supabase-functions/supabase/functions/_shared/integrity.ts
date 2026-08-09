import { createClient } from "npm:@supabase/supabase-js@2";
import type { IntegrityPayload } from "./google.ts";

/**
 * Las siete validaciones del token de Play Integrity.
 *
 * Hoy el backend antiguo solo miraba UNA (`appLicensingVerdict`) y no
 * comprobaba el nonce, así que un token capturado una vez servía para siempre
 * desde cualquier dispositivo. Estas siete cierran esa vía.
 *
 * Todos los campos vienen gratis en el mismo payload que ya se recibía.
 */

const MAX_TOKEN_AGE_MS = 5 * 60 * 1000;

export const db = createClient(
  Deno.env.get("SUPABASE_URL")!,
  Deno.env.get("SUPABASE_SERVICE_ROLE_KEY")!,
);

export interface IntegrityCheck {
  ok: boolean;
  reason?: string;
  licensed: boolean;
  verdict: string;
  certOk: boolean;
  /** El nonce tal cual viene DENTRO del token. Solo para diagnóstico. */
  tokenNonce?: string;
}

/**
 * Normaliza un nonce a una forma canónica antes de compararlo.
 *
 * Play Integrity puede devolver el nonce con una codificación base64 distinta
 * de la que enviamos: con relleno `=`, o en base64 estándar (`+` y `/`) en vez
 * de URL-safe (`-` y `_`). Comparar las cadenas en crudo daría un falso
 * negativo aunque los 32 bytes sean idénticos.
 *
 * Esto NO debilita la comprobación: se siguen comparando los mismos bytes,
 * solo se ignora la variante de codificación.
 */
function normalizeNonce(s: string): string {
  return s.trim().replace(/\+/g, "-").replace(/\//g, "_").replace(/=+$/, "");
}

/**
 * Consume el nonce de forma atómica: solo la primera llamada gana.
 * El UPDATE condicional de Postgres garantiza que dos peticiones simultáneas
 * con el mismo nonce no puedan pasar las dos.
 */
export async function consumeNonce(nonce: string): Promise<boolean> {
  if (!nonce) return false;
  const cutoff = new Date(Date.now() - MAX_TOKEN_AGE_MS).toISOString();

  const { data, error } = await db
    .from("integrity_nonces")
    .update({ used_at: new Date().toISOString() })
    .eq("nonce", nonce)
    .is("used_at", null)          // un solo uso
    .gte("created_at", cutoff)    // TTL de 5 minutos
    .select("nonce");

  if (error) {
    console.error("consumeNonce:", error.message);
    return false;
  }
  return (data?.length ?? 0) === 1;
}

/**
 * Valida el payload decodificado. `nonce` es el que el servidor emitió y que ya
 * fue consumido; aquí se comprueba que sea también el que va DENTRO del token.
 */
export function validateIntegrity(
  payload: IntegrityPayload,
  nonce: string,
  packageName: string,
  expectedCert: string,
  requireDeviceIntegrity: boolean,
): IntegrityCheck {
  const rd = payload.requestDetails ?? {};
  const ai = payload.appIntegrity ?? {};
  const di = payload.deviceIntegrity ?? {};
  const acc = payload.accountDetails ?? {};

  const verdict = acc.appLicensingVerdict ?? "UNEVALUATED";
  const certDigests = ai.certificateSha256Digest ?? [];
  const certOk = expectedCert ? certDigests.includes(expectedCert) : false;

  const tokenNonce = rd.nonce ?? "";

  const deny = (reason: string): IntegrityCheck => ({
    ok: false, reason, licensed: false, verdict, certOk, tokenNonce,
  });

  // 2 · el nonce DENTRO del token es el que emitimos → mata el replay
  if (normalizeNonce(tokenNonce) !== normalizeNonce(nonce)) {
    return deny("nonce_mismatch");
  }

  // 3 · el token es de ESTA app
  if (rd.requestPackageName !== packageName) return deny("package_mismatch");

  // 4 · el token es reciente → mata los tokens guardados
  const age = Date.now() - Number(rd.timestampMillis ?? 0);
  if (!(age > -60_000 && age < MAX_TOKEN_AGE_MS)) return deny("stale_token");

  // 5 · APK no modificado ni recompilado → caza al crack
  if (ai.appRecognitionVerdict !== "PLAY_RECOGNIZED") return deny("not_recognized");

  // 6 · APK no re-firmado → caza al repackager
  if (expectedCert && !certOk) return deny("cert_mismatch");

  // 7 · dispositivo íntegro (configurable: los emuladores lo fallan)
  if (requireDeviceIntegrity) {
    const dev = di.deviceRecognitionVerdict ?? [];
    if (!dev.includes("MEETS_DEVICE_INTEGRITY")) return deny("device_integrity");
  }

  // Y solo AHORA, la titularidad.
  return {
    ok: verdict === "LICENSED",
    reason: verdict === "LICENSED" ? undefined : "not_licensed",
    licensed: verdict === "LICENSED",
    verdict,
    certOk,
  };
}
