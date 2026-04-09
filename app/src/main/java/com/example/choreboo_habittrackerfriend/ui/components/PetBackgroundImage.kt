package com.example.choreboo_habittrackerfriend.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
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
import com.example.choreboo_habittrackerfriend.domain.model.BACKGROUND_DEFAULT_ID
import com.example.choreboo_habittrackerfriend.domain.model.ChorebooMood
import com.example.choreboo_habittrackerfriend.domain.model.backgroundById
import com.example.choreboo_habittrackerfriend.ui.theme.PetMoodContentStart
import com.example.choreboo_habittrackerfriend.ui.theme.PetMoodHappyStart
import com.example.choreboo_habittrackerfriend.ui.theme.PetMoodHungryStart
import com.example.choreboo_habittrackerfriend.ui.theme.PetMoodSadStart
import com.example.choreboo_habittrackerfriend.ui.theme.PetMoodTiredStart

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

/** Convenience helper — resolves the animated mood color to a [Color]. */
fun moodColor(mood: ChorebooMood): Color = when (mood) {
    ChorebooMood.HAPPY -> PetMoodHappyStart
    ChorebooMood.CONTENT -> PetMoodContentStart
    ChorebooMood.HUNGRY -> PetMoodHungryStart
    ChorebooMood.TIRED -> PetMoodTiredStart
    ChorebooMood.SAD -> PetMoodSadStart
    ChorebooMood.IDLE -> Color(0xFFF0F4F8) // StitchSurfaceContainerLow equivalent
}
