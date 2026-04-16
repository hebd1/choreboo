package com.choreboo.app.ui.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.choreboo.app.data.local.dao.HabitLogWithName
import com.choreboo.app.data.datastore.UserPreferences
import com.choreboo.app.data.repository.AuthRepository
import com.choreboo.app.data.repository.HabitRepository
import com.choreboo.app.data.repository.SyncManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class CalendarViewModel @Inject constructor(
    private val habitRepository: HabitRepository,
    private val userPreferences: UserPreferences,
    private val authRepository: AuthRepository,
    private val syncManager: SyncManager,
) : ViewModel() {

    private val _selectedMonth = MutableStateFlow(YearMonth.now())
    val selectedMonth: StateFlow<YearMonth> = _selectedMonth.asStateFlow()

    private val _selectedDate = MutableStateFlow<LocalDate?>(LocalDate.now())
    val selectedDate: StateFlow<LocalDate?> = _selectedDate.asStateFlow()

    /** True while a manual refresh is in progress */
    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    /** True until the first completions load from Room — drives initial skeleton/placeholder */
    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    val totalPoints: StateFlow<Int> = userPreferences.totalPoints
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val profilePhotoUri: StateFlow<String?> = userPreferences.profilePhotoUri
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val googlePhotoUrl: StateFlow<String?> = authRepository.currentUser
        .map { it?.photoUrl?.toString() }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            authRepository.currentFirebaseUser?.photoUrl?.toString(),
        )

    /** Map of date → number of completions for the currently selected month */
    val completionsForMonth: StateFlow<Map<LocalDate, Int>> = _selectedMonth
        .flatMapLatest { month ->
            habitRepository.getLogsForMonth(month.toString())
                .map { logs ->
                    logs.mapNotNull { log ->
                        try {
                            LocalDate.parse(log.date) to log
                        } catch (_: Exception) {
                            null // skip malformed dates
                        }
                    }
                        .groupBy { it.first }
                        .mapValues { (_, pairs) -> pairs.size }
                }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    /** Logs for the currently selected date (with habit names) */
    val selectedDateLogs: StateFlow<List<HabitLogWithName>> = _selectedDate
        .flatMapLatest { date ->
            if (date != null) {
                habitRepository.getLogsWithNamesForDate(date.toString())
            } else {
                flowOf(emptyList())
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        // Clear initial loading state after the first real Room emission.
        // We collect directly from the upstream repo flow (not from the stateIn StateFlow)
        // so we wait for an actual database result, not just the stateIn default value.
        viewModelScope.launch {
            habitRepository.getLogsForMonth(_selectedMonth.value.toString()).first()
            _isLoading.value = false
        }
    }

    fun previousMonth() {
        _selectedMonth.value = _selectedMonth.value.minusMonths(1)
        _selectedDate.value = null
    }

    fun nextMonth() {
        _selectedMonth.value = _selectedMonth.value.plusMonths(1)
        _selectedDate.value = null
    }

    fun selectDate(date: LocalDate) {
        _selectedDate.value = if (_selectedDate.value == date) null else date
    }

    /** Manual refresh: syncs habits and logs from cloud, then Room flows update automatically. */
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

    /**
     * Re-evaluates what "today" is. If the currently selected month/date were tracking
     * the day the ViewModel was constructed (i.e. the user hasn't navigated away), they
     * are updated to the real current date. Call on screen resume to handle midnight crossings.
     */
    fun refreshTodayDate() {
        val today = LocalDate.now()
        val todayMonth = YearMonth.from(today)
        // Only advance the month forward — never move the user backward
        if (_selectedMonth.value.isBefore(todayMonth)) {
            _selectedMonth.value = todayMonth
        }
        // If the selected date is in the future (e.g. ViewModel init ran just before midnight),
        // clamp it back to today
        val sel = _selectedDate.value
        if (sel != null && sel.isAfter(today)) {
            _selectedDate.value = today
        }
    }
}



