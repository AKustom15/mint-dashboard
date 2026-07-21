package com.akustom15.mint.library.security

import android.content.Context
import android.util.Base64
import android.util.Log
import com.google.android.play.core.integrity.IntegrityManagerFactory
import com.google.android.play.core.integrity.IntegrityTokenRequest
import com.google.firebase.functions.FirebaseFunctions
import kotlinx.coroutines.tasks.await
import java.security.SecureRandom

/**
 * Server-side paid-app license verification.
 *
 * Flow:
 *  1. Request a Play Integrity token on the device (classic request).
 *  2. Send it to a Cloud Function ([functionName]) that decodes the token with
 *     Google's Play Integrity API and reads `accountDetails.appLicensingVerdict`.
 *  3. The server returns whether the account actually owns (bought) this paid app.
 *
 * The decision is made ON THE SERVER — a patched client can't fake `LICENSED`
 * because it can't produce a valid Google-signed integrity token for your app.
 * This is the only robust way to protect a PAID app from redistribution.
 */
class MintLicenseVerifier(
    private val context: Context,
    private val functionName: String = "verifyAppLicense",
    private val region: String = ""
) {
    companion object {
        private const val TAG = "MintLicenseVerifier"
    }

    /** Result of a license verification round. */
    sealed class Result {
        /** The account owns the paid app. */
        object Licensed : Result()
        /** The account does NOT own the app (pirated / not purchased). */
        object Unlicensed : Result()
        /**
         * Couldn't determine (network error, function unreachable, verdict
         * UNEVALUATED). Callers should FAIL OPEN here to avoid blocking legit
         * users on transient issues, and re-check later.
         */
        data class Unknown(val reason: String) : Result()
    }

    private val functions: FirebaseFunctions by lazy {
        if (region.isNotBlank()) FirebaseFunctions.getInstance(region)
        else FirebaseFunctions.getInstance()
    }

    private fun randomNonce(): String {
        val bytes = ByteArray(32)
        SecureRandom().nextBytes(bytes)
        return Base64.encodeToString(bytes, Base64.NO_WRAP or Base64.URL_SAFE)
    }

    suspend fun verify(): Result {
        return try {
            // 1. Get the integrity token on-device
            val manager = IntegrityManagerFactory.create(context.applicationContext)
            val response = manager.requestIntegrityToken(
                IntegrityTokenRequest.builder().setNonce(randomNonce()).build()
            ).await()
            val token = response.token()

            // 2. Ask the server to decode & evaluate it
            val data = hashMapOf("integrityToken" to token)
            val callResult = functions.getHttpsCallable(functionName).call(data).await()

            @Suppress("UNCHECKED_CAST")
            val map = callResult.getData() as? Map<String, Any?> ?: emptyMap()
            val verdict = (map["verdict"] as? String) ?: "UNKNOWN"
            val licensed = (map["licensed"] as? Boolean) ?: false

            Log.d(TAG, "Server license verdict: $verdict (licensed=$licensed)")
            when {
                licensed -> Result.Licensed
                verdict == "UNLICENSED" -> Result.Unlicensed
                else -> Result.Unknown("verdict=$verdict")
            }
        } catch (e: Exception) {
            Log.e(TAG, "License verification failed", e)
            Result.Unknown(e.message ?: "error")
        }
    }
}
