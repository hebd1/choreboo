package com.example.choreboo_habittrackerfriend.ui.pet

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Stars
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.choreboo_habittrackerfriend.domain.model.ChorebooMood
import com.example.choreboo_habittrackerfriend.ui.components.WebmAnimationView
import com.example.choreboo_habittrackerfriend.domain.model.Habit
import com.example.choreboo_habittrackerfriend.domain.model.PetType
import com.example.choreboo_habittrackerfriend.ui.components.ProfileAvatar
import com.example.choreboo_habittrackerfriend.ui.components.ShimmerPlaceholder
import com.example.choreboo_habittrackerfriend.ui.components.StitchSnackbar
import com.example.choreboo_habittrackerfriend.ui.components.WebmAnimationView
import com.example.choreboo_habittrackerfriend.ui.habits.components.HabitCard
import com.example.choreboo_habittrackerfriend.ui.theme.PetMoodContentStart
import com.example.choreboo_habittrackerfriend.ui.theme.PetMoodHappyStart
import com.example.choreboo_habittrackerfriend.ui.theme.PetMoodHungryStart
import com.example.choreboo_habittrackerfriend.ui.theme.PetMoodSadStart
import com.example.choreboo_habittrackerfriend.ui.theme.PetMoodTiredStart
import com.example.choreboo_habittrackerfriend.ui.theme.XpPurple
import kotlinx.coroutines.launch

private enum class AnimationPhase { MOOD, IDLE, EATING, INTERACTING, THUMBS_UP, START_SLEEPING, SLEEPING }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PetScreen(
    onAddHabit: () -> Unit = {},
    onEditHabit: (Long) -> Unit = {},
    habitJustCreated: Boolean = false,
    onHabitCreatedConsumed: () -> Unit = {},
    viewModel: PetViewModel = hiltViewModel(),
) {
    // Choreboo state
    val choreboo by viewModel.chorebooState.collectAsStateWithLifecycle()
    val mood by viewModel.currentMood.collectAsStateWithLifecycle()
    val totalPoints by viewModel.totalPoints.collectAsStateWithLifecycle()
    val isEating by viewModel.isEating.collectAsStateWithLifecycle()
    val isSleeping by viewModel.isSleeping.collectAsStateWithLifecycle()
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()
    val profilePhotoUri by viewModel.profilePhotoUri.collectAsStateWithLifecycle()
    val googlePhotoUrl by viewModel.googlePhotoUrl.collectAsStateWithLifecycle()
    val petType by viewModel.petType.collectAsStateWithLifecycle()

    // Habit state
    val habits by viewModel.habits.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val todayCompletions by viewModel.todayCompletions.collectAsStateWithLifecycle()
    val streaks by viewModel.streaks.collectAsStateWithLifecycle()
    val householdCompleterNames by viewModel.householdCompleterNames.collectAsStateWithLifecycle()

    // Local UI state
    val snackbarHostState = remember { SnackbarHostState() }
    var showSleepDialog by remember { mutableStateOf(false) }
    var showDetailedStats by remember { mutableStateOf(false) }
    var showLevelUpDialog by remember { mutableStateOf<PetEvent.HabitCompleted?>(null) }
    var habitToDelete by remember { mutableStateOf<Habit?>(null) }
    var isInteracting by remember { mutableStateOf(false) }
    var showStartSleepAnimation by remember { mutableStateOf(false) }
    var showThumbsUp by remember { mutableStateOf(false) }

    // Event collector
    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is PetEvent.Fed -> {
                    snackbarHostState.showSnackbar(
                        message = "Yum! Your Choreboo loved that! \uD83D\uDE0B",
                        actionLabel = "success",
                        duration = SnackbarDuration.Short,
                    )
                }
                is PetEvent.InsufficientPoints -> {
                    snackbarHostState.showSnackbar(
                        message = "Not enough points to feed! Complete habits to earn more. \uD83D\uDCAA",
                        actionLabel = "error",
                        duration = SnackbarDuration.Short,
                    )
                }
                is PetEvent.Sleeping -> {
                    snackbarHostState.showSnackbar(
                        message = "Your Choreboo is now sleeping! \uD83D\uDE34 Stats are frozen for 24 hours.",
                        actionLabel = "info",
                        duration = SnackbarDuration.Short,
                    )
                }
                is PetEvent.AlreadySleeping -> {
                    snackbarHostState.showSnackbar(
                        message = "Your Choreboo is already sleeping! Let them rest. \uD83D\uDCA4",
                        actionLabel = "info",
                        duration = SnackbarDuration.Short,
                    )
                }
                is PetEvent.HabitCompleted -> {
                    showThumbsUp = true
                    if (event.leveledUp || event.evolved) {
                        showLevelUpDialog = event
                    }
                    snackbarHostState.showSnackbar(
                        message = "+${event.xpEarned} XP! \uD83D\uDD25 ${event.streak}-day streak!",
                        actionLabel = "achievement",
                        duration = SnackbarDuration.Short,
                    )
                }
                is PetEvent.AlreadyComplete -> {
                    snackbarHostState.showSnackbar(
                        message = "Already completed for today! \u2705",
                        actionLabel = "info",
                        duration = SnackbarDuration.Short,
                    )
                }
                is PetEvent.CompletionError -> {
                    snackbarHostState.showSnackbar(
                        message = "Couldn't complete habit: ${event.message}",
                        actionLabel = "error",
                        duration = SnackbarDuration.Short,
                    )
                }
            }
        }
    }

    // Habit creation success snackbar
    LaunchedEffect(habitJustCreated) {
        if (habitJustCreated) {
            snackbarHostState.showSnackbar(
                message = "Habit created! Keep it up! \uD83C\uDF1F",
                duration = SnackbarDuration.Short,
            )
            onHabitCreatedConsumed()
        }
    }

    val moodBgStart by animateColorAsState(
        targetValue = when (mood) {
            ChorebooMood.HAPPY -> PetMoodHappyStart
            ChorebooMood.CONTENT -> PetMoodContentStart
            ChorebooMood.HUNGRY -> PetMoodHungryStart
            ChorebooMood.TIRED -> PetMoodTiredStart
            ChorebooMood.SAD -> PetMoodSadStart
            ChorebooMood.IDLE -> MaterialTheme.colorScheme.surfaceContainerLow
        },
        label = "petBgStart",
    )

    // Compute daily quest stats
    val scheduledToday = remember(habits) { habits.filter { it.isScheduledForToday() } }
    val completedToday = remember(scheduledToday, todayCompletions) {
        scheduledToday.count { (todayCompletions[it.id] ?: 0) >= 1 }
    }

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
                        if (choreboo != null) {
                            Text(
                                choreboo!!.name,
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        } else {
                            ShimmerPlaceholder(
                                width = 120.dp,
                                height = 32.dp,
                            )
                        }
                    }
                },
                actions = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .padding(end = 16.dp)
                            .clip(RoundedCornerShape(50.dp))
                            .background(MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.85f))
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                    ) {
                        Icon(
                            Icons.Default.Stars,
                            contentDescription = "Points",
                            tint = MaterialTheme.colorScheme.secondaryContainer,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            "$totalPoints",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.labelLarge,
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
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
        snackbarHost = { },
    ) { padding ->
        if (choreboo == null) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("Loading your Choreboo...")
            }
            return@Scaffold
        }

        val stats = choreboo!!

        Box(modifier = Modifier.fillMaxSize()) {
            PullToRefreshBox(
                isRefreshing = isRefreshing,
                onRefresh = { viewModel.refreshData() },
                modifier = Modifier.fillMaxSize(),
            ) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                // -------------------------------------------------------
                // Pet animation box
                // -------------------------------------------------------
                item {
                    Spacer(modifier = Modifier.height(20.dp))
                    Box(modifier = Modifier.fillMaxWidth()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(220.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(moodBgStart),
                            contentAlignment = Alignment.Center,
                        ) {
                            PetAnimation(
                                petType = stats.petType,
                                mood = mood,
                                isEating = isEating,
                                isInteracting = isInteracting,
                                isSleeping = isSleeping,
                                showStartSleepAnimation = showStartSleepAnimation,
                                showThumbsUp = showThumbsUp,
                                onEatingComplete = { viewModel.onEatingAnimationComplete() },
                                onInteractComplete = { isInteracting = false },
                                onStartSleepComplete = { showStartSleepAnimation = false },
                                onThumbsUpComplete = { showThumbsUp = false },
                                onTap = { isInteracting = true },
                                modifier = Modifier.size(160.dp),
                            )

                            // Mood pill at bottom — tappable to show stat overlay
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .padding(bottom = 10.dp)
                                    .clip(RoundedCornerShape(50.dp))
                                    .background(MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.8f))
                                    .clickable { showDetailedStats = !showDetailedStats }
                                    .padding(horizontal = 14.dp, vertical = 6.dp),
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                ) {
                                    Text(
                                        text = if (isSleeping) "\uD83D\uDE34 SLEEPING" else "${mood.emoji} ${mood.displayName.uppercase()}",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                    )
                                    Icon(
                                        imageVector = Icons.Default.ExpandMore,
                                        contentDescription = "View stats",
                                        tint = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.size(14.dp),
                                    )
                                }
                            }
                        }

                        // Level badge — top-right, outside the clipped box so it renders unclipped
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .offset(x = 6.dp, y = (-12).dp)
                                .rotate(3f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(MaterialTheme.colorScheme.secondaryContainer)
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                Text("\u2B50", fontSize = 18.sp)
                                Text(
                                    text = "Lv.${stats.level}",
                                    fontWeight = FontWeight.Black,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                )
                            }
                        }
                    }
                }

                // -------------------------------------------------------
                // Action row — Feed / Play / Sleep
                // -------------------------------------------------------
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        ActionButton(
                            modifier = Modifier.weight(1f),
                            label = "Feed",
                            emoji = "\uD83C\uDF56",
                            onClick = { viewModel.feedChoreboo() },
                            enabled = totalPoints >= 10 && !isSleeping,
                            isPrimary = true,
                        )
                        ActionButton(
                            modifier = Modifier.weight(1f),
                            label = "Play",
                            emoji = "\uD83C\uDFAE",
                            onClick = { isInteracting = true },
                            enabled = !isSleeping,
                            isPrimary = true,
                        )
                        ActionButton(
                            modifier = Modifier.weight(1f),
                            label = "Sleep",
                            emoji = "\uD83D\uDE34",
                            onClick = { showSleepDialog = true },
                            enabled = !isSleeping,
                            isPrimary = true,
                        )
                    }
                }

                // -------------------------------------------------------
                // Daily Quest section header + Habit list
                // -------------------------------------------------------
                if (isLoading) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 32.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            CircularProgressIndicator()
                        }
                    }
                } else if (habits.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 48.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(text = "\uD83D\uDCCB", style = MaterialTheme.typography.displayLarge)
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
                } else {
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
                            isOwnedByCurrentUser = habit.ownerUid == viewModel.currentUserUid,
                            householdCompleterName = householdCompleterNames[habit.id],
                        )
                    }
                }

                item { Spacer(modifier = Modifier.height(80.dp)) }
                } // end LazyColumn
            } // end PullToRefreshBox

            // Snackbar pinned above the nav bar
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

            // Stat detail floating overlay
            AnimatedVisibility(
                visible = showDetailedStats,
                enter = fadeIn(animationSpec = tween(200)),
                exit = fadeOut(animationSpec = tween(200)),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.45f))
                        .clickable(
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() },
                        ) { showDetailedStats = false },
                    contentAlignment = Alignment.Center,
                ) {
                    AnimatedVisibility(
                        visible = showDetailedStats,
                        enter = scaleIn(
                            animationSpec = tween(300, easing = FastOutSlowInEasing),
                            initialScale = 0.82f,
                            transformOrigin = TransformOrigin(0.5f, 0.2f),
                        ) + fadeIn(animationSpec = tween(300)),
                        exit = scaleOut(
                            animationSpec = tween(200, easing = FastOutSlowInEasing),
                            targetScale = 0.82f,
                            transformOrigin = TransformOrigin(0.5f, 0.2f),
                        ) + fadeOut(animationSpec = tween(200)),
                    ) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp)
                                .clickable(
                                    indication = null,
                                    interactionSource = remember { MutableInteractionSource() },
                                ) { /* consume tap — prevent scrim dismissal */ },
                            shape = RoundedCornerShape(24.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface,
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                        ) {
                            Column(
                                modifier = Modifier.padding(20.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                // Header row
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    ) {
                                        Text(
                                            text = if (isSleeping) "\uD83D\uDE34" else mood.emoji,
                                            fontSize = 28.sp,
                                        )
                                        Column {
                                            Text(
                                                text = if (isSleeping) "Sleeping" else mood.displayName,
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface,
                                            )
                                            Text(
                                                text = "${stats.name}'s stats",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            )
                                        }
                                    }
                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                                            .clickable { showDetailedStats = false },
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Close",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(16.dp),
                                        )
                                    }
                                }

                                // Hunger + Joy row
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                ) {
                                    StatBentoCard(
                                        modifier = Modifier.weight(1f),
                                        label = "HUNGER",
                                        emoji = "\uD83C\uDF56",
                                        value = stats.hunger,
                                        statusText = when {
                                            stats.hunger >= 80 -> "Full"
                                            stats.hunger >= 50 -> "Peckish"
                                            stats.hunger >= 25 -> "Hungry"
                                            else -> "Starving!"
                                        },
                                        barColor = MaterialTheme.colorScheme.secondary,
                                    )
                                    StatBentoCard(
                                        modifier = Modifier.weight(1f),
                                        label = "JOY",
                                        emoji = "\uD83D\uDC95",
                                        value = stats.happiness,
                                        statusText = when {
                                            stats.happiness >= 80 -> "Happy"
                                            stats.happiness >= 50 -> "Content"
                                            stats.happiness >= 25 -> "Sad"
                                            else -> "Miserable!"
                                        },
                                        barColor = MaterialTheme.colorScheme.primary,
                                    )
                                }

                                // Energy card
                                EnergyBentoCard(
                                    value = stats.energy,
                                    statusText = when {
                                        stats.energy >= 70 -> "Energized"
                                        stats.energy >= 40 -> "Recharging"
                                        stats.energy >= 15 -> "Needs Nap Soon"
                                        else -> "Exhausted!"
                                    },
                                )

                                // XP Progress card
                                XpProgressCard(
                                    xp = stats.xp,
                                    xpToNextLevel = stats.xpToNextLevel,
                                    xpProgressFraction = stats.xpProgressFraction,
                                )
                            }
                        }
                    }
                }
            }
        }

        // Sleep confirmation dialog
        if (showSleepDialog) {
            AlertDialog(
                onDismissRequest = { showSleepDialog = false },
                title = {
                    Text("Put Choreboo to Sleep?", fontWeight = FontWeight.Bold)
                },
                text = {
                    Text(
                        "Your Choreboo will sleep for 24 hours. During this time, their stats will not decrease and they'll be fully rested! \uD83D\uDE34\n\nAfter 24 hours, normal stat decay will resume.",
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showStartSleepAnimation = true
                            viewModel.sleepChoreboo()
                            showSleepDialog = false
                        },
                    ) {
                        Text("Let Them Sleep")
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { showSleepDialog = false },
                    ) {
                        Text("Cancel")
                    }
                },
            )
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

        // Level-up celebration dialog
        showLevelUpDialog?.let { event ->
            LevelUpDialog(
                event = event,
                onDismiss = { showLevelUpDialog = null },
            )
        }
    }
}

// ---------------------------------------------------------------------------
// XP Progress card (shown in expandable stats)
// ---------------------------------------------------------------------------

@Composable
private fun XpProgressCard(
    xp: Int,
    xpToNextLevel: Int,
    xpProgressFraction: Float,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLowest)
            .padding(14.dp),
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = "XP PROGRESS",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = XpPurple,
                    letterSpacing = 1.sp,
                )
                Text(
                    text = "$xp / $xpToNextLevel",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = XpPurple,
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { xpProgressFraction },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(5.dp)),
                color = XpPurple,
                trackColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Action button for Feed, Play, Sleep interactions
// ---------------------------------------------------------------------------

@Composable
private fun ActionButton(
    label: String,
    emoji: String,
    onClick: () -> Unit,
    enabled: Boolean,
    isPrimary: Boolean,
    modifier: Modifier = Modifier,
) {
    val haptic = LocalHapticFeedback.current
    Box(
        modifier = modifier
            .height(64.dp)
            .clip(RoundedCornerShape(50.dp))
            .background(
                when {
                    !enabled -> MaterialTheme.colorScheme.surfaceContainerHighest
                    isPrimary -> MaterialTheme.colorScheme.primary
                    else -> MaterialTheme.colorScheme.surfaceContainerHigh
                },
            )
            .clickable(enabled = enabled) {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onClick()
            },
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                emoji,
                fontSize = 24.sp,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = when {
                    !enabled -> MaterialTheme.colorScheme.onSurfaceVariant
                    isPrimary -> MaterialTheme.colorScheme.onPrimary
                    else -> MaterialTheme.colorScheme.onSurface
                },
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Small bento stat card for Hunger / Joy
// ---------------------------------------------------------------------------

@Composable
private fun StatBentoCard(
    modifier: Modifier = Modifier,
    label: String,
    emoji: String,
    value: Int,
    statusText: String,
    barColor: Color,
) {
    Box(
        modifier = modifier
            .height(110.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLowest)
            .semantics(mergeDescendants = true) { contentDescription = "$label: $value%, $statusText" },
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(emoji, fontSize = 20.sp)
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = 1.sp,
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom,
                ) {
                    Text(
                        text = "$value%",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = statusText,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth((value / 100f).coerceIn(0f, 1f))
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(barColor),
                    )
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Full-width energy bento card
// ---------------------------------------------------------------------------

@Composable
private fun EnergyBentoCard(
    value: Int,
    statusText: String,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(100.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLowest)
            .semantics(mergeDescendants = true) { contentDescription = "Energy levels: $value%, $statusText" },
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text("\u26A1", fontSize = 20.sp)
                    Text(
                        text = "ENERGY LEVELS",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        letterSpacing = 1.sp,
                    )
                }
                Text(
                    text = statusText.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.tertiary,
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom,
                ) {
                    Text(
                        text = "$value%",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth((value / 100f).coerceIn(0f, 1f))
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(MaterialTheme.colorScheme.tertiary),
                    )
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Level-up celebration dialog
// ---------------------------------------------------------------------------

@Composable
private fun LevelUpDialog(
    event: PetEvent.HabitCompleted,
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
                        text = if (event.evolved) "\uD83C\uDF89 Evolution! \uD83C\uDF89" else "\uD83C\uDF89 Level Up! \uD83C\uDF89",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSecondary,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = if (event.evolved)
                            "Your Choreboo evolved to ${event.newStageName}! \uD83C\uDF1F\nNow Level ${event.newLevel}!"
                        else
                            "Your Choreboo reached Level ${event.newLevel}! \uD83C\uDF1F\nKeep completing habits to evolve!",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSecondary,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    TextButton(onClick = onDismiss) {
                        Text(
                            text = "Awesome! \uD83C\uDF8A",
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

// ---------------------------------------------------------------------------
// Pet Lottie animation state machine
// ---------------------------------------------------------------------------

@Composable
private fun PetAnimation(
    petType: PetType,
    mood: ChorebooMood,
    isEating: Boolean,
    isInteracting: Boolean,
    isSleeping: Boolean,
    showStartSleepAnimation: Boolean,
    showThumbsUp: Boolean,
    onEatingComplete: () -> Unit,
    onInteractComplete: () -> Unit,
    onStartSleepComplete: () -> Unit,
    onThumbsUpComplete: () -> Unit,
    onTap: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (petType == PetType.FOX) {
        var phase by remember { mutableStateOf(AnimationPhase.MOOD) }

        LaunchedEffect(isEating) {
            if (isEating && !isSleeping) {
                phase = AnimationPhase.EATING
            }
        }

        LaunchedEffect(isInteracting) {
            if (isInteracting && !isEating && !isSleeping) {
                phase = AnimationPhase.INTERACTING
            }
        }

        LaunchedEffect(showThumbsUp) {
            if (showThumbsUp && !isEating && !isSleeping) {
                phase = AnimationPhase.THUMBS_UP
            }
        }

        LaunchedEffect(isSleeping) {
            if (isSleeping) {
                phase = if (showStartSleepAnimation) AnimationPhase.START_SLEEPING else AnimationPhase.SLEEPING
            } else if (phase == AnimationPhase.SLEEPING || phase == AnimationPhase.START_SLEEPING) {
                phase = AnimationPhase.MOOD
            }
        }

        // Determine the current animation asset and iteration count based on phase
        val (assetPath, iterations) = when (phase) {
            AnimationPhase.MOOD -> {
                val moodAsset = when (mood) {
                    ChorebooMood.HAPPY,
                    ChorebooMood.CONTENT -> "animations/fox/fox_happy.webp"
                    ChorebooMood.HUNGRY -> "animations/fox/fox_hungry.webp"
                    else -> "animations/fox/fox_sad.webp"
                }
                moodAsset to 1
            }
            AnimationPhase.IDLE -> "animations/fox/fox_idle.webp" to 3
            AnimationPhase.EATING -> "animations/fox/fox_eating.webp" to 1
            AnimationPhase.INTERACTING -> "animations/fox/fox_interact.webp" to 1
            AnimationPhase.THUMBS_UP -> "animations/fox/fox_thumbs_up.webp" to 1
            AnimationPhase.START_SLEEPING -> "animations/fox/fox_start_sleep.webp" to 1
            AnimationPhase.SLEEPING -> "animations/fox/fox_loop_sleeping.webp" to Int.MAX_VALUE
        }

        Crossfade(
            targetState = phase,
            animationSpec = tween(durationMillis = 100),
            label = "petAnimationCrossfade",
        ) { animPhase ->
            val (currentAsset, currentIterations) = when (animPhase) {
                AnimationPhase.MOOD -> {
                    val moodAsset = when (mood) {
                        ChorebooMood.HAPPY,
                        ChorebooMood.CONTENT -> "animations/fox/fox_happy.webp"
                        ChorebooMood.HUNGRY -> "animations/fox/fox_hungry.webp"
                        else -> "animations/fox/fox_sad.webp"
                    }
                    moodAsset to 1
                }
                AnimationPhase.IDLE -> "animations/fox/fox_idle.webp" to 3
                AnimationPhase.EATING -> "animations/fox/fox_eating.webp" to 1
                AnimationPhase.INTERACTING -> "animations/fox/fox_interact.webp" to 1
                AnimationPhase.THUMBS_UP -> "animations/fox/fox_thumbs_up.webp" to 1
                AnimationPhase.START_SLEEPING -> "animations/fox/fox_start_sleep.webp" to 1
                AnimationPhase.SLEEPING -> "animations/fox/fox_loop_sleeping.webp" to Int.MAX_VALUE
            }

            WebmAnimationView(
                assetPath = currentAsset,
                iterations = currentIterations,
                onComplete = {
                    when (animPhase) {
                        AnimationPhase.MOOD -> {
                            if (phase == AnimationPhase.MOOD) phase = AnimationPhase.IDLE
                        }
                        AnimationPhase.IDLE -> {
                            if (phase == AnimationPhase.IDLE) phase = AnimationPhase.MOOD
                        }
                        AnimationPhase.EATING -> {
                            if (phase == AnimationPhase.EATING) {
                                onEatingComplete()
                                phase = AnimationPhase.MOOD
                            }
                        }
                        AnimationPhase.INTERACTING -> {
                            if (phase == AnimationPhase.INTERACTING) {
                                onInteractComplete()
                                phase = AnimationPhase.MOOD
                            }
                        }
                        AnimationPhase.THUMBS_UP -> {
                            if (phase == AnimationPhase.THUMBS_UP) {
                                onThumbsUpComplete()
                                phase = AnimationPhase.MOOD
                            }
                        }
                        AnimationPhase.START_SLEEPING -> {
                            if (phase == AnimationPhase.START_SLEEPING) {
                                onStartSleepComplete()
                                phase = AnimationPhase.SLEEPING
                            }
                        }
                        AnimationPhase.SLEEPING -> {
                            if (phase == AnimationPhase.SLEEPING) {
                                phase = if (isSleeping) AnimationPhase.SLEEPING else AnimationPhase.MOOD
                            }
                        }
                    }
                },
                modifier = modifier.clickable { onTap() },
            )
        }
    } else {
        val sizeMultiplier = 64.sp
        Box(modifier = modifier, contentAlignment = Alignment.Center) {
            Text(
                text = petType.emoji,
                fontSize = sizeMultiplier,
            )
        }
    }
}
