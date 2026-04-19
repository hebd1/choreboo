package com.choreboo.app.ui.pet

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
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Stars
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import com.choreboo.app.R
import com.choreboo.app.domain.model.ChorebooMood
import com.choreboo.app.domain.model.Habit
import com.choreboo.app.domain.model.PetType
import com.choreboo.app.ui.components.BannerAdView
import com.choreboo.app.ui.components.ChorebooTopAppBar
import com.choreboo.app.ui.components.ConfettiOverlay
import com.choreboo.app.ui.components.FOX_ANIM_EATING
import com.choreboo.app.ui.components.FOX_ANIM_IDLE
import com.choreboo.app.ui.components.FOX_ANIM_INTERACT
import com.choreboo.app.ui.components.FOX_ANIM_LOOP_SLEEPING
import com.choreboo.app.ui.components.FOX_ANIM_START_SLEEP
import com.choreboo.app.ui.components.FOX_ANIM_THUMBS_UP
import com.choreboo.app.ui.components.FOX_IDLE_ITERATIONS
import com.choreboo.app.ui.components.PANDA_ANIM_EATING
import com.choreboo.app.ui.components.PANDA_ANIM_IDLE
import com.choreboo.app.ui.components.PANDA_ANIM_INTERACT
import com.choreboo.app.ui.components.PANDA_ANIM_LOOP_SLEEPING
import com.choreboo.app.ui.components.PANDA_ANIM_START_SLEEP
import com.choreboo.app.ui.components.PANDA_ANIM_THUMBS_UP
import com.choreboo.app.ui.components.PANDA_IDLE_ITERATIONS
import com.choreboo.app.ui.components.PetBackgroundImage
import com.choreboo.app.ui.components.PremiumBadge
import com.choreboo.app.ui.components.ShimmerPlaceholder
import com.choreboo.app.ui.components.foxMoodAssetPath
import com.choreboo.app.ui.components.pandaMoodAssetPath
import com.choreboo.app.ui.components.SnackbarType
import com.choreboo.app.ui.components.StitchSnackbar
import com.choreboo.app.ui.components.showStitchSnackbar
import com.choreboo.app.ui.components.WebmAnimationView
import com.choreboo.app.ui.habits.components.HabitCard
import com.choreboo.app.ui.pet.components.BackgroundPickerSheet
import com.choreboo.app.ui.theme.PetMoodContentStart
import com.choreboo.app.ui.theme.PetMoodDarkContentStart
import com.choreboo.app.ui.theme.PetMoodDarkHappyStart
import com.choreboo.app.ui.theme.PetMoodDarkHungryStart
import com.choreboo.app.ui.theme.PetMoodDarkSadStart
import com.choreboo.app.ui.theme.PetMoodDarkTiredStart
import com.choreboo.app.ui.theme.PetMoodHappyStart
import com.choreboo.app.ui.theme.PetMoodHungryStart
import com.choreboo.app.ui.theme.PetMoodSadStart
import com.choreboo.app.ui.theme.PetMoodTiredStart
import com.choreboo.app.ui.theme.XpPurple
import com.choreboo.app.ui.util.displayName
import com.choreboo.app.ui.util.labelRes
import com.choreboo.app.ui.util.hasLocalizedLabel
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

private enum class AnimationPhase { MOOD, IDLE, EATING, INTERACTING, THUMBS_UP, START_SLEEPING, SLEEPING }

private val PET_SCENE_OFFSET_X = 12.dp
private val PET_SCENE_OFFSET_Y = 34.dp

private fun formatNextScheduledDay(nextDate: LocalDate, today: LocalDate): String {
    val locale = Locale.getDefault()
    return if (!nextDate.isAfter(today.plusDays(6))) {
        nextDate.dayOfWeek.getDisplayName(TextStyle.FULL, locale)
    } else {
        nextDate.format(DateTimeFormatter.ofPattern("MMM d", locale))
    }
}

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
    val initError by viewModel.initError.collectAsStateWithLifecycle()

    // Habit state
    val habits by viewModel.habits.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val todayCompletions by viewModel.todayCompletions.collectAsStateWithLifecycle()
    val streaks by viewModel.streaks.collectAsStateWithLifecycle()
    val householdCompleterNames by viewModel.householdCompleterNames.collectAsStateWithLifecycle()
    val todayLocalDate by viewModel.todayLocalDate.collectAsStateWithLifecycle()

    // Background state
    val backgroundId by viewModel.backgroundId.collectAsStateWithLifecycle()
    val unlockedBackgroundIds by viewModel.unlockedBackgroundIds.collectAsStateWithLifecycle()
    val backgroundCatalogue by viewModel.backgroundCatalogue.collectAsStateWithLifecycle()
    val isPremium by viewModel.isPremium.collectAsStateWithLifecycle()

    val context = LocalContext.current

    // Local UI state
    val snackbarHostState = remember { SnackbarHostState() }
    var showSleepDialog by rememberSaveable { mutableStateOf(false) }
    var showDetailedStats by rememberSaveable { mutableStateOf(false) }
    var showLevelUpDialog by remember { mutableStateOf<PetEvent.HabitCompleted?>(null) }
    var isInteracting by remember { mutableStateOf(false) }
    var showStartSleepAnimation by remember { mutableStateOf(false) }
    var showThumbsUp by remember { mutableStateOf(false) }
    var showBackgroundPicker by rememberSaveable { mutableStateOf(false) }
    var resubscribeNudgeDismissed by rememberSaveable { mutableStateOf(false) }

    // True when user has a premium pet but no active subscription
    val showResubscribeNudge = !isPremium && petType.isPremium && !resubscribeNudgeDismissed

    // Event collector
    val fedMsg = stringResource(R.string.pet_snack_fed)
    val notEnoughPointsMsg = stringResource(R.string.pet_snack_not_enough_points)
    val sleepingNowMsg = stringResource(R.string.pet_snack_sleeping_now)
    val alreadySleepingMsg = stringResource(R.string.pet_snack_already_sleeping)
    val habitCompletedFmt = stringResource(R.string.pet_snack_habit_completed)
    val alreadyCompletedMsg = stringResource(R.string.pet_snack_already_completed)
    val completionErrorMsg = stringResource(R.string.pet_snack_completion_error_generic)
    val bgUnlockedFmt = stringResource(R.string.pet_snack_background_unlocked)
    val habitCreatedMsg = stringResource(R.string.pet_snack_habit_created)
    val feedErrorMsg = stringResource(R.string.pet_snack_feed_error)
    val sleepErrorMsg = stringResource(R.string.pet_snack_sleep_error)
    val deleteErrorMsg = stringResource(R.string.pet_snack_delete_error)
    val purchaseErrorMsg = stringResource(R.string.pet_snack_purchase_error)
    val nextScheduledFmt = stringResource(R.string.habit_card_next_scheduled)
    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is PetEvent.Fed -> {
                    snackbarHostState.showStitchSnackbar(
                        message = fedMsg,
                        type = SnackbarType.Success,
                    )
                }
                is PetEvent.InsufficientPoints -> {
                    snackbarHostState.showStitchSnackbar(
                        message = notEnoughPointsMsg,
                        type = SnackbarType.Error,
                    )
                }
                is PetEvent.Sleeping -> {
                    snackbarHostState.showStitchSnackbar(
                        message = sleepingNowMsg,
                        type = SnackbarType.Info,
                    )
                }
                is PetEvent.AlreadySleeping -> {
                    snackbarHostState.showStitchSnackbar(
                        message = alreadySleepingMsg,
                        type = SnackbarType.Info,
                    )
                }
                is PetEvent.HabitCompleted -> {
                    showThumbsUp = true
                    if (event.leveledUp || event.evolved) {
                        showLevelUpDialog = event
                    }
                }
                is PetEvent.AlreadyComplete -> {
                    snackbarHostState.showStitchSnackbar(
                        message = alreadyCompletedMsg,
                        type = SnackbarType.Info,
                    )
                }
                is PetEvent.CompletionError -> {
                    snackbarHostState.showStitchSnackbar(
                        message = completionErrorMsg,
                        type = SnackbarType.Error,
                    )
                }
                is PetEvent.FeedError -> {
                    snackbarHostState.showStitchSnackbar(
                        message = feedErrorMsg,
                        type = SnackbarType.Error,
                    )
                }
                is PetEvent.SleepError -> {
                    snackbarHostState.showStitchSnackbar(
                        message = sleepErrorMsg,
                        type = SnackbarType.Error,
                    )
                }
                is PetEvent.DeleteError -> {
                    snackbarHostState.showStitchSnackbar(
                        message = deleteErrorMsg,
                        type = SnackbarType.Error,
                    )
                }
                is PetEvent.PurchaseError -> {
                    snackbarHostState.showStitchSnackbar(
                        message = purchaseErrorMsg,
                        type = SnackbarType.Error,
                    )
                }
                is PetEvent.BackgroundPurchased -> {
                    val label = context.getString(event.item.labelRes())
                    snackbarHostState.showStitchSnackbar(
                        message = bgUnlockedFmt.format(event.item.emoji, label),
                        type = SnackbarType.Success,
                    )
                }
            }
        }
    }

    // Habit creation success snackbar
    LaunchedEffect(habitJustCreated) {
        if (habitJustCreated) {
            snackbarHostState.showStitchSnackbar(
                message = habitCreatedMsg,
                type = SnackbarType.Success,
            )
            onHabitCreatedConsumed()
        }
    }

    val isDark = isSystemInDarkTheme()
    val moodBgStart by animateColorAsState(
        targetValue = if (isDark) {
            when (mood) {
                ChorebooMood.HAPPY -> PetMoodDarkHappyStart
                ChorebooMood.CONTENT -> PetMoodDarkContentStart
                ChorebooMood.HUNGRY -> PetMoodDarkHungryStart
                ChorebooMood.TIRED -> PetMoodDarkTiredStart
                ChorebooMood.SAD -> PetMoodDarkSadStart
                ChorebooMood.IDLE -> MaterialTheme.colorScheme.surfaceContainerLow
            }
        } else {
            when (mood) {
                ChorebooMood.HAPPY -> PetMoodHappyStart
                ChorebooMood.CONTENT -> PetMoodContentStart
                ChorebooMood.HUNGRY -> PetMoodHungryStart
                ChorebooMood.TIRED -> PetMoodTiredStart
                ChorebooMood.SAD -> PetMoodSadStart
                ChorebooMood.IDLE -> MaterialTheme.colorScheme.surfaceContainerLow
            }
        },
        label = "petBgStart",
    )

    // Compute daily quest stats
    val scheduledToday = remember(habits, todayLocalDate) { habits.filter { it.isScheduledForToday(todayLocalDate) } }
    val completedToday = remember(scheduledToday, todayCompletions) {
        scheduledToday.count { (todayCompletions[it.id] ?: 0) >= 1 }
    }

    Scaffold(
        topBar = {
            ChorebooTopAppBar(
                profilePhotoUri = profilePhotoUri,
                googlePhotoUrl = googlePhotoUrl,
                totalPoints = totalPoints,
                pointsContentDescription = stringResource(R.string.pet_points_cd),
            ) {
                if (choreboo != null) {
                    Text(
                        choreboo!!.name,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    PremiumBadge(isPremium = isPremium)
                } else {
                    ShimmerPlaceholder(
                        width = 120.dp,
                        height = 32.dp,
                    )
                }
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddHabit,
                containerColor = MaterialTheme.colorScheme.secondary,
                contentColor = MaterialTheme.colorScheme.onSecondary,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.padding(bottom = 16.dp),
            ) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.pet_add_habit_cd))
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) { data -> StitchSnackbar(data) } },
    ) { padding ->
        if (choreboo == null) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                if (initError) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Text(
                            stringResource(R.string.pet_load_error),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Button(onClick = { viewModel.retryInit() }) {
                            Text(stringResource(R.string.pet_retry_button))
                        }
                    }
                } else {
                    Text(stringResource(R.string.pet_loading))
                }
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
                                .clip(RoundedCornerShape(16.dp)),
                        ) {
                            // Background layer — fills entire clipped box
                            PetBackgroundImage(
                                backgroundId = backgroundId,
                                mood = mood,
                                moodColor = moodBgStart,
                            )
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
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .offset(x = PET_SCENE_OFFSET_X, y = PET_SCENE_OFFSET_Y)
                                    .size(160.dp),
                            )

                             // Mood pill at bottom — tappable to show stat overlay
                             Box(
                                 modifier = Modifier
                                    .align(Alignment.BottomStart)
                                    .padding(start = 12.dp, bottom = 10.dp)
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
                                                    text = if (isSleeping) "\uD83D\uDE34 ${stringResource(R.string.pet_sleeping_label)}" else "${mood.emoji} ${mood.displayName().uppercase()}",
                                                    style = MaterialTheme.typography.labelMedium,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.onSurface,
                                                )
                                    Icon(
                                        imageVector = Icons.Default.ExpandMore,
                                        contentDescription = stringResource(R.string.pet_view_stats_cd),
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
                                     text = stringResource(R.string.level_badge, stats.level),
                                     fontWeight = FontWeight.Black,
                                     style = MaterialTheme.typography.titleMedium,
                                     color = MaterialTheme.colorScheme.onSecondaryContainer,
                                 )
                            }
                        }

                        // Background edit button — bottom-end, outside the clipped box
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .offset(x = 4.dp, y = 4.dp)
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.92f))
                                .clickable { showBackgroundPicker = true },
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = Icons.Default.Palette,
                                contentDescription = stringResource(R.string.pet_change_background_cd),
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(18.dp),
                            )
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
                            label = stringResource(R.string.pet_feed_button),
                            emoji = "\uD83C\uDF56",
                            onClick = { viewModel.feedChoreboo() },
                            enabled = totalPoints >= 10 && !isSleeping,
                            isPrimary = true,
                        )
                        ActionButton(
                            modifier = Modifier.weight(1f),
                            label = stringResource(R.string.pet_play_button),
                            emoji = "\uD83C\uDFAE",
                            onClick = { isInteracting = true },
                            enabled = !isSleeping,
                            isPrimary = true,
                        )
                        ActionButton(
                            modifier = Modifier.weight(1f),
                            label = stringResource(R.string.pet_sleep_button),
                            emoji = "\uD83D\uDE34",
                            onClick = { showSleepDialog = true },
                            enabled = !isSleeping,
                            isPrimary = true,
                        )
                    }
                }

                // -------------------------------------------------------
                // Lapsed-premium resubscribe nudge
                // -------------------------------------------------------
                if (showResubscribeNudge) {
                    item {
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                            ),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                Text("👑", style = MaterialTheme.typography.titleLarge)
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        stringResource(R.string.pet_resubscribe),
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                                    )
                                    Text(
                                        stringResource(R.string.pet_resubscribe_body, petType.name.lowercase().replaceFirstChar { it.uppercase() }),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                                    )
                                }
                                TextButton(
                                    onClick = {
                                        val activity = context as? android.app.Activity
                                        if (activity != null) {
                                            viewModel.launchPremiumPurchase(activity)
                                        }
                                    },
                                ) {
                                    Text(
                                        stringResource(R.string.pet_subscribe_button),
                                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                                        fontWeight = FontWeight.Bold,
                                    )
                                }
                                IconButton(onClick = { resubscribeNudgeDismissed = true }) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = stringResource(R.string.pet_dismiss_cd),
                                        tint = MaterialTheme.colorScheme.onTertiaryContainer,
                                        modifier = Modifier.size(18.dp),
                                    )
                                }
                            }
                        }
                    }
                }

                // -------------------------------------------------------
                // Daily Quest section header + Habit list
                // -------------------------------------------------------
                // Banner ad — only shown for free-tier users
                item {
                    BannerAdView(isPremium = isPremium)
                }

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
                                    text = stringResource(R.string.pet_no_habits_title),
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.SemiBold,
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = stringResource(R.string.pet_no_habits_body),
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
                                text = stringResource(R.string.pet_daily_quest),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Text(
                                text = stringResource(R.string.pet_progress_summary, completedToday, scheduledToday.size),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }

                    items(habits, key = { it.id }) { habit ->
                        val isScheduledToday = habit.isScheduledForToday(todayLocalDate)
                        HabitCard(
                            habit = habit,
                            completedToday = todayCompletions[habit.id] ?: 0,
                            currentStreak = streaks[habit.id] ?: 0,
                            isScheduledToday = isScheduledToday,
                            nextScheduledLabel = remember(habit.customDays, todayLocalDate, isScheduledToday) {
                                if (isScheduledToday) {
                                    null
                                } else {
                                    habit.nextScheduledDate(todayLocalDate)?.let { nextDate ->
                                        nextScheduledFmt.format(
                                            formatNextScheduledDay(nextDate, todayLocalDate),
                                        )
                                    }
                                }
                            },
                            onComplete = { viewModel.completeHabit(habit.id) },
                            onClick = { onEditHabit(habit.id) },
                            householdCompleterName = householdCompleterNames[habit.id],
                        )
                    }
                }

                } // end LazyColumn
            } // end PullToRefreshBox

            // Stat detail floating overlay
            AnimatedVisibility(
                visible = showDetailedStats,
                enter = fadeIn(animationSpec = tween(200)),
                exit = fadeOut(animationSpec = tween(200)),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.45f))
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
                                                text = if (isSleeping) stringResource(R.string.stat_sleeping) else mood.displayName(),
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface,
                                            )
                                            Text(
                                                text = stringResource(R.string.pet_stats_header, stats.name),
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            )
                                        }
                                    }
                                    Box(
                                        modifier = Modifier
                                            .size(48.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                                            .clickable { showDetailedStats = false },
                                        contentAlignment = Alignment.Center,
                                    ) {
                                         Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = stringResource(R.string.pet_close_cd),
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
                                        label = stringResource(R.string.pet_hunger_label),
                                        emoji = "\uD83C\uDF56",
                                        value = stats.hunger,
                                        statusText = when {
                                            stats.hunger >= 80 -> stringResource(R.string.stat_full)
                                            stats.hunger >= 50 -> stringResource(R.string.stat_peckish)
                                            stats.hunger >= 25 -> stringResource(R.string.stat_hungry)
                                            else -> stringResource(R.string.stat_starving)
                                        },
                                        barColor = MaterialTheme.colorScheme.secondary,
                                    )
                                    StatBentoCard(
                                        modifier = Modifier.weight(1f),
                                        label = stringResource(R.string.pet_joy_label),
                                        emoji = "\uD83D\uDC95",
                                        value = stats.happiness,
                                        statusText = when {
                                            stats.happiness >= 80 -> stringResource(R.string.stat_happy)
                                            stats.happiness >= 50 -> stringResource(R.string.stat_content)
                                            stats.happiness >= 25 -> stringResource(R.string.stat_sad)
                                            else -> stringResource(R.string.stat_miserable)
                                        },
                                        barColor = MaterialTheme.colorScheme.primary,
                                    )
                                }

                                // Energy card
                                EnergyBentoCard(
                                    value = stats.energy,
                                    statusText = when {
                                        stats.energy >= 70 -> stringResource(R.string.stat_energized)
                                        stats.energy >= 40 -> stringResource(R.string.stat_recharging)
                                        stats.energy >= 15 -> stringResource(R.string.stat_needs_nap)
                                        else -> stringResource(R.string.stat_exhausted)
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
                    Text(stringResource(R.string.pet_sleep_dialog_title), fontWeight = FontWeight.Bold)
                },
                text = {
                    Text(stringResource(R.string.pet_sleep_dialog_body))
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showStartSleepAnimation = true
                            viewModel.sleepChoreboo()
                            showSleepDialog = false
                        },
                    ) {
                        Text(stringResource(R.string.pet_sleep_dialog_confirm))
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { showSleepDialog = false },
                    ) {
                        Text(stringResource(R.string.pet_cancel_button))
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

    // Background picker — shown as a ModalBottomSheet on top of everything
    if (showBackgroundPicker) {
        BackgroundPickerSheet(
            catalogue = backgroundCatalogue,
            unlockedIds = unlockedBackgroundIds,
            currentBackgroundId = backgroundId,
            totalPoints = totalPoints,
            isPremium = isPremium,
            onSelect = { id ->
                viewModel.selectBackground(id)
                showBackgroundPicker = false
            },
            onPurchase = { item ->
                viewModel.purchaseBackground(item)
                // Keep sheet open so user can see it's been unlocked and then apply
            },
            onDismiss = { showBackgroundPicker = false },
        )
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
                    text = stringResource(R.string.pet_xp_progress_label),
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
    val contentDescriptionText = stringResource(R.string.stat_bento_cd, label, value, statusText)
    Box(
        modifier = modifier
            .height(110.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLowest)
            .semantics(mergeDescendants = true) { contentDescription = contentDescriptionText },
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
    val contentDescriptionText = stringResource(R.string.energy_bento_cd, value, statusText)
    Box(
         modifier = modifier
             .fillMaxWidth()
             .height(100.dp)
             .clip(RoundedCornerShape(16.dp))
             .background(MaterialTheme.colorScheme.surfaceContainerLowest)
             .semantics(mergeDescendants = true) { contentDescription = contentDescriptionText },
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
                        text = stringResource(R.string.pet_energy_label),
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
                .padding(horizontal = 32.dp),
            contentAlignment = Alignment.Center,
        ) {
            // Confetti behind the card
            ConfettiOverlay()

            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondary,
                    contentColor = MaterialTheme.colorScheme.onSecondary,
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = if (event.evolved) stringResource(R.string.levelup_evolved_title) else stringResource(R.string.levelup_level_up_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSecondary,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = if (event.evolved)
                            stringResource(R.string.levelup_evolved_body, event.newStage?.displayName() ?: "", event.newLevel)
                        else
                            stringResource(R.string.levelup_level_up_body, event.newLevel),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSecondary,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.onSecondary,
                            contentColor = MaterialTheme.colorScheme.secondary,
                        ),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            text = stringResource(R.string.levelup_awesome),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(vertical = 4.dp),
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
            AnimationPhase.MOOD -> foxMoodAssetPath(mood) to 1
            AnimationPhase.IDLE -> FOX_ANIM_IDLE to FOX_IDLE_ITERATIONS
            AnimationPhase.EATING -> FOX_ANIM_EATING to 1
            AnimationPhase.INTERACTING -> FOX_ANIM_INTERACT to 1
            AnimationPhase.THUMBS_UP -> FOX_ANIM_THUMBS_UP to 1
            AnimationPhase.START_SLEEPING -> FOX_ANIM_START_SLEEP to 1
            AnimationPhase.SLEEPING -> FOX_ANIM_LOOP_SLEEPING to Int.MAX_VALUE
        }

        Crossfade(
            targetState = phase,
            animationSpec = tween(durationMillis = 100),
            label = "petAnimationCrossfade",
        ) { animPhase ->
            val (currentAsset, currentIterations) = when (animPhase) {
                AnimationPhase.MOOD -> foxMoodAssetPath(mood) to 1
                AnimationPhase.IDLE -> FOX_ANIM_IDLE to FOX_IDLE_ITERATIONS
                AnimationPhase.EATING -> FOX_ANIM_EATING to 1
                AnimationPhase.INTERACTING -> FOX_ANIM_INTERACT to 1
                AnimationPhase.THUMBS_UP -> FOX_ANIM_THUMBS_UP to 1
                AnimationPhase.START_SLEEPING -> FOX_ANIM_START_SLEEP to 1
                AnimationPhase.SLEEPING -> FOX_ANIM_LOOP_SLEEPING to Int.MAX_VALUE
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
    } else if (petType == PetType.PANDA) {
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

        Crossfade(
            targetState = phase,
            animationSpec = tween(durationMillis = 100),
            label = "pandaAnimationCrossfade",
        ) { animPhase ->
            val (currentAsset, currentIterations) = when (animPhase) {
                AnimationPhase.MOOD -> pandaMoodAssetPath(mood) to 1
                AnimationPhase.IDLE -> PANDA_ANIM_IDLE to PANDA_IDLE_ITERATIONS
                AnimationPhase.EATING -> PANDA_ANIM_EATING to 1
                AnimationPhase.INTERACTING -> PANDA_ANIM_INTERACT to 1
                AnimationPhase.THUMBS_UP -> PANDA_ANIM_THUMBS_UP to 1
                AnimationPhase.START_SLEEPING -> PANDA_ANIM_START_SLEEP to 1
                AnimationPhase.SLEEPING -> PANDA_ANIM_LOOP_SLEEPING to Int.MAX_VALUE
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
