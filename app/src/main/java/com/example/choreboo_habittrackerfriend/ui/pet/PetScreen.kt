package com.example.choreboo_habittrackerfriend.ui.pet

import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Stars
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieAnimatable
import com.airbnb.lottie.compose.rememberLottieComposition
import com.example.choreboo_habittrackerfriend.domain.model.ChorebooMood
import com.example.choreboo_habittrackerfriend.domain.model.PetType
import com.example.choreboo_habittrackerfriend.ui.pet.components.StatBar
import com.example.choreboo_habittrackerfriend.ui.theme.GradientUtils
import com.example.choreboo_habittrackerfriend.ui.theme.PetMoodContentEnd
import com.example.choreboo_habittrackerfriend.ui.theme.PetMoodContentStart
import com.example.choreboo_habittrackerfriend.ui.theme.PetMoodHappyEnd
import com.example.choreboo_habittrackerfriend.ui.theme.PetMoodHappyStart
import com.example.choreboo_habittrackerfriend.ui.theme.PetMoodHungryEnd
import com.example.choreboo_habittrackerfriend.ui.theme.PetMoodHungryStart
import com.example.choreboo_habittrackerfriend.ui.theme.PetMoodSadEnd
import com.example.choreboo_habittrackerfriend.ui.theme.PetMoodSadStart
import com.example.choreboo_habittrackerfriend.ui.theme.PetMoodTiredEnd
import com.example.choreboo_habittrackerfriend.ui.theme.PetMoodTiredStart
import com.example.choreboo_habittrackerfriend.ui.theme.XpPurple

private enum class AnimationPhase { MOOD, IDLE, EATING, INTERACTING }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PetScreen(
    viewModel: PetViewModel = hiltViewModel(),
) {
    val choreboo by viewModel.chorebooState.collectAsState()
    val mood by viewModel.currentMood.collectAsState()
    val totalPoints by viewModel.totalPoints.collectAsState()
    val isEating by viewModel.isEating.collectAsState()
    val isSleeping by viewModel.isSleeping.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var showSleepDialog by remember { mutableStateOf(false) }
    var isInteracting by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is PetEvent.Fed -> {
                    snackbarHostState.showSnackbar(
                        "Yum! Your Choreboo loved that! 😋",
                        duration = SnackbarDuration.Short,
                    )
                }
                is PetEvent.InsufficientPoints -> {
                    snackbarHostState.showSnackbar(
                        "Not enough points to feed! Complete habits to earn more. 💪",
                        duration = SnackbarDuration.Short,
                    )
                }
                is PetEvent.Sleeping -> {
                    snackbarHostState.showSnackbar(
                        "Your Choreboo is now sleeping! 😴 Stats are frozen for 24 hours.",
                        duration = SnackbarDuration.Short,
                    )
                }
                is PetEvent.AlreadySleeping -> {
                    snackbarHostState.showSnackbar(
                        "Your Choreboo is already sleeping! Let them rest. 💤",
                        duration = SnackbarDuration.Short,
                    )
                }
            }
        }
    }

    // Mood-based radial gradient bg
    val moodBgColors by animateColorAsState(
        targetValue = when (mood) {
            ChorebooMood.HAPPY -> PetMoodHappyEnd
            ChorebooMood.CONTENT -> PetMoodContentEnd
            ChorebooMood.HUNGRY -> PetMoodHungryEnd
            ChorebooMood.TIRED -> PetMoodTiredEnd
            ChorebooMood.SAD -> PetMoodSadEnd
            ChorebooMood.IDLE -> MaterialTheme.colorScheme.surfaceContainerHigh
        },
        label = "petBgColor",
    )
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "My Choreboo",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                },
                actions = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .padding(end = 16.dp)
                            .clip(RoundedCornerShape(50.dp))
                            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                    ) {
                        Icon(
                            Icons.Default.Stars,
                            contentDescription = "Points",
                            tint = MaterialTheme.colorScheme.secondary,
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
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface),
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        if (choreboo == null) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("Loading your Choreboo...")
            }
            return@Scaffold
        }

        val stats = choreboo!!
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(modifier = Modifier.height(16.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            Brush.radialGradient(
                                colors = listOf(moodBgStart, moodBgColors),
                                radius = 600f,
                            ),
                        )
                        .border(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.outlineVariant,
                            shape = RoundedCornerShape(16.dp),
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    // Lottie animation — only fox has real animations for now
                    PetAnimation(
                        petType = stats.petType,
                        mood = mood,
                        isEating = isEating,
                        isInteracting = isInteracting,
                        onEatingComplete = { viewModel.onEatingAnimationComplete() },
                        onInteractComplete = { isInteracting = false },
                        onTap = { isInteracting = true },
                        modifier = Modifier.size(160.dp),
                    )

                    // Mood pill at bottom
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 10.dp)
                            .clip(RoundedCornerShape(50.dp))
                            .background(MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.8f))
                            .padding(horizontal = 14.dp, vertical = 6.dp),
                    ) {
                        Text(
                            text = "${mood.emoji} ${mood.displayName.uppercase()}",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }

                // Level badge — top-right
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
                        Text("⭐", fontSize = 18.sp)
                        Text(
                            text = "Lv.${stats.level}",
                            fontWeight = FontWeight.Black,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Action row: Feed, Play, Sleep
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item {
                    ActionButton(
                        label = "Feed",
                        emoji = "🍖",
                        onClick = { viewModel.feedChoreboo() },
                        enabled = totalPoints >= 10,
                    )
                }
                item {
                    ActionButton(
                        label = "Play",
                        emoji = "🎮",
                        onClick = { isInteracting = true },
                        enabled = true,
                    )
                }
                item {
                    ActionButton(
                        label = "Sleep",
                        emoji = "😴",
                        onClick = { showSleepDialog = true },
                        enabled = !isSleeping,
                    )
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
                            "Your Choreboo will sleep for 24 hours. During this time, their stats will not decrease and they'll be fully rested! 😴\n\nAfter 24 hours, normal stat decay will resume.",
                        )
                    },
                    confirmButton = {
                        Button(
                            onClick = {
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

            Spacer(modifier = Modifier.height(20.dp))

            // Name & XP card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom,
                    ) {
                        Column {
                            Text(
                                text = stats.name,
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.ExtraBold,
                            )
                            Text(
                                text = "${stats.stage.displayName} ${stats.petType.emoji}",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Text(
                            text = "Lv. ${stats.level}",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Black,
                            color = XpPurple,
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

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
                            text = "${stats.xp} / ${stats.xpToNextLevel}",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = XpPurple,
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    LinearProgressIndicator(
                        progress = { stats.xpProgressFraction },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(5.dp)),
                        color = XpPurple,
                        trackColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Stats bento card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    StatBar(label = "Hunger", value = stats.hunger, emoji = "🍖", statType = "hunger")
                    StatBar(label = "Happiness", value = stats.happiness, emoji = "💕", statType = "happiness")
                    StatBar(label = "Energy", value = stats.energy, emoji = "⚡", statType = "energy")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Feed button
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .height(48.dp)
                    .clip(CircleShape)
                    .background(
                        if (totalPoints >= 10) {
                            Brush.linearGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.primary,
                                    MaterialTheme.colorScheme.primaryContainer,
                                ),
                            )
                        } else {
                            Brush.linearGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.surfaceContainerHighest,
                                    MaterialTheme.colorScheme.surfaceContainerHighest,
                                ),
                            )
                        }
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Button(
                    onClick = { viewModel.feedChoreboo() },
                    enabled = totalPoints >= 10,
                    modifier = Modifier.fillMaxSize(),
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Transparent,
                        disabledContainerColor = Color.Transparent,
                    ),
                    elevation = null,
                ) {
                    Icon(
                        Icons.Default.Restaurant,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = if (totalPoints >= 10) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (totalPoints >= 10) "Feed (10 pts)" else "Feed (need 10 pts)",
                        fontWeight = FontWeight.Bold,
                        color = if (totalPoints >= 10) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

/**
 * Action button for Feed, Play, Sleep interactions.
 */
@Composable
private fun ActionButton(
    label: String,
    emoji: String,
    onClick: () -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .width(80.dp)
            .height(80.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(
                if (enabled) GradientUtils.secondaryGradient() 
                else Brush.linearGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.surfaceContainerHighest,
                        MaterialTheme.colorScheme.surfaceContainerHighest,
                    ),
                ),
            )
            .clickable(enabled = enabled) { onClick() },
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                emoji,
                fontSize = 26.sp,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = if (enabled) MaterialTheme.colorScheme.onSecondary else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/**
 * Renders the correct Lottie animation for the pet type and mood with phase-based state machine.
 * - FOX: cycles through MOOD → IDLE (×3) → MOOD, with EATING and INTERACTING as interrupts
 * - Other pets: falls back to emoji placeholder until animations are added
 *
 * Animation phases:
 * - MOOD: displays mood-specific animation (happy/hungry/sad) once
 * - IDLE: displays idle animation 3 times in a row
 * - EATING: displays eating animation once (triggered by feed)
 * - INTERACTING: displays interact animation once (triggered by play or tap)
 *
 * Transitions fade between phases with a 100ms crossfade.
 */
@Composable
private fun PetAnimation(
    petType: PetType,
    mood: ChorebooMood,
    isEating: Boolean,
    isInteracting: Boolean,
    onEatingComplete: () -> Unit,
    onInteractComplete: () -> Unit,
    onTap: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (petType == PetType.FOX) {
        // State for animation phase cycling
        var phase by remember { mutableStateOf(AnimationPhase.MOOD) }

        // Preload all 6 compositions eagerly for instant switching
        val happyComposition by rememberLottieComposition(
            LottieCompositionSpec.Asset("animations/fox/fox_happy_lottie.json"),
        )
        val hungryComposition by rememberLottieComposition(
            LottieCompositionSpec.Asset("animations/fox/fox_hungry_lottie.json"),
        )
        val sadComposition by rememberLottieComposition(
            LottieCompositionSpec.Asset("animations/fox/fox_sad_lottie.json"),
        )
        val idleComposition by rememberLottieComposition(
            LottieCompositionSpec.Asset("animations/fox/fox_idle_lottie.json"),
        )
        val eatingComposition by rememberLottieComposition(
            LottieCompositionSpec.Asset("animations/fox/fox_eating_lottie.json"),
        )
        val interactComposition by rememberLottieComposition(
            LottieCompositionSpec.Asset("animations/fox/fox_interact_lottie.json"),
        )

        // Lottie animatable for manual control over animation playback
        val lottieAnimatable = rememberLottieAnimatable()

        // Handle external triggers: isEating or isInteracting
        LaunchedEffect(isEating) {
            if (isEating) {
                phase = AnimationPhase.EATING
            }
        }

        LaunchedEffect(isInteracting) {
            if (isInteracting && !isEating) {
                phase = AnimationPhase.INTERACTING
            }
        }

        // Resolve composition for the current phase outside LaunchedEffect so it can be a key.
        // This is critical: compositions load asynchronously, so on first composition the
        // LaunchedEffect fires while the composition is still null and skips the animation.
        // By including the resolved composition as a key, the effect re-triggers the moment
        // the asset finishes loading, ensuring the initial mood animation always plays.
        val currentPhaseComposition = when (phase) {
            AnimationPhase.MOOD -> when (mood) {
                ChorebooMood.HAPPY,
                ChorebooMood.CONTENT -> happyComposition
                ChorebooMood.HUNGRY -> hungryComposition
                else -> sadComposition // SAD, TIRED, IDLE
            }
            AnimationPhase.IDLE -> idleComposition
            AnimationPhase.EATING -> eatingComposition
            AnimationPhase.INTERACTING -> interactComposition
        }

        // State machine: advance phase when animation completes.
        // Keyed on both phase and currentPhaseComposition so the effect re-runs when the
        // Lottie asset finishes loading (null -> loaded), not just when phase changes.
        LaunchedEffect(phase, currentPhaseComposition) {
            if (currentPhaseComposition != null) {
                // Play animation and wait for it to complete
                lottieAnimatable.animate(
                    composition = currentPhaseComposition,
                    iterations = when (phase) {
                        AnimationPhase.MOOD -> 1
                        AnimationPhase.IDLE -> 3
                        AnimationPhase.EATING -> 1
                        AnimationPhase.INTERACTING -> 1
                    },
                )

                // After animation completes, transition to next phase
                when (phase) {
                    AnimationPhase.MOOD -> phase = AnimationPhase.IDLE
                    AnimationPhase.IDLE -> phase = AnimationPhase.MOOD
                    AnimationPhase.EATING -> {
                        onEatingComplete()
                        phase = AnimationPhase.MOOD
                    }
                    AnimationPhase.INTERACTING -> {
                        onInteractComplete()
                        phase = AnimationPhase.MOOD
                    }
                }
            }
        }

        // Render animation with crossfade between phases
        Crossfade(
            targetState = phase,
            animationSpec = tween(durationMillis = 100),
            label = "petAnimationCrossfade",
        ) { animPhase ->
            val currentComposition = when (animPhase) {
                AnimationPhase.MOOD -> {
                    when (mood) {
                        ChorebooMood.HAPPY,
                        ChorebooMood.CONTENT -> happyComposition
                        ChorebooMood.HUNGRY -> hungryComposition
                        else -> sadComposition // SAD, TIRED, IDLE
                    }
                }
                AnimationPhase.IDLE -> idleComposition
                AnimationPhase.EATING -> eatingComposition
                AnimationPhase.INTERACTING -> interactComposition
            }

            LottieAnimation(
                composition = currentComposition,
                progress = { lottieAnimatable.progress },
                modifier = modifier.clickable { onTap() },
            )
        }
    } else {
        // Placeholder emoji for bear/penguin/cat until their Lottie files are added
        val sizeMultiplier = 64.sp
        Box(modifier = modifier, contentAlignment = Alignment.Center) {
            Text(
                text = petType.emoji,
                fontSize = sizeMultiplier,
            )
        }
    }
}
