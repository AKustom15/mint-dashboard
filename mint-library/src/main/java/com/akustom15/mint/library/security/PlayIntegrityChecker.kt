package com.akustom15.mint.library.security

import android.content.Context
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Sistema de verificación de integridad usando Google Play Integrity API
 * Esta es la solución más moderna y recomendada por Google para 2025
 */
class PlayIntegrityChecker(private val context: Context) {
    companion object {
        private const val TAG = "PlayIntegrityChecker"
    }

    private val _integrityState = MutableStateFlow<IntegrityState>(IntegrityState.Checking)
    val integrityState: StateFlow<IntegrityState> = _integrityState

    sealed class IntegrityState {
        object Checking : IntegrityState()
        object Valid : IntegrityState()
        class Invalid(val reason: String) : IntegrityState()
        class Error(val message: String) : IntegrityState()
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
     * Señal local de arranque. Barata y sin llamadas de red.
     *
     * Antes esta clase pedía un token de Play Integrity y LO DESCARTABA: si
     * llegaba un token, marcaba Valid sin mirarlo. Gastaba una llamada de cuota
     * por arranque (~4.000/día con 2.000 usuarios, de un límite de 10.000) sin
     * verificar absolutamente nada.
     *
     * La verificación real —con las siete comprobaciones y el veredicto de
     * licencia— la hace el servidor al solicitar iconos (MintIconRequestGate).
     */
    suspend fun performSecurityChecks(): Boolean {
        _integrityState.value = IntegrityState.Checking

        if (!isInstalledFromPlayStore()) {
            _integrityState.value = IntegrityState.Invalid("App no instalada desde Google Play Store")
            return false
        }

        _integrityState.value = IntegrityState.Valid
        return true
    }
}

