package com.akustom15.mint.library.security

import android.content.Context
import com.akustom15.mint.library.config.MintConfig
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * Gestor unificado de seguridad que combina múltiples métodos de verificación
 * Recomendado para 2025: Google Play Integrity API + Firebase App Check + Verificación de Instalador
 */
class SecurityManager private constructor(private val context: Context, private val config: MintConfig) {
    companion object {
        private const val TAG = "SecurityManager"
        
        @Volatile
        private var instance: SecurityManager? = null

        fun getInstance(context: Context, config: MintConfig): SecurityManager {
            return instance ?: synchronized(this) {
                instance ?: SecurityManager(context.applicationContext, config).also { instance = it }
            }
        }
    }

    private val playIntegrityChecker = PlayIntegrityChecker(context)
    private val licenseChecker = LicenseChecker(context, config.base64LicenseKey)
    private val _securityState = MutableStateFlow<SecurityState>(SecurityState.Checking)
    val securityState: StateFlow<SecurityState> = _securityState

    private val scope = CoroutineScope(Dispatchers.Main)

    sealed class SecurityState {
        object Checking : SecurityState()
        object Valid : SecurityState()
        class Invalid(val reason: String) : SecurityState()
        class Error(val message: String) : SecurityState()
    }

    init {
        // Observar cambios en el estado de Play Integrity
        scope.launch {
            playIntegrityChecker.integrityState.collect { integrityState ->
                when (integrityState) {
                    is PlayIntegrityChecker.IntegrityState.Valid -> {
                        _securityState.value = SecurityState.Valid
                    }
                    is PlayIntegrityChecker.IntegrityState.Invalid -> {
                        _securityState.value = SecurityState.Invalid(integrityState.reason)
                    }
                    is PlayIntegrityChecker.IntegrityState.Error -> {
                        _securityState.value = SecurityState.Error(integrityState.message)
                    }
                    is PlayIntegrityChecker.IntegrityState.Checking -> {
                        _securityState.value = SecurityState.Checking
                    }
                }
            }
        }
    }

    /**
     * Realiza todas las verificaciones de seguridad
     */
    fun performSecurityChecks() {
        scope.launch {
            try {
                Log.d(TAG, "Iniciando verificaciones de seguridad...")
                val isValid = playIntegrityChecker.performSecurityChecks()
                
                if (isValid) {
                    Log.i(TAG, "✅ Todas las verificaciones de seguridad pasaron")
                    _securityState.value = SecurityState.Valid
                } else {
                    Log.w(TAG, "❌ Fallo en verificaciones de seguridad")
                    // El estado ya se actualizó en el collector
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error en verificaciones de seguridad", e)
                _securityState.value = SecurityState.Error("Error: ${e.message}")
            }
        }
    }

    /**
     * Verifica si la app es segura para usar
     */
    fun isAppSecure(): Boolean {
        return _securityState.value is SecurityState.Valid
    }

    /**
     * Fuerza una nueva verificación de seguridad
     */
    fun refreshSecurityChecks() {
        Log.d(TAG, "Forzando nueva verificación de seguridad")
        performSecurityChecks()
    }
} 
