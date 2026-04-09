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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.choreboo_habittrackerfriend.R

@Composable
fun StreakBadge(
    streak: Int,
    modifier: Modifier = Modifier,
) {
    if (streak <= 0) return

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(50.dp))
            .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.12f))
            .padding(horizontal = 8.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
         Icon(
             imageVector = Icons.Default.LocalFireDepartment,
             contentDescription = stringResource(R.string.habit_card_streak_cd),
             tint = MaterialTheme.colorScheme.secondary,
             modifier = Modifier.size(13.dp),
         )
        Spacer(modifier = Modifier.width(3.dp))
        Text(
            text = "$streak",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.secondary,
            fontSize = 11.sp,
        )
    }
}
