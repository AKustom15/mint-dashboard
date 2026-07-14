package com.akustom15.mint.library.ui.composables

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

/**
 * Card with real frosted glass blur effect using [LiquidGlassView].
 * Captures the content behind the card and renders a glass blur overlay.
 */
@Composable
fun LiquidGlassCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(16.dp),
    cornerRadiusPx: Float = 50f,
    blurRadius: Int = 80,
    onClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit
) {
    val cardModifier = if (onClick != null) {
        modifier.clickable(onClick = onClick)
    } else {
        modifier
    }

    RealBlurCard(
        modifier = cardModifier,
        cornerRadius = cornerRadiusPx,
        blurRadius = blurRadius,
        blurPasses = 3,
        addOuterShadow = true
    ) {
        Box(contentAlignment = Alignment.Center) {
            content()
        }
    }
}
