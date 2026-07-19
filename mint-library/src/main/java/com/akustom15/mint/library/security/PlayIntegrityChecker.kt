package com.akustom15.mint.library.security

import android.content.Context
import android.util.Log
import com.google.android.play.core.integrity.IntegrityManager
import com.google.android.play.core.integrity.IntegrityManagerFactory
import com.google.android.play.core.integrity.IntegrityTokenRequest
import com.google.android.play.core.integrity.IntegrityTokenResponse
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import java.security.SecureRandom
import kotlin.coroutines.resume

/**
 * Sistema de verificación de integridad usando Google Play Integrity API
 * Esta es la solución más moderna y recomendada por Google para 2025
 */
class PlayIntegrityChecker(private val context: Context) {
    companion object {
        private const val TAG = "PlayIntegrityChecker"
    }

    /**
     * A fresh, random nonce per request. NOTE: for real protection the nonce must
     * be generated and later verified server-side (bind it to the decoded token in
     * your backend). A client-only random nonce prevents trivial replay but is not
     * a substitute for server verification.
     */
    private fun generateNonce(): String {
        val bytes = ByteArray(24)
        SecureRandom().nextBytes(bytes)
        return android.util.Base64.encodeToString(
            bytes,
            android.util.Base64.NO_WRAP or android.util.Base64.URL_SAFE
        )
    }

    private val integrityManager: IntegrityManager = IntegrityManagerFactory.create(context)
    private val _integrityState = MutableStateFlow<IntegrityState>(IntegrityState.Checking)
    val integrityState: StateFlow<IntegrityState> = _integrityState

    sealed class IntegrityState {
        object Checking : IntegrityState()
        object Valid : IntegrityState()
        class Invalid(val reason: String) : IntegrityState()
        class Error(val message: String) : IntegrityState()
    }

    /**
     * Verifica la integridad de la app usando Google Play Integrity API
     */
    suspend fun verifyIntegrity(): Boolean = suspendCancellableCoroutine { continuation ->
        try {
            val integrityTokenRequest = IntegrityTokenRequest.builder()
                .setNonce(generateNonce())
                .build()

            integrityManager.requestIntegrityToken(integrityTokenRequest)
                .addOnSuccessListener { response: IntegrityTokenResponse ->
                    val token = response.token()
                    Log.d(TAG, "Integrity token obtenido: ${token.take(50)}...")
                    
                    // Aquí normalmente enviarías el token a tu servidor para verificación
                    // Por ahora, asumimos que si obtenemos el token, la app es válida
                    _integrityState.value = IntegrityState.Valid
                    continuation.resume(true)
                }
                .addOnFailureListener { exception ->
                    Log.e(TAG, "Error obteniendo integrity token", exception)
                    _integrityState.value = IntegrityState.Error("Error de integridad: ${exception.message}")
                    continuation.resume(false)
                }
        } catch (e: Exception) {
            Log.e(TAG, "Excepción en verifyIntegrity", e)
            _integrityState.value = IntegrityState.Error("Excepción: ${e.message}")
            continuation.resume(false)
        }
    }

    /**
     * Verifica si la app está instalada desde Google Play Store
     */
    fun isInstalledFromPlayStore(): Boolean {
        return try {
            val installer = context.packageManager.getInstallerPackageName(context.packageName)
            val isFromPlayStore = installer == "com.android.vending"
            Log.d(TAG, "Instalador detectado: $installer, Es Play Store: $isFromPlayStore")
            isFromPlayStore
        } catch (e: Exception) {
            Log.e(TAG, "Error verificando instalador", e)
            false
        }
    }

    /**
     * Realiza todas las verificaciones de seguridad
     */
    suspend fun performSecurityChecks(): Boolean {
        _integrityState.value = IntegrityState.Checking
        
        // 1. Verificar instalador
        if (!isInstalledFromPlayStore()) {
            _integrityState.value = IntegrityState.Invalid("App no instalada desde Google Play Store")
            return false
        }

        // 2. Verificar integridad con Play Integrity API
        val isIntegrityValid = verifyIntegrity()
        if (!isIntegrityValid) {
            _integrityState.value = IntegrityState.Invalid("Fallo en verificación de integridad")
            return false
        }

        return true
    }
} 
