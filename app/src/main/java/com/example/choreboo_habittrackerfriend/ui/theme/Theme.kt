package com.example.choreboo_habittrackerfriend.ui.theme

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
    primary = ChorebooGreenDark,
    secondary = ChorebooTealDark,
    tertiary = ChorebooOrangeDark,
    background = ChorebooDarkBackground,
    surface = ChorebooDarkSurface,
    onPrimary = ChorebooDarkOnPrimary,
    onSurface = ChorebooDarkOnSurface,
    surfaceVariant = ChorebooDarkSurfaceVariant,
    onSurfaceVariant = ChorebooDarkOnSurfaceVariant,
    error = ChorebooDarkError,
    onError = ChorebooDarkOnError,
)

private val LightColorScheme = lightColorScheme(
    primary = ChorebooGreen,
    secondary = ChorebooTeal,
    tertiary = ChorebooOrange,
    background = ChorebooBackground,
    surface = ChorebooSurface,
    onPrimary = ChorebooOnPrimary,
    onSurface = ChorebooOnSurface,
    surfaceVariant = ChorebooSurfaceVariant,
    onSurfaceVariant = ChorebooOnSurfaceVariant,
    error = ChorebooError,
    onError = ChorebooOnError,
)

@Composable
fun ChorebooHabitTrackerFriendTheme(
    themeMode: String = "system",
    dynamicColor: Boolean = false, // Disabled so our Choreboo palette always shows
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