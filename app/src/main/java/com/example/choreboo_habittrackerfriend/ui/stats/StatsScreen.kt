package com.example.choreboo_habittrackerfriend.ui.stats

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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
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
import com.example.choreboo_habittrackerfriend.R
import com.example.choreboo_habittrackerfriend.domain.model.Badge
import com.example.choreboo_habittrackerfriend.domain.model.ChorebooMood
import com.example.choreboo_habittrackerfriend.ui.components.ProfileAvatar
import com.example.choreboo_habittrackerfriend.ui.theme.XpPurple
import com.example.choreboo_habittrackerfriend.ui.util.displayNameRes
import com.example.choreboo_habittrackerfriend.ui.util.descriptionRes
import com.example.choreboo_habittrackerfriend.ui.util.feelingLabelRes
import java.time.DayOfWeek

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(
    viewModel: StatsViewModel = hiltViewModel(),
) {
    val totalPoints by viewModel.totalPoints.collectAsState()
    val profilePhotoUri by viewModel.profilePhotoUri.collectAsState()
    val googlePhotoUrl by viewModel.googlePhotoUrl.collectAsState()
    val maxStreak by viewModel.maxStreak.collectAsState()
    val todayXp by viewModel.todayXp.collectAsState()
    val chorebooStats by viewModel.chorebooStats.collectAsState()
    val weeklyCompletionDays by viewModel.weeklyCompletionDays.collectAsState()
    val allBadges by viewModel.allBadges.collectAsState()
    val habits by viewModel.habits.collectAsState()
    val todayCompletions by viewModel.todayCompletions.collectAsState()
    val monthlyCompletionRate by viewModel.monthlyCompletionRate.collectAsState()

    var showBadgeSheet by remember { mutableStateOf(false) }

    // Refresh today's date on every screen resume to handle midnight crossings
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.refreshTodayDate()
    }

    // Compute daily quest completion fraction for weekly streak card
    val scheduledToday = habits.filter { it.isScheduledForToday() }
    val completedToday = scheduledToday.count { (todayCompletions[it.id] ?: 0) >= 1 }
    val completionFraction = if (scheduledToday.isEmpty()) 0f
    else (completedToday.toFloat() / scheduledToday.size).coerceIn(0f, 1f)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        ProfileAvatar(
                            profilePhotoUri = profilePhotoUri,
                            googlePhotoUrl = googlePhotoUrl,
                            size = 40.dp,
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = stringResource(R.string.stats_title),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                },
                actions = {
                    // Star points chip
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .padding(end = 16.dp)
                            .clip(RoundedCornerShape(50.dp))
                            .background(MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.85f))
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Default.Stars,
                            contentDescription = stringResource(R.string.stats_points_cd),
                            tint = MaterialTheme.colorScheme.secondaryContainer,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "$totalPoints",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.labelLarge,
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = androidx.compose.ui.graphics.Color.Transparent,
                ),
            )
        },
    ) { padding ->
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

            item { Spacer(modifier = Modifier.height(80.dp)) }
        }

        // Badge collection bottom sheet
        if (showBadgeSheet) {
            BadgeBottomSheet(
                badges = allBadges,
                onDismiss = { showBadgeSheet = false },
            )
        }
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
                                        imageVector = badge.definition.icon,
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
                    Text(
                        text = stringResource(R.string.stats_xp_today),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = XpPurple.copy(alpha = 0.7f),
                        letterSpacing = 1.sp,
                    )
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
                    Text(
                        text = stringResource(R.string.stats_star_points),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.7f),
                        letterSpacing = 1.sp,
                    )
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
                            imageVector = badge.definition.icon,
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
