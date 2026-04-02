package com.example.choreboo_habittrackerfriend.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage

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
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primaryContainer)
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
            profilePhotoUri != null -> {
                // Custom photo: load from internal storage
                AsyncImage(
                    model = profilePhotoUri,
                    contentDescription = "User profile photo",
                    modifier = Modifier
                        .size(size)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop,
                )
            }

            googlePhotoUrl != null -> {
                // Google photo: load from Firebase URL
                AsyncImage(
                    model = googlePhotoUrl,
                    contentDescription = "User profile photo",
                    modifier = Modifier
                        .size(size)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop,
                )
            }

            else -> {
                // No photo: default icon
                Icon(
                    Icons.Default.Person,
                    contentDescription = "User profile",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(size / 2),
                )
            }
        }
    }
}
