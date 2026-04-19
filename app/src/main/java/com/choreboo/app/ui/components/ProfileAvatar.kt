package com.choreboo.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.choreboo.app.R
import com.choreboo.app.ui.theme.softGlassSurface

@Composable
fun ProfileAvatar(
    profilePhotoUri: String?,
    googlePhotoUrl: String?,
    size: Dp = 40.dp,
    onClick: (() -> Unit)? = null,
) {
    Box(
        modifier = Modifier
            .size(size)
            .softGlassSurface(
                shape = CircleShape,
                containerColor = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.72f),
                borderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.24f),
            )
            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.18f))
            .then(
                if (onClick != null) {
                    Modifier.clickable { onClick() }
                } else {
                    Modifier
                }
            ),
        contentAlignment = Alignment.Center,
    ) {
        // Priority: custom photo > Google photo > default icon
        when {
            !profilePhotoUri.isNullOrBlank() -> ProfileImage(url = profilePhotoUri, size = size)
            !googlePhotoUrl.isNullOrBlank() -> ProfileImage(url = googlePhotoUrl, size = size)
            else -> DefaultAvatarIcon(size = size)
        }
    }
}

/**
 * Loads a profile image from [url]. Falls back to [DefaultAvatarIcon] when
 * the load fails (e.g. expired URL, no network). Resets the failed state
 * whenever the URL changes so a new image attempt is always made.
 */
@Composable
private fun ProfileImage(url: String, size: Dp) {
    var loadFailed by remember(url) { mutableStateOf(false) }
    if (loadFailed) {
        DefaultAvatarIcon(size = size)
    } else {
         AsyncImage(
             model = url,
             contentDescription = stringResource(R.string.profile_photo_cd),
             modifier = Modifier
                  .size(size)
                  .border(1.dp, MaterialTheme.colorScheme.surface.copy(alpha = 0.65f), CircleShape)
                  .clip(CircleShape),
              contentScale = ContentScale.Crop,
              onError = { loadFailed = true },
         )
    }
}

/** AccountCircle icon tinted with the theme primary color, sized to fill the avatar container. */
@Composable
private fun DefaultAvatarIcon(size: Dp) {
     Icon(
         Icons.Default.AccountCircle,
         contentDescription = stringResource(R.string.profile_default_cd),
         tint = MaterialTheme.colorScheme.primary,
         modifier = Modifier.size(size),
     )
}
