package com.example.choreboo_habittrackerfriend.ui.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.choreboo_habittrackerfriend.data.datastore.UserPreferences
import com.example.choreboo_habittrackerfriend.data.repository.ChorebooRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val userPreferences: UserPreferences,
    private val chorebooRepository: ChorebooRepository,
) : ViewModel() {

    fun completeOnboarding(chorebooName: String) {
        viewModelScope.launch {
            chorebooRepository.getOrCreateChoreboo(chorebooName)
            userPreferences.setOnboardingComplete(true)
        }
    }
}

