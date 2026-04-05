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
import com.example.choreboo_habittrackerfriend.data.repository.ResetRepository
import com.example.choreboo_habittrackerfriend.data.repository.ResetResult
import com.example.choreboo_habittrackerfriend.data.repository.UserRepository
import com.example.choreboo_habittrackerfriend.domain.model.Household
import com.example.choreboo_habittrackerfriend.domain.model.HouseholdMember
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.GoogleAuthProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
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
    private val resetRepository: ResetRepository,
    private val userRepository: UserRepository,
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

    /**
     * True if the current user signed in via Google (provider id = "google.com").
     * Used by the UI to decide whether to show a password field or trigger the Google
     * sign-in flow for re-authentication before account deletion.
     */
    val isGoogleUser: Boolean
        get() = authRepository.currentFirebaseUser?.providerData
            ?.any { it.providerId == "google.com" } == true

    val googlePhotoUrl: StateFlow<String?> = authRepository.currentUser
        .map { it?.photoUrl?.toString() }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            authRepository.currentFirebaseUser?.photoUrl?.toString(),
        )

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

    private val _isResetting = MutableStateFlow(false)
    val isResetting: StateFlow<Boolean> = _isResetting.asStateFlow()

    private val _currentDisplayName = MutableStateFlow(
        authRepository.currentFirebaseUser?.displayName ?: "",
    )
    val currentDisplayName: StateFlow<String> = _currentDisplayName.asStateFlow()

    private val _isUpdatingName = MutableStateFlow(false)
    val isUpdatingName: StateFlow<Boolean> = _isUpdatingName.asStateFlow()

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
            // Cancel all pending reminder alarms before clearing local data
            try {
                habitRepository.cancelAllReminders()
            } catch (e: Exception) {
                android.util.Log.w("SettingsViewModel", "Error cancelling reminders on sign-out", e)
            }

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

    /**
     * Wipes all cloud data for the current user, deletes their Firebase Auth account,
     * and clears all local Room/DataStore state. Navigates to the Auth screen on success
     * so the user can re-register from a blank slate.
     *
     * Firebase requires recent authentication for account deletion. The caller must supply
     * either [password] (email/password accounts) or [googleAccount] (Google accounts) so
     * this method can build the correct [com.google.firebase.auth.AuthCredential] and
     * re-authenticate before deleting.
     *
     * Intended for development/testing only.
     */
    fun resetAccount(password: String? = null, googleAccount: GoogleSignInAccount? = null) {
        viewModelScope.launch {
            val currentUser = authRepository.currentFirebaseUser
            if (currentUser == null) {
                _events.emit(SettingsEvent.ShowError("Not authenticated"))
                return@launch
            }

            val credential = when {
                googleAccount != null ->
                    GoogleAuthProvider.getCredential(googleAccount.idToken, null)
                password != null -> {
                    val email = currentUser.email
                    if (email == null) {
                        _events.emit(SettingsEvent.ShowError("Could not determine account email"))
                        return@launch
                    }
                    EmailAuthProvider.getCredential(email, password)
                }
                else -> {
                    _events.emit(SettingsEvent.ShowError("Please provide your password to confirm account deletion"))
                    return@launch
                }
            }

            _isResetting.value = true
            when (val result = resetRepository.resetAll(credential)) {
                is ResetResult.Success -> {
                    _isResetting.value = false
                    _events.emit(SettingsEvent.AccountReset)
                }
                is ResetResult.Error -> {
                    _isResetting.value = false
                    _events.emit(SettingsEvent.ShowError(result.message))
                }
            }
        }
    }

    fun updateDisplayName(name: String) {
        viewModelScope.launch {
            _isUpdatingName.value = true
            try {
                userRepository.updateDisplayName(name)
                _currentDisplayName.value = name.trim()
                _events.emit(SettingsEvent.ShowSuccess("Username updated!"))
            } catch (e: Exception) {
                val message = when {
                    e is IllegalArgumentException -> e.message ?: "Invalid username"
                    else -> "Failed to update username. Please try again."
                }
                _events.emit(SettingsEvent.ShowError(message))
            } finally {
                _isUpdatingName.value = false
            }
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
    data object AccountReset : SettingsEvent()
    data class ShowError(val message: String) : SettingsEvent()
    data class ShowSuccess(val message: String) : SettingsEvent()
}
