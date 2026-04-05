package com.example.choreboo_habittrackerfriend.ui.habits

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.choreboo_habittrackerfriend.data.repository.HabitRepository
import com.example.choreboo_habittrackerfriend.data.repository.HouseholdRepository
import com.example.choreboo_habittrackerfriend.data.datastore.UserPreferences
import com.example.choreboo_habittrackerfriend.data.repository.AuthRepository
import com.example.choreboo_habittrackerfriend.domain.model.Habit
import com.example.choreboo_habittrackerfriend.worker.HabitReminderScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import com.example.choreboo_habittrackerfriend.domain.model.Household
import com.example.choreboo_habittrackerfriend.domain.model.HouseholdMember
import java.time.LocalTime
import javax.inject.Inject

data class HabitFormState(
    val title: String = "",
    val description: String = "",
    val iconName: String = "emoji_salad",
    val customDays: List<String> = listOf("MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN"),
    val difficulty: Int = 1,
    val baseXp: Int = 10,
    val reminderEnabled: Boolean = false,
    val reminderTime: LocalTime = LocalTime.of(9, 0),
    val frequencyMode: FrequencyMode = FrequencyMode.WEEKLY,
    val suggestedXp: Int? = null,
    val isEditing: Boolean = false,
    val isHouseholdHabit: Boolean = false,
    val assignedToUid: String? = null,
    val assignedToName: String? = null,
)

enum class FrequencyMode {
    WEEKLY, MONTHLY,
}

@HiltViewModel
class AddEditHabitViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val habitRepository: HabitRepository,
    private val householdRepository: HouseholdRepository,
    private val userPreferences: UserPreferences,
    private val authRepository: AuthRepository,
    @ApplicationContext private val context: Context,
) : ViewModel() {

    private val habitId: Long = savedStateHandle.get<Long>("habitId") ?: -1L

    /** Cached existing habit for preserving non-editable fields on save. */
    private var existingHabit: Habit? = null

    private val _formState = MutableStateFlow(HabitFormState())
    val formState = _formState.asStateFlow()

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

    private val _events = MutableSharedFlow<AddEditHabitEvent>()
    val events = _events.asSharedFlow()

    val profilePhotoUri: StateFlow<String?> = userPreferences.profilePhotoUri
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val googlePhotoUrl: StateFlow<String?> = authRepository.currentUser
        .map { it?.photoUrl?.toString() }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            authRepository.currentFirebaseUser?.photoUrl?.toString(),
        )

    /** Current household the user belongs to — used for resolving householdId on save. */
    val currentHousehold: StateFlow<Household?> = householdRepository.currentHousehold
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    /** Members of the user's household — used for the assignee picker. */
    val householdMembers: StateFlow<List<HouseholdMember>> = householdRepository.householdMembers
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        if (habitId > 0) {
            viewModelScope.launch {
                val habit = habitRepository.getHabitById(habitId).firstOrNull()
                if (habit != null) {
                    existingHabit = habit
                    _formState.value = HabitFormState(
                        title = habit.title,
                        description = habit.description ?: "",
                        iconName = habit.iconName,
                        customDays = habit.customDays,
                        difficulty = habit.difficulty,
                        baseXp = habit.baseXp,
                        reminderEnabled = habit.reminderEnabled,
                        reminderTime = habit.reminderTime ?: LocalTime.of(9, 0),
                        isEditing = true,
                        isHouseholdHabit = habit.isHouseholdHabit,
                        assignedToUid = habit.assignedToUid,
                        assignedToName = habit.assignedToName,
                    )
                }
            }
        }

        // Watch title and description for XP suggestion updates
        viewModelScope.launch {
            _formState
                .map { it.title to it.description }
                .distinctUntilChanged()
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

    fun updateIsHouseholdHabit(isHousehold: Boolean) {
        _formState.update {
            it.copy(
                isHouseholdHabit = isHousehold,
                // Clear assignee when household habit is turned off
                assignedToUid = if (isHousehold) it.assignedToUid else null,
                assignedToName = if (isHousehold) it.assignedToName else null,
            )
        }
    }

    fun updateAssignedTo(uid: String?, name: String?) {
        _formState.update { it.copy(assignedToUid = uid, assignedToName = name) }
    }

    fun toggleCustomDay(day: String) {
        _formState.update { state ->
            val days = state.customDays.toMutableList()
            if (day in days) days.remove(day) else days.add(day)
            state.copy(customDays = days)
        }
    }

    fun updateDifficulty(difficulty: Int) {
        _formState.update { state ->
            val newDifficulty = difficulty.coerceIn(1, 3)
            val newXp = xpForDifficulty(newDifficulty)
            state.copy(difficulty = newDifficulty, baseXp = newXp)
        }
    }

    private fun xpForDifficulty(difficulty: Int): Int {
        return when (difficulty) {
            1 -> 10      // Easy
            2 -> 25      // Medium
            3 -> 40      // Hard
            else -> 10
        }
    }

    fun updateFrequencyMode(mode: FrequencyMode) {
        _formState.update { state ->
            val newDays = when (mode) {
                FrequencyMode.WEEKLY -> {
                    // Switch to weekly: use existing days or default to all days
                    if (state.frequencyMode == FrequencyMode.MONTHLY) {
                        listOf("MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN")
                    } else {
                        state.customDays
                    }
                }
                FrequencyMode.MONTHLY -> {
                    // Switch to monthly: convert or default to 1st and 15th
                    if (state.frequencyMode == FrequencyMode.WEEKLY) {
                        listOf("D1", "D15")
                    } else {
                        state.customDays
                    }
                }
            }
            state.copy(frequencyMode = mode, customDays = newDays)
        }
    }

    fun toggleMonthlyDay(dayOfMonth: Int) {
        _formState.update { state ->
            val dayKey = "D$dayOfMonth"
            val days = state.customDays.toMutableList()
            if (dayKey in days) days.remove(dayKey) else days.add(dayKey)
            state.copy(customDays = days.sorted())
        }
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
            _isSaving.value = true
            try {
                // Validate: household habit requires the user to be in a household
                if (state.isHouseholdHabit && existingHabit?.householdId == null && currentHousehold.value == null) {
                    _events.emit(AddEditHabitEvent.ValidationError("Join a household first to create shared habits"))
                    return@launch
                }

                // Resolve householdId: keep existing value, or look up from current household
                val resolvedHouseholdId = if (state.isHouseholdHabit) {
                    existingHabit?.householdId ?: currentHousehold.value?.id
                } else {
                    null
                }

                val habit = Habit(
                    id = if (habitId > 0) habitId else 0,
                    title = state.title.trim(),
                    description = state.description.trim().ifBlank { null },
                    iconName = state.iconName,
                    customDays = state.customDays,
                    difficulty = state.difficulty,
                    baseXp = state.baseXp,
                    reminderEnabled = state.reminderEnabled,
                    reminderTime = if (state.reminderEnabled) state.reminderTime else null,
                    isHouseholdHabit = state.isHouseholdHabit,
                    remoteId = existingHabit?.remoteId,
                    ownerUid = existingHabit?.ownerUid,
                    householdId = resolvedHouseholdId,
                    createdAt = existingHabit?.createdAt ?: System.currentTimeMillis(),
                    isArchived = existingHabit?.isArchived ?: false,
                    assignedToUid = if (state.isHouseholdHabit) state.assignedToUid else null,
                    assignedToName = if (state.isHouseholdHabit) state.assignedToName else null,
                )
                val savedHabitId = habitRepository.upsertHabit(habit)

                // Schedule or cancel reminder (use the returned ID for new habits)
                if (state.reminderEnabled && state.reminderTime != null) {
                    HabitReminderScheduler.scheduleReminder(
                        context,
                        savedHabitId,
                        state.title.trim(),
                        state.reminderTime,
                        state.customDays,
                    )
                } else {
                    HabitReminderScheduler.cancelReminder(context, savedHabitId)
                }

                _events.emit(AddEditHabitEvent.Saved(isNew = habitId == -1L))
            } finally {
                _isSaving.value = false
            }
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
        val lowEffort = listOf("water", "drink", "vitamin", "pill", "brush", "teeth", "skincare", "floss")
        if (lowEffort.any { text.contains(it) }) {
            return 10
        }

        // Default medium
        return 15
    }
}

sealed class AddEditHabitEvent {
    data class Saved(val isNew: Boolean) : AddEditHabitEvent()
    data class ValidationError(val message: String) : AddEditHabitEvent()
}

