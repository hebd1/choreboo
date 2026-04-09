package com.example.choreboo_habittrackerfriend.ui.auth

import android.util.Log
import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.choreboo_habittrackerfriend.R
import com.example.choreboo_habittrackerfriend.data.datastore.UserPreferences
import com.example.choreboo_habittrackerfriend.data.repository.AuthRepository
import com.example.choreboo_habittrackerfriend.data.repository.AuthResult
import com.example.choreboo_habittrackerfriend.data.repository.ChorebooRepository
import com.example.choreboo_habittrackerfriend.data.repository.SyncManager
import com.example.choreboo_habittrackerfriend.data.repository.UserRepository
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val TAG = "AuthViewModel"

/**
 * Typed validation error for auth form fields. Each variant maps to a string resource so the
 * UI can call `stringResource(error.messageRes)` without raw strings in the ViewModel.
 */
sealed class AuthValidationError(@StringRes val messageRes: Int) {
    data object EmailRequired : AuthValidationError(R.string.auth_error_email_required)
    data object EmailInvalid : AuthValidationError(R.string.auth_error_email_invalid)
    data object PasswordRequired : AuthValidationError(R.string.auth_error_password_required)
    data object PasswordTooShort : AuthValidationError(R.string.auth_error_password_too_short)
    data object EnterEmailFirst : AuthValidationError(R.string.auth_error_enter_email_first)
}

data class AuthFormState(
    val email: String = "",
    val password: String = "",
    val isSignUp: Boolean = false,
    val isLoading: Boolean = false,
    val isSyncing: Boolean = false,
    val emailError: AuthValidationError? = null,
    val passwordError: AuthValidationError? = null,
    val showForgotPassword: Boolean = false,
)

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository,
    private val chorebooRepository: ChorebooRepository,
    private val syncManager: SyncManager,
    private val userPreferences: UserPreferences,
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

            // Prefer the GoogleSignInAccount photo URL (highest quality); fall back to the
            // FirebaseUser photo URL in case the account object doesn't carry it.
            val googleAccountPhotoUrl = account.photoUrl?.toString()
                ?: (result as? AuthResult.Success)?.user?.photoUrl?.toString()

            handleResult(result, googleAccountPhotoUrl)
        }
    }

    fun sendPasswordReset() {
        val email = _formState.value.email.trim()
        if (email.isBlank()) {
            _formState.update { it.copy(emailError = AuthValidationError.EnterEmailFirst) }
            return
        }
        viewModelScope.launch {
            _formState.update { it.copy(isLoading = true) }
            val result = authRepository.sendPasswordReset(email)
            _formState.update { it.copy(isLoading = false, showForgotPassword = false) }
            when (result) {
                is AuthResult.ResetEmailSent ->
                    _events.emit(AuthEvent.ShowMessage(R.string.auth_reset_email_sent, email))
                is AuthResult.Error ->
                    _events.emit(AuthEvent.ShowError(result.errorType.messageRes))
                is AuthResult.Success -> { /* Should not happen for password reset */ }
            }
        }
    }

    private fun validate(state: AuthFormState): Boolean {
        var valid = true
        if (state.email.isBlank()) {
            _formState.update { it.copy(emailError = AuthValidationError.EmailRequired) }
            valid = false
        } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(state.email.trim()).matches()) {
            _formState.update { it.copy(emailError = AuthValidationError.EmailInvalid) }
            valid = false
        }
        if (state.password.isBlank()) {
            _formState.update { it.copy(passwordError = AuthValidationError.PasswordRequired) }
            valid = false
        } else if (state.isSignUp && state.password.length < 6) {
            _formState.update { it.copy(passwordError = AuthValidationError.PasswordTooShort) }
            valid = false
        }
        return valid
    }

    private suspend fun handleResult(result: AuthResult, googleAccountPhotoUrl: String? = null) {
        when (result) {
            is AuthResult.Success -> {
                // Auto-save profile photo if none is stored yet.
                // Priority: Google account photo URL > FirebaseUser photo URL (covers email users
                // whose Firebase account was previously linked to Google).
                val photoToSave = googleAccountPhotoUrl ?: result.user.photoUrl?.toString()
                if (photoToSave != null && userPreferences.profilePhotoUri.first().isNullOrBlank()) {
                    userPreferences.setProfilePhotoUri(photoToSave)
                }

                // Phase 1: Pull cloud data into Room (force = true bypasses the cooldown).
                // syncAll() primes the auth token first — syncCurrentUserToCloud() comes
                // after so it can benefit from the token already being in the gRPC interceptor.
                _formState.update { it.copy(isSyncing = true) }
                val syncSucceeded = syncManager.syncAll(force = true)
                _formState.update { it.copy(isSyncing = false) }

                // Phase 2: Push local user record to cloud.
                // Done after syncAll() so the token is already cached by the gRPC interceptor.
                try {
                    userRepository.syncCurrentUserToCloud()
                } catch (_: Exception) {
                    // Non-blocking: cloud user upsert failure shouldn't prevent login
                }

                if (!syncSucceeded) {
                    _events.emit(AuthEvent.ShowMessage(R.string.auth_signed_in_sync_failed))
                }

                // Phase 3: Detect whether this user has already completed onboarding.
                // A non-null choreboo in Room (just synced from cloud) means they have.
                // Restore the local DataStore flag so cold-start and post-login both agree.
                val hasChoreboo = chorebooRepository.getChorebooSync() != null
                if (hasChoreboo) {
                    userPreferences.setOnboardingComplete(true)
                }

                _events.emit(AuthEvent.AuthSuccess(result.user.uid, onboardingComplete = hasChoreboo))
            }
            is AuthResult.Error -> _events.emit(AuthEvent.ShowError(result.errorType.messageRes))
            is AuthResult.ResetEmailSent -> { /* Handled separately in sendPasswordReset() */ }
        }
    }
}

sealed class AuthEvent {
    data class AuthSuccess(val uid: String, val onboardingComplete: Boolean) : AuthEvent()
    data class ShowError(@StringRes val messageRes: Int) : AuthEvent()
    data class ShowMessage(@StringRes val messageRes: Int, val formatArg: String? = null) : AuthEvent()
}
