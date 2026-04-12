package com.example.choreboo_habittrackerfriend.ui.pet

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.choreboo_habittrackerfriend.data.datastore.UserPreferences
import com.example.choreboo_habittrackerfriend.data.repository.AuthRepository
import com.example.choreboo_habittrackerfriend.data.repository.BackgroundRepository
import com.example.choreboo_habittrackerfriend.data.repository.BillingRepository
import com.example.choreboo_habittrackerfriend.data.repository.ChorebooRepository
import com.example.choreboo_habittrackerfriend.data.repository.HabitRepository
import com.example.choreboo_habittrackerfriend.data.repository.HouseholdRepository
import com.example.choreboo_habittrackerfriend.data.repository.SyncManager
import com.example.choreboo_habittrackerfriend.data.repository.UserRepository
import com.example.choreboo_habittrackerfriend.domain.model.ChorebooMood
import com.example.choreboo_habittrackerfriend.domain.model.ChorebooStage
import com.example.choreboo_habittrackerfriend.domain.model.ChorebooStats
import com.example.choreboo_habittrackerfriend.domain.model.Habit
import com.example.choreboo_habittrackerfriend.domain.model.BACKGROUND_DEFAULT_ID
import com.example.choreboo_habittrackerfriend.domain.model.BackgroundItem
import com.example.choreboo_habittrackerfriend.domain.model.BACKGROUND_REGISTRY
import com.example.choreboo_habittrackerfriend.domain.model.PetType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import timber.log.Timber
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject

@HiltViewModel
class PetViewModel @Inject constructor(
    private val chorebooRepository: ChorebooRepository,
    private val habitRepository: HabitRepository,
    private val userPreferences: UserPreferences,
    private val authRepository: AuthRepository,
    private val householdRepository: HouseholdRepository,
    private val syncManager: SyncManager,
    private val userRepository: UserRepository,
    private val backgroundRepository: BackgroundRepository,
    private val billingRepository: BillingRepository,
) : ViewModel() {

    // -----------------------------------------------------------------------
    // Choreboo state — single shared upstream to avoid 4 separate Room observers
    // -----------------------------------------------------------------------

    /** Single Room observer shared by all downstream choreboo-derived StateFlows. */
    private val chorebooFlow = chorebooRepository.getChoreboo()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val chorebooState: StateFlow<ChorebooStats?> = chorebooFlow

    val currentMood: StateFlow<ChorebooMood> = chorebooFlow
        .map { it?.mood ?: ChorebooMood.IDLE }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ChorebooMood.IDLE)

    val totalPoints: StateFlow<Int> = userPreferences.totalPoints
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

     val isSleeping: StateFlow<Boolean> = chorebooFlow
        .map { it?.isSleeping() ?: false }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    /** True while the eating Lottie animation should be playing */
    private val _isEating = MutableStateFlow(false)
    val isEating: StateFlow<Boolean> = _isEating.asStateFlow()

    /** True while a manual refresh is in progress */
    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    // -----------------------------------------------------------------------
    // Reactive today date — updates if the date changes while the app is open
    // -----------------------------------------------------------------------

    /**
     * Holds today's date as an ISO-8601 string. Set once in init; refreshed
     * whenever [refreshData] is called (covers the case where the app stays
     * in the foreground past midnight).
     */
    private val _todayDate = MutableStateFlow(
        LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE),
    )

    // -----------------------------------------------------------------------
    // Habit state (absorbed from HabitListViewModel)
    // -----------------------------------------------------------------------

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    /**
     * Habits for the current user: personal habits they own, or household habits assigned to them.
     * Filters on UID only if authenticated; otherwise shows empty list.
     */
    val habits: StateFlow<List<Habit>> = authRepository.currentUser
        .flatMapLatest { user ->
            if (user != null) {
                habitRepository.getHabitsForUser(user.uid)
            } else {
                flowOf(emptyList())
            }
        }
        .onEach { _isLoading.value = false }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** Today's completions as a reactive flow — no manual refresh needed. */
    val todayCompletions: StateFlow<Map<Long, Int>> = _todayDate
        .flatMapLatest { date -> habitRepository.getLogsForDate(date) }
        .map { logs -> logs.groupBy { it.habitId }.mapValues { it.value.size } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    val streaks: StateFlow<Map<Long, Int>> = habitRepository.getStreaksForToday()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    val petType: StateFlow<PetType> = chorebooFlow
        .map { it?.petType ?: PetType.FOX }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), PetType.FOX)

    val profilePhotoUri: StateFlow<String?> = userPreferences.profilePhotoUri
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val googlePhotoUrl: StateFlow<String?> = authRepository.currentUser
        .map { it?.photoUrl?.toString() }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            authRepository.currentFirebaseUser?.photoUrl?.toString(),
        )

    /** UID of the currently authenticated user — used to distinguish own vs. other-member habits. */
    val currentUserUid: String?
        get() = authRepository.currentFirebaseUser?.uid

    // -----------------------------------------------------------------------
    // Background state
    // -----------------------------------------------------------------------

    /**
     * The current background ID for this user's Choreboo.
     * Null / "default" both mean the free mood-gradient should be shown.
     */
    val backgroundId: StateFlow<String?> = chorebooFlow
        .map { it?.backgroundId }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    /**
     * Set of background IDs that the current user has unlocked (default always included).
     */
    val unlockedBackgroundIds: StateFlow<Set<String>> = backgroundRepository
        .getPurchasedBackgrounds()
        .map { entities ->
            val ids = entities.map { it.backgroundId }.toMutableSet()
            ids.add(BACKGROUND_DEFAULT_ID) // default is always unlocked
            ids
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), setOf(BACKGROUND_DEFAULT_ID))

    /** The full background catalogue enriched with unlock state — drives the picker UI. */
    val backgroundCatalogue: StateFlow<List<BackgroundItem>> = MutableStateFlow(BACKGROUND_REGISTRY)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), BACKGROUND_REGISTRY)

    /** True when the user has an active Choreboo Premium subscription. */
    val isPremium: StateFlow<Boolean> = billingRepository.isPremium
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    /**
     * Maps local habit ID → display name of the household member who completed it today
     * (only populated for habits NOT completed by the current user).
     * Used to show "Completed by [name]" on household habit cards.
     */
    val householdCompleterNames: StateFlow<Map<Long, String?>> = combine(
        _todayDate.flatMapLatest { date -> habitRepository.getLogsForDate(date) },
        householdRepository.householdMembers,
    ) { logs, members ->
        val memberNameMap = members.associate { it.uid to it.displayName }
        val currentUid = authRepository.currentFirebaseUser?.uid
        logs
            .filter { log -> log.completedByUid != null && log.completedByUid != currentUid }
            .associate { log -> log.habitId to memberNameMap[log.completedByUid] }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    // -----------------------------------------------------------------------
    // Events
    // -----------------------------------------------------------------------

    private val _events = MutableSharedFlow<PetEvent>()
    val events = _events.asSharedFlow()

    // -----------------------------------------------------------------------
    // Init error state (E4/E5)
    // -----------------------------------------------------------------------

    private val _initError = MutableStateFlow(false)
    val initError: StateFlow<Boolean> = _initError.asStateFlow()

    // -----------------------------------------------------------------------
    // Init
    // -----------------------------------------------------------------------

    init {
        runInit()
    }

    private fun runInit() {
        viewModelScope.launch {
            try {
                _initError.value = false
                // Ensure a choreboo exists (handles empty DB after destructive migration)
                chorebooRepository.getOrCreateChoreboo()
                chorebooRepository.applyStatDecay()
            } catch (e: Exception) {
                Timber.e(e, "PetViewModel init failed")
                _initError.value = true
            }
        }
    }

    /** Retry init after an error — clears the error flag and re-runs startup logic. */
    fun retryInit() {
        runInit()
    }

    // -----------------------------------------------------------------------
    // Pet actions
    // -----------------------------------------------------------------------

    /** Manual feed: costs 10 points, adds +20 hunger, triggers eating animation. */
    fun feedChoreboo() {
        viewModelScope.launch {
            try {
                val deducted = userPreferences.deductPoints(10)
                if (deducted) {
                    chorebooRepository.feedChoreboo()
                    _isEating.value = true
                    _events.emit(PetEvent.Fed)
                    // Write-through: sync deducted points so cloud stays current
                    val newPoints = userPreferences.totalPoints.first()
                    val newLifetimeXp = userPreferences.totalLifetimeXp.first()
                    userRepository.syncPointsToCloud(newPoints, newLifetimeXp)
                } else {
                    _events.emit(PetEvent.InsufficientPoints)
                }
            } catch (e: Exception) {
                Timber.e(e, "feedChoreboo failed")
                _events.emit(PetEvent.FeedError)
            }
        }
    }

    /** Put pet to sleep for 24 hours — freezes stat decay. */
    fun sleepChoreboo() {
         viewModelScope.launch {
            try {
                val choreboo = chorebooRepository.getChorebooSync()
                if (choreboo?.isSleeping() == true) {
                    _events.emit(PetEvent.AlreadySleeping)
                } else {
                    chorebooRepository.putToSleep()
                    _events.emit(PetEvent.Sleeping)
                }
            } catch (e: Exception) {
                Timber.e(e, "sleepChoreboo failed")
                _events.emit(PetEvent.SleepError)
            }
        }
    }

    /** Called by UI when the eating animation finishes. */
    fun onEatingAnimationComplete() {
        _isEating.value = false
    }

    /** Manual refresh: syncs choreboo and habits from cloud, then applies stat decay. */
    fun refreshData() {
        viewModelScope.launch {
            _isRefreshing.value = true
            try {
                // Refresh today's date in case the app stayed open past midnight
                _todayDate.value = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
                syncManager.syncAll(force = true)
                chorebooRepository.applyStatDecay()
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    // -----------------------------------------------------------------------
    // Habit actions
    // -----------------------------------------------------------------------

    fun completeHabit(habitId: Long) {
        viewModelScope.launch {
            try {
                val result = habitRepository.completeHabit(habitId)
                if (result.alreadyComplete) {
                    _events.emit(PetEvent.AlreadyComplete)
                    return@launch
                }
                val xpResult = chorebooRepository.addXp(result.xpEarned)

                // Auto-feed: if hunger < 30 and user has enough points, silently feed
                chorebooRepository.autoFeedIfNeeded(userPreferences)

                _events.emit(
                    PetEvent.HabitCompleted(
                        xpEarned = result.xpEarned,
                        streak = result.newStreak,
                        leveledUp = xpResult.levelsGained > 0,
                        newLevel = xpResult.newLevel,
                        evolved = xpResult.evolved,
                        newStage = xpResult.newStage,
                    ),
                )
            } catch (e: Exception) {
                _events.emit(PetEvent.CompletionError)
            }
        }
    }

    fun deleteHabit(id: Long) {
        viewModelScope.launch {
            try {
                habitRepository.deleteHabit(id)
            } catch (e: Exception) {
                Timber.e(e, "deleteHabit failed for id=$id")
                _events.emit(PetEvent.DeleteError)
            }
        }
    }

    // -----------------------------------------------------------------------
    // Background actions
    // -----------------------------------------------------------------------

    /**
     * Purchase a background: deduct points and record the unlock.
     * Emits [PetEvent.BackgroundPurchased] on success or [PetEvent.InsufficientPoints] if
     * the user doesn't have enough points.
     */
    fun purchaseBackground(item: BackgroundItem) {
        viewModelScope.launch {
            try {
                val cost = item.cost
                val newPoints: Int
                if (cost > 0) {
                    val deducted = userPreferences.deductPoints(cost)
                    if (!deducted) {
                        _events.emit(PetEvent.InsufficientPoints)
                        return@launch
                    }
                    // Write-through deducted points
                    newPoints = userPreferences.totalPoints.first()
                    val newLifetimeXp = userPreferences.totalLifetimeXp.first()
                    userRepository.syncPointsToCloud(newPoints, newLifetimeXp)
                } else {
                    newPoints = userPreferences.totalPoints.first()
                }
                backgroundRepository.purchaseBackground(item.id, cost, newPoints)
                _events.emit(PetEvent.BackgroundPurchased(item))
            } catch (e: Exception) {
                Timber.e(e, "purchaseBackground failed for item=${item.id}")
                _events.emit(PetEvent.PurchaseError)
            }
        }
    }

    /**
     * Apply a background that the user already owns (or revert to default).
     * No point cost — just update the choreboo's backgroundId.
     */
    fun selectBackground(backgroundId: String?) {
        viewModelScope.launch {
            chorebooRepository.updateBackground(backgroundId)
        }
    }

    // -----------------------------------------------------------------------
    // Private helpers
    // -----------------------------------------------------------------------

    /** Launches the Google Play subscription purchase flow from the given Activity. */
    fun launchPremiumPurchase(activity: android.app.Activity) {
        billingRepository.launchPurchaseFlow(activity)
    }
}

sealed class PetEvent {
    data object Fed : PetEvent()
    data object InsufficientPoints : PetEvent()
    data object Sleeping : PetEvent()
    data object AlreadySleeping : PetEvent()
    data class HabitCompleted(
        val xpEarned: Int,
        val streak: Int,
        val leveledUp: Boolean = false,
        val newLevel: Int = 0,
        val evolved: Boolean = false,
        val newStage: ChorebooStage? = null,
    ) : PetEvent()
    data object AlreadyComplete : PetEvent()
    data object CompletionError : PetEvent()
    data object FeedError : PetEvent()
    data object SleepError : PetEvent()
    data object DeleteError : PetEvent()
    data object PurchaseError : PetEvent()
    data class BackgroundPurchased(val item: BackgroundItem) : PetEvent()
}
