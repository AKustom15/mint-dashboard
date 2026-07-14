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
    val isDark: Boolean
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
            isDark = true
        )
    } else {
        LiquidGlassColors(
            background = colorConfig.backgroundLight,
            glassSurface = Color(0xFFFFFFFF).copy(alpha = 0.6f),
            textPrimary = colorConfig.textOnLight,
            textSecondary = colorConfig.textOnLightMuted,
            isDark = false
        )
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = Color.Transparent.toArgb()
            window.navigationBarColor = Color.Transparent.toArgb()
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
