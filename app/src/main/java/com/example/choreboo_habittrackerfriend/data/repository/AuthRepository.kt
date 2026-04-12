package com.example.choreboo_habittrackerfriend.data.repository

import androidx.annotation.StringRes
import com.example.choreboo_habittrackerfriend.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Typed auth error — each variant maps 1:1 to a string resource so the UI can call
 * `stringResource(error.messageRes)` without raw strings in the ViewModel.
 */
sealed class AuthErrorType(@StringRes val messageRes: Int) {
    data object Unknown : AuthErrorType(R.string.auth_error_unknown)
    data object InvalidCredentials : AuthErrorType(R.string.auth_error_invalid_credentials)
    data object EmailAlreadyExists : AuthErrorType(R.string.auth_error_email_already_exists)
    data object WeakPassword : AuthErrorType(R.string.auth_error_weak_password)
    data object InvalidEmail : AuthErrorType(R.string.auth_error_invalid_email)
    data object UserNotFound : AuthErrorType(R.string.auth_error_user_not_found)
    data object TooManyRequests : AuthErrorType(R.string.auth_error_too_many_requests)
    data object NetworkError : AuthErrorType(R.string.auth_error_network)
    data object SignInNoUser : AuthErrorType(R.string.auth_error_sign_in_no_user)
    data object SignUpNoUser : AuthErrorType(R.string.auth_error_sign_up_no_user)
    data object GoogleNoUser : AuthErrorType(R.string.auth_error_google_no_user)
}

sealed class AuthResult {
    data class Success(val user: FirebaseUser) : AuthResult()
    data class Error(val errorType: AuthErrorType) : AuthResult()
    data object ResetEmailSent : AuthResult()
}

@Singleton
class AuthRepository @Inject constructor(
    private val firebaseAuth: FirebaseAuth,
) {

    val currentUser: Flow<FirebaseUser?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            trySend(auth.currentUser)
        }
        firebaseAuth.addAuthStateListener(listener)
        awaitClose { firebaseAuth.removeAuthStateListener(listener) }
    }

    val isAuthenticated: Boolean
        get() = firebaseAuth.currentUser != null

    val currentFirebaseUser: FirebaseUser?
        get() = firebaseAuth.currentUser

    suspend fun signInWithEmail(email: String, password: String): AuthResult {
        require(email.isNotBlank()) { "Email must not be blank" }
        require(password.isNotBlank()) { "Password must not be blank" }

        return try {
            val result = firebaseAuth.signInWithEmailAndPassword(email, password).await()
            val user = result.user ?: return AuthResult.Error(AuthErrorType.SignInNoUser)
            AuthResult.Success(user)
        } catch (e: Exception) {
            AuthResult.Error(e.toAuthErrorType())
        }
    }

    suspend fun signUpWithEmail(email: String, password: String): AuthResult {
        require(email.isNotBlank()) { "Email must not be blank" }
        require(password.isNotBlank()) { "Password must not be blank" }

        return try {
            val result = firebaseAuth.createUserWithEmailAndPassword(email, password).await()
            val user = result.user ?: return AuthResult.Error(AuthErrorType.SignUpNoUser)
            AuthResult.Success(user)
        } catch (e: Exception) {
            AuthResult.Error(e.toAuthErrorType())
        }
    }

    suspend fun signInWithGoogle(idToken: String): AuthResult {
        return try {
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            val result = firebaseAuth.signInWithCredential(credential).await()
            val user = result.user ?: return AuthResult.Error(AuthErrorType.GoogleNoUser)
            AuthResult.Success(user)
        } catch (e: Exception) {
            AuthResult.Error(e.toAuthErrorType())
        }
    }

    suspend fun sendPasswordReset(email: String): AuthResult {
        require(email.isNotBlank()) { "Email must not be blank" }

        return try {
            firebaseAuth.sendPasswordResetEmail(email).await()
            AuthResult.ResetEmailSent
        } catch (e: Exception) {
            AuthResult.Error(e.toAuthErrorType())
        }
    }

    fun signOut() {
        firebaseAuth.signOut()
    }
}

private fun Exception.toAuthErrorType(): AuthErrorType {
    val raw = message ?: return AuthErrorType.Unknown
    return when {
        "INVALID_LOGIN_CREDENTIALS" in raw || "invalid-credential" in raw ->
            AuthErrorType.InvalidCredentials
        "EMAIL_EXISTS" in raw || "email-already-in-use" in raw ->
            AuthErrorType.EmailAlreadyExists
        "WEAK_PASSWORD" in raw || "weak-password" in raw ->
            AuthErrorType.WeakPassword
        "INVALID_EMAIL" in raw || "invalid-email" in raw ->
            AuthErrorType.InvalidEmail
        "USER_NOT_FOUND" in raw || "user-not-found" in raw ->
            AuthErrorType.UserNotFound
        "TOO_MANY_REQUESTS" in raw || "too-many-requests" in raw ->
            AuthErrorType.TooManyRequests
        "NETWORK_ERROR" in raw || "network-request-failed" in raw ->
            AuthErrorType.NetworkError
        else -> AuthErrorType.Unknown
    }
}
