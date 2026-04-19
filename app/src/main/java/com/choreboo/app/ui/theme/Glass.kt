package com.choreboo.app.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

fun Modifier.softGlassSurface(
    shape: Shape,
    containerColor: Color,
    borderColor: Color,
): Modifier = this
    .shadow(
        elevation = 16.dp,
        shape = shape,
        ambientColor = containerColor.copy(alpha = 0.22f),
        spotColor = borderColor.copy(alpha = 0.18f),
    )
    .clip(shape)
    .background(
        brush = Brush.verticalGradient(
            colors = listOf(
                containerColor.copy(alpha = 0.92f),
                containerColor.copy(alpha = 0.72f),
            ),
        ),
        shape = shape,
    )
    .border(1.dp, borderColor, shape)

@Composable
fun ChorebooGlassBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    val colorScheme = MaterialTheme.colorScheme
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        colorScheme.background,
                        colorScheme.surfaceContainerLow,
                        colorScheme.background,
                    ),
                ),
            ),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            colorScheme.primary.copy(alpha = 0.14f),
                            Color.Transparent,
                        ),
                        center = Offset(120f, 120f),
                        radius = 520f,
                    ),
                ),
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            colorScheme.tertiary.copy(alpha = 0.12f),
                            Color.Transparent,
                        ),
                        center = Offset(900f, 260f),
                        radius = 700f,
                    ),
                ),
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            colorScheme.secondary.copy(alpha = 0.08f),
                            Color.Transparent,
                        ),
                        center = Offset(520f, 1540f),
                        radius = 760f,
                    ),
                ),
        )
        content()
    }
}

@Composable
fun glassPanelColor(): Color = MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.84f)

@Composable
fun glassPanelBorderColor(): Color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.32f)

@Composable
fun glassBadgeColor(): Color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.72f)

@Composable
fun glassBadgeBorderColor(): Color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.24f)

val GlassCircleShape = CircleShape
