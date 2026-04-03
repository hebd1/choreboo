package com.example.choreboo_habittrackerfriend.ui.auth

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.choreboo_habittrackerfriend.data.repository.AuthRepository
import com.example.choreboo_habittrackerfriend.data.repository.AuthResult
import com.example.choreboo_habittrackerfriend.data.repository.ChorebooRepository
import com.example.choreboo_habittrackerfriend.data.repository.HabitRepository
import com.example.choreboo_habittrackerfriend.data.repository.UserRepository
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val TAG = "AuthViewModel"

data class AuthFormState(
    val email: String = "",
    val password: String = "",
    val isSignUp: Boolean = false,
    val isLoading: Boolean = false,
    val isSyncing: Boolean = false,
    val emailError: String? = null,
    val passwordError: String? = null,
    val showForgotPassword: Boolean = false,
)

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository,
    private val habitRepository: HabitRepository,
    private val chorebooRepository: ChorebooRepository,
) : ViewModel() {

    private val _formState = MutableStateFlow(AuthFormState())
    val formState: StateFlow<AuthFormState> = _formState.asStateFlow()

    private val _events = MutableSharedFlow<AuthEvent>()
    val events: SharedFlow<AuthEvent> = _events.asSharedFlow()

    fun onEmailChange(value: String) {
        _formState.update { it.copy(email = value, emailError = null) }
    }

    fun onPasswordChange(value: String) {
        _formState.update { it.copy(password = value, passwordError = null) }
    }

    fun toggleMode() {
        _formState.update { it.copy(isSignUp = !it.isSignUp, emailError = null, passwordError = null) }
    }

    fun toggleForgotPassword(show: Boolean) {
        _formState.update { it.copy(showForgotPassword = show) }
    }

    fun submit() {
        val state = _formState.value
        if (!validate(state)) return

        viewModelScope.launch {
            _formState.update { it.copy(isLoading = true) }
            val result = if (state.isSignUp) {
                authRepository.signUpWithEmail(state.email.trim(), state.password)
            } else {
                authRepository.signInWithEmail(state.email.trim(), state.password)
            }
            _formState.update { it.copy(isLoading = false) }
            handleResult(result)
        }
    }

    fun signInWithGoogle(account: GoogleSignInAccount) {
        viewModelScope.launch {
            _formState.update { it.copy(isLoading = true) }
            val result = authRepository.signInWithGoogle(account)
            _formState.update { it.copy(isLoading = false) }
            handleResult(result)
        }
    }

    fun sendPasswordReset() {
        val email = _formState.value.email.trim()
        if (email.isBlank()) {
            _formState.update { it.copy(emailError = "Enter your email address first.") }
            return
        }
        viewModelScope.launch {
            _formState.update { it.copy(isLoading = true) }
            authRepository.sendPasswordReset(email)
            _formState.update { it.copy(isLoading = false, showForgotPassword = false) }
            _events.emit(AuthEvent.ShowMessage("Password reset email sent to $email"))
        }
    }

    private fun validate(state: AuthFormState): Boolean {
        var valid = true
        if (state.email.isBlank()) {
            _formState.update { it.copy(emailError = "Email is required.") }
            valid = false
        } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(state.email.trim()).matches()) {
            _formState.update { it.copy(emailError = "Enter a valid email address.") }
            valid = false
        }
        if (state.password.isBlank()) {
            _formState.update { it.copy(passwordError = "Password is required.") }
            valid = false
        } else if (state.isSignUp && state.password.length < 6) {
            _formState.update { it.copy(passwordError = "Password must be at least 6 characters.") }
            valid = false
        }
        return valid
    }

    private suspend fun handleResult(result: AuthResult) {
        when (result) {
            is AuthResult.Success -> {
                // Phase 1: Push local user to cloud
                try {
                    userRepository.syncCurrentUserToCloud()
                } catch (_: Exception) {
                    // Non-blocking: cloud user upsert failure shouldn't prevent login
                }

                // Phase 2: Pull cloud data into Room
                _formState.update { it.copy(isSyncing = true) }
                val syncFailed = !syncCloudDataToLocal()
                _formState.update { it.copy(isSyncing = false) }

                if (syncFailed) {
                    _events.emit(AuthEvent.ShowMessage("Signed in — couldn't sync cloud data"))
                }

                _events.emit(AuthEvent.AuthSuccess(result.user.uid))
            }
            is AuthResult.Error -> _events.emit(AuthEvent.ShowError(result.message))
        }
    }

    /**
     * Pull all cloud data into Room after auth.
     * Returns true if sync succeeded (fully or partially), false if all failed.
     */
    private suspend fun syncCloudDataToLocal(): Boolean {
        var anySuccess = false

        // Sync habits first (logs depend on habit remote IDs being in Room)
        try {
            habitRepository.syncHabitsFromCloud()
            anySuccess = true
        } catch (e: Exception) {
            Log.e(TAG, "Habit sync from cloud failed", e)
        }

        // Sync choreboo
        try {
            chorebooRepository.syncFromCloud()
            anySuccess = true
        } catch (e: Exception) {
            Log.e(TAG, "Choreboo sync from cloud failed", e)
        }

        // Sync habit logs (depends on habits being synced first for ID mapping)
        try {
            habitRepository.syncHabitLogsFromCloud()
            anySuccess = true
        } catch (e: Exception) {
            Log.e(TAG, "Habit log sync from cloud failed", e)
        }

        return anySuccess
    }
}

sealed class AuthEvent {
    data class AuthSuccess(val uid: String) : AuthEvent()
    data class ShowError(val message: String) : AuthEvent()
    data class ShowMessage(val message: String) : AuthEvent()
}
