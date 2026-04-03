package com.example.choreboo_habittrackerfriend.ui.settings
import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.choreboo_habittrackerfriend.data.datastore.UserPreferences
import com.example.choreboo_habittrackerfriend.data.repository.AuthRepository
import com.example.choreboo_habittrackerfriend.data.repository.ChorebooRepository
import com.example.choreboo_habittrackerfriend.data.repository.HabitRepository
import com.example.choreboo_habittrackerfriend.data.repository.HouseholdRepository
import com.example.choreboo_habittrackerfriend.data.repository.HouseholdResult
import com.example.choreboo_habittrackerfriend.domain.model.Household
import com.example.choreboo_habittrackerfriend.domain.model.HouseholdMember
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject
import java.io.File
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val application: Application,
    private val userPreferences: UserPreferences,
    private val authRepository: AuthRepository,
    private val householdRepository: HouseholdRepository,
    private val habitRepository: HabitRepository,
    private val chorebooRepository: ChorebooRepository,
) : ViewModel() {
    val themeMode: StateFlow<String> = userPreferences.themeMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "system")
     val soundEnabled: StateFlow<Boolean> = userPreferences.soundEnabled
         .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)
     val totalPoints: StateFlow<Int> = userPreferences.totalPoints
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)
    val profilePhotoUri: StateFlow<String?> = userPreferences.profilePhotoUri
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val currentUserEmail: String?
        get() = authRepository.currentFirebaseUser?.email

    val googlePhotoUrl: String?
        get() = authRepository.currentFirebaseUser?.photoUrl?.toString()

    // Household state
    val currentHousehold: StateFlow<Household?> = householdRepository.currentHousehold
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val householdMembers: StateFlow<List<HouseholdMember>> = householdRepository.householdMembers
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _householdNotificationsEnabled = userPreferences.householdNotificationsEnabled
    val householdNotificationsEnabled: StateFlow<Boolean> = _householdNotificationsEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    private val _events = MutableSharedFlow<SettingsEvent>()
    val events: SharedFlow<SettingsEvent> = _events.asSharedFlow()

    fun setThemeMode(mode: String) {
        viewModelScope.launch { userPreferences.setThemeMode(mode) }
    }
     fun setSoundEnabled(enabled: Boolean) {
         viewModelScope.launch { userPreferences.setSoundEnabled(enabled) }
     }

    fun setHouseholdNotificationsEnabled(enabled: Boolean) {
        viewModelScope.launch { userPreferences.setHouseholdNotificationsEnabled(enabled) }
    }

    fun createHousehold(name: String) {
        viewModelScope.launch {
            when (val result = householdRepository.createHousehold(name)) {
                is HouseholdResult.Success -> {
                    _events.emit(SettingsEvent.ShowSuccess("Household \"${result.household.name}\" created!"))
                }
                is HouseholdResult.Error -> {
                    _events.emit(SettingsEvent.ShowError(result.message))
                }
            }
        }
    }

    fun joinHousehold(inviteCode: String) {
        viewModelScope.launch {
            when (val result = householdRepository.joinHousehold(inviteCode)) {
                is HouseholdResult.Success -> {
                    _events.emit(SettingsEvent.ShowSuccess("Joined \"${result.household.name}\"!"))
                }
                is HouseholdResult.Error -> {
                    _events.emit(SettingsEvent.ShowError(result.message))
                }
            }
        }
    }

    fun leaveHousehold() {
        viewModelScope.launch {
            when (val result = householdRepository.leaveHousehold()) {
                is HouseholdResult.Success -> {
                    _events.emit(SettingsEvent.ShowSuccess("Left household."))
                }
                is HouseholdResult.Error -> {
                    _events.emit(SettingsEvent.ShowError(result.message))
                }
            }
        }
    }

     fun signOut() {
        viewModelScope.launch {
            // Clear all local data before signing out
            try {
                habitRepository.clearLocalData()
                chorebooRepository.clearLocalData()
                householdRepository.clearState()
                userPreferences.clearAllData()
            } catch (e: Exception) {
                // Best-effort cleanup — proceed with sign-out even if cleanup fails
                android.util.Log.e("SettingsViewModel", "Error during sign-out cleanup", e)
            }
            authRepository.signOut()
            _events.emit(SettingsEvent.SignedOut)
        }
    }

    fun onProfilePhotoPicked(contentUri: Uri) {
        viewModelScope.launch {
            try {
                val internalPhotoFile = File(application.filesDir, "profile_photo.jpg")
                application.contentResolver.openInputStream(contentUri)?.use { input ->
                    internalPhotoFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
                userPreferences.setProfilePhotoUri(internalPhotoFile.absolutePath)
            } catch (e: Exception) {
                _events.emit(SettingsEvent.ShowError("Failed to save profile photo"))
            }
        }
    }

    fun clearProfilePhoto() {
        viewModelScope.launch {
            try {
                val internalPhotoFile = File(application.filesDir, "profile_photo.jpg")
                if (internalPhotoFile.exists()) {
                    internalPhotoFile.delete()
                }
                userPreferences.setProfilePhotoUri(null)
            } catch (e: Exception) {
                _events.emit(SettingsEvent.ShowError("Failed to clear profile photo"))
            }
        }
    }
}

sealed class SettingsEvent {
    data object SignedOut : SettingsEvent()
    data class ShowError(val message: String) : SettingsEvent()
    data class ShowSuccess(val message: String) : SettingsEvent()
}
