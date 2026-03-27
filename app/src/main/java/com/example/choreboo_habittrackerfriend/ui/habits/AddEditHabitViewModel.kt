package com.example.choreboo_habittrackerfriend.ui.habits

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.choreboo_habittrackerfriend.data.repository.HabitRepository
import com.example.choreboo_habittrackerfriend.domain.model.Habit
import com.example.choreboo_habittrackerfriend.worker.HabitReminderScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.snapshotFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalTime
import javax.inject.Inject

data class HabitFormState(
    val title: String = "",
    val description: String = "",
    val iconName: String = "CheckCircle",
    val customDays: List<String> = listOf("MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN"),
    val targetCount: Int = 1,
    val baseXp: Int = 10,
    val reminderEnabled: Boolean = false,
    val reminderTime: LocalTime = LocalTime.of(9, 0),
    val suggestedXp: Int? = null,
    val isEditing: Boolean = false,
)

@HiltViewModel
class AddEditHabitViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val habitRepository: HabitRepository,
    @ApplicationContext private val context: Context,
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
                            customDays = habit.customDays,
                            targetCount = habit.targetCount,
                            baseXp = habit.baseXp,
                            reminderEnabled = habit.reminderEnabled,
                            reminderTime = habit.reminderTime ?: LocalTime.of(9, 0),
                            isEditing = true,
                        )
                    }
                }
            }
        }

        // Watch title and description for XP suggestion updates
        viewModelScope.launch {
            snapshotFlow { _formState.value.let { it.title to it.description } }
                .debounce(300)
                .collect { (title, description) ->
                    val suggested = calculateSuggestedXp(title, description)
                    _formState.update { it.copy(suggestedXp = suggested) }
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

    fun updateReminderEnabled(enabled: Boolean) {
        _formState.update { it.copy(reminderEnabled = enabled) }
    }

    fun updateReminderTime(time: LocalTime) {
        _formState.update { it.copy(reminderTime = time) }
    }

    fun applySuggestedXp() {
        _formState.value.suggestedXp?.let { suggested ->
            _formState.update { it.copy(baseXp = suggested.coerceIn(1, 100)) }
        }
    }

    fun saveHabit() {
        val state = _formState.value
        if (state.title.isBlank()) {
            viewModelScope.launch { _events.emit(AddEditHabitEvent.ValidationError("Title is required")) }
            return
        }

        // Validate that at least one day is selected
        if (state.customDays.isEmpty()) {
            viewModelScope.launch { _events.emit(AddEditHabitEvent.ValidationError("Select at least one day")) }
            return
        }

        viewModelScope.launch {
            val habit = Habit(
                id = if (habitId > 0) habitId else 0,
                title = state.title.trim(),
                description = state.description.trim().ifBlank { null },
                iconName = state.iconName,
                customDays = state.customDays,
                targetCount = state.targetCount,
                baseXp = state.baseXp,
                reminderEnabled = state.reminderEnabled,
                reminderTime = if (state.reminderEnabled) state.reminderTime else null,
            )
            habitRepository.upsertHabit(habit)

            // Schedule or cancel reminder
            if (habit.reminderEnabled && habit.reminderTime != null) {
                HabitReminderScheduler.scheduleReminder(
                    context,
                    habit.id,
                    habit.title,
                    habit.reminderTime,
                    habit.customDays,
                )
            } else {
                HabitReminderScheduler.cancelReminder(context, habit.id)
            }

            _events.emit(AddEditHabitEvent.Saved)
        }
    }

    private fun calculateSuggestedXp(title: String, description: String): Int {
        val text = (title + " " + description).lowercase()

        // High effort keywords
        val highEffort = listOf("exercise", "workout", "gym", "run", "jog", "study", "code", "programming", "clean", "organize", "train", "practice", "swim")
        if (highEffort.any { text.contains(it) }) {
            return 35
        }

        // Medium effort keywords
        val mediumEffort = listOf("read", "cook", "meditate", "stretch", "journal", "walk", "yoga", "hobby", "project")
        if (mediumEffort.any { text.contains(it) }) {
            return 20
        }

        // Low effort keywords
        val lowEffort = listOf("water", "drink", "vitamin", "pill", "brush", "teeth", "skincare", "floss", "stretch")
        if (lowEffort.any { text.contains(it) }) {
            return 10
        }

        // Default medium
        return 15
    }
}

sealed class AddEditHabitEvent {
    data object Saved : AddEditHabitEvent()
    data class ValidationError(val message: String) : AddEditHabitEvent()
}

