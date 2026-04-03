package com.example.choreboo_habittrackerfriend.data.repository

import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

sealed class AuthResult {
    data class Success(val user: FirebaseUser) : AuthResult()
    data class Error(val message: String) : AuthResult()
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
        return try {
            val result = firebaseAuth.signInWithEmailAndPassword(email, password).await()
            val user = result.user ?: return AuthResult.Error("Sign-in failed: no user returned.")
            AuthResult.Success(user)
        } catch (e: Exception) {
            AuthResult.Error(e.toFriendlyMessage())
        }
    }

    suspend fun signUpWithEmail(email: String, password: String): AuthResult {
        return try {
            val result = firebaseAuth.createUserWithEmailAndPassword(email, password).await()
            val user = result.user ?: return AuthResult.Error("Sign-up failed: no user returned.")
            AuthResult.Success(user)
        } catch (e: Exception) {
            AuthResult.Error(e.toFriendlyMessage())
        }
    }

    suspend fun signInWithGoogle(account: GoogleSignInAccount): AuthResult {
        return try {
            val credential = GoogleAuthProvider.getCredential(account.idToken, null)
            val result = firebaseAuth.signInWithCredential(credential).await()
            val user = result.user ?: return AuthResult.Error("Google sign-in failed: no user returned.")
            AuthResult.Success(user)
        } catch (e: Exception) {
            AuthResult.Error(e.toFriendlyMessage())
        }
    }

    suspend fun sendPasswordReset(email: String): AuthResult {
        return try {
            firebaseAuth.sendPasswordResetEmail(email).await()
            AuthResult.ResetEmailSent
        } catch (e: Exception) {
            AuthResult.Error(e.toFriendlyMessage())
        }
    }

    fun signOut() {
        firebaseAuth.signOut()
    }
}

private fun Exception.toFriendlyMessage(): String {
    val raw = message ?: return "An unknown error occurred."
    return when {
        "INVALID_LOGIN_CREDENTIALS" in raw || "invalid-credential" in raw ->
            "Incorrect email or password."
        "EMAIL_EXISTS" in raw || "email-already-in-use" in raw ->
            "An account with this email already exists."
        "WEAK_PASSWORD" in raw || "weak-password" in raw ->
            "Password must be at least 6 characters."
        "INVALID_EMAIL" in raw || "invalid-email" in raw ->
            "Please enter a valid email address."
        "USER_NOT_FOUND" in raw || "user-not-found" in raw ->
            "No account found with this email."
        "TOO_MANY_REQUESTS" in raw || "too-many-requests" in raw ->
            "Too many attempts. Please try again later."
        "NETWORK_ERROR" in raw || "network-request-failed" in raw ->
            "Network error. Check your connection and try again."
        else -> raw.substringAfterLast("] ").ifBlank { raw }
    }
}
