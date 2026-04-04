package com.example.choreboo_habittrackerfriend.data.repository

import com.example.choreboo_habittrackerfriend.data.local.dao.HouseholdMemberDao
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

/**
 * Tests for [HouseholdRepository] input validation guards.
 *
 * The connector is `lazy` and never initialised in these tests because the
 * `require()` guards fire before any network call is made.
 */
class HouseholdRepositoryValidationTest {

    private lateinit var repo: HouseholdRepository

    @Before
    fun setUp() {
        repo = HouseholdRepository(
            householdMemberDao = mockk(relaxed = true),
            userRepository = mockk(relaxed = true),
            habitRepository = mockk(relaxed = true),
        )
    }

    // ── createHousehold — blank name ────────────────────────────────────

    @Test(expected = IllegalArgumentException::class)
    fun `createHousehold throws on blank name`() = runTest {
        repo.createHousehold(name = "")
    }

    @Test(expected = IllegalArgumentException::class)
    fun `createHousehold throws on whitespace-only name`() = runTest {
        repo.createHousehold(name = "   ")
    }

    // ── createHousehold — name too long ─────────────────────────────────

    @Test(expected = IllegalArgumentException::class)
    fun `createHousehold throws when name exceeds 50 chars`() = runTest {
        repo.createHousehold(name = "A".repeat(51))
    }

    @Test
    fun `createHousehold accepts name of exactly 50 chars`() = runTest {
        // Should not throw — 50 chars is the boundary and is valid.
        // The call will fail later when the connector tries to reach the network,
        // but we only care that the require() guard does NOT fire.
        try {
            repo.createHousehold(name = "A".repeat(50))
        } catch (e: IllegalArgumentException) {
            throw AssertionError("createHousehold should not throw IllegalArgumentException for a 50-char name", e)
        } catch (_: Exception) {
            // Any other exception (network, auth, etc.) is expected in a unit test — ignore it.
        }
    }

    // ── joinHousehold — blank invite code ───────────────────────────────

    @Test(expected = IllegalArgumentException::class)
    fun `joinHousehold throws on blank invite code`() = runTest {
        repo.joinHousehold(inviteCode = "")
    }

    @Test(expected = IllegalArgumentException::class)
    fun `joinHousehold throws on whitespace-only invite code`() = runTest {
        repo.joinHousehold(inviteCode = "   ")
    }
}
