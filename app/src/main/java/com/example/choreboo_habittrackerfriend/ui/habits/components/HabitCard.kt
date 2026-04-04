package com.example.choreboo_habittrackerfriend.ui.habits.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.choreboo_habittrackerfriend.domain.model.Habit

@Composable
fun HabitCard(
    habit: Habit,
    completedToday: Int,
    currentStreak: Int = 0,
    isScheduledToday: Boolean = true,
    onComplete: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    /** True if this habit is owned by the current user (show edit/delete controls). */
    isOwnedByCurrentUser: Boolean = true,
    /**
     * If non-null, a household member already completed this habit today.
     * The card shows "Completed by [name]" instead of the generic "✓ Completed".
     */
    householdCompleterName: String? = null,
) {
    val isComplete = completedToday >= 1
    val emoji = getEmojiForIconName(habit.iconName)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer { alpha = if (isScheduledToday) 1f else 0.5f },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isComplete)
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.18f)
            else
                MaterialTheme.colorScheme.surfaceContainerLowest,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // 56dp icon circle — primary when complete, surfaceContainerHighest when not
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(
                            if (isComplete) MaterialTheme.colorScheme.primary
                            else if (!isScheduledToday) MaterialTheme.colorScheme.surfaceContainerHighest
                            else MaterialTheme.colorScheme.surfaceContainerHighest,
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    if (!isScheduledToday) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Not scheduled",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(24.dp),
                        )
                    } else {
                        Text(
                            text = emoji,
                            fontSize = 28.sp,
                        )
                    }
                }

                Spacer(modifier = Modifier.width(14.dp))

                // Content
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text(
                            text = habit.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (isComplete)
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            else
                                MaterialTheme.colorScheme.onSurface,
                            textDecoration = if (isComplete) TextDecoration.LineThrough else TextDecoration.None,
                        )
                        // XP badge — tertiaryContainer pill
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50.dp))
                                .background(MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f))
                                .padding(horizontal = 6.dp, vertical = 2.dp),
                        ) {
                            Text(
                                text = "+${habit.baseXp} XP",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onTertiaryContainer,
                                fontSize = 10.sp,
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    if (isComplete) {
                        Text(
                            text = if (householdCompleterName != null)
                                "✓ Completed by $householdCompleterName"
                            else
                                "✓ Completed",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    } else {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            // Frequency label
                            val frequencyLabel = remember(habit.customDays, isScheduledToday) {
                                val days = habit.customDays

                                // Check for monthly pattern (days starting with "D")
                                val monthlyDays = days.filter { it.startsWith("D") }
                                if (monthlyDays.isNotEmpty()) {
                                    "Monthly"
                                } else {
                                    // Handle weekly pattern
                                    val allDays = setOf("MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN")
                                    val weekdays = setOf("MON", "TUE", "WED", "THU", "FRI")
                                    val weekends = setOf("SAT", "SUN")
                                    val daysUpper = days.map { it.uppercase() }.toSet()
                                    when {
                                        daysUpper == allDays -> "Daily"
                                        daysUpper == weekdays -> "Weekdays"
                                        daysUpper == weekends -> "Weekends"
                                        !isScheduledToday -> "Not today"
                                        else -> "Custom"
                                    }
                                }
                            }
                            Text(
                                text = frequencyLabel,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            // Streak (fire icon + count in secondary color)
                            if (currentStreak > 0) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.LocalFireDepartment,
                                        contentDescription = "Streak",
                                        tint = MaterialTheme.colorScheme.secondary,
                                        modifier = Modifier.size(13.dp),
                                    )
                                    Spacer(modifier = Modifier.width(2.dp))
                                    Text(
                                        text = "$currentStreak day streak",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.secondary,
                                    )
                                }
                            }
                        }
                    }
                }

                // Right side: complete button
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    // Complete button — filled primary circle vs outlined
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (isOwnedByCurrentUser) {
                            IconButton(
                                onClick = onEdit,
                                modifier = Modifier.size(32.dp),
                            ) {
                                Icon(
                                    Icons.Default.Edit,
                                    contentDescription = "Edit",
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            IconButton(
                                onClick = onDelete,
                                modifier = Modifier.size(32.dp),
                            ) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = "Delete",
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                                )
                            }
                        }
                        // Completion circle button
                        Box(
                            modifier = Modifier
                                .then(
                                    if (isComplete) Modifier.shadow(
                                        elevation = 6.dp,
                                        shape = CircleShape,
                                        ambientColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                                        spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                                    ) else Modifier
                                )
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(
                                    when {
                                        !isScheduledToday -> MaterialTheme.colorScheme.surfaceContainerHighest
                                        isComplete -> MaterialTheme.colorScheme.primary
                                        else -> MaterialTheme.colorScheme.surface
                                    },
                                )
                                .then(
                                    if (!isComplete && isScheduledToday)
                                        Modifier.background(MaterialTheme.colorScheme.surface)
                                    else Modifier
                                ),
                            contentAlignment = Alignment.Center,
                        ) {
                            if (isComplete) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Completed",
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(22.dp),
                                )
                            } else if (!isScheduledToday) {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = "Not scheduled",
                                    tint = MaterialTheme.colorScheme.outline,
                                    modifier = Modifier.size(18.dp),
                                )
                            } else {
                                IconButton(
                                    onClick = onComplete,
                                    modifier = Modifier.size(40.dp),
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.surface),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(32.dp)
                                                .clip(CircleShape)
                                                .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

fun getEmojiForIconName(name: String): String {
    return when (name) {
        "emoji_salad" -> "🥗"
        "emoji_water" -> "💧"
        "emoji_running" -> "🏃"
        "emoji_book" -> "📚"
        "emoji_meditate" -> "🧘"
        "emoji_cleaning" -> "🧹"
        "emoji_cooking" -> "🍳"
        "emoji_music" -> "🎵"
        "emoji_sleep" -> "😴"
        "emoji_code" -> "💻"
        "emoji_art" -> "🎨"
        "emoji_strength" -> "💪"
        "emoji_yoga" -> "🧘"
        "emoji_walk" -> "🚶"
        "emoji_study" -> "📖"
        else -> name
    }
}
