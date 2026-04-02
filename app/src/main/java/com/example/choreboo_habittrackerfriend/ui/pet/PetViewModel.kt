package com.example.choreboo_habittrackerfriend.ui.pet

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.choreboo_habittrackerfriend.data.datastore.UserPreferences
import com.example.choreboo_habittrackerfriend.data.repository.ChorebooRepository
import com.example.choreboo_habittrackerfriend.domain.model.ChorebooMood
import com.example.choreboo_habittrackerfriend.domain.model.ChorebooStats
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
import javax.inject.Inject

@HiltViewModel
class PetViewModel @Inject constructor(
    private val chorebooRepository: ChorebooRepository,
    private val userPreferences: UserPreferences,
) : ViewModel() {

    val chorebooState: StateFlow<ChorebooStats?> = chorebooRepository.getChoreboo()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val currentMood: StateFlow<ChorebooMood> = chorebooRepository.getChoreboo()
        .map { it?.mood ?: ChorebooMood.IDLE }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ChorebooMood.IDLE)

    val totalPoints: StateFlow<Int> = userPreferences.totalPoints
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val isSleeping: StateFlow<Boolean> = chorebooRepository.getChoreboo()
        .map { it?.isSleeping ?: false }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    /** True while the eating Lottie animation should be playing */
    private val _isEating = MutableStateFlow(false)
    val isEating: StateFlow<Boolean> = _isEating.asStateFlow()

    private val _events = MutableSharedFlow<PetEvent>()
    val events = _events.asSharedFlow()

    init {
        viewModelScope.launch {
            // Ensure a choreboo exists (handles empty DB after destructive migration)
            chorebooRepository.getOrCreateChoreboo()
            chorebooRepository.applyStatDecay()
        }
    }

    /** Manual feed: costs 10 points, adds +20 hunger, triggers eating animation. */
    fun feedChoreboo() {
        viewModelScope.launch {
            val deducted = userPreferences.deductPoints(10)
            if (deducted) {
                chorebooRepository.feedChoreboo()
                _isEating.value = true
                _events.emit(PetEvent.Fed)
            } else {
                _events.emit(PetEvent.InsufficientPoints)
            }
        }
    }

    /** Put pet to sleep for 24 hours — freezes stat decay. */
    fun sleepChoreboo() {
        viewModelScope.launch {
            val choreboo = chorebooRepository.getChorebooSync()
            if (choreboo?.isSleeping == true) {
                _events.emit(PetEvent.AlreadySleeping)
            } else {
                chorebooRepository.putToSleep()
                _events.emit(PetEvent.Sleeping)
            }
        }
    }

    /** Called by UI when the eating animation finishes. */
    fun onEatingAnimationComplete() {
        _isEating.value = false
    }
}

sealed class PetEvent {
    data object Fed : PetEvent()
    data object InsufficientPoints : PetEvent()
    data object Sleeping : PetEvent()
    data object AlreadySleeping : PetEvent()
}
