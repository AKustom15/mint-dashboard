package com.akustom15.mint.library.config

/**
 * Represents an app to showcase in the "More Apps" carousel on the Settings screen.
 *
 * @param name App display name
 * @param description Short description
 * @param iconUrl URL to the app icon image
 * @param screenshotUrls List of screenshot URLs (first one used as banner)
 * @param playStoreUrl Google Play Store URL for the app
 */
data class MoreApp(
    val name: String,
    val description: String,
    val iconUrl: String,
    val screenshotUrls: List<String> = emptyList(),
    val playStoreUrl: String
)
