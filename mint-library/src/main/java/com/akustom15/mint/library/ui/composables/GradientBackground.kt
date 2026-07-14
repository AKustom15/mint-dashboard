package com.akustom15.mint.library.ui.composables

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.akustom15.mint.library.ui.theme.LocalLiquidGlassColors
import com.akustom15.mint.library.ui.theme.MintColors
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.haze

@Composable
fun GradientBackground(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val liquidColors = LocalLiquidGlassColors.current

    val startColor = if (liquidColors.isDark) MintColors.GradientStartDark else MintColors.GradientStartLight
    val endColor = if (liquidColors.isDark) MintColors.GradientEndDark else MintColors.GradientEndLight
    val blob1 = if (liquidColors.isDark) MintColors.GradientBlob1Dark else MintColors.GradientBlob1Light
    val blob2 = if (liquidColors.isDark) MintColors.GradientBlob2Dark else MintColors.GradientBlob2Light

    val infiniteTransition = rememberInfiniteTransition(label = "background")
    val offset1 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(20000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "offset1"
    )
    val offset2 by infiniteTransition.animateFloat(
        initialValue = 1000f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(25000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "offset2"
    )

    // Per-screen Haze state for the glass CARDS: the gradient layer below is the
    // blur source and the cards above consume it via Modifier.hazeChild (as
    // siblings). This is deliberately independent from the bottom nav's own Haze
    // state (see MintScreen) — sharing one state across the nav/graph boundary
    // would make a hazeChild a descendant of its haze source, which Haze forbids.
    val hazeState = remember { HazeState() }

    Box(modifier = modifier.fillMaxSize()) {
        // ── Blur source layer: animated gradient + blobs ──
        // Must stay a SIBLING of the content: Haze forbids a hazeChild (the glass
        // cards) from being a descendant of the haze source node.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .haze(state = hazeState)
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(startColor, endColor),
                        start = Offset(0f, 0f),
                        end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
                    )
                )
        ) {
            // Blob 1
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(blob1, Color.Transparent),
                            center = Offset(offset1, offset2),
                            radius = 1200f
                        )
                    )
            )
            // Blob 2
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(blob2, Color.Transparent),
                            center = Offset(offset2, offset1),
                            radius = 1500f
                        )
                    )
            )
        }

        // ── Content layer on top (glass surfaces blur the source above) ──
        CompositionLocalProvider(LocalHazeState provides hazeState) {
            content()
        }
    }
}
