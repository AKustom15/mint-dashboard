package com.akustom15.mint.library.security

import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import com.android.billingclient.api.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.delay
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class LicenseChecker(private val context: Context, private val base64PublicKey: String) {
    companion object {
        private const val TAG = "LicenseChecker"
        private const val LUCKY_PATCHER_PACKAGE = "com.android.vending.billing.InAppBillingService.COIN"
        
        private val PIRATE_STORES = listOf(
            "com.aptoide.app",
            "com.blackmart.market",
            "com.ac.market",
            "com.blackmart.alpha"
        )
    }

    private val _licenseState = MutableStateFlow<LicenseState>(LicenseState.Checking)
    val licenseState: StateFlow<LicenseState> = _licenseState

    private val billingClient by lazy {
        BillingClient.newBuilder(context)
            .setListener(purchasesUpdatedListener)
            .enablePendingPurchases()
            .build()
    }

    private val purchasesUpdatedListener = PurchasesUpdatedListener { billingResult, purchases ->
        when (billingResult.responseCode) {
            BillingClient.BillingResponseCode.OK -> {
                Log.d(TAG, "PurchasesUpdatedListener: OK. Purchases: ${purchases?.size ?: "null"}")
                purchases?.forEachIndexed { index, purchase ->
                    Log.d(TAG, "PurchasesUpdatedListener: Purchase #$index - Products: ${purchase.products.joinToString()}, State: ${purchase.purchaseState}, Ack: ${purchase.isAcknowledged}, Signature: ${purchase.signature.isNotBlank()}, OriginalJson: ${purchase.originalJson.isNotBlank()}")
                    val isSignatureValid = verifyValidSignature(purchase.originalJson, purchase.signature)
                    Log.d(TAG, "PurchasesUpdatedListener: Purchase #$index - Signature valid: $isSignatureValid")
                }

                val validPurchaseFound = if (!purchases.isNullOrEmpty()) {
                    purchases.any {
                        val isPurchased = it.purchaseState == Purchase.PurchaseState.PURCHASED
                        val isAcknowledged = it.isAcknowledged
                        val isSignatureValid = verifyValidSignature(it.originalJson, it.signature)
                        val isValid = isPurchased && isAcknowledged && isSignatureValid
                        Log.d(TAG, "PurchasesUpdatedListener: Checking purchase ${it.products.joinToString()} - Purchased: $isPurchased, Ack: $isAcknowledged, SigValid: $isSignatureValid, OverallValid: $isValid")
                        isValid
                    }
                } else {
                    false
                }

                if (validPurchaseFound) {
                    Log.i(TAG, "PurchasesUpdatedListener: Valid purchase found and acknowledged.")
                    _licenseState.value = LicenseState.Valid
                } else {
                    val currentState = _licenseState.value
                    Log.w(TAG, "PurchasesUpdatedListener: No valid, acknowledged purchases found in update. Current state: $currentState")
                    // Solo cambiar a Invalid si no estamos ya en Valid Y
                    // si el estado actual no es un Invalid por piratería (para no borrar esa detección).
                    if (currentState !is LicenseState.Valid && !(currentState is LicenseState.Invalid && currentState.isPiracyRelated)) {
                         _licenseState.value = LicenseState.Invalid(
                            reason = if (purchases.isNullOrEmpty()) "No se encontraron compras en la actualización" else "Compra en actualización no válida o no reconocida",
                            isPiracyRelated = false // Un 'Invalid' desde el listener no es por piratería directa
                        )
                    }
                }
            }
            else -> {
                Log.e(TAG, "PurchasesUpdatedListener: Error ${billingResult.responseCode} - ${billingResult.debugMessage}")
                _licenseState.value = LicenseState.Error("Error en actualización de compras: ${billingResult.responseCode} (${billingResult.debugMessage})")
            }
        }
    }

    /**
     * Verifica si Lucky Patcher está instalado
     */
    private fun checkLuckyPatcher(): Boolean {
        return try {
            context.packageManager.getPackageInfo(LUCKY_PATCHER_PACKAGE, 0) // Usar 0 si solo se necesita saber si está instalado
            Log.w(TAG, "Lucky Patcher detectado")
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }

    /**
     * Verifica si hay tiendas de terceros instaladas
     */
    private fun checkPirateStores(): Boolean {
        val packageManager = context.packageManager
        return Companion.PIRATE_STORES.any { packageName ->
            try {
                packageManager.getPackageInfo(packageName, 0) // Usar 0 si solo se necesita saber si está instalado
                Log.w(TAG, "Tienda no oficial detectada: $packageName")
                true
            } catch (e: PackageManager.NameNotFoundException) {
                false
            }
        }
    }

    /**
     * Verifica la licencia usando Google Play Billing Library
     */
    suspend fun checkLicense(): Boolean {
        // Definimos un tiempo total máximo para los intentos de verificación inicial.
        // Durante este tiempo, el estado general de la licencia será 'Checking'.
        val VERIFICATION_TIMEOUT_MS = 20000L // 20 segundos
        val ATTEMPT_INTERVAL_MS = 2000L     // Intervalo entre el inicio de los intentos (si el anterior falla)
        var elapsedTime = 0L
        var attemptNumber = 1

        while (elapsedTime < VERIFICATION_TIMEOUT_MS) {
            Log.d(TAG, "checkLicense: Attempt $attemptNumber. Elapsed time: $elapsedTime ms / $VERIFICATION_TIMEOUT_MS ms")
            val success = performSingleLicenseCheckAttempt()
            if (success) {
                Log.i(TAG, "checkLicense: License validated successfully on attempt $attemptNumber.")
                return true // Licencia válida encontrada
            } else {
                Log.e(TAG, "checkLicense: Attempt $attemptNumber failed. Detalles: " +
                    "Internet: ${isInternetAvailable(context)}, " +
                    "PlayStore: ${isPlayStoreInstalled(context)}")
            }

            // Si falló y aún no hemos alcanzado el timeout total
            elapsedTime += ATTEMPT_INTERVAL_MS // Sumamos el intervalo para el próximo intento
            if (elapsedTime < VERIFICATION_TIMEOUT_MS) {
                Log.w(TAG, "checkLicense: Attempt $attemptNumber failed. Waiting ${ATTEMPT_INTERVAL_MS}ms before next attempt.")
                delay(ATTEMPT_INTERVAL_MS)
            } else {
                Log.e(TAG, "checkLicense: Failed to validate license within the timeout of $VERIFICATION_TIMEOUT_MS ms.")
                break // Salir del bucle si se alcanzó el timeout
            }
            attemptNumber++
        }
        return false // Falló todos los intentos o se alcanzó el timeout
    }

    private suspend fun performSingleLicenseCheckAttempt(): Boolean = suspendCancellableCoroutine { continuation ->
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    // Verificar compras existentes
                    val params = QueryPurchasesParams.newBuilder()
                        .setProductType(BillingClient.ProductType.INAPP)
                        .build()
                    
                    billingClient.queryPurchasesAsync(params) { result, purchases ->
                        val isValid = when {
                            result.responseCode != BillingClient.BillingResponseCode.OK -> {
                                Log.e(TAG, "checkLicense: Error querying purchases: ${result.responseCode} - ${result.debugMessage}")
                                false
                            }
                            purchases.isEmpty() -> {
                                Log.w(TAG, "checkLicense: No active purchases found.")
                                false
                            }
                            else -> {
                                Log.d(TAG, "checkLicense: Found ${purchases.size} purchases. Verifying...")
                                purchases.forEachIndexed { index, p ->
                                    Log.d(TAG, "checkLicense: Purchase #$index - Products: ${p.products.joinToString()}, State: ${p.purchaseState}, Ack: ${p.isAcknowledged}, Signature: ${p.signature.isNotBlank()}, OriginalJson: ${p.originalJson.isNotBlank()}")
                                    val isSignatureValid = verifyValidSignature(p.originalJson, p.signature)
                                    Log.d(TAG, "checkLicense: Purchase #$index - Signature valid: $isSignatureValid")
                                }
                                val validPurchase = purchases.any { purchase ->
                                    val isPurchased = purchase.purchaseState == Purchase.PurchaseState.PURCHASED
                                    val isAcknowledged = purchase.isAcknowledged
                                    val isSignatureValid = verifyValidSignature(purchase.originalJson, purchase.signature)
                                    val isValid = isPurchased && isAcknowledged && isSignatureValid
                                    Log.d(TAG, "checkLicense: Checking purchase ${purchase.products.joinToString()} - Purchased: $isPurchased, Ack: $isAcknowledged, SigValid: $isSignatureValid, OverallValid: $isValid")
                                    if (isValid) Log.i(TAG, "checkLicense: Valid acknowledged purchase found: ${purchase.products.joinToString()}")
                                    isValid
                                }

                                Log.d(TAG, "checkLicense: Overall purchase validation result: $validPurchase")
                                validPurchase
                            }
                        }
                        if (continuation.isActive) {
                            continuation.resume(isValid)
                        } else {
                            Log.w(TAG, "checkLicense: Continuation no longer active when resuming for queryPurchasesAsync.")
                        }
                    }
                } else {
                    Log.e(TAG, "checkLicense: Billing setup failed: ${billingResult.responseCode} - ${billingResult.debugMessage}")
                    if (continuation.isActive) {
                        continuation.resume(false)
                    } else {
                        Log.w(TAG, "checkLicense: Continuation no longer active when resuming for billing setup failure.")
                    }
                }
            }

            override fun onBillingServiceDisconnected() {
                Log.w(TAG, "checkLicense: Billing service disconnected.")
                // Es importante reanudar la corrutina aquí para evitar que quede suspendida indefinidamente.
                if (continuation.isActive) {
                    continuation.resume(false) // No se puede verificar la licencia si el servicio está desconectado
                } else {
                    Log.w(TAG, "checkLicense: Continuation no longer active on billing service disconnected.")
                }
            }
        })
    }

    private fun verifyValidSignature(signedData: String, signature: String): Boolean {
        return Security.verifyPurchase(base64PublicKey, signedData, signature)
    }

    /**
     * Realiza todas las verificaciones de seguridad
     */
    suspend fun performSecurityChecks() {
        // Asegurar que el estado inicial para esta operación sea Checking
        // Evitar cambiar de Valid a Checking innecesariamente si ya se validó.
        if (_licenseState.value !is LicenseState.Valid) {
            _licenseState.value = LicenseState.Checking
        }
        Log.d(TAG, "performSecurityChecks started. Current state: ${_licenseState.value}")
        
        // Verificar Lucky Patcher
        if (checkLuckyPatcher()) {
            _licenseState.value = LicenseState.Invalid("Detectada aplicación no permitida", isPiracyRelated = true)
            return
        }

        // Verificar tiendas no oficiales
        if (checkPirateStores()) {
            _licenseState.value = LicenseState.Invalid("Detectada tienda no oficial", isPiracyRelated = true)
            return
        }

        try {
            // Verificar licencia con Billing
            Log.d(TAG, "performSecurityChecks: Calling checkLicense()")
            val isBillingLicenseValid = checkLicense()
            Log.d(TAG, "performSecurityChecks: checkLicense() returned $isBillingLicenseValid. Current state: ${_licenseState.value}")

            if (isBillingLicenseValid) {
                _licenseState.value = LicenseState.Valid
            } else {
                // Si checkLicense() devuelve false, pero el listener ya puso Valid, no lo sobrescribas.
                if (_licenseState.value !is LicenseState.Valid) {
                    _licenseState.value = LicenseState.Invalid(
                        reason = "Licencia no válida o no encontrada en Play Store",
                        isPiracyRelated = false // Falla de Billing no es piratería directa
                    )
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error en verificación de licencia", e)
            _licenseState.value = LicenseState.Error("Error durante la verificación de licencia: ${e.message}")
        }
    }

    fun onDestroy() {
        billingClient.endConnection()
    }

    private fun isInternetAvailable(context: Context): Boolean {
        return try {
            val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as android.net.ConnectivityManager
            val networkInfo = connectivityManager.activeNetworkInfo
            networkInfo != null && networkInfo.isConnected
        } catch (e: Exception) {
            false
        }
    }

    private fun isPlayStoreInstalled(context: Context): Boolean {
        return try {
            context.packageManager.getPackageInfo("com.android.vending", 0)
            true
        } catch (e: Exception) {
            false
        }
    }
}
