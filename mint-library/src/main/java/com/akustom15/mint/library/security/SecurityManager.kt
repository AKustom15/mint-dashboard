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
 * Señales de seguridad del ARRANQUE. Fail-open a propósito.
 *
 * Solo comprobaciones locales y baratas: instalador de Play y Lucky Patcher.
 * Ninguna llamada de red, ningún bloqueo por no poder contactar al servidor —
 * un comprador con mala cobertura no debe quedarse fuera de la app.
 *
 * La verificación FUERTE (nonce de servidor + las siete validaciones de Play
 * Integrity + `appLicensingVerdict`) vive en [MintIconRequestGate] y corre al
 * solicitar iconos, con fail-CLOSED. Esa es la distinción clave:
 *
 *   · ¿La app abre?          → fail-open   (aquí)
 *   · ¿Se entrega servicio?  → fail-closed (MintIconRequestGate)
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
    private val licenseGate = MintLicenseGate(context, config.nonceUrl, config.licenseVerificationUrl)

    private val _securityState = MutableStateFlow<SecurityState>(SecurityState.Checking)
    val securityState: StateFlow<SecurityState> = _securityState

    /**
     * Veredicto del servidor. Solo UNLICENSED bloquea: ver [MintLicenseGate].
     * null = todavía sin comprobar en este arranque.
     */
    private val _licenseVerdict = MutableStateFlow<MintLicenseGate.Verdict?>(null)
    val licenseVerdict: StateFlow<MintLicenseGate.Verdict?> = _licenseVerdict

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
                _licenseVerdict
            ) { integrityState, licenseState, verdict ->
                when {
                    integrityState is PlayIntegrityChecker.IntegrityState.Checking ||
                        licenseState is LicenseState.Checking ->
                        SecurityState.Checking

                    // Integridad inválida (no instalada desde Play, etc.)
                    integrityState is PlayIntegrityChecker.IntegrityState.Invalid ->
                        SecurityState.Invalid(integrityState.reason)

                    // Piratería detectada (Lucky Patcher)
                    licenseState is LicenseState.Invalid ->
                        SecurityState.Invalid(licenseState.reason)

                    // El SERVIDOR confirmó que esta cuenta NO compró la app.
                    // Es el único veredicto remoto que bloquea: UNKNOWN nunca
                    // bloquea, para no acusar a un comprador sin pruebas.
                    verdict == MintLicenseGate.Verdict.UNLICENSED ->
                        SecurityState.Invalid("Esta cuenta no compró la aplicación")

                    integrityState is PlayIntegrityChecker.IntegrityState.Error ->
                        SecurityState.Error(integrityState.message)
                    licenseState is LicenseState.Error ->
                        SecurityState.Error(licenseState.message)

                    integrityState is PlayIntegrityChecker.IntegrityState.Valid &&
                        licenseState is LicenseState.Valid -> SecurityState.Valid

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
                // 1 · Señales locales, sin red, instantáneas.
                playIntegrityChecker.performSecurityChecks()
                licenseChecker.performSecurityChecks()

                // 2 · Titularidad confirmada por el servidor. Cacheada 7 días,
                //     así que casi ningún arranque llega a hacer la llamada.
                //     Solo bloquea con un UNLICENSED explícito.
                if (config.requireValidLicense) {
                    _licenseVerdict.value = licenseGate.check()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error en verificaciones de seguridad", e)
                _securityState.value = SecurityState.Error("Error: ${e.message}")
            }
        }
    }

    fun isAppSecure(): Boolean = _securityState.value is SecurityState.Valid

    /** Fuerza una comprobación nueva, ignorando la caché (p. ej. tras comprar). */
    fun refreshSecurityChecks() {
        licenseGate.invalidate()
        _licenseVerdict.value = null
        performSecurityChecks()
    }
}
