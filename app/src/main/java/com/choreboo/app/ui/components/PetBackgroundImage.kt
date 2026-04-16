package com.choreboo.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import com.choreboo.app.domain.model.BACKGROUND_DEFAULT_ID
import com.choreboo.app.domain.model.ChorebooMood
import com.choreboo.app.domain.model.backgroundById
import com.choreboo.app.ui.theme.PetMoodContentStart
import com.choreboo.app.ui.theme.PetMoodDarkContentStart
import com.choreboo.app.ui.theme.PetMoodDarkHappyStart
import com.choreboo.app.ui.theme.PetMoodDarkHungryStart
import com.choreboo.app.ui.theme.PetMoodDarkIdleStart
import com.choreboo.app.ui.theme.PetMoodDarkSadStart
import com.choreboo.app.ui.theme.PetMoodDarkTiredStart
import com.choreboo.app.ui.theme.PetMoodHappyStart
import com.choreboo.app.ui.theme.PetMoodHungryStart
import com.choreboo.app.ui.theme.PetMoodSadStart
import com.choreboo.app.ui.theme.PetMoodTiredStart
import androidx.compose.foundation.isSystemInDarkTheme

/**
 * Fills its parent with either:
 *  - A mood-based solid color when [backgroundId] is null or [BACKGROUND_DEFAULT_ID], or
 *  - A loaded asset image from `assets/backgrounds/<assetPath>`.
 *
 * Falls back to the mood color if the asset fails to load.
 *
 * Intended to be placed as the first child inside a [Box] so subsequent children
 * (the pet animation, badges, etc.) are drawn on top.
 */
@Composable
fun PetBackgroundImage(
    backgroundId: String?,
    mood: ChorebooMood,
    moodColor: Color,
    modifier: Modifier = Modifier,
) {
    val item = remember(backgroundId) { backgroundById(backgroundId) }
    val assetPath = item?.assetPath

    // Resolve asset URI — Coil can load "file:///android_asset/<path>"
    val context = LocalContext.current
    var imageFailed by remember(assetPath) { mutableStateOf(false) }

    if (assetPath != null && !imageFailed) {
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data("file:///android_asset/$assetPath")
                .build(),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = modifier.fillMaxSize(),
            onError = { imageFailed = true },
        )
    } else {
        // Default mood-gradient fallback (solid color base)
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(moodColor),
        )
    }
}

/**
 * Theme-aware helper — resolves the mood background color respecting light/dark theme.
 * Must be called from a composable context.
 */
@Composable
fun moodColor(mood: ChorebooMood): Color {
    val isDark = isSystemInDarkTheme()
    val idle = MaterialTheme.colorScheme.surfaceContainerLow
    return if (isDark) {
        when (mood) {
            ChorebooMood.HAPPY -> PetMoodDarkHappyStart
            ChorebooMood.CONTENT -> PetMoodDarkContentStart
            ChorebooMood.HUNGRY -> PetMoodDarkHungryStart
            ChorebooMood.TIRED -> PetMoodDarkTiredStart
            ChorebooMood.SAD -> PetMoodDarkSadStart
            ChorebooMood.IDLE -> PetMoodDarkIdleStart
        }
    } else {
        when (mood) {
            ChorebooMood.HAPPY -> PetMoodHappyStart
            ChorebooMood.CONTENT -> PetMoodContentStart
            ChorebooMood.HUNGRY -> PetMoodHungryStart
            ChorebooMood.TIRED -> PetMoodTiredStart
            ChorebooMood.SAD -> PetMoodSadStart
            ChorebooMood.IDLE -> idle
        }
    }
}
