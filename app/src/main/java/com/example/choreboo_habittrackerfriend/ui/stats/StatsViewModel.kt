package com.example.choreboo_habittrackerfriend.ui.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.choreboo_habittrackerfriend.data.datastore.UserPreferences
import com.example.choreboo_habittrackerfriend.data.repository.AuthRepository
import com.example.choreboo_habittrackerfriend.data.repository.BadgeRepository
import com.example.choreboo_habittrackerfriend.data.repository.ChorebooRepository
import com.example.choreboo_habittrackerfriend.data.repository.HabitRepository
import com.example.choreboo_habittrackerfriend.data.repository.SyncManager
import com.example.choreboo_habittrackerfriend.domain.model.Badge
import com.example.choreboo_habittrackerfriend.domain.model.ChorebooStats
import com.example.choreboo_habittrackerfriend.domain.model.PetType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import javax.inject.Inject

@HiltViewModel
class StatsViewModel @Inject constructor(
    private val habitRepository: HabitRepository,
    private val chorebooRepository: ChorebooRepository,
    private val userPreferences: UserPreferences,
    private val authRepository: AuthRepository,
    private val badgeRepository: BadgeRepository,
    private val syncManager: SyncManager,
) : ViewModel() {

    /** True while a manual pull-to-refresh is in progress */
    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    /** True until the first badge/data load from Room — drives the initial loading spinner */
    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    val totalPoints: StateFlow<Int> = userPreferences.totalPoints
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val totalLifetimeXp: StateFlow<Int> = userPreferences.totalLifetimeXp
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val earnedBadgeCount: StateFlow<Int> = badgeRepository.getEarnedBadgeCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val allBadges: StateFlow<List<Badge>> = badgeRepository.getAllBadges()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recentBadges: StateFlow<List<Badge>> = allBadges
        .map { badges ->
            badges
                .filter { it.isUnlocked }
                .sortedByDescending { it.definition.threshold }
                .take(3)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val profilePhotoUri: StateFlow<String?> = userPreferences.profilePhotoUri
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val googlePhotoUrl: StateFlow<String?> = authRepository.currentUser
        .map { it?.photoUrl?.toString() }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            authRepository.currentFirebaseUser?.photoUrl?.toString(),
        )

    val streaks: StateFlow<Map<Long, Int>> = habitRepository.getStreaksForToday()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    val maxStreak: StateFlow<Int> = streaks
        .map { streakMap -> streakMap.values.maxOrNull() ?: 0 }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val weeklyCompletionDays: StateFlow<Set<DayOfWeek>> = habitRepository.getCompletionDaysForCurrentWeek()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    /** Single shared choreboo observer for petType and chorebooStats. */
    private val chorebooFlow = chorebooRepository.getChoreboo()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val petType: StateFlow<PetType> = chorebooFlow
        .map { it?.petType ?: PetType.FOX }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), PetType.FOX)

    val chorebooStats: StateFlow<ChorebooStats?> = chorebooFlow

    /**
     * Reactive today date — holds an ISO-8601 string. Initialized at construction;
     * external callers can trigger a refresh (e.g. on screen resume) via [refreshTodayDate].
     */
    private val _todayDate = MutableStateFlow(
        LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE),
    )

    /** Manual pull-to-refresh: force-syncs from cloud; Room flows update automatically. */
    fun refreshData() {
        viewModelScope.launch {
            _isRefreshing.value = true
            try {
                refreshTodayDate()
                syncManager.syncAll(force = true)
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    /** Refreshes the today date — call when the screen becomes visible to handle midnight crossings. */
    fun refreshTodayDate() {
        _todayDate.value = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
    }

    private val todayLogsFlow = _todayDate
        .flatMapLatest { date -> habitRepository.getLogsForDate(date) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Directly observes today's logs — no dependency on _todayCompletions so XP
    // is correct immediately on cold launch even if completions exist from earlier.
    val todayXp: StateFlow<Int> = todayLogsFlow
        .map { logs -> logs.sumOf { it.xpEarned } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    /** Fraction of today's scheduled habits that have been completed — drives the weekly streak card. */
    val habits = habitRepository.getAllHabits()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val todayCompletions: StateFlow<Map<Long, Int>> = todayLogsFlow
        .map { logs -> logs.groupBy { it.habitId }.mapValues { it.value.size } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    /** Percentage of days in the current month (up to today) that have at least one habit completion. */
    val monthlyCompletionRate: StateFlow<Int> = _todayDate
        .flatMapLatest { dateStr ->
            val yearMonth = YearMonth.parse(dateStr.substring(0, 7))
            habitRepository.getLogsForMonth(yearMonth.toString())
                .map { logs ->
                    val today = LocalDate.parse(dateStr)
                    val daysElapsed = today.dayOfMonth
                    val daysWithAny = logs.groupBy { it.date }.keys.size
                    if (daysElapsed > 0) (daysWithAny * 100 / daysElapsed) else 0
                }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    init {
        // Clear initial loading state once the first badges value arrives from Room
        viewModelScope.launch {
            allBadges.first()
            _isLoading.value = false
        }
    }
}
