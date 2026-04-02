package com.example.choreboo_habittrackerfriend.ui.habits

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stars
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarData
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.rememberLottieAnimatable
import com.airbnb.lottie.compose.rememberLottieComposition
import com.example.choreboo_habittrackerfriend.domain.model.Habit
import com.example.choreboo_habittrackerfriend.ui.habits.components.HabitCard
import com.example.choreboo_habittrackerfriend.ui.theme.GradientUtils
import com.example.choreboo_habittrackerfriend.ui.components.ProfileAvatar
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val COMPLETION_ANIMATION_COOLDOWN_MS = 30_000L

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HabitListScreen(
    onAddHabit: () -> Unit,
    onEditHabit: (Long) -> Unit,
    viewModel: HabitListViewModel = hiltViewModel(),
) {
    val habits by viewModel.habits.collectAsState()
    val totalPoints by viewModel.totalPoints.collectAsState()
    val profilePhotoUri by viewModel.profilePhotoUri.collectAsState()
    val todayCompletions by viewModel.todayCompletions.collectAsState()
    val streaks by viewModel.streaks.collectAsState()
    val petType by viewModel.petType.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var habitToDelete by remember { mutableStateOf<Habit?>(null) }
    var showLevelUpDialog by remember { mutableStateOf<HabitListEvent.HabitCompleted?>(null) }

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
            val animDurationMs = completionComposition!!.duration.toLong()
            val hideDelay = (animDurationMs - 2000L).coerceAtLeast(0L)

            val animJob = launch {
                completionAnimatable.animate(composition = completionComposition, iterations = 1)
            }

            delay(hideDelay)
            isOverlayVisible = false

            animJob.join()
            lastAnimationEndTime = System.currentTimeMillis()
            showCompletionAnimation = false
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

    // Compute "Weekly Narrative" hero stats
    val scheduledToday = habits.filter { it.isScheduledForToday() }
    val completedToday = scheduledToday.count { (todayCompletions[it.id] ?: 0) >= 1 }
    val completionFraction = if (scheduledToday.isEmpty()) 0f
    else (completedToday.toFloat() / scheduledToday.size).coerceIn(0f, 1f)
    val completionPct = (completionFraction * 100).toInt()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Profile avatar circle
                        ProfileAvatar(
                            profilePhotoUri = profilePhotoUri,
                            googlePhotoUrl = viewModel.googlePhotoUrl,
                            size = 40.dp,
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "My Habits",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                },
                actions = {
                    // Points pill — secondaryFixed orange background
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(50.dp))
                            .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.25f))
                            .padding(horizontal = 10.dp, vertical = 4.dp),
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
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddHabit,
                containerColor = MaterialTheme.colorScheme.secondary,
                contentColor = MaterialTheme.colorScheme.onSecondary,
                shape = RoundedCornerShape(16.dp),
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Habit")
            }
        },
        snackbarHost = {
            SnackbarHost(snackbarHostState) { data ->
                StitchSnackbar(data)
            }
        },
    ) { padding ->
        AnimatedVisibility(
            visible = habits.isEmpty(),
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
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

        if (habits.isNotEmpty()) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item { Spacer(modifier = Modifier.height(8.dp)) }

                // "Weekly Narrative" hero card
                item {
                    WeeklyNarrativeCard(
                        completionPct = completionPct,
                        completionFraction = completionFraction,
                    )
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
                item { Spacer(modifier = Modifier.height(80.dp)) }
            }
        }

        // Overscreen completion animation — renders above the habit list but below the snackbar
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
                        .height(400.dp)
                        .offset(y = 80.dp),
                )
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

        // Level-up celebration dialog
        showLevelUpDialog?.let { event ->
            AlertDialog(
                onDismissRequest = { showLevelUpDialog = null },
                title = {
                    Text(
                        text = if (event.evolved) "🎉 Evolution! 🎉" else "🎉 Level Up! 🎉",
                        fontWeight = FontWeight.Bold,
                    )
                },
                text = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = if (event.evolved)
                                "Your Choreboo evolved to ${event.newStageName}! 🌟\nNow Level ${event.newLevel}!"
                            else
                                "Your Choreboo reached Level ${event.newLevel}! 🌟\nKeep completing habits to evolve!",
                            style = MaterialTheme.typography.bodyLarge,
                            textAlign = TextAlign.Center,
                        )
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showLevelUpDialog = null }) {
                        Text("Awesome! 🎊")
                    }
                },
            )
        }
    }
}

@Composable
private fun WeeklyNarrativeCard(
    completionPct: Int,
    completionFraction: Float,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(GradientUtils.primaryGradient())
    ) {
        // Decorative circle (bottom-right)
        Box(
            modifier = Modifier
                .size(100.dp)
                .align(Alignment.BottomEnd)
                .background(
                    MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.1f),
                    shape = CircleShape
                )
        )
        
        Column(modifier = Modifier.padding(24.dp)) {
            // Label
            Text(
                text = "THE VERDANT FOREST",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.75f),
                letterSpacing = 1.5.sp,
            )
            Spacer(modifier = Modifier.height(8.dp))
            
            // Title
            Text(
                text = if (completionPct >= 80) "You're on Fire! 🔥"
                else if (completionPct >= 50) "Keep Going! 💪"
                else "Start Your Quest! ⚡",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onPrimary,
            )
            Spacer(modifier = Modifier.height(12.dp))
            
            // Progress bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.25f)),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(completionFraction)
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(MaterialTheme.colorScheme.onPrimary),
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            
            // Progress text
            Text(
                text = "$completionPct% of your daily quests finished.",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onPrimary,
            )
        }
    }
}

@Composable
private fun StitchSnackbar(data: SnackbarData) {
    Row(
        modifier = Modifier
            .padding(start = 16.dp, end = 16.dp, bottom = 8.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(50.dp))
            .background(
                MaterialTheme.colorScheme.inverseSurface.copy(alpha = 0.92f),
            )
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.secondaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            Text("🎉", fontSize = 18.sp)
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Achievement",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.inverseOnSurface.copy(alpha = 0.6f),
                letterSpacing = 0.8.sp,
            )
            Text(
                text = data.visuals.message,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.inverseOnSurface,
            )
        }
    }
}
