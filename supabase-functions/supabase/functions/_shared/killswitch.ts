/**
 * Interruptor de solicitudes, comprobado EN EL SERVIDOR.
 *
 * ── Por qué existe ──
 * El interruptor vivía solo en el cliente: la app se descargaba el JSON y, si
 * decía que no, escondía el botón. Dos agujeros:
 *
 *   1. FALLABA EN ABIERTO. `RemoteIconConfig` tiene allowFreeRequests = true por
 *      defecto y `loadRemoteConfig` se traga los errores. Sin red, con la
 *      descarga lenta, o simplemente abriendo la pantalla antes de que llegara
 *      el JSON, el botón aparecía y la solicitud salía. No hacía falta piratear
 *      nada: bastaba mala conexión. (Esto explica el usuario que consiguió
 *      enviar con el botón "apagado" el 2026-08-13.)
 *
 *   2. ERA BORRABLE. Está dentro del APK. Quien lo decompile puede quitar la
 *      comprobación o bloquear el dominio del JSON.
 *
 * Ahora la decisión la toma el servidor. Da igual lo que muestre el cliente:
 * si las solicitudes están en pausa, /iconRequest las rechaza.
 *
 * Se lee el MISMO archivo que ya usas, para no tener dos interruptores que
 * mantener sincronizados.
 */

const KILL_SWITCH_URL = Deno.env.get("KILL_SWITCH_URL") ??
  "https://raw.githubusercontent.com/AKustom15/GlassWave_General/refs/heads/main/interruptor_solicitud_icons_v2.json";

const CACHE_MS = 60_000;

export interface RequestsGate {
  open: boolean;
  message: string;
}

let cache: { value: RequestsGate; at: number } | null = null;

/**
 * ¿Están abiertas las solicitudes?
 *
 * Se cachea 60 s en memoria para no pedir el JSON en cada solicitud.
 *
 * **Falla en CERRADO**, al revés que el cliente. Si el archivo no se puede
 * leer y no hay valor previo, se asume pausa: el objetivo de este interruptor
 * es que la pausa se respete, y una pausa de más es preferible a una fuga.
 * Si ya se leyó antes con éxito, se reutiliza ese último valor conocido, así
 * que una caída puntual de GitHub no corta el servicio.
 */
export async function requestsOpen(): Promise<RequestsGate> {
  const now = Date.now();
  if (cache && now - cache.at < CACHE_MS) return cache.value;

  try {
    const res = await fetch(KILL_SWITCH_URL, {
      signal: AbortSignal.timeout(5000),
      headers: { "Cache-Control": "no-cache" },
    });
    if (!res.ok) throw new Error(`http_${res.status}`);

    const cfg = await res.json();
    const value: RequestsGate = {
      open: cfg.allow_free_requests === true,
      message: typeof cfg.pause_message === "string" ? cfg.pause_message : "",
    };
    cache = { value, at: now };
    return value;
  } catch (e) {
    console.error("killswitch fetch failed:", e instanceof Error ? e.message : e);
    // Último valor conocido si lo hay; si no, cerrado.
    if (cache) return cache.value;
    return { open: false, message: "" };
  }
}
