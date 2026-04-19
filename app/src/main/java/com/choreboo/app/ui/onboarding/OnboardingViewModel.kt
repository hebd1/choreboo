package com.choreboo.app.ui.onboarding

import android.app.Activity
import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.choreboo.app.R
import com.choreboo.app.data.datastore.UserPreferences
import com.choreboo.app.data.repository.BillingRepository
import com.choreboo.app.data.repository.ChorebooRepository
import com.choreboo.app.domain.model.PetType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

// ------------------------------------------------------------------------------------------------
// Step constants
// ------------------------------------------------------------------------------------------------
const val STEP_SURVEY_USAGE = 0       // "How will you use Choreboo?"
const val STEP_SURVEY_STRUGGLE = 1    // "What's your biggest challenge?"
const val STEP_HOW_IT_HELPS = 2       // Personalized "How Choreboo Helps" screen
const val STEP_PET_SELECT = 3         // Choose your companion (FOX/PANDA free; AXOLOTL/CAPYBARA premium)
const val STEP_NAME = 4               // Name + hatch
const val STEP_PAYWALL = 5            // Paywall (subscribe or skip)

// ------------------------------------------------------------------------------------------------
// Survey answer types
// ------------------------------------------------------------------------------------------------
enum class UsageIntent(val emoji: String) {
    CHORES_WITH_FRIENDS("🏠"),
    HABITS_ROUTINES("🎯"),
    TASK_MANAGER("✅"),
}

enum class BiggestStruggle(val emoji: String) {
    MOTIVATION("🔥"),
    TIME("⏰"),
    REMEMBERING("🧠"),
    GETTING_STARTED("🚀"),
}

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val userPreferences: UserPreferences,
    private val chorebooRepository: ChorebooRepository,
    private val billingRepository: BillingRepository,
) : ViewModel() {

    // ---- Step navigation ----
    private val _currentStep = MutableStateFlow(STEP_SURVEY_USAGE)
    val currentStep: StateFlow<Int> = _currentStep.asStateFlow()

    // ---- Survey state (in-memory only — not persisted) ----
    private val _usageIntent = MutableStateFlow<UsageIntent?>(null)
    val usageIntent: StateFlow<UsageIntent?> = _usageIntent.asStateFlow()

    private val _biggestStruggle = MutableStateFlow<BiggestStruggle?>(null)
    val biggestStruggle: StateFlow<BiggestStruggle?> = _biggestStruggle.asStateFlow()

    // ---- Pet selection ----
    private val _selectedPetType = MutableStateFlow(PetType.FOX)
    val selectedPetType: StateFlow<PetType> = _selectedPetType.asStateFlow()

    // ---- Loading / purchase state ----
    private val _isHatching = MutableStateFlow(false)
    val isHatching: StateFlow<Boolean> = _isHatching.asStateFlow()

    private val _isPurchasing = MutableStateFlow(false)
    val isPurchasing: StateFlow<Boolean> = _isPurchasing.asStateFlow()

    // Expose product details for the paywall price display
    val productDetails = billingRepository.productDetails

    // True once an active, acknowledged premium subscription is detected.
    val isPremium: StateFlow<Boolean> = billingRepository.isPremium

    // ---- Events ----
    private val _events = MutableSharedFlow<OnboardingEvent>()
    val events: SharedFlow<OnboardingEvent> = _events.asSharedFlow()

    // ------------------------------------------------------------------------------------------------
    // Step navigation
    // ------------------------------------------------------------------------------------------------

    fun goToStep(step: Int) {
        _currentStep.value = step
    }

    fun goBack() {
        val prev = _currentStep.value - 1
        if (prev >= STEP_SURVEY_USAGE) _currentStep.value = prev
    }

    // ------------------------------------------------------------------------------------------------
    // Survey
    // ------------------------------------------------------------------------------------------------

    fun selectUsageIntent(intent: UsageIntent) {
        _usageIntent.value = intent
    }

    fun selectBiggestStruggle(struggle: BiggestStruggle) {
        _biggestStruggle.value = struggle
    }

    // ------------------------------------------------------------------------------------------------
    // Pet selection
    // ------------------------------------------------------------------------------------------------

    fun selectPetType(petType: PetType) {
        _selectedPetType.value = petType
    }

    // ------------------------------------------------------------------------------------------------
    // Hatch (creates Choreboo in Room + cloud, marks onboarding in progress)
    // Called from STEP_NAME — navigates to paywall after hatching.
    // ------------------------------------------------------------------------------------------------

    fun hatchChoreboo(chorebooName: String) {
        viewModelScope.launch {
            _isHatching.value = true
            try {
                val name = chorebooName.ifBlank { _selectedPetType.value.name.lowercase().replaceFirstChar { it.uppercase() } }
                if (_selectedPetType.value.isPremium && !billingRepository.isPremium.value) {
                    _currentStep.value = STEP_PAYWALL
                    return@launch
                }
                chorebooRepository.createOrActivatePetType(name, _selectedPetType.value)
                // Move to paywall — do NOT mark onboarding complete yet.
                _currentStep.value = STEP_PAYWALL
            } catch (e: Exception) {
                _events.emit(OnboardingEvent.Error(R.string.onboarding_error_generic))
            } finally {
                _isHatching.value = false
            }
        }
    }

    // ------------------------------------------------------------------------------------------------
    // Paywall actions
    // ------------------------------------------------------------------------------------------------

    /**
     * Launch the Google Play purchase flow from the paywall.
     * [activity] is needed by BillingClient to anchor the purchase dialog.
     */
    fun startPurchase(activity: Activity) {
        viewModelScope.launch {
            _isPurchasing.value = true
            val launched = billingRepository.launchPurchaseFlow(activity)
            if (!launched) {
                _events.emit(OnboardingEvent.Error(R.string.onboarding_error_purchase_screen))
            }
            // isPurchasing is cleared by the PurchasesUpdatedListener flow — or by skipPremium.
            // We keep the spinner visible until BillingRepository emits isPremium=true or
            // the user taps "Skip".
            _isPurchasing.value = false
        }
    }

    /**
     * User taps "Maybe Later" — skip premium, complete onboarding as a free user.
     * If they selected a premium pet, fall back to FOX.
     */
    fun skipPremium() {
        viewModelScope.launch {
            if (_selectedPetType.value.isPremium) {
                // Fall back to FOX and make it the active pet for free users.
                _selectedPetType.value = PetType.FOX
                try {
                    chorebooRepository.createOrActivatePetType(
                        chorebooRepository.getChorebooNameSync() ?: PetType.FOX.name.lowercase().replaceFirstChar { it.uppercase() },
                        PetType.FOX,
                    )
                } catch (_: Exception) { /* Silent — best effort */ }
                _events.emit(OnboardingEvent.PremiumPetFallback)
            }
            completeOnboarding()
        }
    }

    /**
     * Called after a successful purchase is detected (BillingRepository.isPremium becomes true).
     * Completes onboarding as a premium user.
     */
    fun completePremiumOnboarding() {
        viewModelScope.launch {
            completeOnboarding()
        }
    }

    private suspend fun completeOnboarding() {
        userPreferences.setOnboardingComplete(true)
        _events.emit(OnboardingEvent.NavigateToHome)
    }
}

// ------------------------------------------------------------------------------------------------
// Events
// ------------------------------------------------------------------------------------------------

sealed class OnboardingEvent {
    data object NavigateToHome : OnboardingEvent()
    data object PremiumPetFallback : OnboardingEvent()
    data class Error(@StringRes val messageRes: Int) : OnboardingEvent()
}
