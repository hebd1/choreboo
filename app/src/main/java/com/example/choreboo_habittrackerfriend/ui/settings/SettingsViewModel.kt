package com.example.choreboo_habittrackerfriend.ui.settings
import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.choreboo_habittrackerfriend.data.datastore.UserPreferences
import com.example.choreboo_habittrackerfriend.data.repository.AuthRepository
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

    private val _events = MutableSharedFlow<SettingsEvent>()
    val events: SharedFlow<SettingsEvent> = _events.asSharedFlow()

    fun setThemeMode(mode: String) {
        viewModelScope.launch { userPreferences.setThemeMode(mode) }
    }
     fun setSoundEnabled(enabled: Boolean) {
         viewModelScope.launch { userPreferences.setSoundEnabled(enabled) }
     }

     fun signOut() {
        authRepository.signOut()
        viewModelScope.launch { _events.emit(SettingsEvent.SignedOut) }
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
}
