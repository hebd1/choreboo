package com.example.choreboo_habittrackerfriend

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.choreboo_habittrackerfriend.data.datastore.UserPreferences
import com.example.choreboo_habittrackerfriend.data.repository.AuthRepository
import com.example.choreboo_habittrackerfriend.data.repository.ChorebooRepository
import com.example.choreboo_habittrackerfriend.domain.model.ChorebooMood
import com.example.choreboo_habittrackerfriend.domain.model.ChorebooStats
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    val userPreferences: UserPreferences,
    chorebooRepository: ChorebooRepository,
    val authRepository: AuthRepository,
) : ViewModel() {

    val themeMode: StateFlow<String> = userPreferences.themeMode
        .stateIn(viewModelScope, SharingStarted.Eagerly, "system")

    val onboardingComplete: StateFlow<Boolean?> = userPreferences.onboardingComplete
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val petMood: StateFlow<ChorebooMood> = chorebooRepository.getChoreboo()
        .map { it?.mood ?: ChorebooMood.IDLE }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ChorebooMood.IDLE)
}
