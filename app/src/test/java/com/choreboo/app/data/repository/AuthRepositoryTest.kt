package com.choreboo.app.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.FirebaseUser
import com.google.android.gms.tasks.Tasks
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for [AuthRepository].
 *
 * Covers: validation guards, sign-in/sign-up success paths, error mapping, and sign-out.
 * Firebase Tasks are mocked — no real network calls are made.
 */
class AuthRepositoryTest {

    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var repo: AuthRepository

    @Before
    fun setUp() {
        firebaseAuth = mockk(relaxed = true)
        repo = AuthRepository(firebaseAuth)
    }

    // ── validation guards ────────────────────────────────────────────────────

    @Test(expected = IllegalArgumentException::class)
    fun `signInWithEmail throws on blank email`() = runTest {
        repo.signInWithEmail("", "password123")
    }

    @Test(expected = IllegalArgumentException::class)
    fun `signInWithEmail throws on blank password`() = runTest {
        repo.signInWithEmail("test@example.com", "")
    }

    @Test(expected = IllegalArgumentException::class)
    fun `signUpWithEmail throws on blank email`() = runTest {
        repo.signUpWithEmail("", "password123")
    }

    @Test(expected = IllegalArgumentException::class)
    fun `signUpWithEmail throws on blank password`() = runTest {
        repo.signUpWithEmail("test@example.com", "")
    }

    @Test(expected = IllegalArgumentException::class)
    fun `sendPasswordReset throws on blank email`() = runTest {
        repo.sendPasswordReset("")
    }

    // ── signInWithEmail success ──────────────────────────────────────────────

    @Test
    fun `signInWithEmail returns Success when Firebase returns a user`() = runTest {
        val mockUser = mockk<FirebaseUser>(relaxed = true)
        val mockResult = mockk<com.google.firebase.auth.AuthResult> {
            every { user } returns mockUser
        }
        every { firebaseAuth.signInWithEmailAndPassword(any(), any()) } returns Tasks.forResult(mockResult)

        val result = repo.signInWithEmail("test@example.com", "password123")

        assertTrue(result is AuthResult.Success)
        assertEquals(mockUser, (result as AuthResult.Success).user)
    }

    // ── signUpWithEmail success ──────────────────────────────────────────────

    @Test
    fun `signUpWithEmail returns Success when Firebase returns a user`() = runTest {
        val mockUser = mockk<FirebaseUser>(relaxed = true)
        val mockResult = mockk<com.google.firebase.auth.AuthResult> {
            every { user } returns mockUser
        }
        every { firebaseAuth.createUserWithEmailAndPassword(any(), any()) } returns Tasks.forResult(mockResult)

        val result = repo.signUpWithEmail("new@example.com", "strongpass")

        assertTrue(result is AuthResult.Success)
    }

    // ── signInWithEmail error mapping ────────────────────────────────────────

    @Test
    fun `signInWithEmail returns InvalidCredentials for INVALID_LOGIN_CREDENTIALS`() = runTest {
        val exception = RuntimeException("INVALID_LOGIN_CREDENTIALS: bad creds")
        every { firebaseAuth.signInWithEmailAndPassword(any(), any()) } returns Tasks.forException(exception)

        val result = repo.signInWithEmail("a@b.com", "wrongpass")

        assertTrue(result is AuthResult.Error)
        assertEquals(AuthErrorType.InvalidCredentials, (result as AuthResult.Error).errorType)
    }

    @Test
    fun `signUpWithEmail returns EmailAlreadyExists for email-already-in-use`() = runTest {
        val exception = RuntimeException("email-already-in-use: exists")
        every { firebaseAuth.createUserWithEmailAndPassword(any(), any()) } returns Tasks.forException(exception)

        val result = repo.signUpWithEmail("taken@example.com", "pass123")

        assertTrue(result is AuthResult.Error)
        assertEquals(AuthErrorType.EmailAlreadyExists, (result as AuthResult.Error).errorType)
    }

    @Test
    fun `signInWithEmail returns NetworkError for NETWORK_ERROR`() = runTest {
        val exception = RuntimeException("NETWORK_ERROR: no connection")
        every { firebaseAuth.signInWithEmailAndPassword(any(), any()) } returns Tasks.forException(exception)

        val result = repo.signInWithEmail("a@b.com", "pass")

        assertTrue(result is AuthResult.Error)
        assertEquals(AuthErrorType.NetworkError, (result as AuthResult.Error).errorType)
    }

    @Test
    fun `signInWithEmail returns Unknown for unrecognised exception message`() = runTest {
        val exception = RuntimeException("some totally unknown error")
        every { firebaseAuth.signInWithEmailAndPassword(any(), any()) } returns Tasks.forException(exception)

        val result = repo.signInWithEmail("a@b.com", "pass")

        assertTrue(result is AuthResult.Error)
        assertEquals(AuthErrorType.Unknown, (result as AuthResult.Error).errorType)
    }

    // ── sendPasswordReset ────────────────────────────────────────────────────

    @Test
    fun `sendPasswordReset returns ResetEmailSent on success`() = runTest {
        every { firebaseAuth.sendPasswordResetEmail(any()) } returns Tasks.forResult(null)

        val result = repo.sendPasswordReset("reset@example.com")

        assertTrue(result is AuthResult.ResetEmailSent)
    }

    // ── signOut ──────────────────────────────────────────────────────────────

    @Test
    fun `signOut calls firebaseAuth signOut`() {
        repo.signOut()
        verify { firebaseAuth.signOut() }
    }

    // ── isAuthenticated ──────────────────────────────────────────────────────

    @Test
    fun `isAuthenticated returns true when currentUser is non-null`() {
        every { firebaseAuth.currentUser } returns mockk()
        assertTrue(repo.isAuthenticated)
    }

    @Test
    fun `isAuthenticated returns false when currentUser is null`() {
        every { firebaseAuth.currentUser } returns null
        assertFalse(repo.isAuthenticated)
    }
}
