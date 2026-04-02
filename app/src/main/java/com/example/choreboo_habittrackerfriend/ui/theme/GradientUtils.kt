package com.example.choreboo_habittrackerfriend.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush

/**
 * Reusable gradient brushes for the Stitch Design System.
 * Use these instead of constructing gradients ad-hoc to maintain consistency.
 */
object GradientUtils {
    /**
     * Primary gradient for main CTAs: primary → primaryContainer (top-left to bottom-right).
     * Creates a jewel-like depth for button fills and banner backdrops.
     */
    @Composable
    fun primaryGradient(): Brush {
        return Brush.linearGradient(
            colors = listOf(
                MaterialTheme.colorScheme.primary,
                MaterialTheme.colorScheme.primaryContainer,
            ),
        )
    }

    /**
     * Secondary gradient for FABs and achievement indicators: secondary → secondaryContainer.
     * Used for orange/warm accent elements.
     */
    @Composable
    fun secondaryGradient(): Brush {
        return Brush.linearGradient(
            colors = listOf(
                MaterialTheme.colorScheme.secondary,
                MaterialTheme.colorScheme.secondaryContainer,
            ),
        )
    }

    /**
     * Tertiary gradient for XP/energy bars and long-term mastery: tertiary → tertiaryContainer.
     * Used for purple/deep accent elements.
     */
    @Composable
    fun tertiaryGradient(): Brush {
        return Brush.linearGradient(
            colors = listOf(
                MaterialTheme.colorScheme.tertiary,
                MaterialTheme.colorScheme.tertiaryContainer,
            ),
        )
    }

    /**
     * Energy stat bar gradient (specific override for stat bars).
     * Can be customized per stat type (hunger=secondary, happiness=primary, energy=tertiary).
     */
    @Composable
    fun energyGradient(): Brush {
        return tertiaryGradient()
    }
}
