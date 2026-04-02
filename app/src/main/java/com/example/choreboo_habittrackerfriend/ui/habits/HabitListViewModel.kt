package com.example.choreboo_habittrackerfriend.ui.habits

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.choreboo_habittrackerfriend.data.datastore.UserPreferences
import com.example.choreboo_habittrackerfriend.data.repository.HabitRepository
import com.example.choreboo_habittrackerfriend.data.repository.AuthRepository
import com.example.choreboo_habittrackerfriend.data.repository.ChorebooRepository
import com.example.choreboo_habittrackerfriend.domain.model.Habit
import com.example.choreboo_habittrackerfriend.domain.model.PetType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject

@HiltViewModel
class HabitListViewModel @Inject constructor(
    private val habitRepository: HabitRepository,
    private val chorebooRepository: ChorebooRepository,
    private val userPreferences: UserPreferences,
    private val authRepository: AuthRepository,
) : ViewModel() {

    val habits: StateFlow<List<Habit>> = habitRepository.getAllHabits()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val totalPoints: StateFlow<Int> = userPreferences.totalPoints
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val profilePhotoUri: StateFlow<String?> = userPreferences.profilePhotoUri
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val googlePhotoUrl: String?
        get() = authRepository.currentFirebaseUser?.photoUrl?.toString()

    private val _todayCompletions = MutableStateFlow<Map<Long, Int>>(emptyMap())
    val todayCompletions: StateFlow<Map<Long, Int>> = _todayCompletions.asStateFlow()

    val streaks: StateFlow<Map<Long, Int>> = habitRepository.getStreaksForToday()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    val petType: StateFlow<PetType> = chorebooRepository.getChoreboo()
        .map { it?.petType ?: PetType.FOX }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), PetType.FOX)

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
