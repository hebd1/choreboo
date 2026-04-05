package com.example.choreboo_habittrackerfriend.ui.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.choreboo_habittrackerfriend.data.datastore.UserPreferences
import com.example.choreboo_habittrackerfriend.data.repository.ChorebooRepository
import com.example.choreboo_habittrackerfriend.domain.model.PetType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val userPreferences: UserPreferences,
    private val chorebooRepository: ChorebooRepository,
) : ViewModel() {

    private val _selectedPetType = MutableStateFlow(PetType.FOX)
    val selectedPetType: StateFlow<PetType> = _selectedPetType.asStateFlow()

    private val _isHatching = MutableStateFlow(false)
    val isHatching: StateFlow<Boolean> = _isHatching.asStateFlow()

    fun selectPetType(petType: PetType) {
        _selectedPetType.value = petType
    }

    fun completeOnboarding(chorebooName: String) {
        viewModelScope.launch {
            _isHatching.value = true
            try {
                chorebooRepository.getOrCreateChoreboo(chorebooName, _selectedPetType.value)
                userPreferences.setOnboardingComplete(true)
            } finally {
                _isHatching.value = false
            }
        }
    }
}
