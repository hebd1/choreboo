package com.choreboo.app.ui.stats

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Stars
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import com.choreboo.app.R
import com.choreboo.app.domain.model.Badge
import com.choreboo.app.domain.model.ChorebooMood
import com.choreboo.app.ui.components.ChorebooTopAppBar
import com.choreboo.app.ui.theme.XpPurple
import com.choreboo.app.ui.util.displayNameRes
import com.choreboo.app.ui.util.descriptionRes
import com.choreboo.app.ui.util.feelingLabelRes
import com.choreboo.app.ui.util.resolveIcon
import java.time.DayOfWeek

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(
    viewModel: StatsViewModel = hiltViewModel(),
) {
    val totalPoints by viewModel.totalPoints.collectAsStateWithLifecycle()
    val profilePhotoUri by viewModel.profilePhotoUri.collectAsStateWithLifecycle()
    val googlePhotoUrl by viewModel.googlePhotoUrl.collectAsStateWithLifecycle()
    val maxStreak by viewModel.maxStreak.collectAsStateWithLifecycle()
    val todayXp by viewModel.todayXp.collectAsStateWithLifecycle()
    val chorebooStats by viewModel.chorebooStats.collectAsStateWithLifecycle()
    val weeklyCompletionDays by viewModel.weeklyCompletionDays.collectAsStateWithLifecycle()
    val allBadges by viewModel.allBadges.collectAsStateWithLifecycle()
    val habits by viewModel.habits.collectAsStateWithLifecycle()
    val todayCompletions by viewModel.todayCompletions.collectAsStateWithLifecycle()
    val monthlyCompletionRate by viewModel.monthlyCompletionRate.collectAsStateWithLifecycle()
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val todayLocalDate by viewModel.todayLocalDate.collectAsStateWithLifecycle()

    var showBadgeSheet by rememberSaveable { mutableStateOf(false) }

    // Refresh today's date on every screen resume to handle midnight crossings
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.refreshTodayDate()
    }

    // Compute daily quest completion fraction for weekly streak card
    val scheduledToday = habits.filter { it.isScheduledForToday(todayLocalDate) }
    val completedToday = scheduledToday.count { (todayCompletions[it.id] ?: 0) >= 1 }
    val completionFraction = if (scheduledToday.isEmpty()) 0f
    else (completedToday.toFloat() / scheduledToday.size).coerceIn(0f, 1f)

    Scaffold(
        topBar = {
            ChorebooTopAppBar(
                profilePhotoUri = profilePhotoUri,
                googlePhotoUrl = googlePhotoUrl,
                totalPoints = totalPoints,
                pointsContentDescription = stringResource(R.string.stats_points_cd),
            ) {
                Text(
                    text = stringResource(R.string.stats_title),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        },
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = { viewModel.refreshData() },
            modifier = Modifier.fillMaxSize(),
        ) {
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }
        } else {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item { Spacer(modifier = Modifier.height(8.dp)) }

            // Weekly Streak card
            item {
                WeeklyStreakCard(
                    maxStreak = maxStreak,
                    weeklyCompletionDays = weeklyCompletionDays,
                    completionFraction = completionFraction,
                )
            }

            // Streak / XP bento grid
            item {
                StreakXpBentoGrid(
                    previewBadges = allBadges.sortedWith(
                        compareByDescending<Badge> { it.isUnlocked }
                            .thenByDescending { it.definition.threshold },
                    ).take(4),
                    todayXp = todayXp,
                    starPoints = totalPoints,
                    onBadgesTapped = { showBadgeSheet = true },
                )
            }

            // Pet Mood + Level/XP bento grid
            item {
                PetStatusBentoGrid(
                    mood = chorebooStats?.mood ?: ChorebooMood.IDLE,
                    level = chorebooStats?.level ?: 1,
                    xp = chorebooStats?.xp ?: 0,
                    xpToNextLevel = chorebooStats?.xpToNextLevel ?: 50,
                )
            }

            // Monthly Mastery tile
            item {
                MonthlyMasteryCard(completionRate = monthlyCompletionRate)
            }

        }
        } // end else (isLoading)

        // Badge collection bottom sheet
        if (showBadgeSheet) {
            BadgeBottomSheet(
                badges = allBadges,
                onDismiss = { showBadgeSheet = false },
            )
        }
        } // end PullToRefreshBox
    }
}

// ---------------------------------------------------------------------------
// Weekly Streak card
// ---------------------------------------------------------------------------

private val weekDays = listOf(
    DayOfWeek.MONDAY,
    DayOfWeek.TUESDAY,
    DayOfWeek.WEDNESDAY,
    DayOfWeek.THURSDAY,
    DayOfWeek.FRIDAY,
    DayOfWeek.SATURDAY,
    DayOfWeek.SUNDAY,
)

@Composable
private fun getDayLabels(): List<String> {
    return listOf(
        stringResource(R.string.day_abbreviation_monday),
        stringResource(R.string.day_abbreviation_tuesday),
        stringResource(R.string.day_abbreviation_wednesday),
        stringResource(R.string.day_abbreviation_thursday),
        stringResource(R.string.day_abbreviation_friday),
        stringResource(R.string.day_abbreviation_saturday),
        stringResource(R.string.day_abbreviation_sunday),
    )
}

@Composable
private fun WeeklyStreakCard(
    maxStreak: Int,
    weeklyCompletionDays: Set<DayOfWeek>,
    completionFraction: Float,
) {
    val dayLabels = getDayLabels()
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLowest),
    ) {
        // Fire watermark — faint, overlapping top-right
        Box(
            modifier = Modifier
                .size(120.dp)
                .align(Alignment.TopEnd)
                .padding(end = 4.dp),
        ) {
            Text(
                text = "\uD83D\uDD25",
                fontSize = 100.sp,
                modifier = Modifier
                    .align(Alignment.Center)
                    .alpha(0.15f),
            )
        }

        Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 20.dp)) {
            Text(
                text = stringResource(R.string.stats_weekly_streak),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                letterSpacing = 2.sp,
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = "$maxStreak",
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.stats_total_days),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 4.dp),
                )
            }
            Spacer(modifier = Modifier.height(16.dp))

            // 7 day-of-week circles
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                weekDays.forEachIndexed { index, day ->
                    val isCompleted = day in weeklyCompletionDays
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isCompleted) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.surfaceContainerHighest,
                                ),
                            contentAlignment = Alignment.Center,
                        ) {
                            if (isCompleted) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = stringResource(R.string.stats_completed_cd),
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(18.dp),
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = dayLabels[index],
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (isCompleted) FontWeight.Bold else FontWeight.Normal,
                            color = if (isCompleted) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Streak / XP bento grid
// ---------------------------------------------------------------------------

@Composable
private fun StreakXpBentoGrid(
    previewBadges: List<Badge>,
    todayXp: Int,
    starPoints: Int,
    onBadgesTapped: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // Left — badge preview
        Box(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerLowest)
                .clickable { onBadgesTapped() },
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
            ) {
                Text(
                    text = stringResource(R.string.stats_badges_section),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = 1.sp,
                )
                Spacer(modifier = Modifier.height(8.dp))
                if (previewBadges.isEmpty()) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Text("🏆", fontSize = 32.sp)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = stringResource(R.string.stats_no_badges),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                        )
                    }
                } else {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        previewBadges.forEach { badge ->
                            val isUnlocked = badge.isUnlocked
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(26.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (isUnlocked) MaterialTheme.colorScheme.secondaryContainer
                                            else MaterialTheme.colorScheme.surfaceContainerHigh,
                                        ),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(
                                         imageVector = badge.definition.resolveIcon(),
                                        contentDescription = stringResource(badge.definition.displayNameRes()),
                                        tint = if (isUnlocked) MaterialTheme.colorScheme.onSecondaryContainer
                                        else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f),
                                        modifier = Modifier.size(16.dp),
                                    )
                                }
                                Text(
                                    text = stringResource(badge.definition.displayNameRes()),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isUnlocked) MaterialTheme.colorScheme.onSurface
                                    else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    }
                }
            }
        }

        // Right — two stacked pills
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            // XP Today — purple tint
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(XpPurple.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = stringResource(R.string.stats_xp_today), fontSize = 16.sp)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "+$todayXp",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Black,
                            color = XpPurple,
                        )
                    }

                }
            }

            // Star Points
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.20f)),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = stringResource(R.string.stats_star_points), fontSize = 16.sp)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "$starPoints",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.secondary,
                        )
                    }

                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Pet Status bento grid (Pet Mood + Level/XP)
// ---------------------------------------------------------------------------

@Composable
private fun PetStatusBentoGrid(
    mood: ChorebooMood,
    level: Int,
    xp: Int,
    xpToNextLevel: Int,
) {
    val moodEmoji = when (mood) {
        ChorebooMood.HAPPY -> "\uD83E\uDD70"
        ChorebooMood.CONTENT -> "\uD83D\uDE0A"
        ChorebooMood.HUNGRY -> "\uD83D\uDE29"
        ChorebooMood.TIRED -> "\uD83D\uDE34"
        ChorebooMood.SAD -> "\uD83D\uDE22"
        ChorebooMood.IDLE -> "\uD83D\uDE10"
    }
    val moodLabel = stringResource(mood.feelingLabelRes())

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // Pet Mood card
        Box(
            modifier = Modifier
                .weight(1f)
                .aspectRatio(1f)
                .clip(RoundedCornerShape(20.dp))
                .background(MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.30f)),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = stringResource(R.string.stats_pet_mood),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                    letterSpacing = 1.sp,
                )
                Text(
                    text = moodEmoji,
                    fontSize = 44.sp,
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                )
                Text(
                    text = moodLabel,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                )
            }
        }

        // Level / XP card
        Box(
            modifier = Modifier
                .weight(1f)
                .aspectRatio(1f)
                .clip(RoundedCornerShape(20.dp))
                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = stringResource(R.string.stats_level_label, level),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    letterSpacing = 1.sp,
                )
                Text(
                    text = level.toString(),
                    fontSize = 44.sp,
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                )
                Text(
                    text = stringResource(R.string.stats_xp_progress, xp, xpToNextLevel),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Monthly Mastery card
// ---------------------------------------------------------------------------

@Composable
private fun MonthlyMasteryCard(completionRate: Int) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(140.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.primary)
            .padding(20.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
        Text(
            text = stringResource(R.string.stats_monthly_mastery),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f),
            letterSpacing = 1.sp,
        )
            Column {
                Text(
                    text = "$completionRate%",
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onPrimary,
                )
                Text(
                    text = when {
                        completionRate >= 80 -> stringResource(R.string.stats_mastery_warrior)
                        completionRate >= 50 -> stringResource(R.string.stats_mastery_legendary)
                        completionRate >= 20 -> stringResource(R.string.stats_mastery_champion)
                        else -> stringResource(R.string.stats_mastery_beginner)
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.9f),
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Badge collection bottom sheet
// ---------------------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BadgeBottomSheet(
    badges: List<Badge>,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
        ) {
            // Header
            Text(
                text = stringResource(R.string.stats_badge_collection),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.stats_badges_earned_count, badges.count { it.isUnlocked }, badges.size),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(16.dp))

            // Badge list
            badges.forEach { badge ->
                val isUnlocked = badge.isUnlocked
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    // Icon circle
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(
                                if (isUnlocked) MaterialTheme.colorScheme.secondaryContainer
                                else MaterialTheme.colorScheme.surfaceContainerHigh,
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = badge.definition.resolveIcon(),
                            contentDescription = stringResource(badge.definition.displayNameRes()),
                            tint = if (isUnlocked) MaterialTheme.colorScheme.onSecondaryContainer
                            else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f),
                            modifier = Modifier.size(22.dp),
                        )
                    }

                    // Name + description
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(badge.definition.displayNameRes()),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (isUnlocked) MaterialTheme.colorScheme.onSurface
                            else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        )
                        Text(
                            text = stringResource(badge.definition.descriptionRes()),
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isUnlocked) MaterialTheme.colorScheme.onSurfaceVariant
                            else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f),
                        )
                    }

                    // Status indicator
                    if (isUnlocked) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary),
                            contentAlignment = Alignment.Center,
                        ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = stringResource(R.string.stats_badge_earned_cd),
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(14.dp),
                        )
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = stringResource(R.string.stats_badge_locked_cd),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f),
                                modifier = Modifier.size(14.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}
