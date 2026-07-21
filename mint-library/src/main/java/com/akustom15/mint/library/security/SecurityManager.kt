package com.akustom15.mint.library.security

import android.content.Context
import android.util.Log
import com.akustom15.mint.library.config.MintConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

/**
 * Unified security gate.
 *
 * Combines:
 *  - Play Integrity (client): installer + integrity token (soft signal).
 *  - Piracy detection (client): Lucky Patcher / unofficial stores.
 *  - Paid-app ownership (SERVER): [MintLicenseVerifier] verifies the Play
 *    Integrity `appLicensingVerdict` in a Cloud Function. This is the robust
 *    part — a patched client can't fake it. Only enforced when
 *    [MintConfig.requireValidLicense] is true (i.e. the app is paid).
 */
class SecurityManager private constructor(
    private val context: Context,
    private val config: MintConfig
) {
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
    private val licenseChecker = LicenseChecker(context, config.base64LicenseKey, config.requireValidLicense)
    private val licenseVerifier = MintLicenseVerifier(context)

    private val _securityState = MutableStateFlow<SecurityState>(SecurityState.Checking)
    val securityState: StateFlow<SecurityState> = _securityState

    // Server paid-app license verdict (null = not yet checked). Only meaningful
    // when config.requireValidLicense is true.
    private val _serverLicense = MutableStateFlow<MintLicenseVerifier.Result?>(null)

    private val scope = CoroutineScope(Dispatchers.Main)

    sealed class SecurityState {
        object Checking : SecurityState()
        object Valid : SecurityState()
        class Invalid(val reason: String) : SecurityState()
        class Error(val message: String) : SecurityState()
    }

    init {
        scope.launch {
            combine(
                playIntegrityChecker.integrityState,
                licenseChecker.licenseState,
                _serverLicense
            ) { integrityState, licenseState, serverLicense ->
                when {
                    integrityState is PlayIntegrityChecker.IntegrityState.Checking ||
                        licenseState is LicenseState.Checking ->
                        SecurityState.Checking

                    // Integridad inválida (no instalada desde Play, etc.)
                    integrityState is PlayIntegrityChecker.IntegrityState.Invalid ->
                        SecurityState.Invalid(integrityState.reason)

                    // Piratería detectada (Lucky Patcher / tiendas no oficiales)
                    licenseState is LicenseState.Invalid ->
                        SecurityState.Invalid(licenseState.reason)

                    // Titularidad de app de pago, verificada en servidor
                    config.requireValidLicense && serverLicense is MintLicenseVerifier.Result.Unlicensed ->
                        SecurityState.Invalid("Esta cuenta no compró la aplicación")

                    integrityState is PlayIntegrityChecker.IntegrityState.Error ->
                        SecurityState.Error(integrityState.message)
                    licenseState is LicenseState.Error ->
                        SecurityState.Error(licenseState.message)

                    // Integridad OK + sin piratería
                    integrityState is PlayIntegrityChecker.IntegrityState.Valid &&
                        licenseState is LicenseState.Valid -> {
                        // Si la app es de pago, esperar el veredicto del servidor.
                        // (Licensed o Unknown → válido: fail-open para no bloquear
                        //  a compradores legítimos por errores transitorios.)
                        if (config.requireValidLicense && serverLicense == null) SecurityState.Checking
                        else SecurityState.Valid
                    }

                    else -> SecurityState.Checking
                }
            }.collect { combined ->
                _securityState.value = combined
            }
        }
    }

    /** Runs all security checks. No-op (Valid) when anti-piracy is disabled. */
    fun performSecurityChecks() {
        if (!config.enableAntiPiracy) {
            Log.d(TAG, "Anti-piratería deshabilitada. Saltando comprobaciones.")
            _securityState.value = SecurityState.Valid
            return
        }

        scope.launch {
            try {
                Log.d(TAG, "Iniciando verificaciones de seguridad…")
                // Señales de cliente (integridad + piratería)
                playIntegrityChecker.performSecurityChecks()
                licenseChecker.performSecurityChecks()

                // Titularidad de app de pago (servidor)
                if (config.requireValidLicense) {
                    _serverLicense.value = licenseVerifier.verify()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error en verificaciones de seguridad", e)
                _securityState.value = SecurityState.Error("Error: ${e.message}")
            }
        }
    }

    fun isAppSecure(): Boolean = _securityState.value is SecurityState.Valid

    fun refreshSecurityChecks() {
        Log.d(TAG, "Forzando nueva verificación de seguridad")
        _serverLicense.value = null
        performSecurityChecks()
    }
}
