package com.akustom15.mint.library.config

import androidx.annotation.DrawableRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.akustom15.mint.library.billing.MintPremiumProduct
import com.akustom15.mint.library.config.MoreApp

/**
 * Color configuration for the Mint library.
 * Consumer apps can override any of these to customize the look and feel.
 * Default values match the Mint design system (menta/teal accent on dark).
 */
data class MintColorConfig(
    // Accent colors
    val primary: Color = Color(0xFF4ECDC4),
    val primaryVariant: Color = Color(0xFF2AB7AB),
    val primaryLight: Color = Color(0xFF81E3DC),
    val secondary: Color = Color(0xFF7C83FF),
    val secondaryVariant: Color = Color(0xFF556BF5),
    val tertiary: Color = Color(0xFFFF6B6B),
    val onPrimary: Color = Color.White,

    // Backgrounds
    val backgroundDark: Color = Color(0xFF0A0E1A),
    val backgroundLight: Color = Color(0xFFF4F7FB),

    // Surfaces
    val surfaceDark: Color = Color(0xFF161B2B),
    val surfaceLight: Color = Color(0xFFFFFFFF),

    // Text
    val textOnDark: Color = Color(0xFFF0F4F8),
    val textOnDarkMuted: Color = Color(0xFF9BA4B5),
    val textOnLight: Color = Color(0xFF1E293B),
    val textOnLightMuted: Color = Color(0xFF64748B),

    // Gradient blobs
    val gradientStartDark: Color = Color(0xFF1A1F35),
    val gradientEndDark: Color = Color(0xFF0D1223)
)

/**
 * Configuration class for the Mint Icon Pack Library.
 * Consumer apps provide this to configure the dashboard behavior and appearance.
 */
data class MintConfig(
    // App Identity
    val appName: String,
    val appSubtitle: String = "",
    @DrawableRes val appIcon: Int? = null,
    val packageName: String,
    val versionName: String = "1.0.0",
    val versionCode: Int = 1,

    // Colors (configurable per app)
    val colorConfig: MintColorConfig = MintColorConfig(),

    // Security & Anti-Piracy
    val enableAntiPiracy: Boolean = false,
    // Require a valid, signature-verified Play purchase to run (PAID apps only).
    // Leave false for FREE apps so users aren't blocked for not having bought
    // anything — piracy signals (Lucky Patcher, pirate stores) still block.
    val requireValidLicense: Boolean = false,
    val base64LicenseKey: String = "",
    val gcpProjectNumber: Long = 0L,

    // Feature Toggles
    val showWidgets: Boolean = true,
    val showWallpapers: Boolean = true,
    val showIconRequest: Boolean = true,
    val showIconsPreview: Boolean = true,
    val showSettings: Boolean = true,
    val showAbout: Boolean = true,

    // App Update Info / Changelog
    val updateInfo: String = "",

    // URLs
    val appConfigUrl: String = "",
    val cloudWallpapersUrl: String = "",
    val updateJsonUrl: String = "",
    val privacyPolicyUrl: String = "",
    val moreAppsUrl: String = "",

    // Developer Info
    val developerName: String = "AKustom15",
    val developerLogoUrl: String = "",
    val developerWebsite: String = "",

    // Social Links (URL strings)
    val xUrl: String = "",
    val instagramUrl: String = "",
    val youtubeUrl: String = "",
    val facebookUrl: String = "",
    val telegramUrl: String = "",

    // Social Icons (resource IDs from consumer app)
    @DrawableRes val xIcon: Int = android.R.drawable.ic_menu_send,
    @DrawableRes val instagramIcon: Int = android.R.drawable.ic_menu_camera,
    @DrawableRes val youtubeIcon: Int = android.R.drawable.ic_media_play,
    @DrawableRes val facebookIcon: Int = android.R.drawable.ic_menu_share,
    @DrawableRes val telegramIcon: Int = android.R.drawable.ic_menu_send,

    // Icon Request
    val iconRequestEmail: String = "",
    val iconRequestSubject: String = "Icon Request",
    val freeRequestLimit: Int = 10,
    val premiumEnabled: Boolean = true,
    val premiumProductId: String = "",

    // Premium Products (configurable per icon pack)
    val premiumProducts: List<MintPremiumProduct> = defaultPremiumProducts(),

    // Firestore
    val firestoreCollection: String = "icon_requests",
    val firestoreUpdateDocument: String = "",

    // Wallpapers
    val wallpaperCategories: List<String> = emptyList(),

    // "More Apps" carousel shown at the bottom of Settings (empty = hidden).
    // If moreAppsJsonUrl is set, it takes priority over the static moreApps list.
    val moreApps: List<MoreApp> = emptyList(),
    val moreAppsJsonUrl: String = "",

    // Additional composable content slot (e.g., PUM billing widget)
    val additionalDashboardContent: (@Composable () -> Unit)? = null,

    // Available languages for settings
    val availableLanguages: List<LanguageOption> = defaultLanguages()
)

data class LanguageOption(
    val code: String,
    val displayName: String
)

fun defaultPremiumProducts(): List<MintPremiumProduct> = listOf(
    MintPremiumProduct("premium_icon_request_5", 5, "$2.00"),
    MintPremiumProduct("premium_icon_request_10", 10, "$3.50"),
    MintPremiumProduct("premium_icon_request_20", 20, "$6.00"),
    MintPremiumProduct("premium_icon_request_50", 50, "$10.00")
)

fun defaultLanguages(): List<LanguageOption> = listOf(
    LanguageOption("system", "System Default"),
    LanguageOption("en", "English"),
    LanguageOption("es", "Español"),
    LanguageOption("pt", "Português"),
    LanguageOption("pt-rBR", "Português (Brasil)"),
    LanguageOption("fr", "Français"),
    LanguageOption("de", "Deutsch"),
    LanguageOption("it", "Italiano"),
    LanguageOption("ar", "العربية"),
    LanguageOption("hi", "हिन्दी"),
    LanguageOption("in", "Bahasa Indonesia"),
    LanguageOption("zh", "中文")
)
