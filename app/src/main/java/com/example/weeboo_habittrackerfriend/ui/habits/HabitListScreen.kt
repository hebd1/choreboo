package com.example.weeboo_habittrackerfriend.ui.habits

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stars
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.weeboo_habittrackerfriend.domain.model.Habit
import com.example.weeboo_habittrackerfriend.ui.habits.components.HabitCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HabitListScreen(
    onAddHabit: () -> Unit,
    onEditHabit: (Long) -> Unit,
    onNavigateToSettings: () -> Unit,
    viewModel: HabitListViewModel = hiltViewModel(),
) {
    val habits by viewModel.habits.collectAsState()
    val totalPoints by viewModel.totalPoints.collectAsState()
    val todayCompletions by viewModel.todayCompletions.collectAsState()
    val streaks by viewModel.streaks.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var habitToDelete by remember { mutableStateOf<Habit?>(null) }

    var showLevelUpDialog by remember { mutableStateOf<HabitListEvent.HabitCompleted?>(null) }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is HabitListEvent.HabitCompleted -> {
                    if (event.leveledUp || event.evolved) {
                        showLevelUpDialog = event
                    }
                    val foodMsg = if (event.foodReward != null) " 🎁 Got ${event.foodReward}!" else ""
                    snackbarHostState.showSnackbar(
                        message = "+${event.xpEarned} XP! \uD83D\uDD25 ${event.streak}-day streak!$foodMsg",
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "My Habits",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                    )
                },
                actions = {
                    // Points display
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(end = 4.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Default.Stars,
                            contentDescription = "Points",
                            tint = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.size(20.dp),
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "$totalPoints",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.tertiary,
                        )
                    }
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings",
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
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = RoundedCornerShape(16.dp),
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Habit")
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
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
                        text = "Tap + to create your first habit\nand start earning rewards for your Weeboo!",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
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
                                "Your Weeboo evolved to ${event.newStageName}! 🌟\nNow Level ${event.newLevel}!"
                            else
                                "Your Weeboo reached Level ${event.newLevel}! 🌟\nKeep completing habits to evolve!",
                            style = MaterialTheme.typography.bodyLarge,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
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

