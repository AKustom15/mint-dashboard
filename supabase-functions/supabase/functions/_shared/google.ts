import { GoogleAuth } from "npm:google-auth-library@9.0.0";

/**
 * Acceso a las APIs de Google con la cuenta de servicio.
 *
 * Necesita DOS ámbitos:
 *  - playintegrity → decodificar el token de integridad
 *  - datastore     → escribir en Firestore saltándose las reglas de seguridad
 *
 * Lo segundo es intencionado: las reglas de Firestore prohíben la escritura al
 * cliente, y es el servidor quien escribe tras validar. Una cuenta de servicio
 * NO está sujeta a las reglas, por diseño de Firebase.
 */

const PROJECT_ID = Deno.env.get("FIREBASE_PROJECT_ID") ?? "glasswave-20e7a";

let cachedToken: { value: string; expiresAt: number } | null = null;

/** Token OAuth de la cuenta de servicio, cacheado hasta 5 min antes de caducar. */
export async function googleAccessToken(): Promise<string> {
  const now = Date.now();
  if (cachedToken && cachedToken.expiresAt > now + 5 * 60_000) {
    return cachedToken.value;
  }

  const auth = new GoogleAuth({
    credentials: JSON.parse(Deno.env.get("GOOGLE_SERVICE_ACCOUNT_JSON")!),
    scopes: [
      "https://www.googleapis.com/auth/playintegrity",
      "https://www.googleapis.com/auth/datastore",
    ],
  });

  const client = await auth.getClient();
  const res = await client.getAccessToken();
  if (!res.token) throw new Error("no_access_token");

  cachedToken = { value: res.token, expiresAt: now + 45 * 60_000 };
  return res.token;
}

// ── Play Integrity ─────────────────────────────────────────────────────────

export interface IntegrityPayload {
  requestDetails?: {
    requestPackageName?: string;
    nonce?: string;
    timestampMillis?: string | number;
  };
  appIntegrity?: {
    appRecognitionVerdict?: string;
    certificateSha256Digest?: string[];
    packageName?: string;
  };
  deviceIntegrity?: { deviceRecognitionVerdict?: string[] };
  accountDetails?: { appLicensingVerdict?: string };
}

/** Decodifica el token CON Google. El cliente no puede falsificar el resultado. */
export async function decodeIntegrityToken(
  packageName: string,
  integrityToken: string,
): Promise<IntegrityPayload> {
  const token = await googleAccessToken();
  const res = await fetch(
    `https://playintegrity.googleapis.com/v1/${packageName}:decodeIntegrityToken`,
    {
      method: "POST",
      headers: {
        Authorization: `Bearer ${token}`,
        "Content-Type": "application/json",
      },
      body: JSON.stringify({ integrityToken }),
    },
  );

  if (!res.ok) {
    const detail = await res.text();
    throw new Error(`decode_failed_${res.status}: ${detail.slice(0, 200)}`);
  }

  const body = await res.json();
  return body?.tokenPayloadExternal ?? {};
}

// ── Firestore REST ─────────────────────────────────────────────────────────

const FS_BASE =
  `https://firestore.googleapis.com/v1/projects/${PROJECT_ID}/databases/(default)/documents`;

/** Lee la lista `icons` de un documento. Devuelve [] si el documento no existe. */
export async function readRequestedIcons(
  collection: string,
  installId: string,
): Promise<string[]> {
  const token = await googleAccessToken();
  const res = await fetch(`${FS_BASE}/${collection}/${encodeURIComponent(installId)}`, {
    headers: { Authorization: `Bearer ${token}` },
  });

  if (res.status === 404) return [];
  if (!res.ok) throw new Error(`firestore_read_${res.status}`);

  const doc = await res.json();
  const values = doc?.fields?.icons?.arrayValue?.values ?? [];
  return values
    .map((v: { stringValue?: string }) => v.stringValue)
    .filter((s: string | undefined): s is string => typeof s === "string");
}

/** Sobrescribe el documento con la lista completa. Solo lo llama el servidor. */
export async function writeRequestedIcons(
  collection: string,
  installId: string,
  icons: string[],
  appVersion: string,
): Promise<void> {
  const token = await googleAccessToken();
  const url = `${FS_BASE}/${collection}/${encodeURIComponent(installId)}`;

  const res = await fetch(url, {
    method: "PATCH",
    headers: {
      Authorization: `Bearer ${token}`,
      "Content-Type": "application/json",
    },
    body: JSON.stringify({
      fields: {
        icons: {
          arrayValue: { values: icons.map((s) => ({ stringValue: s })) },
        },
        lastUpdate: { timestampValue: new Date().toISOString() },
        appVersion: { stringValue: appVersion || "unknown" },
      },
    }),
  });

  if (!res.ok) {
    const detail = await res.text();
    throw new Error(`firestore_write_${res.status}: ${detail.slice(0, 200)}`);
  }
}
