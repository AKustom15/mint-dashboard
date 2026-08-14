package com.akustom15.mint.library.security

import android.content.Context
import com.akustom15.mint.library.data.MintInstallId
import com.google.android.play.core.integrity.IntegrityManagerFactory
import com.google.android.play.core.integrity.IntegrityTokenRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

/**
 * Puerta de las solicitudes de iconos.
 *
 * El cliente ya NO escribe en Firestore. Pide permiso al servidor, y el
 * servidor —que es quien puede comprobar con Google si esta cuenta compró la
 * app— decide y escribe.
 *
 * Flujo (los tres pasos son obligatorios):
 *   1. POST /issueNonce   → el SERVIDOR emite el nonce (TTL 5 min, un solo uso)
 *   2. requestIntegrityToken con ESE nonce
 *   3. POST /iconRequest  → el servidor valida las 7 cosas + la cuota y escribe
 *
 * Fail-CLOSED: si algo falla, no hay solicitud. Es lo contrario del arranque de
 * la app, que sigue siendo fail-open para no dejar fuera a un comprador con
 * mala conexión.
 */
class MintIconRequestGate(
    private val context: Context,
    private val nonceUrl: String,
    private val iconRequestUrl: String,
    private val iconStatusUrl: String = ""
) {

    /** Estado de solicitudes según el SERVIDOR, que es quien lleva la cuenta. */
    data class Status(
        val requested: Set<String>,
        val freeRemaining: Int,
        val totalRequested: Int
    )

    sealed class Result {
        /** El servidor aceptó y ya escribió en Firestore. */
        data class Accepted(
            val requestId: String?,
            val accepted: List<String>,
            val totalRequested: Int,
            val freeRemaining: Int,
            val premiumConsumed: Int
        ) : Result()

        /** Sin cuota suficiente. Es un motivo legítimo y se puede explicar. */
        data class QuotaExceeded(val freeRemaining: Int) : Result()

        /**
         * Las solicitudes están en pausa. Lo decide el SERVIDOR, no el cliente.
         *
         * El interruptor del cliente falla en abierto (valor por defecto true y
         * errores ignorados al descargar el JSON), así que se podía saltar sin
         * piratear nada: bastaba mala conexión o abrir la pantalla antes de que
         * llegara la configuración. Ahora el servidor tiene la última palabra.
         */
        data class Paused(val message: String) : Result()

        /**
         * No se pudo verificar: sin red, servidor caído, o la cuenta no tiene
         * la app comprada. Deliberadamente NO se distingue el caso: al usuario
         * se le muestra un mensaje neutro y a un atacante no se le dice qué
         * comprobación falló ni qué parchear.
         */
        data class Unavailable(val reason: String) : Result()
    }

    private suspend fun post(url: String, body: String, appCheck: String): Pair<Int, String?> =
        withContext(Dispatchers.IO) {
            var conn: HttpURLConnection? = null
            try {
                conn = (URL(url).openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    setRequestProperty("Content-Type", "application/json; utf-8")
                    setRequestProperty("Accept", "application/json")
                    setRequestProperty("X-Firebase-AppCheck", appCheck)
                    doOutput = true
                    connectTimeout = 15_000
                    readTimeout = 20_000
                }
                OutputStreamWriter(conn.outputStream).use { it.write(body); it.flush() }

                val code = conn.responseCode
                val stream = if (code in 200..299) conn.inputStream else conn.errorStream
                val text = stream?.bufferedReader()?.use { it.readText() }
                code to text
            } catch (e: Exception) {
                -1 to null
            } finally {
                conn?.disconnect()
            }
        }

    /**
     * Consulta al servidor cuántas solicitudes lleva esta instalación.
     *
     * Sustituye a la lectura directa de Firestore desde el cliente, que era
     * frágil: si fallaba, el `catch` devolvía vacío y la app mostraba la cuota
     * entera disponible aunque estuviera agotada.
     *
     * Devuelve null si no se pudo consultar. El llamante debe distinguir eso de
     * "no ha pedido nada" y NO mostrar la cuota completa por defecto.
     */
    suspend fun status(): Status? {
        if (iconStatusUrl.isBlank()) return null
        return try {
            val appCheck = MintAppCheck.token() ?: return null
            val body = JSONObject()
                .put("installId", MintInstallId.get(context))
                .toString()

            val (code, response) = post(iconStatusUrl, body, appCheck)
            if (code !in 200..299 || response == null) return null

            val json = JSONObject(response)
            if (!json.optBoolean("ok", false)) return null

            val arr = json.optJSONArray("requested")
            val requested = buildSet {
                if (arr != null) for (i in 0 until arr.length()) add(arr.getString(i))
            }
            Status(
                requested = requested,
                freeRemaining = json.optInt("freeRemaining", 0),
                totalRequested = json.optInt("totalRequested", requested.size)
            )
        } catch (e: Exception) {
            null
        }
    }

    /**
     * @param packages paquetes que el usuario quiere solicitar
     * @param premiumCredits créditos premium locales disponibles
     * @param appVersion versionName, solo para trazabilidad
     */
    suspend fun request(
        packages: Collection<String>,
        premiumCredits: Int,
        appVersion: String
    ): Result {
        if (nonceUrl.isBlank() || iconRequestUrl.isBlank()) {
            return Result.Unavailable("not_configured")
        }
        if (packages.isEmpty()) return Result.Unavailable("empty")

        return try {
            val appCheck = MintAppCheck.token()
                ?: return Result.Unavailable("no_appcheck")

            // 1 · Nonce emitido por el servidor
            val (nonceCode, nonceBody) = post(nonceUrl, "{}", appCheck)
            if (nonceCode !in 200..299 || nonceBody == null) {
                return Result.Unavailable("nonce_$nonceCode")
            }
            val nonce = JSONObject(nonceBody).optString("nonce")
            if (nonce.isBlank()) return Result.Unavailable("no_nonce")

            // 2 · Token de integridad firmado con ESE nonce
            val integrityToken = IntegrityManagerFactory
                .create(context.applicationContext)
                .requestIntegrityToken(
                    IntegrityTokenRequest.builder().setNonce(nonce).build()
                )
                .await()
                .token()

            // 3 · El servidor decide
            val payload = JSONObject().apply {
                put("nonce", nonce)
                put("integrityToken", integrityToken)
                put("installId", MintInstallId.get(context))
                put("packages", JSONArray(packages.toList()))
                put("premiumClaimed", premiumCredits)
                put("appVersion", appVersion)
            }.toString()

            val (code, body) = post(iconRequestUrl, payload, appCheck)
            if (body == null) return Result.Unavailable("http_$code")

            val json = JSONObject(body)

            if (code in 200..299 && json.optBoolean("ok", false)) {
                val acceptedArray = json.optJSONArray("accepted")
                val accepted = buildList {
                    if (acceptedArray != null) {
                        for (i in 0 until acceptedArray.length()) add(acceptedArray.getString(i))
                    }
                }
                return Result.Accepted(
                    requestId = json.optString("requestId").takeIf { it.isNotBlank() },
                    accepted = accepted,
                    totalRequested = json.optInt("totalRequested", 0),
                    freeRemaining = json.optInt("freeRemaining", 0),
                    premiumConsumed = json.optInt("premiumConsumed", 0)
                )
            }

            when (json.optString("reason")) {
                "quota_exceeded" -> Result.QuotaExceeded(json.optInt("freeRemaining", 0))
                "paused" -> Result.Paused(json.optString("message", ""))
                else -> Result.Unavailable(json.optString("reason", "rejected"))
            }
        } catch (e: Exception) {
            Result.Unavailable("error")
        }
    }
}
