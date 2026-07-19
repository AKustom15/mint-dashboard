package com.akustom15.mint.library.billing

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * Centralized premium preferences management for icon request system.
 * Reusable across all icon packs using the Mint library.
 *
 * Entitlement state is stored in [EncryptedSharedPreferences] (AES-256) so it
 * can't be trivially edited from a rooted device, ADB backup, or file managers.
 * Values are still local — the source of truth remains Google Play (a valid,
 * signature-verified purchase is required to add credits; see MintBillingManager).
 */
object MintPremiumPreferences {

    private const val TAG = "MintPremiumPrefs"
    private const val PREF_NAME = "mint_premium_preferences"        // legacy plaintext
    private const val SECURE_PREF_NAME = "mint_premium_secure"      // encrypted
    private const val KEY_PREMIUM_REQUEST = "premium_request"
    private const val KEY_PREMIUM_REQUEST_PRODUCT_ID = "premium_request_product_id"
    private const val KEY_PREMIUM_REQUEST_COUNT = "premium_request_count"
    private const val KEY_PREMIUM_REQUEST_TOTAL = "premium_request_total"
    private const val KEY_LAST_PROCESSED_ORDER_ID = "last_processed_order_id"

    @Volatile
    private var cachedPrefs: SharedPreferences? = null

    private fun getSharedPreferences(context: Context): SharedPreferences {
        cachedPrefs?.let { return it }
        return synchronized(this) {
            cachedPrefs ?: buildSecurePrefs(context.applicationContext).also { cachedPrefs = it }
        }
    }

    private fun buildSecurePrefs(appContext: Context): SharedPreferences {
        return try {
            val masterKey = MasterKey.Builder(appContext)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()
            val secure = EncryptedSharedPreferences.create(
                appContext,
                SECURE_PREF_NAME,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
            migrateFromPlaintext(appContext, secure)
            secure
        } catch (e: Exception) {
            // Never crash the app if the keystore is unavailable — fall back to
            // private prefs (still MODE_PRIVATE) so functionality keeps working.
            Log.e(TAG, "EncryptedSharedPreferences unavailable, falling back", e)
            appContext.getSharedPreferences(SECURE_PREF_NAME, Context.MODE_PRIVATE)
        }
    }

    /** One-time migration of any legacy plaintext values into the encrypted store. */
    private fun migrateFromPlaintext(appContext: Context, secure: SharedPreferences) {
        val legacy = appContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        if (legacy.all.isEmpty()) return
        if (!secure.contains(KEY_PREMIUM_REQUEST_COUNT)) {
            secure.edit().apply {
                putBoolean(KEY_PREMIUM_REQUEST, legacy.getBoolean(KEY_PREMIUM_REQUEST, false))
                putString(KEY_PREMIUM_REQUEST_PRODUCT_ID, legacy.getString(KEY_PREMIUM_REQUEST_PRODUCT_ID, ""))
                putInt(KEY_PREMIUM_REQUEST_COUNT, legacy.getInt(KEY_PREMIUM_REQUEST_COUNT, 0))
                putInt(KEY_PREMIUM_REQUEST_TOTAL, legacy.getInt(KEY_PREMIUM_REQUEST_TOTAL, 0))
                putString(KEY_LAST_PROCESSED_ORDER_ID, legacy.getString(KEY_LAST_PROCESSED_ORDER_ID, null))
                apply()
            }
        }
        // Wipe legacy plaintext so it can't be tampered with anymore.
        legacy.edit().clear().apply()
    }

    fun isPremiumRequest(context: Context): Boolean {
        return getSharedPreferences(context).getBoolean(KEY_PREMIUM_REQUEST, false)
    }

    fun getPremiumRequestCount(context: Context): Int {
        return getSharedPreferences(context).getInt(KEY_PREMIUM_REQUEST_COUNT, 0)
    }

    fun getPremiumRequestTotal(context: Context): Int {
        return getSharedPreferences(context).getInt(KEY_PREMIUM_REQUEST_TOTAL, 0)
    }

    fun getPremiumRequestProductId(context: Context): String {
        return getSharedPreferences(context).getString(KEY_PREMIUM_REQUEST_PRODUCT_ID, "") ?: ""
    }

    fun getLastProcessedOrderId(context: Context): String? {
        return getSharedPreferences(context).getString(KEY_LAST_PROCESSED_ORDER_ID, null)
    }

    /**
     * Consume premium requests after sending email.
     */
    fun consumePremiumRequests(context: Context, count: Int): Boolean {
        return try {
            val prefs = getSharedPreferences(context)
            val currentCount = prefs.getInt(KEY_PREMIUM_REQUEST_COUNT, 0)

            if (currentCount >= count) {
                prefs.edit().putInt(KEY_PREMIUM_REQUEST_COUNT, currentCount - count).apply()
                Log.d(TAG, "Consumed $count requests. Remaining: ${currentCount - count}")

                if ((currentCount - count) <= 0) {
                    prefs.edit().putBoolean(KEY_PREMIUM_REQUEST, false).apply()
                    Log.d(TAG, "Premium deactivated (no remaining requests)")
                }
                true
            } else {
                Log.w(TAG, "Insufficient requests: available=$currentCount, required=$count")
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error consuming requests: ${e.message}")
            false
        }
    }

    /**
     * Process a premium purchase (called from MintBillingManager).
     */
    fun processPremiumPurchase(context: Context, productId: String, orderId: String, requestCount: Int) {
        try {
            val prefs = getSharedPreferences(context)
            val lastOrderId = prefs.getString(KEY_LAST_PROCESSED_ORDER_ID, null)
            if (lastOrderId == orderId) {
                Log.w(TAG, "Purchase with orderId $orderId already processed. Skipping.")
                return
            }

            val currentAvailable = prefs.getInt(KEY_PREMIUM_REQUEST_COUNT, 0)
            val currentTotal = prefs.getInt(KEY_PREMIUM_REQUEST_TOTAL, 0)

            prefs.edit().apply {
                putBoolean(KEY_PREMIUM_REQUEST, true)
                putString(KEY_PREMIUM_REQUEST_PRODUCT_ID, productId)
                putInt(KEY_PREMIUM_REQUEST_COUNT, currentAvailable + requestCount)
                putInt(KEY_PREMIUM_REQUEST_TOTAL, currentTotal + requestCount)
                putString(KEY_LAST_PROCESSED_ORDER_ID, orderId)
                apply()
            }

            Log.d(TAG, "Purchase processed: $requestCount requests added. Available: ${currentAvailable + requestCount}")
        } catch (e: Exception) {
            Log.e(TAG, "Error processing premium purchase", e)
        }
    }

    /**
     * Reset premium state completely (for settings/debug).
     */
    fun resetPremiumState(context: Context) {
        try {
            getSharedPreferences(context).edit().apply {
                putBoolean(KEY_PREMIUM_REQUEST, false)
                putString(KEY_PREMIUM_REQUEST_PRODUCT_ID, "")
                putInt(KEY_PREMIUM_REQUEST_COUNT, 0)
                putInt(KEY_PREMIUM_REQUEST_TOTAL, 0)
                putString(KEY_LAST_PROCESSED_ORDER_ID, null)
                apply()
            }
            Log.d(TAG, "Premium state reset completely")
        } catch (e: Exception) {
            Log.e(TAG, "Error resetting premium state", e)
        }
    }
}
