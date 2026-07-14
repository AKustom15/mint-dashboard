package com.akustom15.mint.library.ui.composables

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.akustom15.mint.library.ui.theme.LocalLiquidGlassColors
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeChild

/**
 * Frosted glass card powered by the Haze library (real-time GPU blur).
 *
 * Consumes the shared [LocalHazeState] provided by [GradientBackground] and
 * renders a real blur of the animated gradient behind it via
 * `Modifier.hazeChild`. A subtle glass gradient + hairline border are layered
 * on top for the liquid-glass look.
 *
 * If no [LocalHazeState] is available (e.g. inside a Dialog window), it falls
 * back to a semi-transparent tinted surface so content stays visible.
 *
 * @param modifier Compose modifier for sizing/positioning
 * @param cornerRadius Corner radius in px for the rounded glass shape
 * @param blurRadius Blur strength (dp)
 * @param blurPasses Unused (kept for API compatibility)
 * @param addOuterShadow Unused (kept for API compatibility)
 * @param content Compose content to render inside the glass card
 */
@Composable
fun RealBlurCard(
    modifier: Modifier = Modifier,
    cornerRadius: Float = 50f,
    blurRadius: Int = 80,
    @Suppress("UNUSED_PARAMETER") blurPasses: Int = 3,
    @Suppress("UNUSED_PARAMETER") addOuterShadow: Boolean = true,
    content: @Composable () -> Unit
) {
    val density = LocalDensity.current
    val cornerDp = with(density) { cornerRadius.toDp() }
    val shape = RoundedCornerShape(cornerDp)
    // Follow the app's own theme (LIGHT/DARK/SYSTEM) instead of the OS setting,
    // so glass surfaces match the UI when the user overrides the theme in-app.
    val isDark = LocalLiquidGlassColors.current.isDark
    val hazeState = LocalHazeState.current

    // Opaque base color of the content behind the glass (required by Haze)
    val backgroundColor = MaterialTheme.colorScheme.background

    // Glass tint applied over the blur. Kept translucent (especially in light
    // mode) so the blurred background shows through as glass rather than a flat
    // solid fill.
    val tintColor = if (isDark) Color(0x591A1A2E) else Color(0x40FFFFFF)
    val blurDp = with(density) { (blurRadius / 4).coerceIn(12, 30).dp }

    // Subtle top-to-bottom gloss for the liquid-glass sheen
    val glossOverlay = if (isDark) {
        Brush.verticalGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.10f),
                Color.Transparent,
                Color.White.copy(alpha = 0.03f)
            )
        )
    } else {
        Brush.verticalGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.22f),
                Color.Transparent,
                Color.White.copy(alpha = 0.06f)
            )
        )
    }
    // In light mode a white rim vanishes against the pale background, so use a
    // hairline dark edge for definition; in dark mode a soft white rim reads best.
    val borderColor = if (isDark) Color.White.copy(alpha = 0.14f) else Color.Black.copy(alpha = 0.08f)

    val glassModifier = if (hazeState != null) {
        Modifier
            .clip(shape)
            .hazeChild(
                state = hazeState,
                style = HazeStyle(
                    backgroundColor = backgroundColor,
                    tints = listOf(HazeTint(tintColor)),
                    blurRadius = blurDp,
                    noiseFactor = 0f
                )
            )
            .background(glossOverlay)
            .border(width = 0.5.dp, color = borderColor, shape = shape)
    } else {
        // Fallback (no blur source available, e.g. Dialog window)
        Modifier
            .clip(shape)
            .background(if (isDark) Color(0xCC1A1A2E) else Color(0xCCFFFFFF))
            .background(glossOverlay)
            .border(width = 0.5.dp, color = borderColor, shape = shape)
    }

    Box(modifier = modifier.then(glassModifier)) {
        content()
    }
}
