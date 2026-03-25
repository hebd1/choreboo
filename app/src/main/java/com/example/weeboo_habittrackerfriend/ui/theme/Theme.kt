package com.example.weeboo_habittrackerfriend.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = WeebooGreenDark,
    secondary = WeebooTealDark,
    tertiary = WeebooOrangeDark,
    background = WeebooDarkBackground,
    surface = WeebooDarkSurface,
    onPrimary = WeebooDarkOnPrimary,
    onSurface = WeebooDarkOnSurface,
    surfaceVariant = WeebooDarkSurfaceVariant,
    onSurfaceVariant = WeebooDarkOnSurfaceVariant,
    error = WeebooDarkError,
    onError = WeebooDarkOnError,
)

private val LightColorScheme = lightColorScheme(
    primary = WeebooGreen,
    secondary = WeebooTeal,
    tertiary = WeebooOrange,
    background = WeebooBackground,
    surface = WeebooSurface,
    onPrimary = WeebooOnPrimary,
    onSurface = WeebooOnSurface,
    surfaceVariant = WeebooSurfaceVariant,
    onSurfaceVariant = WeebooOnSurfaceVariant,
    error = WeebooError,
    onError = WeebooOnError,
)

@Composable
fun WeebooHabitTrackerFriendTheme(
    themeMode: String = "system",
    dynamicColor: Boolean = false, // Disabled so our Weeboo palette always shows
    content: @Composable () -> Unit
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
        typography = Typography,
        content = content
    )
}