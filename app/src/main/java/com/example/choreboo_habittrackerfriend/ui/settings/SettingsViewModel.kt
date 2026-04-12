package com.example.choreboo_habittrackerfriend.ui.settings
import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.choreboo_habittrackerfriend.R
import com.example.choreboo_habittrackerfriend.data.datastore.UserPreferences
import com.example.choreboo_habittrackerfriend.data.repository.AuthRepository
import com.example.choreboo_habittrackerfriend.data.repository.BillingRepository
import com.example.choreboo_habittrackerfriend.data.repository.ChorebooRepository
import com.example.choreboo_habittrackerfriend.data.repository.HabitRepository
import com.example.choreboo_habittrackerfriend.data.repository.HouseholdRepository
import com.example.choreboo_habittrackerfriend.data.repository.HouseholdResult
import com.example.choreboo_habittrackerfriend.data.repository.ResetRepository
import com.example.choreboo_habittrackerfriend.data.repository.ResetResult
import com.example.choreboo_habittrackerfriend.data.repository.UserRepository
import com.example.choreboo_habittrackerfriend.domain.model.Household
import com.example.choreboo_habittrackerfriend.domain.model.HouseholdMember
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
import timber.log.Timber
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
    private val billingRepository: BillingRepository,
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

    private val _isUploadingPhoto = MutableStateFlow(false)
    val isUploadingPhoto: StateFlow<Boolean> = _isUploadingPhoto.asStateFlow()

    private val _isCreatingHousehold = MutableStateFlow(false)
    val isCreatingHousehold: StateFlow<Boolean> = _isCreatingHousehold.asStateFlow()

    private val _isJoiningHousehold = MutableStateFlow(false)
    val isJoiningHousehold: StateFlow<Boolean> = _isJoiningHousehold.asStateFlow()

    private val _isLeavingHousehold = MutableStateFlow(false)
    val isLeavingHousehold: StateFlow<Boolean> = _isLeavingHousehold.asStateFlow()

    // Premium / subscription state
    val isPremium: StateFlow<Boolean> = billingRepository.isPremium
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    private val _isRestoringPurchases = MutableStateFlow(false)
    val isRestoringPurchases: StateFlow<Boolean> = _isRestoringPurchases.asStateFlow()

    fun restorePurchases() {
        viewModelScope.launch {
            _isRestoringPurchases.value = true
            try {
                billingRepository.restorePurchases()
                val active = billingRepository.isPremium.value
                _events.emit(
                    if (active) SettingsEvent.ShowSuccess(R.string.settings_msg_premium_restored)
                    else SettingsEvent.ShowSuccess(R.string.settings_msg_no_subscription)
                )
            } catch (e: Exception) {
                _events.emit(SettingsEvent.ShowError(R.string.settings_msg_restore_failed))
            } finally {
                _isRestoringPurchases.value = false
            }
        }
    }

    fun launchPremiumPurchase(activity: android.app.Activity) {
        val launched = billingRepository.launchPurchaseFlow(activity)
        if (!launched) {
            viewModelScope.launch {
                _events.emit(SettingsEvent.ShowError(R.string.settings_msg_purchase_error))
            }
        }
    }

    fun setThemeMode(mode: String) {
        viewModelScope.launch { userPreferences.setThemeMode(mode) }
    }
     fun setSoundEnabled(enabled: Boolean) {
         viewModelScope.launch { userPreferences.setSoundEnabled(enabled) }
     }

    fun createHousehold(name: String) {
        viewModelScope.launch {
            _isCreatingHousehold.value = true
            try {
                when (val result = householdRepository.createHousehold(name)) {
                    is HouseholdResult.Success -> {
                        _events.emit(SettingsEvent.ShowSuccess(R.string.settings_msg_household_created, result.household.name))
                    }
                    is HouseholdResult.Error -> {
                        _events.emit(SettingsEvent.ShowRawError(result.message))
                    }
                }
            } finally {
                _isCreatingHousehold.value = false
            }
        }
    }

    fun joinHousehold(inviteCode: String) {
        viewModelScope.launch {
            _isJoiningHousehold.value = true
            try {
                when (val result = householdRepository.joinHousehold(inviteCode)) {
                    is HouseholdResult.Success -> {
                        _events.emit(SettingsEvent.ShowSuccess(R.string.settings_msg_household_joined, result.household.name))
                    }
                    is HouseholdResult.Error -> {
                        _events.emit(SettingsEvent.ShowRawError(result.message))
                    }
                }
            } finally {
                _isJoiningHousehold.value = false
            }
        }
    }

    fun leaveHousehold() {
        viewModelScope.launch {
            _isLeavingHousehold.value = true
            try {
                when (val result = householdRepository.leaveHousehold()) {
                    is HouseholdResult.Success -> {
                        _events.emit(SettingsEvent.ShowSuccess(R.string.settings_msg_household_left))
                    }
                    is HouseholdResult.Error -> {
                        _events.emit(SettingsEvent.ShowRawError(result.message))
                    }
                }
            } finally {
                _isLeavingHousehold.value = false
            }
        }
    }

     fun signOut() {
        viewModelScope.launch {
            // Cancel all pending reminder alarms before clearing local data
            try {
                habitRepository.cancelAllReminders()
            } catch (e: Exception) {
                Timber.w(e, "Error cancelling reminders on sign-out")
            }

            // Cancel in-flight write-through coroutines to prevent stale cloud writes after sign-out
            habitRepository.cancelPendingWrites()
            chorebooRepository.cancelPendingWrites()

            // Clear all local data before signing out
            try {
                habitRepository.clearLocalData()
                chorebooRepository.clearLocalData()
                householdRepository.clearState()
                userPreferences.clearAllData()
            } catch (e: Exception) {
                // Best-effort cleanup — proceed with sign-out even if cleanup fails
                Timber.e(e, "Error during sign-out cleanup")
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
     * either [password] (email/password accounts) or [googleIdToken] (Google accounts) so
     * this method can build the correct [com.google.firebase.auth.AuthCredential] and
     * re-authenticate before deleting.
     *
     * Intended for development/testing only.
     */
    fun resetAccount(password: String? = null, googleIdToken: String? = null) {
        viewModelScope.launch {
            val currentUser = authRepository.currentFirebaseUser
            if (currentUser == null) {
                _events.emit(SettingsEvent.ShowError(R.string.settings_msg_not_authenticated))
                return@launch
            }

            val credential = when {
                googleIdToken != null ->
                    GoogleAuthProvider.getCredential(googleIdToken, null)
                password != null -> {
                    val email = currentUser.email
                    if (email == null) {
                        _events.emit(SettingsEvent.ShowError(R.string.settings_msg_no_account_email))
                        return@launch
                    }
                    EmailAuthProvider.getCredential(email, password)
                }
                else -> {
                    _events.emit(SettingsEvent.ShowError(R.string.settings_msg_provide_password))
                    return@launch
                }
            }

            _isResetting.value = true
            try {
                when (val result = resetRepository.resetAll(credential)) {
                    is ResetResult.Success -> _events.emit(SettingsEvent.AccountReset)
                    is ResetResult.Error -> _events.emit(SettingsEvent.ShowRawError(result.message))
                }
            } finally {
                _isResetting.value = false
            }
        }
    }

    fun updateDisplayName(name: String) {
        viewModelScope.launch {
            _isUpdatingName.value = true
            try {
                userRepository.updateDisplayName(name)
                _currentDisplayName.value = name.trim()
                _events.emit(SettingsEvent.ShowSuccess(R.string.settings_msg_username_updated))
            } catch (e: Exception) {
                val messageRes = when {
                    e is IllegalArgumentException -> R.string.settings_msg_invalid_username
                    else -> R.string.settings_msg_username_update_failed
                }
                _events.emit(SettingsEvent.ShowError(messageRes))
            } finally {
                _isUpdatingName.value = false
            }
        }
    }

    fun onProfilePhotoPicked(contentUri: Uri) {
        viewModelScope.launch {
            try {
                val internalPhotoFile = File(application.filesDir, "profile_photo.jpg")

                // 1. Save locally for instant display
                application.contentResolver.openInputStream(contentUri)?.use { input ->
                    internalPhotoFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
                // Set local path immediately so UI shows the photo right away
                userPreferences.setProfilePhotoUri(internalPhotoFile.absolutePath)
                _events.emit(SettingsEvent.ShowSuccess(R.string.settings_msg_photo_saved))

                // 2. Upload to Firebase Storage in background
                _isUploadingPhoto.value = true
                try {
                    userRepository.uploadProfilePhoto(internalPhotoFile)
                    _events.emit(SettingsEvent.ShowSuccess(R.string.settings_msg_photo_synced))
                } catch (e: Exception) {
                    _events.emit(SettingsEvent.ShowError(R.string.settings_msg_photo_sync_failed))
                } finally {
                    _isUploadingPhoto.value = false
                }
            } catch (e: Exception) {
                _events.emit(SettingsEvent.ShowError(R.string.settings_msg_photo_save_failed))
            }
        }
    }

    fun clearProfilePhoto() {
        viewModelScope.launch {
            try {
                val internalPhotoFile = File(application.filesDir, "profile_photo.jpg")

                // Clear locally first for instant UI update
                userPreferences.setProfilePhotoUri(null)

                // Delete from cloud in background
                _isUploadingPhoto.value = true
                try {
                    userRepository.deleteProfilePhoto(internalPhotoFile)
                    _events.emit(SettingsEvent.ShowSuccess(R.string.settings_msg_photo_removed))
                } catch (e: Exception) {
                    _events.emit(SettingsEvent.ShowError(R.string.settings_msg_photo_remove_sync_failed))
                } finally {
                    _isUploadingPhoto.value = false
                }
            } catch (e: Exception) {
                _events.emit(SettingsEvent.ShowError(R.string.settings_msg_photo_clear_failed))
            }
        }
    }
}

sealed class SettingsEvent {
    data object SignedOut : SettingsEvent()
    data object AccountReset : SettingsEvent()
    /** Error with a string-resource message (optionally parameterized). */
    data class ShowError(@StringRes val messageRes: Int, val formatArg: String? = null) : SettingsEvent()
    /** Success with a string-resource message (optionally parameterized). */
    data class ShowSuccess(@StringRes val messageRes: Int, val formatArg: String? = null) : SettingsEvent()
    /** Fallback for errors from external APIs (HouseholdResult, ResetResult) that still carry raw strings. */
    data class ShowRawError(val message: String) : SettingsEvent()
}
