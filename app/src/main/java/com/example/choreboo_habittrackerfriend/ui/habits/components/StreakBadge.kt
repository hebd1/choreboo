package com.example.choreboo_habittrackerfriend.ui.habits.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.choreboo_habittrackerfriend.ui.theme.StreakFlame

@Composable
fun StreakBadge(
    streak: Int,
    modifier: Modifier = Modifier,
) {
    if (streak <= 0) return

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(StreakFlame.copy(alpha = 0.15f))
            .padding(horizontal = 6.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Default.LocalFireDepartment,
            contentDescription = "Streak",
            tint = StreakFlame,
            modifier = Modifier.size(14.dp),
        )
        Spacer(modifier = Modifier.width(2.dp))
        Text(
            text = "$streak",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = StreakFlame,
        )
    }
}

