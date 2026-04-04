package com.example.choreboo_habittrackerfriend.ui.household

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.choreboo_habittrackerfriend.data.repository.HouseholdRepository
import com.example.choreboo_habittrackerfriend.domain.model.Household
import com.example.choreboo_habittrackerfriend.domain.model.HouseholdHabitStatus
import com.example.choreboo_habittrackerfriend.domain.model.HouseholdMember
import com.example.choreboo_habittrackerfriend.domain.model.HouseholdPet
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HouseholdViewModel @Inject constructor(
    private val householdRepository: HouseholdRepository,
) : ViewModel() {

    val currentHousehold: StateFlow<Household?> = householdRepository.currentHousehold
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val householdMembers: StateFlow<List<HouseholdMember>> = householdRepository.householdMembers
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val householdPets: StateFlow<List<HouseholdPet>> = householdRepository.householdPets
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val householdHabits: StateFlow<List<HouseholdHabitStatus>> = householdRepository.householdHabits
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _events = MutableSharedFlow<HouseholdEvent>()
    val events = _events.asSharedFlow()

    init {
        viewModelScope.launch {
            householdRepository.refreshAll()
        }
    }

    fun refreshData() {
        viewModelScope.launch {
            householdRepository.refreshAll()
        }
    }
}

sealed class HouseholdEvent {
    data class ShowSnackbar(val message: String) : HouseholdEvent()
}
