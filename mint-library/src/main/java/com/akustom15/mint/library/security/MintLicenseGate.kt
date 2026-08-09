package com.akustom15.mint.library.security

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.google.android.play.core.integrity.IntegrityManagerFactory
import com.google.android.play.core.integrity.IntegrityTokenRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

/**
 * Verificación de titularidad AL ARRANCAR la app.
 *
 * ── Por qué existe, si ya está MintIconRequestGate ──
 * El gate de solicitudes protege el servicio (que es lo que cuesta dinero), pero
 * no impide que una copia pirata se abra y se use. Esto sí: si el servidor
 * CONFIRMA que la cuenta no compró la app, se bloquea el acceso.
 *
 * ── La regla que evita falsos positivos ──
 * Solo se bloquea con un `UNLICENSED` explícito del servidor. Cualquier otra
 * situación —sin red, servidor caído, dominio bloqueado, cuota agotada— deja
 * pasar. Esto es deliberado y es la diferencia entre proteger y hacerse daño:
 *
 *   Servidor dice UNLICENSED  → BLOQUEA        (pirata confirmado)
 *   Servidor dice LICENSED    → pasa, 7 días   (comprador confirmado)
 *   No hay respuesta          → pasa          (no acusamos sin pruebas)
 *
 * Un comprador en el metro sin cobertura entra. Un pirata con la app abierta y
 * conexión, no. Bloquear "por si acaso" costaría reseñas de 1 estrella de gente
 * que sí pagó, que es un daño peor que el pirata que se cuela.
 *
 * ── Honestidad sobre su alcance ──
 * Esta comprobación vive en el cliente, así que un cracker puede parchearla.
 * Por eso NO es la defensa principal: la defensa que no se puede parchear es
 * MintIconRequestGate, porque la decisión la toma el servidor. Esta capa sube
 * el coste del crack; Automatic Integrity Protection (Play Console) lo sube más.
 *
 * ── Coste ──
 * Una llamada por dispositivo cada 7 días. Con 2.000 usuarios son ~285/día de
 * una cuota de 10.000. Antes se hacía en CADA arranque: ~4.000/día para nada.
 */
class MintLicenseGate(
    private val context: Context,
    private val nonceUrl: String,
    private val verifyUrl: String
) {

    enum class Verdict { LICENSED, UNLICENSED, UNKNOWN }

    private companion object {
        const val PREFS = "mint_license_cache"
        const val KEY_VERDICT = "verdict"
        const val KEY_TIME = "checked_at"

        // Un comprador confirmado no se vuelve a molestar en una semana.
        const val TTL_LICENSED = 7L * 24 * 60 * 60 * 1000
        // Un UNLICENSED se reintenta al día siguiente, por si fue un falso
        // positivo (cuenta secundaria, compra familiar, cambio de dispositivo).
        const val TTL_UNLICENSED = 24L * 60 * 60 * 1000
        // Sin respuesta: no martillear al servidor ni a la cuota.
        const val TTL_UNKNOWN = 6L * 60 * 60 * 1000
    }

    private val prefs by lazy {
        try {
            EncryptedSharedPreferences.create(
                context,
                PREFS,
                MasterKey.Builder(context)
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build(),
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (e: Exception) {
            context.getSharedPreferences("${PREFS}_plain", Context.MODE_PRIVATE)
        }
    }

    private fun cached(): Verdict? {
        val name = prefs.getString(KEY_VERDICT, null) ?: return null
        val verdict = runCatching { Verdict.valueOf(name) }.getOrNull() ?: return null
        val age = System.currentTimeMillis() - prefs.getLong(KEY_TIME, 0)
        val ttl = when (verdict) {
            Verdict.LICENSED -> TTL_LICENSED
            Verdict.UNLICENSED -> TTL_UNLICENSED
            Verdict.UNKNOWN -> TTL_UNKNOWN
        }
        // age < 0 → el reloj se movió hacia atrás: no fiarse de la caché.
        if (age < 0 || age > ttl) return null
        return verdict
    }

    private fun store(verdict: Verdict) {
        prefs.edit()
            .putString(KEY_VERDICT, verdict.name)
            .putLong(KEY_TIME, System.currentTimeMillis())
            .apply()
    }

    /** Fuerza una comprobación nueva (por ejemplo tras una compra). */
    fun invalidate() = prefs.edit().clear().apply()

    private suspend fun post(url: String, body: String, appCheck: String): String? =
        withContext(Dispatchers.IO) {
            var conn: HttpURLConnection? = null
            try {
                conn = (URL(url).openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    setRequestProperty("Content-Type", "application/json; utf-8")
                    setRequestProperty("X-Firebase-AppCheck", appCheck)
                    doOutput = true
                    connectTimeout = 12_000
                    readTimeout = 15_000
                }
                OutputStreamWriter(conn.outputStream).use { it.write(body); it.flush() }
                val stream =
                    if (conn.responseCode in 200..299) conn.inputStream else conn.errorStream
                stream?.bufferedReader()?.use { it.readText() }
            } catch (e: Exception) {
                null
            } finally {
                conn?.disconnect()
            }
        }

    suspend fun check(force: Boolean = false): Verdict {
        if (nonceUrl.isBlank() || verifyUrl.isBlank()) return Verdict.UNKNOWN
        if (!force) cached()?.let { return it }

        val verdict = try {
            val appCheck = MintAppCheck.token() ?: return Verdict.UNKNOWN.also { store(it) }

            // 1 · Nonce emitido por el servidor
            val nonceBody = post(nonceUrl, "{}", appCheck) ?: return cacheUnknown()
            val nonce = JSONObject(nonceBody).optString("nonce")
            if (nonce.isBlank()) return cacheUnknown()

            // 2 · Token de integridad firmado con ESE nonce
            val token = IntegrityManagerFactory.create(context.applicationContext)
                .requestIntegrityToken(
                    IntegrityTokenRequest.builder().setNonce(nonce).build()
                ).await().token()

            // 3 · El servidor valida las siete cosas y responde
            val body = JSONObject()
                .put("integrityToken", token)
                .put("nonce", nonce)
                .toString()

            val response = post(verifyUrl, body, appCheck) ?: return cacheUnknown()
            val json = JSONObject(response)

            when {
                json.optBoolean("licensed", false) -> Verdict.LICENSED
                json.optString("verdict") == "UNLICENSED" -> Verdict.UNLICENSED
                // REJECTED, UNEVALUATED, UNKNOWN… → no bloqueamos sin certeza.
                else -> Verdict.UNKNOWN
            }
        } catch (e: Exception) {
            Verdict.UNKNOWN
        }

        store(verdict)
        return verdict
    }

    private fun cacheUnknown(): Verdict {
        store(Verdict.UNKNOWN)
        return Verdict.UNKNOWN
    }
}
