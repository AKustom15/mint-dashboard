package com.akustom15.mint.library.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.akustom15.mint.library.config.MintColorConfig

// Clases para el sistema Custom Liquid Glass
data class LiquidGlassColors(
    val background: Color,
    val glassSurface: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val isDark: Boolean,
    /**
     * Tinte de las tarjetas de vista previa (iconos y widgets).
     *
     * Viaja por el tema para que las pantallas no tengan que recibir el
     * MintConfig entero: IconsPreviewScreen no lo recibe y no merecía la pena
     * cambiar su firma solo por un color.
     *
     * Solo lo usan las tarjetas que lo piden con usePreviewTint = true.
     */
    val previewCardTint: Color = Color.Unspecified
)

val LocalLiquidGlassColors = staticCompositionLocalOf {
    LiquidGlassColors(
        background = Color.Unspecified,
        glassSurface = Color.Unspecified,
        textPrimary = Color.Unspecified,
        textSecondary = Color.Unspecified,
        isDark = false
    )
}

enum class ThemeMode {
    SYSTEM, LIGHT, DARK
}

@Composable
fun MintTheme(
    colorConfig: MintColorConfig = MintColorConfig(),
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    content: @Composable () -> Unit
) {
    // Read theme from MintPreferences for reactive theme switching
    val context = LocalView.current.context
    val prefs = remember { com.akustom15.mint.library.data.MintPreferences.getInstance(context) }
    val prefsTheme by prefs.themeMode.collectAsState()

    val darkTheme = when (prefsTheme) {
        com.akustom15.mint.library.data.MintThemeMode.SYSTEM -> isSystemInDarkTheme()
        com.akustom15.mint.library.data.MintThemeMode.LIGHT -> false
        com.akustom15.mint.library.data.MintThemeMode.DARK -> true
    }

    // Build Material3 color schemes from the config
    val colorScheme = if (darkTheme) {
        darkColorScheme(
            primary = colorConfig.primary,
            onPrimary = colorConfig.textOnLight,
            primaryContainer = colorConfig.primaryVariant,
            onPrimaryContainer = colorConfig.textOnDark,
            secondary = colorConfig.secondary,
            onSecondary = colorConfig.textOnDark,
            background = colorConfig.backgroundDark,
            onBackground = colorConfig.textOnDark,
            surface = colorConfig.surfaceDark,
            onSurface = colorConfig.textOnDark,
            surfaceVariant = colorConfig.surfaceDark.copy(alpha = 0.5f),
            onSurfaceVariant = colorConfig.textOnDarkMuted
        )
    } else {
        lightColorScheme(
            primary = colorConfig.primary,
            onPrimary = colorConfig.textOnLight,
            primaryContainer = colorConfig.primaryLight,
            onPrimaryContainer = colorConfig.textOnLight,
            secondary = colorConfig.secondary,
            onSecondary = colorConfig.textOnLight,
            background = colorConfig.backgroundLight,
            onBackground = colorConfig.textOnLight,
            surface = colorConfig.surfaceLight,
            onSurface = colorConfig.textOnLight,
            surfaceVariant = Color(0xFFFFFFFF).copy(alpha = 0.6f),
            onSurfaceVariant = colorConfig.textOnLightMuted
        )
    }

    val liquidGlassColors = if (darkTheme) {
        LiquidGlassColors(
            background = colorConfig.backgroundDark,
            glassSurface = colorConfig.surfaceDark.copy(alpha = 0.5f),
            textPrimary = colorConfig.textOnDark,
            textSecondary = colorConfig.textOnDarkMuted,
            isDark = true,
            previewCardTint = colorConfig.previewCardDark
        )
    } else {
        LiquidGlassColors(
            background = colorConfig.backgroundLight,
            glassSurface = Color(0xFFFFFFFF).copy(alpha = 0.6f),
            textPrimary = colorConfig.textOnLight,
            textSecondary = colorConfig.textOnLightMuted,
            isDark = false,
            previewCardTint = colorConfig.previewCardLight
        )
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window

            // Aquí se ponían a mano statusBarColor y navigationBarColor en
            // transparente. Ambas quedaron OBSOLETAS en Android 15 (API 35) y
            // Play lo señala en "Calidad técnica".
            //
            // Además eran redundantes: enableEdgeToEdge(), que la app llama en
            // su Activity, ya deja las barras transparentes y dibuja de borde a
            // borde. Quitarlas no cambia nada visualmente.
            //
            // Lo que SÍ hay que conservar es esto: controla si los iconos de las
            // barras se pintan oscuros o claros. No está obsoleto y sin ello los
            // iconos se volverían ilegibles según el tema.
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !darkTheme
                isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    CompositionLocalProvider(
        LocalLiquidGlassColors provides liquidGlassColors,
        LocalMintColorConfig provides colorConfig
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = MintTypography,
            content = content
        )
    }
}
