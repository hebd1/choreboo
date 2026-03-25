package com.example.weeboo_habittrackerfriend.ui.settings
import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.weeboo_habittrackerfriend.data.datastore.UserPreferences
import com.example.weeboo_habittrackerfriend.worker.ReminderWorker
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val application: Application,
    private val userPreferences: UserPreferences,
) : ViewModel() {
    val themeMode: StateFlow<String> = userPreferences.themeMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "system")
    val soundEnabled: StateFlow<Boolean> = userPreferences.soundEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)
    val reminderEnabled: StateFlow<Boolean> = userPreferences.reminderEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)
    fun setThemeMode(mode: String) {
        viewModelScope.launch { userPreferences.setThemeMode(mode) }
    }
    fun setSoundEnabled(enabled: Boolean) {
        viewModelScope.launch { userPreferences.setSoundEnabled(enabled) }
    }
    fun setReminderEnabled(enabled: Boolean) {
        viewModelScope.launch {
            userPreferences.setReminderEnabled(enabled)
            if (enabled) {
                ReminderWorker.schedule(application)
            } else {
                ReminderWorker.cancel(application)
            }
        }
    }
}
