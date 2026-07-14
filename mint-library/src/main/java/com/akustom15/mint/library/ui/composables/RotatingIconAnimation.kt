package com.akustom15.mint.library.ui.composables

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import android.view.animation.OvershootInterpolator

private const val GRID_SIZE = 3
private const val ITEMS_PER_GRID = GRID_SIZE * GRID_SIZE

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun RotatingIconAnimation(
    modifier: Modifier = Modifier,
    iconResourceNames: List<String>,
    batchDisplayDurationMillis: Long = 4000,
    iconAppearanceDurationMillis: Long = 800,
    staggerDelayMillis: Long = 60,
    iconSize: Int = 80
) {
    if (iconResourceNames.isEmpty()) {
        return
    }

    val iconBatches = remember(iconResourceNames) {
        iconResourceNames.chunked(ITEMS_PER_GRID)
    }

    if (iconBatches.isEmpty()) {
        return
    }

    var currentBatchIndex by rememberSaveable { mutableStateOf(0) }

    val fixedPauseDurationMillis = 2000L
    val maxStaggerTime = (ITEMS_PER_GRID -1) * staggerDelayMillis
    val actualBatchCycleTime = iconAppearanceDurationMillis + maxStaggerTime + fixedPauseDurationMillis

    LaunchedEffect(key1 = currentBatchIndex, key2 = iconBatches.size) {
        delay(actualBatchCycleTime)
        currentBatchIndex = (currentBatchIndex + 1) % iconBatches.size
    }

    AnimatedContent(
        targetState = currentBatchIndex,
        modifier = modifier.fillMaxSize(),
        transitionSpec = {
            fadeIn(animationSpec = tween(durationMillis = 500)) togetherWith
                    fadeOut(animationSpec = tween(durationMillis = 500))
        },
        label = "BatchTransition"
    ) { batchIndex ->
        val currentBatch = iconBatches[batchIndex]
        GridOfAnimatedIcons(
            icons = currentBatch,
            iconSize = iconSize,
            appearanceDurationMillis = iconAppearanceDurationMillis.toInt(),
            staggerDelayMillis = staggerDelayMillis,
            gridKey = batchIndex
        )
    }
}

@Composable
private fun GridOfAnimatedIcons(
    icons: List<String>,
    iconSize: Int,
    appearanceDurationMillis: Int,
    staggerDelayMillis: Long,
    gridKey: Any
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        for (i in 0 until GRID_SIZE) {
            Row(horizontalArrangement = Arrangement.Center) {
                for (j in 0 until GRID_SIZE) {
                    val indexInBatch = i * GRID_SIZE + j
                    if (indexInBatch < icons.size) {
                        val iconName = icons[indexInBatch]
                        key(iconName, gridKey) {
                            AnimatedGridIcon(
                                iconResourceName = iconName,
                                iconSize = iconSize,
                                appearanceDurationMillis = appearanceDurationMillis,
                                delayMillis = (indexInBatch * staggerDelayMillis)
                            )
                        }
                    } else {
                        Spacer(Modifier.size(iconSize.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun AnimatedGridIcon(
    iconResourceName: String,
    iconSize: Int,
    appearanceDurationMillis: Int,
    delayMillis: Long
) {
    val context = LocalContext.current
    val packageName = context.packageName
    val resourceId = remember(iconResourceName) {
        context.resources.getIdentifier(iconResourceName, "drawable", packageName)
    }

    var startAnimation by remember { mutableStateOf(false) }

    val alpha = animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0f,
        animationSpec = tween(durationMillis = appearanceDurationMillis),
        label = "alpha_$iconResourceName"
    )

    val scale = animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0.3f,
        animationSpec = tween(durationMillis = appearanceDurationMillis, easing = { OvershootInterpolator(1.5f).getInterpolation(it) }),
        label = "scale_$iconResourceName"
    )

    val rotation = animateFloatAsState(
        targetValue = if (startAnimation) 0f else -360f, 
        animationSpec = tween(durationMillis = appearanceDurationMillis, easing = LinearEasing),
        label = "rotation_$iconResourceName"
    )

    LaunchedEffect(Unit) {
        delay(delayMillis)
        startAnimation = true
    }

    Box(
        modifier = Modifier
            .size(iconSize.dp)
            .padding(12.dp),
        contentAlignment = Alignment.Center
    ) {
        if (resourceId != 0) {
            Image(
                painter = painterResource(id = resourceId),
                contentDescription = iconResourceName,
                modifier = Modifier
                    .fillMaxSize()
                    .alpha(alpha.value)
                    .graphicsLayer {
                        scaleX = scale.value
                        scaleY = scale.value
                        rotationZ = rotation.value
                    }
            )
        }
    }
}
