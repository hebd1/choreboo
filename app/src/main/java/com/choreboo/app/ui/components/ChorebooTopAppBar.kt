package com.choreboo.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Shared top app bar used by Pet, Stats, Calendar, and Settings screens.
 *
 * Structure:
 * - **Title slot**: `ProfileAvatar` (40dp) + 12dp spacer + [titleContent] composable.
 * - **Actions slot**: `StarPointsChip` with 16dp end padding.
 *
 * The [titleContent] slot lets each screen supply its own title text (and any extras
 * like `PremiumBadge` or `ShimmerPlaceholder`).
 *
 * @param profilePhotoUri       Local URI string for the user's profile photo (may be null).
 * @param googlePhotoUrl        Remote Google profile photo URL fallback (may be null).
 * @param totalPoints           Current star-point balance shown in the chip.
 * @param pointsContentDescription Accessibility label for the star-points chip icon.
 * @param titleContent          Composable slot for screen-specific title content.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChorebooTopAppBar(
    profilePhotoUri: String?,
    googlePhotoUrl: String?,
    totalPoints: Int,
    pointsContentDescription: String,
    titleContent: @Composable () -> Unit,
) {
    TopAppBar(
        title = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(end = 12.dp, start = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                ProfileAvatar(
                    profilePhotoUri = profilePhotoUri,
                    googlePhotoUrl = googlePhotoUrl,
                    size = 40.dp,
                )
                titleContent()
            }
        },
        actions = {
            StarPointsChip(
                points = totalPoints,
                contentDescription = pointsContentDescription,
                modifier = Modifier.padding(end = 16.dp),
            )
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
    )
}
