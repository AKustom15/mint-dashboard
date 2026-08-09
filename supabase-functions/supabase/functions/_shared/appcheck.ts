import { createRemoteJWKSet, jwtVerify } from "npm:jose@5";

/**
 * Verificación del token de Firebase App Check.
 *
 * Es la primera puerta de todos los endpoints: sin un token válido, la petición
 * no llega ni a Google. Eso impide que cualquiera en internet invoque las
 * funciones y agote tu cuota de Play Integrity (que son 10.000/día y, al
 * agotarse, dejaría la protección apagada).
 *
 * El token lo emite Firebase solo a instalaciones genuinas de tu app, firmadas
 * por Play. Un script o un APK re-firmado no puede producirlo.
 */

const JWKS = createRemoteJWKSet(
  new URL("https://firebaseappcheck.googleapis.com/v1/jwks"),
);

const PROJECT_NUMBER = Deno.env.get("FIREBASE_PROJECT_NUMBER") ?? "";

/** Devuelve el appId si el token es válido, o null. */
export async function verifyAppCheck(token: string | null): Promise<string | null> {
  if (!token || !PROJECT_NUMBER) return null;
  try {
    const { payload } = await jwtVerify(token, JWKS, {
      issuer: `https://firebaseappcheck.googleapis.com/${PROJECT_NUMBER}`,
      audience: `projects/${PROJECT_NUMBER}`,
      algorithms: ["RS256"],
    });
    return typeof payload.sub === "string" ? payload.sub : null;
  } catch {
    return null;
  }
}

/**
 * Respuesta JSON con CORS cerrado.
 * Un cliente Android no necesita CORS; abrirlo solo facilitaría el abuso desde
 * un navegador (hallazgo B-3 de la auditoría).
 */
export function json(body: unknown, status = 200): Response {
  return new Response(JSON.stringify(body), {
    status,
    headers: { "Content-Type": "application/json" },
  });
}

/** Codifica bytes en base64url sin padding (el formato que usa Play Integrity). */
export function b64url(bytes: Uint8Array): string {
  let bin = "";
  for (const b of bytes) bin += String.fromCharCode(b);
  return btoa(bin).replace(/\+/g, "-").replace(/\//g, "_").replace(/=+$/, "");
}
