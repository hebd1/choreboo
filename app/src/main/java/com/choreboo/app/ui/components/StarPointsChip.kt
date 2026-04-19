package com.choreboo.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Stars
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.choreboo.app.ui.theme.GlassCircleShape
import com.choreboo.app.ui.theme.glassBadgeBorderColor
import com.choreboo.app.ui.theme.glassBadgeColor
import com.choreboo.app.ui.theme.softGlassSurface

/**
 * Pill-shaped chip showing the user's star-point balance with a star icon.
 *
 * Used in top app bars (icon 18dp, 0.85 alpha background) and in the
 * [BackgroundPickerSheet] header (icon 16dp, no alpha).
 *
 * @param points              Current star-point balance to display.
 * @param contentDescription  Accessibility label for the star icon.
 * @param modifier            Applied to the outermost [Row].
 * @param iconSize            Size of the [Icons.Default.Stars] icon. Default 18dp.
 * @param backgroundAlpha     Alpha applied to [MaterialTheme.colorScheme.surfaceContainerHigh].
 *                            Pass 1f for a fully opaque background (e.g. in bottom sheets).
 */
@Composable
fun StarPointsChip(
    points: Int,
    contentDescription: String,
    modifier: Modifier = Modifier,
    iconSize: Dp = 18.dp,
    backgroundAlpha: Float = 0.85f,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .softGlassSurface(
                shape = RoundedCornerShape(50.dp),
                containerColor = glassBadgeColor().copy(alpha = backgroundAlpha),
                borderColor = glassBadgeBorderColor(),
            )
            .padding(horizontal = 12.dp, vertical = 6.dp),
    ) {
        Icon(
            imageVector = Icons.Default.Stars,
            contentDescription = contentDescription,
            tint = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.size(iconSize),
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = "$points",
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}
