package com.example.weeboo_habittrackerfriend.ui.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.weeboo_habittrackerfriend.data.local.dao.HabitLogWithName
import com.example.weeboo_habittrackerfriend.data.repository.HabitRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class CalendarViewModel @Inject constructor(
    private val habitRepository: HabitRepository,
) : ViewModel() {

    private val _selectedMonth = MutableStateFlow(YearMonth.now())
    val selectedMonth: StateFlow<YearMonth> = _selectedMonth.asStateFlow()

    private val _selectedDate = MutableStateFlow<LocalDate?>(LocalDate.now())
    val selectedDate: StateFlow<LocalDate?> = _selectedDate.asStateFlow()

    /** Map of date → number of completions for the currently selected month */
    val completionsForMonth: StateFlow<Map<LocalDate, Int>> = _selectedMonth
        .flatMapLatest { month ->
            habitRepository.getLogsForMonth(month.toString())
                .map { logs ->
                    logs.groupBy { log ->
                        LocalDate.parse(log.date)
                    }.mapValues { (_, logList) -> logList.size }
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
}



