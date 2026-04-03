package com.example.choreboo_habittrackerfriend.ui.habits

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Stars
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.rememberLottieAnimatable
import com.airbnb.lottie.compose.rememberLottieComposition
import com.example.choreboo_habittrackerfriend.domain.model.Badge
import com.example.choreboo_habittrackerfriend.domain.model.ChorebooMood
import com.example.choreboo_habittrackerfriend.domain.model.Habit
import com.example.choreboo_habittrackerfriend.ui.habits.components.HabitCard
import com.example.choreboo_habittrackerfriend.ui.theme.XpPurple
import com.example.choreboo_habittrackerfriend.ui.components.ProfileAvatar
import com.example.choreboo_habittrackerfriend.ui.components.StitchSnackbar
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.DayOfWeek

private const val COMPLETION_ANIMATION_COOLDOWN_MS = 30_000L

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HabitListScreen(
    onAddHabit: () -> Unit,
    onEditHabit: (Long) -> Unit,
    viewModel: HabitListViewModel = hiltViewModel(),
) {
    val habits by viewModel.habits.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val totalPoints by viewModel.totalPoints.collectAsState()
    val profilePhotoUri by viewModel.profilePhotoUri.collectAsState()
    val todayCompletions by viewModel.todayCompletions.collectAsState()
    val streaks by viewModel.streaks.collectAsState()
    val maxStreak by viewModel.maxStreak.collectAsState()
    val todayXp by viewModel.todayXp.collectAsState()
    val petType by viewModel.petType.collectAsState()
    val chorebooStats by viewModel.chorebooStats.collectAsState()
    val earnedBadgeCount by viewModel.earnedBadgeCount.collectAsState()
    val totalLifetimeXp by viewModel.totalLifetimeXp.collectAsState()
    val weeklyCompletionDays by viewModel.weeklyCompletionDays.collectAsState()
    val recentBadges by viewModel.recentBadges.collectAsState()
    val allBadges by viewModel.allBadges.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var habitToDelete by remember { mutableStateOf<Habit?>(null) }
    var showLevelUpDialog by remember { mutableStateOf<HabitListEvent.HabitCompleted?>(null) }
    var showBadgeSheet by remember { mutableStateOf(false) }

    // Overscreen completion animation state
    var showCompletionAnimation by remember { mutableStateOf(false) }
    var isOverlayVisible by remember { mutableStateOf(false) }
    var lastAnimationEndTime by remember { mutableLongStateOf(0L) }
    val petFolder = petType.name.lowercase()
    val completionComposition by rememberLottieComposition(
        LottieCompositionSpec.Asset("animations/$petFolder/overscreen_${petFolder}_lottie.json"),
    )
    val completionAnimatable = rememberLottieAnimatable()

    LaunchedEffect(showCompletionAnimation, completionComposition) {
        if (showCompletionAnimation && completionComposition != null) {
            val animJob = launch {
                completionAnimatable.animate(composition = completionComposition, iterations = 1)
            }

            animJob.join()
            isOverlayVisible = false
            delay(500) // wait for slide-out exit transition to complete

            lastAnimationEndTime = System.currentTimeMillis()
            showCompletionAnimation = false
        } else if (showCompletionAnimation && completionComposition == null) {
            // Composition failed to load (e.g. missing asset for non-fox pet types).
            // Reset state so future completions are not permanently blocked.
            showCompletionAnimation = false
            isOverlayVisible = false
        }
    }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is HabitListEvent.HabitCompleted -> {
                    val now = System.currentTimeMillis()
                    if (!showCompletionAnimation &&
                        now - lastAnimationEndTime >= COMPLETION_ANIMATION_COOLDOWN_MS
                    ) {
                        showCompletionAnimation = true
                        isOverlayVisible = true
                    }
                    if (event.leveledUp || event.evolved) {
                        showLevelUpDialog = event
                    }
                    snackbarHostState.showSnackbar(
                        message = "+${event.xpEarned} XP! 🔥 ${event.streak}-day streak!",
                        duration = SnackbarDuration.Short,
                    )
                }
                is HabitListEvent.AlreadyComplete -> {
                    snackbarHostState.showSnackbar(
                        message = "Already completed for today! ✅",
                        duration = SnackbarDuration.Short,
                    )
                }
            }
        }
    }

    // Compute daily quest stats
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
                            googlePhotoUrl = viewModel.googlePhotoUrl,
                            size = 40.dp,
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "My Habits",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                },
                actions = {
                    // Star points chip — matches Pet/Calendar/Settings screens
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
                            contentDescription = "Points",
                            tint = MaterialTheme.colorScheme.secondary,
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
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddHabit,
                containerColor = MaterialTheme.colorScheme.secondary,
                contentColor = MaterialTheme.colorScheme.onSecondary,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.padding(bottom = 72.dp),
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Habit")
            }
        },
    ) { padding ->
        when {
            isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            }
            habits.isEmpty() -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = "📋", style = MaterialTheme.typography.displayLarge)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "No habits yet!",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Tap + to create your first habit\nand start earning rewards for your Choreboo!",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }
            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    item { Spacer(modifier = Modifier.height(8.dp)) }

                    // "My Habits" hero text
                    item {
                        Column {
                            Text(
                                text = "My Habits",
                                style = MaterialTheme.typography.displaySmall,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.onSurface,
                                letterSpacing = (-0.5).sp,
                            )
                            Text(
                                text = "Keep your pet happy and growing!",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }

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

                    // "Daily Quest" section header
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = "Daily Quest",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Text(
                                text = "$completedToday of ${scheduledToday.size} Completed",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }

                    items(habits, key = { it.id }) { habit ->
                        HabitCard(
                            habit = habit,
                            completedToday = todayCompletions[habit.id] ?: 0,
                            currentStreak = streaks[habit.id] ?: 0,
                            isScheduledToday = habit.isScheduledForToday(),
                            onComplete = { viewModel.completeHabit(habit.id) },
                            onEdit = { onEditHabit(habit.id) },
                            onDelete = { habitToDelete = habit },
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

                    item { Spacer(modifier = Modifier.height(80.dp)) }
                }
            }
        }

        // Delete confirmation dialog
        habitToDelete?.let { habit ->
            AlertDialog(
                onDismissRequest = { habitToDelete = null },
                title = { Text("Delete Habit?", fontWeight = FontWeight.Bold) },
                text = {
                    Text("Are you sure you want to delete \"${habit.title}\"? This will also remove all completion history.")
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            viewModel.deleteHabit(habit.id)
                            habitToDelete = null
                        },
                    ) {
                        Text("Delete", color = MaterialTheme.colorScheme.error)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { habitToDelete = null }) {
                        Text("Cancel")
                    }
                },
            )
        }

        // Achievement snackbar pinned above the bottom nav bar
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 90.dp),
            contentAlignment = Alignment.BottomCenter,
        ) {
            SnackbarHost(snackbarHostState) { data ->
                StitchSnackbar(data)
            }
        }

        // Overscreen completion animation — rendered last so it draws above the snackbar
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentAlignment = Alignment.BottomCenter,
        ) {
            AnimatedVisibility(
                visible = isOverlayVisible && completionComposition != null,
                enter = slideInVertically(
                    animationSpec = tween(durationMillis = 500),
                    initialOffsetY = { it },
                ),
                exit = slideOutVertically(
                    animationSpec = tween(durationMillis = 500),
                    targetOffsetY = { it },
                ),
            ) {
                LottieAnimation(
                    composition = completionComposition,
                    progress = { completionAnimatable.progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxSize(),
                )
            }
        }

        // Level-up celebration dialog
        showLevelUpDialog?.let { event ->
            LevelUpDialog(
                event = event,
                onDismiss = { showLevelUpDialog = null },
            )
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
// Today's Streak card
// ---------------------------------------------------------------------------

// ---------------------------------------------------------------------------
// Weekly Streak card (replaces TodaysStreakCard)
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

private val dayLabels = listOf("M", "T", "W", "T", "F", "S", "S")

@Composable
private fun WeeklyStreakCard(
    maxStreak: Int,
    weeklyCompletionDays: Set<DayOfWeek>,
    completionFraction: Float,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        // Fire watermark — faint, overlapping top-right
        Box(
            modifier = Modifier
                .size(120.dp)
                .align(Alignment.TopEnd)
                .padding(end = 4.dp),
        ) {
            Text(
                text = "🔥",
                fontSize = 100.sp,
                modifier = Modifier
                    .align(Alignment.Center)
                    .graphicsAlpha(0.15f),
            )
        }

        Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 20.dp)) {
            Text(
                text = "WEEKLY STREAK",
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
                    text = "Total Days",
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
                                    contentDescription = "Completed",
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

// Helper modifier extension for alpha without wrapping in another Box
private fun androidx.compose.ui.Modifier.graphicsAlpha(alphaValue: Float) =
    this.alpha(alphaValue)

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
                .background(MaterialTheme.colorScheme.surfaceContainerLow)
                .clickable { onBadgesTapped() },
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
            ) {
                Text(
                    text = "BADGES",
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
                        Text("\uD83C\uDFC5", fontSize = 32.sp)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Complete habits\nto earn badges!",
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
                                        contentDescription = badge.definition.displayName,
                                        tint = if (isUnlocked) MaterialTheme.colorScheme.onSecondaryContainer
                                        else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f),
                                        modifier = Modifier.size(16.dp),
                                    )
                                }
                                Text(
                                    text = badge.definition.displayName,
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
                        Text(text = "✨", fontSize = 16.sp)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "+$todayXp",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Black,
                            color = XpPurple,
                        )
                    }
                    Text(
                        text = "XP TODAY",
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
                        Text(text = "⭐", fontSize = 16.sp)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "$starPoints",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.secondary,
                        )
                    }
                    Text(
                        text = "STAR POINTS",
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
                text = "Badge Collection",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "${badges.count { it.isUnlocked }} of ${badges.size} earned",
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
                            contentDescription = badge.definition.displayName,
                            tint = if (isUnlocked) MaterialTheme.colorScheme.onSecondaryContainer
                            else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f),
                            modifier = Modifier.size(22.dp),
                        )
                    }

                    // Name + description
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = badge.definition.displayName,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (isUnlocked) MaterialTheme.colorScheme.onSurface
                            else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        )
                        Text(
                            text = badge.definition.description,
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
                                contentDescription = "Earned",
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
                                contentDescription = "Locked",
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
        ChorebooMood.HAPPY -> "🥰"
        ChorebooMood.CONTENT -> "😊"
        ChorebooMood.HUNGRY -> "😩"
        ChorebooMood.TIRED -> "😴"
        ChorebooMood.SAD -> "😢"
        ChorebooMood.IDLE -> "😐"
    }
    val moodLabel = when (mood) {
        ChorebooMood.HAPPY -> "Feeling Loved"
        ChorebooMood.CONTENT -> "Feeling Good"
        ChorebooMood.HUNGRY -> "Feeling Hungry"
        ChorebooMood.TIRED -> "Feeling Tired"
        ChorebooMood.SAD -> "Feeling Sad"
        ChorebooMood.IDLE -> "Feeling Idle"
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // Pet Mood card — tertiaryFixed-like lavender
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
                    text = "PET MOOD",
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

        // Level / XP card — primaryFixed-like light green
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
                    text = "LEVEL $level",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    letterSpacing = 1.sp,
                )
                Text(
                    text = "🍗",
                    fontSize = 44.sp,
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                )
                Text(
                    text = "$xp / $xpToNextLevel XP",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Level-up celebration dialog
// ---------------------------------------------------------------------------

@Composable
private fun LevelUpDialog(
    event: HabitListEvent.HabitCompleted,
    onDismiss: () -> Unit,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 100.dp)
                .padding(horizontal = 24.dp),
            contentAlignment = Alignment.BottomCenter,
        ) {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondary,
                    contentColor = MaterialTheme.colorScheme.onSecondary,
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = if (event.evolved) "🎉 Evolution! 🎉" else "🎉 Level Up! 🎉",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSecondary,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = if (event.evolved)
                            "Your Choreboo evolved to ${event.newStageName}! 🌟\nNow Level ${event.newLevel}!"
                        else
                            "Your Choreboo reached Level ${event.newLevel}! 🌟\nKeep completing habits to evolve!",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSecondary,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    TextButton(onClick = onDismiss) {
                        Text(
                            text = "Awesome! 🎊",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSecondary,
                        )
                    }
                }
            }
        }
    }
}
