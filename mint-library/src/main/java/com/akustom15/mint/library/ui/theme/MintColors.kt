package com.akustom15.mint.library.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import com.akustom15.mint.library.config.MintColorConfig

/**
 * CompositionLocal that holds the current [MintColorConfig].
 * Provided by [MintTheme]; consumer apps override via MintConfig.colorConfig.
 */
val LocalMintColorConfig = staticCompositionLocalOf { MintColorConfig() }

/**
 * Central color accessor for the Mint library.
 * All colors derive from [MintColorConfig] defaults but can be overridden
 * at runtime through [LocalMintColorConfig].
 *
 * Usage in Composables: `MintColors.Primary` reads from the current config.
 * Usage in non-Composable code: reads from the static defaults.
 */
object MintColors {

    // ── Helper: current config (composable context) ──
    private val config: MintColorConfig
        @Composable get() = LocalMintColorConfig.current

    // ── Static defaults (for non-composable contexts like color scheme init) ──
    private val defaults = MintColorConfig()

    // ── Fondos principales ──
    val BackgroundDark: Color @Composable get() = config.backgroundDark
    val BackgroundLight: Color @Composable get() = config.backgroundLight

    // ── Superficies Glass ──
    val GlassDark: Color @Composable get() = config.surfaceDark.copy(alpha = 0.5f)
    val GlassLight: Color get() = Color(0xFFFFFFFF).copy(alpha = 0.6f)

    // ── Superficies sólidas (fallbacks) ──
    val SurfaceDark: Color @Composable get() = config.surfaceDark
    val SurfaceLight: Color @Composable get() = config.surfaceLight

    // ── Colores de acento principal ──
    val Primary: Color @Composable get() = config.primary
    val PrimaryVariant: Color @Composable get() = config.primaryVariant
    val PrimaryLight: Color @Composable get() = config.primaryLight

    // ── Colores de acento secundario ──
    val Secondary: Color @Composable get() = config.secondary
    val SecondaryVariant: Color @Composable get() = config.secondaryVariant
    val Tertiary: Color @Composable get() = config.tertiary

    // ── Textos ──
    val TextOnDark: Color @Composable get() = config.textOnDark
    val TextOnDarkMuted: Color @Composable get() = config.textOnDarkMuted
    val TextOnLight: Color @Composable get() = config.textOnLight
    val TextOnLightMuted: Color @Composable get() = config.textOnLightMuted

    // ── Gradientes dinámicos para el fondo Liquid Glass ──
    val GradientStartDark: Color @Composable get() = config.gradientStartDark
    val GradientEndDark: Color @Composable get() = config.gradientEndDark
    val GradientBlob1Dark: Color @Composable get() = config.primary.copy(alpha = 0.15f)
    val GradientBlob2Dark: Color @Composable get() = config.secondary.copy(alpha = 0.15f)

    val GradientStartLight: Color get() = Color(0xFFE2E8F0)
    val GradientEndLight: Color get() = Color(0xFFF8FAFC)
    // Stronger color blobs in light mode so glass surfaces have real color to
    // blur (otherwise the near-white background makes them read as solid white).
    val GradientBlob1Light: Color @Composable get() = config.primary.copy(alpha = 0.32f)
    val GradientBlob2Light: Color @Composable get() = config.secondary.copy(alpha = 0.32f)

    // ── Bordes Glass (cards, botones, bottom nav) ──
    val GlassBorderDark = Color.White.copy(alpha = 0.1f)
    val GlassBorderLight = Color.White.copy(alpha = 0.5f)
    val NavBorderDark = Color.White.copy(alpha = 0.08f)
    val NavBorderLight = Color.Black.copy(alpha = 0.06f)
    val FabBorderColor = Color.White.copy(alpha = 0.12f)

    // ── Highlights y sombras para botón frosted glass ──
    val ButtonHighlightDark = Color.White.copy(alpha = 0.18f)
    val ButtonHighlightLight = Color.White.copy(alpha = 0.55f)
    val ButtonShadowDark = Color.Black.copy(alpha = 0.5f)
    val ButtonShadowLight = Color.Black.copy(alpha = 0.2f)
    val ButtonBorderDark = Color.White.copy(alpha = 0.15f)
    val ButtonBorderLight = Color.Black.copy(alpha = 0.08f)
    val ButtonSurfaceDark: Color @Composable get() = config.surfaceDark.copy(alpha = 0.55f)
    val ButtonSurfaceLight: Color get() = Color(0xFFFFFFFF).copy(alpha = 0.72f)

    // ── Colores de contenido sobre Primary ──
    val OnPrimary: Color @Composable get() = config.onPrimary

    // ── Static accessors (for non-composable contexts: color scheme init) ──
    object Defaults {
        val Primary = defaults.primary
        val PrimaryVariant = defaults.primaryVariant
        val PrimaryLight = defaults.primaryLight
        val Secondary = defaults.secondary
        val BackgroundDark = defaults.backgroundDark
        val BackgroundLight = defaults.backgroundLight
        val SurfaceDark = defaults.surfaceDark
        val SurfaceLight = defaults.surfaceLight
        val TextOnDark = defaults.textOnDark
        val TextOnDarkMuted = defaults.textOnDarkMuted
        val TextOnLight = defaults.textOnLight
        val TextOnLightMuted = defaults.textOnLightMuted
        val GlassDark = defaults.surfaceDark.copy(alpha = 0.5f)
        val GlassLight = Color(0xFFFFFFFF).copy(alpha = 0.6f)
        val OnPrimary = defaults.onPrimary
    }
}
