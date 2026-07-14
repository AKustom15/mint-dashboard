package com.akustom15.mint.library.ui.composables

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.akustom15.mint.library.ui.theme.LocalLiquidGlassColors
import com.akustom15.mint.library.ui.theme.MintColors

/**
 * Dialog card with a real frosted-glass look, matching the icon-detail card in
 * the icons preview screen.
 *
 * Dialog windows are separate from the Activity window, so Haze cannot capture
 * the content behind them. Instead the effect relies on two things working
 * together:
 *  1. The host screen blurs its own content via [Modifier.blur] while the dialog
 *     is open.
 *  2. This card uses a highly translucent surface (~20% alpha) so that blurred
 *     content shows through, producing the frosted-glass blur.
 *
 * A subtle gloss gradient + hairline border complete the liquid-glass sheen.
 */
@Composable
fun FrostedGlassDialogCard(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val isDark = LocalLiquidGlassColors.current.isDark
    val shape = RoundedCornerShape(28.dp)

    // Highly translucent in BOTH themes so the blurred background (blurred by the
    // host screen via Modifier.blur while the dialog is open) shows through as
    // real frosted glass. Light mode is only slightly more opaque than dark so
    // dark text keeps enough backing; dialog text should use high-contrast colors
    // (textPrimary) rather than muted grays to stay legible over the blur.
    val bgColor = if (isDark) Color(0x331A1A2E) else Color(0x4DFFFFFF)

    val surfaceColor = if (isDark) MintColors.ButtonSurfaceDark else MintColors.ButtonSurfaceLight
    val borderColor = if (isDark) MintColors.GlassBorderDark else MintColors.GlassBorderLight
    val highlight = if (isDark) MintColors.ButtonHighlightDark else MintColors.ButtonHighlightLight
    val shadowColor = if (isDark) MintColors.ButtonShadowDark else MintColors.ButtonShadowLight

    val gradientOverlay = Brush.verticalGradient(
        colors = listOf(
            highlight.copy(alpha = 0.15f),
            Color.Transparent,
            surfaceColor.copy(alpha = 0.1f)
        )
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp)
            .shadow(
                elevation = 32.dp,
                shape = shape,
                ambientColor = shadowColor,
                spotColor = shadowColor
            )
            .clip(shape)
            .background(bgColor)
            .background(gradientOverlay)
            .border(width = 1.dp, color = borderColor.copy(alpha = 0.5f), shape = shape),
        content = content
    )
}
