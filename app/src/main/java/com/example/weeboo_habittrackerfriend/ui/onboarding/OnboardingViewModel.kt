package com.example.weeboo_habittrackerfriend.ui.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.weeboo_habittrackerfriend.data.datastore.UserPreferences
import com.example.weeboo_habittrackerfriend.data.repository.WeebooRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val userPreferences: UserPreferences,
    private val weebooRepository: WeebooRepository,
) : ViewModel() {

    fun completeOnboarding(weebooName: String) {
        viewModelScope.launch {
            weebooRepository.getOrCreateWeeboo(weebooName)
            userPreferences.setOnboardingComplete(true)
        }
    }
}

