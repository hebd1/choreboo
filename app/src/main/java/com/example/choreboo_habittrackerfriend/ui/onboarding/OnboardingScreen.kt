package com.example.choreboo_habittrackerfriend.ui.onboarding

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.animation.slideInVertically
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.choreboo_habittrackerfriend.R
import com.example.choreboo_habittrackerfriend.domain.model.PetType
import com.example.choreboo_habittrackerfriend.ui.util.displayName
import com.example.choreboo_habittrackerfriend.ui.util.findActivity
import com.example.choreboo_habittrackerfriend.ui.util.localizedLabel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// ------------------------------------------------------------------------------------------------
// Step indicator
// ------------------------------------------------------------------------------------------------

@Composable
private fun StepIndicator(currentStep: Int, totalSteps: Int) {
    val progress = (currentStep + 1).toFloat() / totalSteps.toFloat()
    LinearProgressIndicator(
        progress = { progress },
        modifier = Modifier
            .fillMaxWidth()
            .height(3.dp)
            .clip(RoundedCornerShape(2.dp)),
        color = MaterialTheme.colorScheme.primary,
        trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
    )
}

// ------------------------------------------------------------------------------------------------
// Root OnboardingScreen
// ------------------------------------------------------------------------------------------------

@Composable
fun OnboardingScreen(
    onComplete: () -> Unit,
    viewModel: OnboardingViewModel = hiltViewModel(),
) {
    val currentStep by viewModel.currentStep.collectAsStateWithLifecycle()
    val selectedPetType by viewModel.selectedPetType.collectAsStateWithLifecycle()
    val usageIntent by viewModel.usageIntent.collectAsStateWithLifecycle()
    val biggestStruggle by viewModel.biggestStruggle.collectAsStateWithLifecycle()
    val isHatching by viewModel.isHatching.collectAsStateWithLifecycle()
    val isPurchasing by viewModel.isPurchasing.collectAsStateWithLifecycle()
    val productDetails by viewModel.productDetails.collectAsStateWithLifecycle()

    var chorebooName by rememberSaveable { mutableStateOf("") }
    var showContent by rememberSaveable { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        delay(200)
        showContent = true
    }

    // When the billing library confirms a premium purchase, complete onboarding.
    val isPremium by viewModel.isPremium.collectAsStateWithLifecycle()
    LaunchedEffect(isPremium, currentStep) {
        if (isPremium && currentStep == STEP_PAYWALL) {
            viewModel.completePremiumOnboarding()
        }
    }

    // Listen for one-shot navigation / error events.
    val premiumFallbackMsg = stringResource(R.string.onboarding_snack_premium_fallback)
    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is OnboardingEvent.NavigateToHome -> onComplete()
                is OnboardingEvent.PremiumPetFallback -> {
                    scope.launch {
                        snackbarHostState.showSnackbar(
                            premiumFallbackMsg,
                            duration = SnackbarDuration.Short,
                        )
                    }
                }
                is OnboardingEvent.Error -> scope.launch {
                    snackbarHostState.showSnackbar(
                        context.getString(event.messageRes),
                        duration = SnackbarDuration.Short,
                    )
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(modifier = Modifier.height(52.dp))

            // Progress bar
            StepIndicator(
                currentStep = currentStep,
                totalSteps = STEP_PAYWALL + 1,
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Animated step content
            AnimatedVisibility(
                visible = showContent,
                enter = fadeIn() + slideInVertically { -40 },
            ) {
                AnimatedContent(
                    targetState = currentStep,
                    transitionSpec = {
                        if (targetState > initialState) {
                            slideInHorizontally { it } + fadeIn() togetherWith
                                slideOutHorizontally { -it } + fadeOut()
                        } else {
                            slideInHorizontally { -it } + fadeIn() togetherWith
                                slideOutHorizontally { it } + fadeOut()
                        }
                    },
                    label = "onboardingStep",
                ) { step ->
                    when (step) {
                        STEP_SURVEY_USAGE -> SurveyUsageStep(
                            selected = usageIntent,
                            onSelect = { viewModel.selectUsageIntent(it) },
                            onNext = { viewModel.goToStep(STEP_SURVEY_STRUGGLE) },
                        )
                        STEP_SURVEY_STRUGGLE -> SurveyStruggleStep(
                            selected = biggestStruggle,
                            onSelect = { viewModel.selectBiggestStruggle(it) },
                            onBack = { viewModel.goBack() },
                            onNext = { viewModel.goToStep(STEP_HOW_IT_HELPS) },
                        )
                        STEP_HOW_IT_HELPS -> HowItHelpsStep(
                            usageIntent = usageIntent,
                            biggestStruggle = biggestStruggle,
                            onBack = { viewModel.goBack() },
                            onNext = { viewModel.goToStep(STEP_PET_SELECT) },
                        )
                        STEP_PET_SELECT -> PetSelectionStep(
                            selectedPetType = selectedPetType,
                            onSelect = { viewModel.selectPetType(it) },
                            onBack = { viewModel.goBack() },
                            onNext = { viewModel.goToStep(STEP_NAME) },
                        )
                        STEP_NAME -> NameStep(
                            chorebooName = chorebooName,
                            onNameChange = { chorebooName = it },
                            petType = selectedPetType,
                            isHatching = isHatching,
                            onBack = { viewModel.goBack() },
                            onHatch = { viewModel.hatchChoreboo(chorebooName) },
                        )
                        STEP_PAYWALL -> PaywallStep(
                            selectedPetType = selectedPetType,
                            productDetails = productDetails,
                            isPurchasing = isPurchasing,
                            onSubscribe = {
                                val activity = context.findActivity() ?: return@PaywallStep
                                viewModel.startPurchase(activity)
                            },
                            onSkip = { viewModel.skipPremium() },
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(48.dp))
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}

// ------------------------------------------------------------------------------------------------
// Step 0 — Survey: How will you use Choreboo?
// ------------------------------------------------------------------------------------------------

@Composable
private fun SurveyUsageStep(
    selected: UsageIntent?,
    onSelect: (UsageIntent) -> Unit,
    onNext: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "🐾",
            fontSize = 56.sp,
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.onboarding_how_use_title),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = stringResource(R.string.onboarding_how_use_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(24.dp))

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            UsageIntent.entries.forEach { intent ->
                SurveyCard(
                    emoji = intent.emoji,
                    label = intent.localizedLabel(),
                    isSelected = intent == selected,
                    onClick = { onSelect(intent) },
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        PrimaryButton(
            text = stringResource(R.string.onboarding_continue),
            enabled = selected != null,
            onClick = onNext,
        )
    }
}

// ------------------------------------------------------------------------------------------------
// Step 1 — Survey: What's your biggest challenge?
// ------------------------------------------------------------------------------------------------

@Composable
private fun SurveyStruggleStep(
    selected: BiggestStruggle?,
    onSelect: (BiggestStruggle) -> Unit,
    onBack: () -> Unit,
    onNext: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(text = "🤔", fontSize = 56.sp)
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.onboarding_biggest_challenge_title),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = stringResource(R.string.onboarding_biggest_challenge_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(24.dp))

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            BiggestStruggle.entries.forEach { struggle ->
                SurveyCard(
                    emoji = struggle.emoji,
                    label = struggle.localizedLabel(),
                    isSelected = struggle == selected,
                    onClick = { onSelect(struggle) },
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        PrimaryButton(
            text = stringResource(R.string.onboarding_continue),
            enabled = selected != null,
            onClick = onNext,
        )
        TextButton(onClick = onBack) {
            Text(stringResource(R.string.onboarding_back), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

// ------------------------------------------------------------------------------------------------
// Step 2 — How Choreboo Helps (personalised based on survey)
// ------------------------------------------------------------------------------------------------

@Composable
private fun HowItHelpsStep(
    usageIntent: UsageIntent?,
    biggestStruggle: BiggestStruggle?,
    onBack: () -> Unit,
    onNext: () -> Unit,
) {
    // Build personalised bullet points
    val bulletBase = stringResource(R.string.onboarding_help_bullet_base)
    val bulletStages = stringResource(R.string.onboarding_help_bullet_stages)
    val bulletChoresHousehold = stringResource(R.string.onboarding_help_chores_household)
    val bulletChoresSchedule = stringResource(R.string.onboarding_help_chores_schedule)
    val bulletChoresTasks = stringResource(R.string.onboarding_help_chores_tasks)
    val bulletMotivation = stringResource(R.string.onboarding_help_motivation)
    val bulletTime = stringResource(R.string.onboarding_help_time)
    val bulletRemembering = stringResource(R.string.onboarding_help_remembering)
    val bulletGettingStarted = stringResource(R.string.onboarding_help_getting_started)

    val bullets = buildList {
        add("🐾" to bulletBase)
        when (usageIntent) {
            UsageIntent.CHORES_WITH_FRIENDS ->
                add("🏠" to bulletChoresHousehold)
            UsageIntent.HABITS_ROUTINES ->
                add("📅" to bulletChoresSchedule)
            UsageIntent.TASK_MANAGER ->
                add("✅" to bulletChoresTasks)
            null -> Unit
        }
        when (biggestStruggle) {
            BiggestStruggle.MOTIVATION ->
                add("🔥" to bulletMotivation)
            BiggestStruggle.TIME ->
                add("⏰" to bulletTime)
            BiggestStruggle.REMEMBERING ->
                add("🧠" to bulletRemembering)
            BiggestStruggle.GETTING_STARTED ->
                add("🚀" to bulletGettingStarted)
            null -> Unit
        }
        add("🌟" to bulletStages)
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(text = "✨", fontSize = 56.sp)
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.onboarding_how_it_helps_title),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(24.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerLowest)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            bullets.forEach { (emoji, text) ->
                Row(verticalAlignment = Alignment.Top) {
                    Text(text = emoji, fontSize = 20.sp, modifier = Modifier.width(32.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = text,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        PrimaryButton(text = stringResource(R.string.onboarding_next_pick_pet), onClick = onNext)
        TextButton(onClick = onBack) {
            Text(stringResource(R.string.onboarding_back), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

// ------------------------------------------------------------------------------------------------
// Step 3 — Pet selection (with premium lock overlay)
// ------------------------------------------------------------------------------------------------

@Composable
private fun PetSelectionStep(
    selectedPetType: PetType,
    onSelect: (PetType) -> Unit,
    onBack: () -> Unit,
    onNext: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(text = "🐾", fontSize = 56.sp)
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.onboarding_choose_companion),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = stringResource(R.string.onboarding_pet_premium_note),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(20.dp))

        val pets = PetType.entries
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            pets.chunked(2).forEach { rowPets ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    rowPets.forEach { pet ->
                        PetSelectionCard(
                            petType = pet,
                            isSelected = pet == selectedPetType,
                            onSelect = { onSelect(pet) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                    if (rowPets.size < 2) Spacer(modifier = Modifier.weight(1f))
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        PrimaryButton(
            text = stringResource(R.string.onboarding_choose_pet, selectedPetType.displayName(), selectedPetType.emoji),
            onClick = onNext,
        )
        TextButton(onClick = onBack) {
            Text(stringResource(R.string.onboarding_back), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun PetSelectionCard(
    petType: PetType,
    isSelected: Boolean,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(
                if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
                else MaterialTheme.colorScheme.surfaceContainerLowest,
            )
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                shape = RoundedCornerShape(20.dp),
            )
            .clickable { onSelect() }
            .padding(16.dp),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Box(contentAlignment = Alignment.TopEnd) {
                Text(text = petType.emoji, fontSize = 52.sp)
                if (isSelected) {
                    Box(
                        modifier = Modifier
                            .size(22.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = stringResource(R.string.onboarding_selected_cd),
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(14.dp),
                        )
                    }
                } else if (petType.isPremium) {
                    // Premium lock badge
                    Box(
                        modifier = Modifier
                            .size(22.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(MaterialTheme.colorScheme.tertiaryContainer),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = stringResource(R.string.onboarding_premium_cd),
                            tint = MaterialTheme.colorScheme.onTertiaryContainer,
                            modifier = Modifier.size(13.dp),
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = petType.displayName(),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            )
            if (petType.isPremium) {
                Text(
                    text = stringResource(R.string.onboarding_premium_cd),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.tertiary,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

// ------------------------------------------------------------------------------------------------
// Step 4 — Name + Hatch
// ------------------------------------------------------------------------------------------------

@Composable
private fun NameStep(
    chorebooName: String,
    onNameChange: (String) -> Unit,
    petType: PetType,
    isHatching: Boolean,
    onBack: () -> Unit,
    onHatch: () -> Unit,
) {
    val uriHandler = LocalUriHandler.current
    val focusManager = LocalFocusManager.current

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(text = petType.emoji, fontSize = 72.sp)
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = stringResource(R.string.onboarding_name_your_pet, petType.displayName()),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = stringResource(R.string.onboarding_name_change_later),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(20.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerLowest)
                .padding(20.dp),
        ) {
            Column {
                Text(
                    text = stringResource(R.string.onboarding_name_hint_label, petType.displayName()),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = chorebooName,
                    onValueChange = { if (it.length <= 20) onNameChange(it) },
                    placeholder = {
                        Text(stringResource(R.string.onboarding_name_placeholder), color = MaterialTheme.colorScheme.outline)
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth(),
                    trailingIcon = {
                        Text(
                            text = "${chorebooName.length}/20",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary.copy(
                                alpha = if (chorebooName.isEmpty()) 0.4f else 1f,
                            ),
                            modifier = Modifier.padding(end = 8.dp),
                        )
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                        unfocusedBorderColor = Color.Transparent,
                        focusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                    ),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(
                        onDone = { focusManager.clearFocus() },
                    ),
                )
                Spacer(modifier = Modifier.height(20.dp))

                // Hatch CTA
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            if (isHatching) MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                            else MaterialTheme.colorScheme.primary,
                        )
                        .clickable(enabled = !isHatching) { onHatch() },
                    contentAlignment = Alignment.Center,
                ) {
                    if (isHatching) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp,
                            )
                            Text(
                                text = stringResource(R.string.onboarding_hatching),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onPrimary,
                            )
                        }
                    } else {
                        Text(
                            text = stringResource(R.string.onboarding_hatch_cta, petType.emoji),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onPrimary,
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                TextButton(
                    onClick = onBack,
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                    enabled = !isHatching,
                ) {
                    Text(
                        stringResource(R.string.onboarding_change_pet),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp),
                ) {
                    Text(
                        text = stringResource(R.string.onboarding_terms),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline,
                        textAlign = TextAlign.Center,
                    )
                    TextButton(
                        onClick = {
                            uriHandler.openUri("https://www.notion.so/elihebdon/Privacy-Policy-for-Choreboo-Habit-Tracker-Friend-3306b7634ff3805bad3ac4306bd087a8")
                        },
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
                    ) {
                        Text(
                            stringResource(R.string.auth_privacy_policy),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                        )
                    }
                }
            }
        }
    }
}

// ------------------------------------------------------------------------------------------------
// Step 5 — Paywall
// ------------------------------------------------------------------------------------------------

@Composable
private fun PaywallStep(
    selectedPetType: PetType,
    productDetails: com.android.billingclient.api.ProductDetails?,
    isPurchasing: Boolean,
    onSubscribe: () -> Unit,
    onSkip: () -> Unit,
) {
    // Derive price string from product details; fall back to "$1.99/month"
    val priceText = remember(productDetails) {
        productDetails?.subscriptionOfferDetails
             ?.firstOrNull()
             ?.pricingPhases
             ?.pricingPhaseList
             ?.lastOrNull()
             ?.formattedPrice
             ?: "$1.99"
     }

     val trialText = remember(productDetails) {
         productDetails?.subscriptionOfferDetails
             ?.firstOrNull()
             ?.pricingPhases
             ?.pricingPhaseList
             ?.firstOrNull { it.priceAmountMicros == 0L }
             ?.let { "3-day free trial, then $priceText" }
             ?: "3-day free trial, then $priceText"
     }
     
     // Resolve strings at composable scope
     val pricingPerMonth = stringResource(R.string.pricing_per_month, priceText)
     val pricingTrialText = stringResource(R.string.pricing_trial, pricingPerMonth)

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Crown + pet emoji header
        Box(contentAlignment = Alignment.TopEnd) {
            Text(text = if (selectedPetType.isPremium) selectedPetType.emoji else "🦊", fontSize = 72.sp)
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.tertiaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Default.WorkspacePremium,
                    contentDescription = stringResource(R.string.onboarding_premium_cd),
                    tint = MaterialTheme.colorScheme.onTertiaryContainer,
                    modifier = Modifier.size(18.dp),
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = stringResource(R.string.onboarding_unlock_premium),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.ExtraBold,
             color = MaterialTheme.colorScheme.primary,
             textAlign = TextAlign.Center,
         )
         Spacer(modifier = Modifier.height(4.dp))
         Text(
             text = pricingTrialText,
             style = MaterialTheme.typography.bodyMedium,
             color = MaterialTheme.colorScheme.onSurfaceVariant,
             textAlign = TextAlign.Center,
         )

        Spacer(modifier = Modifier.height(20.dp))

        // Feature list card
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerLowest)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            PremiumFeatureRow(emoji = "🦎", text = stringResource(R.string.onboarding_premium_feature_pets))
            PremiumFeatureRow(emoji = "🌌", text = stringResource(R.string.onboarding_premium_feature_backgrounds))
            PremiumFeatureRow(emoji = "👑", text = stringResource(R.string.onboarding_premium_feature_badge))
            PremiumFeatureRow(emoji = "🚫", text = stringResource(R.string.onboarding_premium_feature_no_ads))
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Subscribe CTA
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(
                    if (isPurchasing) MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                    else MaterialTheme.colorScheme.primary,
                )
                .clickable(enabled = !isPurchasing) { onSubscribe() },
            contentAlignment = Alignment.Center,
        ) {
            if (isPurchasing) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp,
                    )
                    Text(
                        text = stringResource(R.string.onboarding_opening_store),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                }
            } else {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = stringResource(R.string.onboarding_start_free_trial),
                         style = MaterialTheme.typography.titleLarge,
                         fontWeight = FontWeight.ExtraBold,
                         color = MaterialTheme.colorScheme.onPrimary,
                     )
                     Text(
                         text = pricingTrialText,
                         style = MaterialTheme.typography.labelSmall,
                         color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f),
                     )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        TextButton(
            onClick = onSkip,
            enabled = !isPurchasing,
        ) {
            Text(
                text = stringResource(R.string.onboarding_continue_free),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = stringResource(R.string.onboarding_cancel_sub_note),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.outline,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp),
        )
    }
}

@Composable
private fun PremiumFeatureRow(emoji: String, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(text = emoji, fontSize = 22.sp, modifier = Modifier.width(32.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

// ------------------------------------------------------------------------------------------------
// Shared composable helpers
// ------------------------------------------------------------------------------------------------

@Composable
private fun SurveyCard(
    emoji: String,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(
                if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                else MaterialTheme.colorScheme.surfaceContainerLowest,
            )
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                shape = RoundedCornerShape(16.dp),
            )
            .clickable { onClick() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(text = emoji, fontSize = 26.sp)
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        if (isSelected) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

@Composable
private fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(60.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(
                if (enabled) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.surfaceContainerHighest,
            )
            .clickable(enabled = enabled) { onClick() },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.ExtraBold,
            color = if (enabled) MaterialTheme.colorScheme.onPrimary
            else MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
