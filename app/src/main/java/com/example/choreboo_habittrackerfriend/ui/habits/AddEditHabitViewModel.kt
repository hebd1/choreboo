package com.example.choreboo_habittrackerfriend.ui.habits

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.choreboo_habittrackerfriend.data.repository.HabitRepository
import com.example.choreboo_habittrackerfriend.domain.model.Habit
import com.example.choreboo_habittrackerfriend.domain.model.HabitFrequency
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HabitFormState(
    val title: String = "",
    val description: String = "",
    val iconName: String = "CheckCircle",
    val frequency: HabitFrequency = HabitFrequency.DAILY,
    val customDays: List<String> = emptyList(),
    val targetCount: Int = 1,
    val baseXp: Int = 10,
    val isEditing: Boolean = false,
)

@HiltViewModel
class AddEditHabitViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val habitRepository: HabitRepository,
) : ViewModel() {

    private val habitId: Long = savedStateHandle.get<Long>("habitId") ?: -1L

    private val _formState = MutableStateFlow(HabitFormState())
    val formState = _formState.asStateFlow()

    private val _events = MutableSharedFlow<AddEditHabitEvent>()
    val events = _events.asSharedFlow()

    init {
        if (habitId > 0) {
            viewModelScope.launch {
                habitRepository.getHabitById(habitId).collect { habit ->
                    if (habit != null) {
                        _formState.value = HabitFormState(
                            title = habit.title,
                            description = habit.description ?: "",
                            iconName = habit.iconName,
                            frequency = habit.frequency,
                            customDays = habit.customDays ?: emptyList(),
                            targetCount = habit.targetCount,
                            baseXp = habit.baseXp,
                            isEditing = true,
                        )
                    }
                }
            }
        }
    }

    fun updateTitle(title: String) {
        _formState.update { it.copy(title = title) }
    }

    fun updateDescription(description: String) {
        _formState.update { it.copy(description = description) }
    }

    fun updateIconName(iconName: String) {
        _formState.update { it.copy(iconName = iconName) }
    }

    fun updateFrequency(frequency: HabitFrequency) {
        _formState.update { it.copy(frequency = frequency) }
    }

    fun toggleCustomDay(day: String) {
        _formState.update { state ->
            val days = state.customDays.toMutableList()
            if (day in days) days.remove(day) else days.add(day)
            state.copy(customDays = days)
        }
    }

    fun updateTargetCount(count: Int) {
        _formState.update { it.copy(targetCount = count.coerceIn(1, 20)) }
    }

    fun updateBaseXp(xp: Int) {
        _formState.update { it.copy(baseXp = xp.coerceIn(1, 100)) }
    }

    fun saveHabit() {
        val state = _formState.value
        if (state.title.isBlank()) {
            viewModelScope.launch { _events.emit(AddEditHabitEvent.ValidationError("Title is required")) }
            return
        }
        viewModelScope.launch {
            val habit = Habit(
                id = if (habitId > 0) habitId else 0,
                title = state.title.trim(),
                description = state.description.trim().ifBlank { null },
                iconName = state.iconName,
                frequency = state.frequency,
                customDays = if (state.frequency == HabitFrequency.CUSTOM) state.customDays else null,
                targetCount = state.targetCount,
                baseXp = state.baseXp,
            )
            habitRepository.upsertHabit(habit)
            _events.emit(AddEditHabitEvent.Saved)
        }
    }
}

sealed class AddEditHabitEvent {
    data object Saved : AddEditHabitEvent()
    data class ValidationError(val message: String) : AddEditHabitEvent()
}

