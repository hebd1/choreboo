package com.choreboo.app.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import kotlin.math.sin
import kotlin.random.Random

private data class ConfettiParticle(
    val x: Float,        // 0f..1f normalised horizontal position
    val speed: Float,     // 0.6..1.0 fall-speed multiplier
    val size: Float,      // px
    val color: Color,
    val rotation: Float,  // degrees per progress unit
    val wobble: Float,    // horizontal sine amplitude (px)
    val wobbleFreq: Float,
)

private val CONFETTI_COLORS = listOf(
    Color(0xFFFF6D00), // orange
    Color(0xFFFFD54F), // gold
    Color(0xFF006E1C), // green
    Color(0xFF6833EA), // purple
    Color(0xFFE91E63), // pink
    Color(0xFF00BCD4), // cyan
    Color(0xFFFF5252), // red
)

/**
 * Full-screen confetti particle overlay. Animates for [durationMs] then calls [onFinished].
 * Designed to be layered on top of celebration dialogs.
 */
@Composable
fun ConfettiOverlay(
    modifier: Modifier = Modifier,
    particleCount: Int = 80,
    durationMs: Int = 3000,
    onFinished: () -> Unit = {},
) {
    val progress = remember { Animatable(0f) }
    val particles = remember {
        List(particleCount) {
            ConfettiParticle(
                x = Random.nextFloat(),
                speed = 0.6f + Random.nextFloat() * 0.4f,
                size = 6f + Random.nextFloat() * 10f,
                color = CONFETTI_COLORS[Random.nextInt(CONFETTI_COLORS.size)],
                rotation = Random.nextFloat() * 720f * if (Random.nextBoolean()) 1f else -1f,
                wobble = 20f + Random.nextFloat() * 40f,
                wobbleFreq = 2f + Random.nextFloat() * 3f,
            )
        }
    }

    LaunchedEffect(Unit) {
        progress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMs, easing = LinearEasing),
        )
        onFinished()
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        val p = progress.value

        particles.forEach { particle ->
            // y travels from above the screen (-0.1) to below (1.1)
            val y = (-0.1f + 1.2f * p * particle.speed) * h
            val x = particle.x * w + sin((p * particle.wobbleFreq * Math.PI * 2).toFloat()) * particle.wobble
            val angle = p * particle.rotation

            rotate(degrees = angle, pivot = Offset(x, y)) {
                drawRect(
                    color = particle.color,
                    topLeft = Offset(x - particle.size / 2, y - particle.size / 2),
                    size = Size(particle.size, particle.size * 0.6f),
                    alpha = (1f - p).coerceIn(0.2f, 1f),
                )
            }
        }
    }
}
