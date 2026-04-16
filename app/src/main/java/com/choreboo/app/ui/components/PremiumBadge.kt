package com.choreboo.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.choreboo.app.R

/**
 * Small chip indicating the user has an active Choreboo Premium subscription.
 * Renders nothing when [isPremium] is false.
 */
@Composable
fun PremiumBadge(
    isPremium: Boolean,
    modifier: Modifier = Modifier,
) {
    if (!isPremium) return

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(50.dp))
            .background(MaterialTheme.colorScheme.tertiaryContainer)
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
         Icon(
             imageVector = Icons.Default.WorkspacePremium,
             contentDescription = stringResource(R.string.premium_subscriber_cd),
             tint = MaterialTheme.colorScheme.onTertiaryContainer,
             modifier = Modifier.size(14.dp),
         )
         Text(
             text = stringResource(R.string.premium_badge),
             style = MaterialTheme.typography.labelSmall,
             fontWeight = FontWeight.Bold,
             color = MaterialTheme.colorScheme.onTertiaryContainer,
         )
    }
}
