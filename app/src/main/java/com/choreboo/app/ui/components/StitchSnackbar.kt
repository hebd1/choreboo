package com.choreboo.app.ui.components

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarData
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.choreboo.app.R

private data class SnackbarStyle(
    val header: String,
    val emoji: String,
    val background: Color,
    val contentColor: Color,
)

@Composable
fun StitchSnackbar(data: SnackbarData) {
    val type = (data.visuals as? StitchSnackbarVisuals)?.type
        ?: when (data.visuals.actionLabel) {
            "achievement" -> SnackbarType.Achievement
            "success"     -> SnackbarType.Success
            "error"       -> SnackbarType.Error
            else          -> SnackbarType.Info
        }
    val style = when (type) {
        is SnackbarType.Achievement -> SnackbarStyle(
            header = stringResource(R.string.snackbar_achievement),
            emoji = "\uD83C\uDF89",
            background = MaterialTheme.colorScheme.secondary,
            contentColor = MaterialTheme.colorScheme.onSecondary,
        )
        is SnackbarType.Success -> SnackbarStyle(
            header = stringResource(R.string.snackbar_success),
            emoji = "\u2705",
            background = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
        )
        is SnackbarType.Error -> SnackbarStyle(
            header = stringResource(R.string.snackbar_error),
            emoji = "\u26A0\uFE0F",
            background = MaterialTheme.colorScheme.error,
            contentColor = MaterialTheme.colorScheme.onError,
        )
        is SnackbarType.Info -> SnackbarStyle(
            header = stringResource(R.string.snackbar_info),
            emoji = "\u2139\uFE0F",
            background = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }

    Row(
        modifier = Modifier
            .padding(start = 16.dp, end = 16.dp, bottom = 8.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(50.dp))
            .background(style.background)
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(style.contentColor.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center,
        ) {
            Text(style.emoji, fontSize = 18.sp)
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = style.header,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = style.contentColor.copy(alpha = 0.6f),
                letterSpacing = 0.8.sp,
            )
            Text(
                text = data.visuals.message,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold,
                color = style.contentColor,
            )
        }
    }
}
