package com.akustom15.mint.library.ui.composables

import androidx.compose.runtime.staticCompositionLocalOf
import dev.chrisbanes.haze.HazeState

/**
 * CompositionLocal that provides the shared [HazeState] used for the frosted
 * glass blur effect (Haze library).
 *
 * [GradientBackground] creates the state, registers itself as the blur source
 * via `Modifier.haze(state)`, and provides it here. Glass surfaces
 * ([RealBlurCard], bottom navigation, etc.) consume it via
 * `Modifier.hazeChild(state, ...)` to render a real GPU blur of the background
 * behind them.
 */
val LocalHazeState = staticCompositionLocalOf<HazeState?> { null }
