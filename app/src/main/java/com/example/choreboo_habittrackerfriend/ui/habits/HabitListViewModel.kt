package com.example.choreboo_habittrackerfriend.ui.habits

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.choreboo_habittrackerfriend.data.datastore.UserPreferences
import com.example.choreboo_habittrackerfriend.data.repository.BadgeRepository
import com.example.choreboo_habittrackerfriend.data.repository.HabitRepository
import com.example.choreboo_habittrackerfriend.data.repository.AuthRepository
import com.example.choreboo_habittrackerfriend.data.repository.ChorebooRepository
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
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject

@HiltViewModel
class HabitListViewModel @Inject constructor(
    private val habitRepository: HabitRepository,
    private val chorebooRepository: ChorebooRepository,
    private val userPreferences: UserPreferences,
    private val authRepository: AuthRepository,
    private val badgeRepository: BadgeRepository,
) : ViewModel() {

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    val habits: StateFlow<List<Habit>> = habitRepository.getAllHabits()
        .onEach { _isLoading.value = false }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val totalPoints: StateFlow<Int> = userPreferences.totalPoints
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    /** Total lifetime XP earned — star points = totalLifetimeXp / 100. */
    val totalLifetimeXp: StateFlow<Int> = userPreferences.totalLifetimeXp
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    /** Number of badges the user has earned. */
    val earnedBadgeCount: StateFlow<Int> = badgeRepository.getEarnedBadgeCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val profilePhotoUri: StateFlow<String?> = userPreferences.profilePhotoUri
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val googlePhotoUrl: String?
        get() = authRepository.currentFirebaseUser?.photoUrl?.toString()

    private val _todayCompletions = MutableStateFlow<Map<Long, Int>>(emptyMap())
    val todayCompletions: StateFlow<Map<Long, Int>> = _todayCompletions.asStateFlow()

    val streaks: StateFlow<Map<Long, Int>> = habitRepository.getStreaksForToday()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    /** Longest active streak across all habits — drives the bento streak counter. */
    val maxStreak: StateFlow<Int> = habitRepository.getStreaksForToday()
        .map { streakMap -> streakMap.values.maxOrNull() ?: 0 }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    /** Days of the current week (Mon-Sun) that have at least one completion. */
    val weeklyCompletionDays: StateFlow<Set<DayOfWeek>> = habitRepository.getCompletionDaysForCurrentWeek()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    val petType: StateFlow<PetType> = chorebooRepository.getChoreboo()
        .map { it?.petType ?: PetType.FOX }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), PetType.FOX)

    /** Live Choreboo stats for the pet bento card at the bottom of the habits list. */
    val chorebooStats: StateFlow<ChorebooStats?> = chorebooRepository.getChoreboo()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    /** Total XP earned from completed habits today. */
    val todayXp: StateFlow<Int> = _todayCompletions
        .flatMapLatest { completionsMap ->
            if (completionsMap.isEmpty()) {
                flowOf(0)
            } else {
                val today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
                habitRepository.getLogsForDate(today).map { logs ->
                    logs.sumOf { it.xpEarned }
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    private val _events = MutableSharedFlow<HabitListEvent>()
    val events = _events.asSharedFlow()

    init {
        refreshTodayCompletions()
    }

    private fun refreshTodayCompletions() {
        viewModelScope.launch {
            val today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
            habitRepository.getLogsForDate(today).collect { logs ->
                val map = logs.groupBy { it.habitId }.mapValues { it.value.size }
                _todayCompletions.value = map
            }
        }
    }

    fun completeHabit(habitId: Long) {
        viewModelScope.launch {
            val result = habitRepository.completeHabit(habitId)
            if (result.alreadyComplete) {
                _events.emit(HabitListEvent.AlreadyComplete)
                return@launch
            }
            val xpResult = chorebooRepository.addXp(result.xpEarned)

            // Auto-feed: if hunger < 30 and user has enough points, silently feed
            chorebooRepository.autoFeedIfNeeded(userPreferences)

            _events.emit(
                HabitListEvent.HabitCompleted(
                    xpEarned = result.xpEarned,
                    streak = result.newStreak,
                    leveledUp = xpResult.levelsGained > 0,
                    newLevel = xpResult.newLevel,
                    evolved = xpResult.evolved,
                    newStageName = xpResult.newStage?.displayName,
                )
            )
        }
    }

    fun deleteHabit(id: Long) {
        viewModelScope.launch {
            habitRepository.deleteHabit(id)
        }
    }
}

sealed class HabitListEvent {
    data class HabitCompleted(
        val xpEarned: Int,
        val streak: Int,
        val leveledUp: Boolean = false,
        val newLevel: Int = 0,
        val evolved: Boolean = false,
        val newStageName: String? = null,
    ) : HabitListEvent()
    data object AlreadyComplete : HabitListEvent()
}
