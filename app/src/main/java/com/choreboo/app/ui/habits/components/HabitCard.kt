package com.choreboo.app.ui.habits.components

import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.NotificationsNone
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.choreboo.app.R
import com.choreboo.app.domain.model.Habit
import com.choreboo.app.ui.theme.softGlassSurface
import java.time.LocalDateTime

@Composable
fun HabitCard(
    habit: Habit,
    completedToday: Int,
    currentStreak: Int = 0,
    isScheduledToday: Boolean = true,
    nextScheduledLabel: String? = null,
    modifier: Modifier = Modifier,
    isAnimatingComplete: Boolean = false,
    onComplete: () -> Unit,
    onClick: () -> Unit = {},
    /**
     * If non-null, a household member already completed this habit today.
     * The card shows "Completed by [name]" instead of the generic "✓ Completed".
     */
    householdCompleterName: String? = null,
    now: LocalDateTime = LocalDateTime.now(),
) {
    val isComplete = completedToday >= 1
    val isVisuallyComplete = isComplete || isAnimatingComplete
    val emoji = getEmojiForIconName(habit.iconName)
    val cardContainerColor = animateColorAsState(
        targetValue = if (isVisuallyComplete) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.42f)
        } else {
            MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.82f)
        },
        label = "habitCardContainerColor",
    )
    val cardBorderColor = animateColorAsState(
        targetValue = if (isVisuallyComplete) {
            MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
        } else {
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.24f)
        },
        label = "habitCardBorderColor",
    )
    val completionScale = animateFloatAsState(
        targetValue = if (isAnimatingComplete && !isComplete) 1.12f else 1f,
        animationSpec = spring(dampingRatio = 0.55f, stiffness = 500f),
        label = "habitCompletionScale",
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer { alpha = if (isScheduledToday) 1f else 0.5f }
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = androidx.compose.ui.graphics.Color.Transparent,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .softGlassSurface(
                    shape = RoundedCornerShape(16.dp),
                    containerColor = cardContainerColor.value,
                    borderColor = cardBorderColor.value,
                )
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
                            if (isVisuallyComplete) MaterialTheme.colorScheme.primary
                            else if (!isScheduledToday) MaterialTheme.colorScheme.surfaceContainerHighest
                            else MaterialTheme.colorScheme.surfaceContainerHighest,
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    if (!isScheduledToday) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = stringResource(R.string.habit_card_not_scheduled_cd),
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
                            color = if (isVisuallyComplete)
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            else
                                MaterialTheme.colorScheme.onSurface,
                            textDecoration = if (isVisuallyComplete) TextDecoration.LineThrough else TextDecoration.None,
                        )
                        // XP badge — tertiaryContainer pill
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
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    if (isVisuallyComplete) {
                        Text(
                            text = if (householdCompleterName != null)
                                stringResource(R.string.habit_card_completed_by, householdCompleterName)
                            else
                                stringResource(R.string.habit_card_completed),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            // Frequency label
                            val frequencyLabelResId = remember(habit.customDays, isScheduledToday) {
                                val days = habit.customDays

                                // Check for monthly pattern (days starting with "D")
                                val monthlyDays = days.filter { it.startsWith("D") }
                                if (monthlyDays.isNotEmpty()) {
                                    R.string.habit_frequency_monthly
                                } else {
                                    // Handle weekly pattern
                                    val allDays = setOf("MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN")
                                    val weekdays = setOf("MON", "TUE", "WED", "THU", "FRI")
                                    val weekends = setOf("SAT", "SUN")
                                    val daysUpper = days.map { it.uppercase() }.toSet()
                                    when {
                                        daysUpper == allDays -> R.string.habit_frequency_daily
                                        daysUpper == weekdays -> R.string.habit_frequency_weekdays
                                        daysUpper == weekends -> R.string.habit_frequency_weekends
                                        !isScheduledToday -> R.string.habit_frequency_not_today
                                        else -> R.string.habit_frequency_custom
                                    }
                                }
                            }
                            Text(
                                text = stringResource(frequencyLabelResId),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            // Streak (fire icon + count in secondary color)
                            if (currentStreak > 0) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.LocalFireDepartment,
                                        contentDescription = stringResource(R.string.habit_card_streak_cd),
                                        tint = MaterialTheme.colorScheme.secondary,
                                        modifier = Modifier.size(13.dp),
                                    )
                                    Spacer(modifier = Modifier.width(2.dp))
                                    Text(
                                        text = stringResource(R.string.habit_card_streak_label, currentStreak),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.secondary,
                                    )
                                }
                             }
                          }
                      }
                      // Reminder countdown — shown only when not yet complete and reminder is set
                      if (!isComplete && !isScheduledToday && nextScheduledLabel != null) {
                          Text(
                              text = nextScheduledLabel,
                              style = MaterialTheme.typography.labelSmall,
                              color = MaterialTheme.colorScheme.onSurfaceVariant,
                          )
                      }
                      if (!isComplete && isScheduledToday) {
                           val minutesUntil = remember(habit.reminderEnabled, habit.reminderTime, habit.customDays, now) {
                               habit.timeUntilNextReminderMinutes(now)
                           }
                          if (minutesUntil != null && minutesUntil > 0) {
                              Spacer(modifier = Modifier.height(4.dp))
                              Row(
                                  verticalAlignment = Alignment.CenterVertically,
                                  horizontalArrangement = Arrangement.spacedBy(4.dp),
                              ) {
                                  Icon(
                                      imageVector = Icons.Default.NotificationsNone,
                                      contentDescription = null,
                                      tint = MaterialTheme.colorScheme.tertiary,
                                      modifier = Modifier.size(11.dp),
                                  )
                                  val reminderText = if (minutesUntil >= 60) {
                                      val hours = minutesUntil / 60
                                      val mins = minutesUntil % 60
                                      if (mins > 0) "${hours}h ${mins}m" else "${hours}h"
                                  } else {
                                      "${minutesUntil}m"
                                  }
                                  Text(
                                      text = reminderText,
                                      style = MaterialTheme.typography.labelSmall,
                                      color = MaterialTheme.colorScheme.tertiary,
                                      fontSize = 10.sp,
                                  )
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
                        // Completion circle button
                        Box(
                            modifier = Modifier
                                .then(
                                    if (isVisuallyComplete) Modifier.shadow(
                                        elevation = 6.dp,
                                        shape = CircleShape,
                                        ambientColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                                        spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                                    ) else Modifier
                                )
                                .size(40.dp)
                                .scale(completionScale.value)
                                .clip(CircleShape)
                                .background(
                                    when {
                                        !isScheduledToday -> MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.8f)
                                        isVisuallyComplete -> MaterialTheme.colorScheme.primary
                                        else -> MaterialTheme.colorScheme.surface.copy(alpha = 0.82f)
                                    },
                                )
                                .then(
                                    if (!isVisuallyComplete && isScheduledToday)
                                        Modifier.background(MaterialTheme.colorScheme.surface)
                                    else Modifier
                                ),
                            contentAlignment = Alignment.Center,
                        ) {
                            if (isVisuallyComplete) {
                                Crossfade(targetState = isAnimatingComplete && !isComplete, label = "habitCheckCrossfade") { animating ->
                                    if (animating) {
                                        Box(
                                            modifier = Modifier
                                                .size(22.dp)
                                                .clip(CircleShape)
                                                .background(MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.22f)),
                                        )
                                    } else {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = stringResource(R.string.habit_card_completed),
                                            tint = MaterialTheme.colorScheme.onPrimary,
                                            modifier = Modifier.size(22.dp),
                                        )
                                    }
                                }
                            } else if (!isScheduledToday) {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = stringResource(R.string.habit_card_not_scheduled_cd),
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
