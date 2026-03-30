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
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
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
) {
    val isComplete = completedToday >= habit.targetCount
    val emoji = getEmojiForIconName(habit.iconName)
    val isMultiStep = habit.targetCount > 1

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer { alpha = if (isScheduledToday) 1f else 0.5f },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isComplete)
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.20f)
            else
                MaterialTheme.colorScheme.surfaceContainerLow,
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
                            color = MaterialTheme.colorScheme.onSurface,
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
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        // Frequency label
                        Text(
                            text = run {
                                val allDays = setOf("MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN")
                                val weekdays = setOf("MON", "TUE", "WED", "THU", "FRI")
                                val weekends = setOf("SAT", "SUN")
                                val days = habit.customDays.map { it.uppercase() }.toSet()
                                when {
                                    days == allDays -> "Daily"
                                    days == weekdays -> "Weekdays"
                                    days == weekends -> "Weekends"
                                    !isScheduledToday -> "Not today"
                                    else -> "Custom"
                                }
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        // Streak (fire icon + count in secondary color)
                        if (currentStreak > 0) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.LocalFireDepartment,
                                    contentDescription = "Streak",
                                    tint = if (currentStreak > 0)
                                        MaterialTheme.colorScheme.secondary
                                    else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                    modifier = Modifier.size(13.dp),
                                )
                                Spacer(modifier = Modifier.width(2.dp))
                                Text(
                                    text = "$currentStreak day streak",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (currentStreak > 0)
                                        MaterialTheme.colorScheme.secondary
                                    else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                )
                            }
                        }
                    }
                }

                // Right side: progress + complete button
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    // Progress count
                    Text(
                        text = "$completedToday / ${habit.targetCount}",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (isComplete) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    // Complete button — filled primary circle vs outlined
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = { if (!isComplete && isScheduledToday) onEdit() },
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
                            onClick = { if (!isComplete && isScheduledToday) onDelete() },
                            modifier = Modifier.size(32.dp),
                        ) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "Delete",
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                            )
                        }
                        // Completion circle button
                        Box(
                            modifier = Modifier
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

            // Multi-step progress bar
            if (isMultiStep && isScheduledToday) {
                Spacer(modifier = Modifier.height(10.dp))
                LinearProgressIndicator(
                    progress = { (completedToday.toFloat() / habit.targetCount).coerceIn(0f, 1f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                )
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
        else -> "🥗"
    }
}
