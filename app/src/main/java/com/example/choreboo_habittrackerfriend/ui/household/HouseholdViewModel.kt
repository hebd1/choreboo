package com.example.choreboo_habittrackerfriend.ui.household

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.choreboo_habittrackerfriend.data.datastore.UserPreferences
import com.example.choreboo_habittrackerfriend.data.repository.AuthRepository
import com.example.choreboo_habittrackerfriend.data.repository.HouseholdRepository
import com.example.choreboo_habittrackerfriend.data.repository.UserRepository
import com.example.choreboo_habittrackerfriend.domain.model.Household
import com.example.choreboo_habittrackerfriend.domain.model.HouseholdHabitStatus
import com.example.choreboo_habittrackerfriend.domain.model.HouseholdMember
import com.example.choreboo_habittrackerfriend.domain.model.HouseholdPet
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HouseholdViewModel @Inject constructor(
    private val householdRepository: HouseholdRepository,
    private val userRepository: UserRepository,
    private val userPreferences: UserPreferences,
    private val authRepository: AuthRepository,
) : ViewModel() {

    val currentHousehold: StateFlow<Household?> = householdRepository.currentHousehold
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val householdMembers: StateFlow<List<HouseholdMember>> = householdRepository.householdMembers
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /**
     * Household pets enriched with the current user's best available profile photo.
     *
     * The cloud `User.photoUrl` field is only populated for Google sign-in users.
     * Email/password users who pick a profile photo via Settings have it stored as a
     * local file path in DataStore (`profilePhotoUri`). This combine block patches the
     * current user's pet card with the local/Google photo when the cloud value is null.
     */
    val householdPets: StateFlow<List<HouseholdPet>> = combine(
        householdRepository.householdPets,
        userPreferences.profilePhotoUri,
        authRepository.currentUser.map { it?.photoUrl?.toString() },
    ) { pets, localPhoto, googlePhoto ->
        val currentUid = userRepository.getCurrentUid()
        pets.map { pet ->
            if (pet.ownerUid == currentUid && pet.ownerPhotoUrl.isNullOrBlank()) {
                pet.copy(ownerPhotoUrl = localPhoto ?: googlePhoto)
            } else {
                pet
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val householdHabits: StateFlow<List<HouseholdHabitStatus>> = householdRepository.householdHabits
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _events = MutableSharedFlow<HouseholdEvent>()
    val events = _events.asSharedFlow()

    /** The pet card the user tapped — non-null triggers the habits popup. */
    private val _selectedPet = MutableStateFlow<HouseholdPet?>(null)
    val selectedPet: StateFlow<HouseholdPet?> = _selectedPet.asStateFlow()

    /** True while a manual refresh is in progress */
    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    init {
        viewModelScope.launch {
            householdRepository.refreshAll()
        }
    }

    fun refreshData() {
        viewModelScope.launch {
            _isRefreshing.value = true
            try {
                householdRepository.refreshAll()
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    fun selectPet(pet: HouseholdPet) {
        _selectedPet.value = pet
    }

    fun clearSelectedPet() {
        _selectedPet.value = null
    }
}

sealed class HouseholdEvent {
    data class ShowSnackbar(val message: String) : HouseholdEvent()
}
