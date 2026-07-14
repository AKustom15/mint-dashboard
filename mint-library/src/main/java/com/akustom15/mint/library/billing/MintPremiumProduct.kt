package com.akustom15.mint.library.billing

/**
 * Data class representing a premium product for icon requests.
 * Each icon pack configures its own products via MintConfig.
 */
data class MintPremiumProduct(
    val productId: String,
    val requestCount: Int,
    val displayPrice: String = ""
)
