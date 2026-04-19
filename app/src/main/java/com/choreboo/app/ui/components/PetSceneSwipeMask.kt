package com.choreboo.app.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import kotlin.math.abs

private const val SHIMMER_DURATION_MS = 260
private const val SHIMMER_BAND_WIDTH_RATIO = 0.7f

@Composable
fun PetSceneSwipeMask(
    transitionKey: Any,
    modifier: Modifier = Modifier,
) {
    val progress = remember { Animatable(1f) }
    var hasSeenFirstKey by remember { mutableStateOf(false) }
    val surface = MaterialTheme.colorScheme.surface
    val surfaceContainer = MaterialTheme.colorScheme.surfaceContainerHighest
    val onSurface = MaterialTheme.colorScheme.onSurface

    LaunchedEffect(transitionKey) {
        if (!hasSeenFirstKey) {
            hasSeenFirstKey = true
            progress.snapTo(1f)
            return@LaunchedEffect
        }

        progress.snapTo(0f)
        progress.animateTo(
            targetValue = 1f,
            animationSpec = tween(
                durationMillis = SHIMMER_DURATION_MS,
                easing = FastOutSlowInEasing,
            ),
        )
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        val sweepProgress = progress.value
        val shimmerStrength = 1f - abs((sweepProgress - 0.5f) / 0.5f)
        val scrimAlpha = 0.22f * shimmerStrength
        val bandWidth = size.width * SHIMMER_BAND_WIDTH_RATIO
        val travelDistance = size.width + bandWidth * 2
        val bandCenterX = -bandWidth + (travelDistance * sweepProgress)

        drawRect(
            color = surfaceContainer.copy(alpha = scrimAlpha),
        )

        drawRect(
            brush = Brush.linearGradient(
                colors = listOf(
                    Color.Transparent,
                    surface.copy(alpha = 0.18f * shimmerStrength),
                    onSurface.copy(alpha = 0.32f * shimmerStrength),
                    surfaceContainer.copy(alpha = 0.68f * shimmerStrength),
                    onSurface.copy(alpha = 0.32f * shimmerStrength),
                    surface.copy(alpha = 0.18f * shimmerStrength),
                    Color.Transparent,
                ),
                start = Offset(bandCenterX - bandWidth, 0f),
                end = Offset(bandCenterX + bandWidth, size.height),
            ),
        )
    }
}
