package com.choreboo.app.data.repository

import com.choreboo.app.data.datastore.UserPreferences
import com.choreboo.app.dataconnect.GetCurrentUserQuery
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.storage.FirebaseStorage
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class UserRepositorySyncTest {

    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var userPreferences: UserPreferences
    private lateinit var firebaseStorage: FirebaseStorage
    private lateinit var cloudDataSource: UserCloudDataSource
    private lateinit var repo: UserRepository

    private val testUid = "test-uid-sync"

    @Before
    fun setUp() {
        firebaseAuth = mockk()
        userPreferences = mockk(relaxed = true)
        firebaseStorage = mockk()
        cloudDataSource = mockk(relaxed = true)

        val user = mockk<FirebaseUser>()
        every { firebaseAuth.currentUser } returns user
        every { user.uid } returns testUid
        every { user.displayName } returns "Test User"
        every { user.email } returns "test@example.com"
        every { user.photoUrl } returns null
        every { user.providerData } returns emptyList()

        every { userPreferences.totalPoints } returns flowOf(100)
        every { userPreferences.totalLifetimeXp } returns flowOf(500)

        repo = UserRepository(
            firebaseAuth = firebaseAuth,
            userPreferences = userPreferences,
            firebaseStorage = firebaseStorage,
            cloudDataSource = cloudDataSource,
        )
    }

    @Test
    fun `syncPointsFromCloud returns immediately when not authenticated`() = runTest {
        every { firebaseAuth.currentUser } returns null

        repo.syncPointsFromCloud()

        coVerify(exactly = 0) { userPreferences.setPointsAndLifetimeXp(any(), any()) }
        coVerify(exactly = 0) { cloudDataSource.getCurrentUser() }
    }

    @Test
    fun `syncPointsFromCloud uses cloud points and higher lifetime xp`() = runTest {
        every { userPreferences.totalPoints } returns flowOf(120)
        every { userPreferences.totalLifetimeXp } returns flowOf(900)
        coEvery { cloudDataSource.getCurrentUser() } returns currentUserData(
            totalPoints = 70,
            totalLifetimeXp = 800,
        )

        repo.syncPointsFromCloud()

        coVerify { userPreferences.setPointsAndLifetimeXp(70, 900) }
        coVerify { cloudDataSource.updateUserPoints(70, 900) }
    }

    @Test
    fun `syncPointsFromCloud does not push back when cloud already matches merged values`() = runTest {
        every { userPreferences.totalPoints } returns flowOf(10)
        every { userPreferences.totalLifetimeXp } returns flowOf(300)
        coEvery { cloudDataSource.getCurrentUser() } returns currentUserData(
            totalPoints = 10,
            totalLifetimeXp = 300,
        )

        repo.syncPointsFromCloud()

        coVerify { userPreferences.setPointsAndLifetimeXp(10, 300) }
        coVerify(exactly = 0) { cloudDataSource.updateUserPoints(any(), any()) }
    }

    @Test
    fun `syncPointsFromCloud leaves lower local lifetime xp behind without restoring stale local points`() = runTest {
        every { userPreferences.totalPoints } returns flowOf(250)
        every { userPreferences.totalLifetimeXp } returns flowOf(100)
        coEvery { cloudDataSource.getCurrentUser() } returns currentUserData(
            totalPoints = 40,
            totalLifetimeXp = 600,
        )

        repo.syncPointsFromCloud()

        coVerify { userPreferences.setPointsAndLifetimeXp(40, 600) }
        coVerify(exactly = 0) { cloudDataSource.updateUserPoints(any(), any()) }
    }

    @Test
    fun `syncPointsToCloud returns immediately when not authenticated`() = runTest {
        every { firebaseAuth.currentUser } returns null

        repo.syncPointsToCloud(totalPoints = 100, totalLifetimeXp = 500)

        coVerify(exactly = 0) { cloudDataSource.updateUserPoints(any(), any()) }
    }

    @Test
    fun `syncCurrentUserToCloud returns immediately when not authenticated`() = runTest {
        every { firebaseAuth.currentUser } returns null

        repo.syncCurrentUserToCloud()

        coVerify(exactly = 0) { cloudDataSource.upsertUser(any(), any(), any()) }
    }

    @Test
    fun `fetchCurrentUserFromCloud returns null when not authenticated`() = runTest {
        every { firebaseAuth.currentUser } returns null

        val result = repo.fetchCurrentUserFromCloud()

        assertEquals(null, result)
    }

    @Test
    fun `fetchCurrentUserFromCloud returns local AppUser when cloud fetch throws`() = runTest {
        coEvery { cloudDataSource.getCurrentUser() } throws RuntimeException("boom")

        val result = repo.fetchCurrentUserFromCloud()

        assertEquals(testUid, result?.uid)
        assertEquals("Test User", result?.displayName)
    }

    private fun currentUserData(
        totalPoints: Int,
        totalLifetimeXp: Int,
    ): GetCurrentUserQuery.Data = GetCurrentUserQuery.Data(
        user = GetCurrentUserQuery.Data.User(
            id = testUid,
            displayName = "Cloud User",
            email = "cloud@example.com",
            photoUrl = null,
            activeChoreboo = null,
            household = null,
            createdAt = Timestamp.now(),
            totalPoints = totalPoints,
            totalLifetimeXp = totalLifetimeXp,
        ),
    )
}
