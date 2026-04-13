package com.example.choreboo_habittrackerfriend.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

// ── Stitch Design System – Shape tokens ──────────────────────────────────────
private val ChorebooShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),     // input fields
    large = RoundedCornerShape(16.dp),      // standard cards
    extraLarge = RoundedCornerShape(24.dp), // large layout sections
)

// ── Stitch Design System – Full M3 token color schemes ───────────────────────
private val DarkColorScheme = darkColorScheme(
    primary = StitchDarkPrimary,
    onPrimary = StitchDarkOnPrimary,
    primaryContainer = StitchDarkPrimaryContainer,
    onPrimaryContainer = StitchDarkOnPrimaryContainer,
    secondary = StitchDarkSecondary,
    onSecondary = StitchDarkOnSecondary,
    secondaryContainer = StitchDarkSecondaryContainer,
    onSecondaryContainer = StitchDarkOnSecondaryContainer,
    tertiary = StitchDarkTertiary,
    onTertiary = StitchDarkOnTertiary,
    tertiaryContainer = StitchDarkTertiaryContainer,
    onTertiaryContainer = StitchDarkOnTertiaryContainer,
    background = StitchDarkBackground,
    onBackground = StitchDarkOnBackground,
    surface = StitchDarkSurface,
    onSurface = StitchDarkOnSurface,
    surfaceBright = StitchDarkSurfaceBright,
    surfaceVariant = StitchDarkSurfaceVariant,
    onSurfaceVariant = StitchDarkOnSurfaceVariant,
    surfaceContainerLowest = StitchDarkSurfaceContainerLowest,
    surfaceContainerLow = StitchDarkSurfaceContainerLow,
    surfaceContainer = StitchDarkSurfaceContainer,
    surfaceContainerHigh = StitchDarkSurfaceContainerHigh,
    surfaceContainerHighest = StitchDarkSurfaceContainerHighest,
    inverseSurface = StitchDarkInverseSurface,
    inverseOnSurface = StitchDarkInverseOnSurface,
    inversePrimary = StitchDarkInversePrimary,
    outline = StitchDarkOutline,
    outlineVariant = StitchDarkOutlineVariant,
    error = StitchDarkError,
    onError = StitchDarkOnError,
    errorContainer = StitchDarkErrorContainer,
    onErrorContainer = StitchDarkOnErrorContainer,
    scrim = StitchOnBackground,
)

private val LightColorScheme = lightColorScheme(
    primary = StitchPrimary,
    onPrimary = StitchOnPrimary,
    primaryContainer = StitchPrimaryContainer,
    onPrimaryContainer = StitchOnPrimaryContainer,
    secondary = StitchSecondary,
    onSecondary = StitchOnSecondary,
    secondaryContainer = StitchSecondaryContainer,
    onSecondaryContainer = StitchOnSecondaryContainer,
    tertiary = StitchTertiary,
    onTertiary = StitchOnTertiary,
    tertiaryContainer = StitchTertiaryContainer,
    onTertiaryContainer = StitchOnTertiaryContainer,
    background = StitchBackground,
    onBackground = StitchOnBackground,
    surface = StitchSurface,
    onSurface = StitchOnSurface,
    surfaceBright = StitchSurfaceBright,
    surfaceVariant = StitchSurfaceVariant,
    onSurfaceVariant = StitchOnSurfaceVariant,
    surfaceContainerLowest = StitchSurfaceContainerLowest,
    surfaceContainerLow = StitchSurfaceContainerLow,
    surfaceContainer = StitchSurfaceContainer,
    surfaceContainerHigh = StitchSurfaceContainerHigh,
    surfaceContainerHighest = StitchSurfaceContainerHighest,
    inverseSurface = StitchInverseSurface,
    inverseOnSurface = StitchInverseOnSurface,
    inversePrimary = StitchInversePrimary,
    outline = StitchOutline,
    outlineVariant = StitchOutlineVariant,
    error = StitchError,
    onError = StitchOnError,
    errorContainer = StitchErrorContainer,
    onErrorContainer = StitchOnErrorContainer,
    scrim = StitchOnBackground,
)

@Composable
fun ChorebooHabitTrackerFriendTheme(
    themeMode: String = "system",
    dynamicColor: Boolean = false, // disabled — Stitch palette always applied
    content: @Composable () -> Unit,
) {
    val darkTheme = when (themeMode) {
        "dark" -> true
        "light" -> false
        else -> isSystemInDarkTheme()
    }

    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        shapes = ChorebooShapes,
        typography = ChorebooTypography,
        content = content,
    )
}
