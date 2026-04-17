package com.choreboo.app.ui.household.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.choreboo.app.R
import com.choreboo.app.domain.model.HouseholdHabitStatus
import androidx.compose.ui.res.stringResource

/**
 * Card showing a single household habit and its today-completion status.
 * When completed, a green "Done" chip shows who completed it.
 * When not yet done, an amber "Pending" chip is shown.
 *
 * @param habit The household habit data
 * @param modifier Optional modifier
 */
@Composable
fun HouseholdHabitCard(
    habit: HouseholdHabitStatus,
    modifier: Modifier = Modifier,
) {
    val isCompleted = habit.completedByName != null

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isCompleted)
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
            else
                MaterialTheme.colorScheme.surfaceContainerLowest,
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = habit.title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = if (habit.assignedToName != null) {
                        stringResource(R.string.household_habit_assigned_to, habit.assignedToName)
                    } else {
                        stringResource(R.string.household_habit_unassigned)
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // XP badge
            Box(
                modifier = Modifier
                     .clip(RoundedCornerShape(50.dp))
                     .background(MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f))
                     .padding(horizontal = 6.dp, vertical = 2.dp),
             ) {
                 Text(
                     text = stringResource(R.string.habit_xp_badge, habit.baseXp),
                     style = MaterialTheme.typography.labelSmall,
                     fontWeight = FontWeight.Bold,
                     color = MaterialTheme.colorScheme.onTertiaryContainer,
                     fontSize = 10.sp,
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Status chip
            if (isCompleted) {
                StatusChip(
                    label = stringResource(R.string.household_done_by_short, habit.completedByName),
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                )
            } else {
                StatusChip(
                    label = stringResource(R.string.household_pending),
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                    contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                )
            }
        }
    }
}

@Composable
private fun StatusChip(
    label: String,
    containerColor: androidx.compose.ui.graphics.Color,
    contentColor: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(containerColor)
            .padding(horizontal = 6.dp, vertical = 2.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = contentColor,
            fontWeight = FontWeight.Bold,
            fontSize = 10.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
