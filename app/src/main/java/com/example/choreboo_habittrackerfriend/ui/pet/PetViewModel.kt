package com.example.choreboo_habittrackerfriend.ui.pet

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.choreboo_habittrackerfriend.data.datastore.UserPreferences
import com.example.choreboo_habittrackerfriend.data.repository.AuthRepository
import com.example.choreboo_habittrackerfriend.data.repository.ChorebooRepository
import com.example.choreboo_habittrackerfriend.data.repository.HabitRepository
import com.example.choreboo_habittrackerfriend.data.repository.HouseholdRepository
import com.example.choreboo_habittrackerfriend.domain.model.ChorebooMood
import com.example.choreboo_habittrackerfriend.domain.model.ChorebooStats
import com.example.choreboo_habittrackerfriend.domain.model.Habit
import com.example.choreboo_habittrackerfriend.domain.model.PetType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
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
        .map { it?.isSleeping ?: false }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    /** True while the eating Lottie animation should be playing */
    private val _isEating = MutableStateFlow(false)
    val isEating: StateFlow<Boolean> = _isEating.asStateFlow()

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
    val todayCompletions: StateFlow<Map<Long, Int>> =
        habitRepository.getLogsForDate(LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE))
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

    /**
     * Maps local habit ID → display name of the household member who completed it today
     * (only populated for habits NOT completed by the current user).
     * Used to show "Completed by [name]" on household habit cards.
     */
    val householdCompleterNames: StateFlow<Map<Long, String?>> = combine(
        habitRepository.getLogsForDate(LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)),
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
    // Init
    // -----------------------------------------------------------------------

    init {
        viewModelScope.launch {
            // Ensure a choreboo exists (handles empty DB after destructive migration)
            chorebooRepository.getOrCreateChoreboo()
            chorebooRepository.applyStatDecay()
        }
    }

    // -----------------------------------------------------------------------
    // Pet actions
    // -----------------------------------------------------------------------

    /** Manual feed: costs 10 points, adds +20 hunger, triggers eating animation. */
    fun feedChoreboo() {
        viewModelScope.launch {
            val deducted = userPreferences.deductPoints(10)
            if (deducted) {
                chorebooRepository.feedChoreboo()
                _isEating.value = true
                _events.emit(PetEvent.Fed)
            } else {
                _events.emit(PetEvent.InsufficientPoints)
            }
        }
    }

    /** Put pet to sleep for 24 hours — freezes stat decay. */
    fun sleepChoreboo() {
        viewModelScope.launch {
            val choreboo = chorebooRepository.getChorebooSync()
            if (choreboo?.isSleeping == true) {
                _events.emit(PetEvent.AlreadySleeping)
            } else {
                chorebooRepository.putToSleep()
                _events.emit(PetEvent.Sleeping)
            }
        }
    }

    /** Called by UI when the eating animation finishes. */
    fun onEatingAnimationComplete() {
        _isEating.value = false
    }

    // -----------------------------------------------------------------------
    // Habit actions
    // -----------------------------------------------------------------------

    fun completeHabit(habitId: Long) {
        viewModelScope.launch {
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
                    newStageName = xpResult.newStage?.displayName,
                ),
            )
        }
    }

    fun deleteHabit(id: Long) {
        viewModelScope.launch {
            habitRepository.deleteHabit(id)
        }
    }

    // -----------------------------------------------------------------------
    // Private helpers
    // -----------------------------------------------------------------------
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
        val newStageName: String? = null,
    ) : PetEvent()
    data object AlreadyComplete : PetEvent()
}
