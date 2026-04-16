package com.choreboo.app.ui.pet.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.choreboo.app.R

@Composable
fun StatBar(
    label: String,
    value: Int,
    maxValue: Int = 100,
    emoji: String = "",
    statType: String = "",
    modifier: Modifier = Modifier,
) {
    val fraction = (value.toFloat() / maxValue).coerceIn(0f, 1f)
    val contentDescriptionText = stringResource(R.string.stat_bar_cd, label, value, maxValue)

    // Stat-type color: design spec says bar color based on stat type, not value
    val barColor = when (statType.lowercase()) {
        "hunger" -> MaterialTheme.colorScheme.primary
        "happiness" -> MaterialTheme.colorScheme.primaryContainer
        "energy" -> MaterialTheme.colorScheme.secondaryContainer
        else -> MaterialTheme.colorScheme.primary
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .clearAndSetSemantics { contentDescription = contentDescriptionText },
    ) {
        if (emoji.isNotEmpty()) {
            Text(text = emoji, style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.width(8.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = label.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = "$value/$maxValue",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            LinearProgressIndicator(
                progress = { fraction },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = barColor,
                // No surfaceVariant — use surfaceContainerHighest per design spec
                trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
            )
        }
    }
}
