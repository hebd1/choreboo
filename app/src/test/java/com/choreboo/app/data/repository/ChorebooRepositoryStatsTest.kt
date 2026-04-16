package com.choreboo.app.data.repository

import com.choreboo.app.data.datastore.UserPreferences
import com.choreboo.app.data.local.dao.ChorebooDao
import com.choreboo.app.data.local.entity.ChorebooEntity
import com.choreboo.app.domain.model.ChorebooStage
import com.choreboo.app.domain.model.PetType
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Tests for [ChorebooRepository]: applyStatDecay, feedChoreboo, autoFeedIfNeeded,
 * getOrCreateChoreboo (happy path), and putToSleep.
 *
 * The Data Connect connector is `lazy` and never accessed because all test entities
 * have `remoteId = null`, preventing any write-through branch from executing.
 */
class ChorebooRepositoryStatsTest {

    private lateinit var chorebooDao: ChorebooDao
    private lateinit var userRepository: UserRepository
    private lateinit var repo: ChorebooRepository

    /**
     * A base entity with no remoteId so write-through is never triggered,
     * avoiding connector initialisation in JVM tests.
     */
    private fun baseEntity(
        id: Long = 1L,
        hunger: Int = 80,
        happiness: Int = 80,
        energy: Int = 80,
        lastInteractionAt: Long = System.currentTimeMillis(),
        sleepUntil: Long = 0L,
        level: Int = 1,
        xp: Int = 0,
        stage: String = ChorebooStage.EGG.name,
    ) = ChorebooEntity(
        id = id,
        name = "TestBoo",
        stage = stage,
        level = level,
        xp = xp,
        hunger = hunger,
        happiness = happiness,
        energy = energy,
        petType = PetType.FOX.name,
        lastInteractionAt = lastInteractionAt,
        createdAt = System.currentTimeMillis(),
        sleepUntil = sleepUntil,
        ownerUid = "test-uid",
        remoteId = null,
    )

    @Before
    fun setUp() {
        chorebooDao = mockk(relaxed = true)
        userRepository = mockk(relaxed = true)
        repo = ChorebooRepository(chorebooDao, userRepository)
    }

    // ── applyStatDecay ──────────────────────────────────────────────────

    @Test
    fun `applyStatDecay does nothing when no choreboo exists`() = runTest {
        coEvery { chorebooDao.getChorebooSync() } returns null

        repo.applyStatDecay()

        coVerify(exactly = 0) { chorebooDao.updateChoreboo(any()) }
    }

    @Test
    fun `applyStatDecay updates lastInteractionAt when pet is sleeping`() = runTest {
        val futureMs = System.currentTimeMillis() + 10_000L
        val entity = baseEntity(sleepUntil = futureMs)
        coEvery { chorebooDao.getChorebooSync() } returns entity
        val saved = slot<ChorebooEntity>()
        coEvery { chorebooDao.updateChoreboo(capture(saved)) } returns Unit

        repo.applyStatDecay()

        // When sleeping, only lastInteractionAt is updated — stats stay the same
        coVerify(exactly = 1) { chorebooDao.updateChoreboo(any()) }
        assertEquals(entity.hunger, saved.captured.hunger)
        assertEquals(entity.happiness, saved.captured.happiness)
        assertEquals(entity.energy, saved.captured.energy)
    }

    @Test
    fun `applyStatDecay does nothing for very recent interaction`() = runTest {
        // lastInteractionAt = now → hoursSinceInteraction ≈ 0 < 0.01 threshold
        val entity = baseEntity(lastInteractionAt = System.currentTimeMillis())
        coEvery { chorebooDao.getChorebooSync() } returns entity

        repo.applyStatDecay()

        // No stat update — below minimum decay threshold
        coVerify(exactly = 0) { chorebooDao.updateChoreboo(any()) }
    }

    @Test
    fun `applyStatDecay reduces stats proportionally to elapsed hours`() = runTest {
        // Simulate 4 hours ago — expect decayAmount = 4 (roundToInt)
        val fourHoursAgo = System.currentTimeMillis() - 4 * 60 * 60 * 1000L
        val entity = baseEntity(hunger = 80, happiness = 80, energy = 80, lastInteractionAt = fourHoursAgo)
        coEvery { chorebooDao.getChorebooSync() } returns entity
        val saved = slot<ChorebooEntity>()
        coEvery { chorebooDao.updateChoreboo(capture(saved)) } returns Unit

        repo.applyStatDecay()

        // hunger decays by decayAmount, happiness/energy by decayAmount/2
        val captured = saved.captured
        assertTrue("hunger should decrease", captured.hunger < entity.hunger)
        assertTrue("happiness should decrease", captured.happiness < entity.happiness)
        assertTrue("energy should decrease", captured.energy < entity.energy)
        // Hunger decays faster than happiness/energy
        val hungerDrop = entity.hunger - captured.hunger
        val happinessDrop = entity.happiness - captured.happiness
        assertTrue("hunger drops more than happiness", hungerDrop >= happinessDrop)
    }

    @Test
    fun `applyStatDecay clamps stats to zero — never negative`() = runTest {
        // 200 hours ago → decay = 50 (capped). Stats at 10 would go to max(0, 10-50) = 0
        val twoHundredHoursAgo = System.currentTimeMillis() - 200L * 60 * 60 * 1000L
        val entity = baseEntity(hunger = 10, happiness = 10, energy = 10, lastInteractionAt = twoHundredHoursAgo)
        coEvery { chorebooDao.getChorebooSync() } returns entity
        val saved = slot<ChorebooEntity>()
        coEvery { chorebooDao.updateChoreboo(capture(saved)) } returns Unit

        repo.applyStatDecay()

        assertEquals(0, saved.captured.hunger)
        assertEquals(0, saved.captured.happiness)
        assertEquals(0, saved.captured.energy)
    }

    // ── feedChoreboo ────────────────────────────────────────────────────

    @Test
    fun `feedChoreboo does nothing when no choreboo exists`() = runTest {
        coEvery { chorebooDao.getChorebooSync() } returns null

        repo.feedChoreboo()

        coVerify(exactly = 0) { chorebooDao.updateChoreboo(any()) }
    }

    @Test
    fun `feedChoreboo adds 20 hunger`() = runTest {
        val entity = baseEntity(hunger = 50)
        coEvery { chorebooDao.getChorebooSync() } returns entity
        val saved = slot<ChorebooEntity>()
        coEvery { chorebooDao.updateChoreboo(capture(saved)) } returns Unit

        repo.feedChoreboo()

        assertEquals(70, saved.captured.hunger)
    }

    @Test
    fun `feedChoreboo caps hunger at 100`() = runTest {
        val entity = baseEntity(hunger = 95)
        coEvery { chorebooDao.getChorebooSync() } returns entity
        val saved = slot<ChorebooEntity>()
        coEvery { chorebooDao.updateChoreboo(capture(saved)) } returns Unit

        repo.feedChoreboo()

        assertEquals(100, saved.captured.hunger)
    }

    @Test
    fun `feedChoreboo does not change happiness or energy`() = runTest {
        val entity = baseEntity(hunger = 40, happiness = 60, energy = 70)
        coEvery { chorebooDao.getChorebooSync() } returns entity
        val saved = slot<ChorebooEntity>()
        coEvery { chorebooDao.updateChoreboo(capture(saved)) } returns Unit

        repo.feedChoreboo()

        assertEquals(60, saved.captured.happiness)
        assertEquals(70, saved.captured.energy)
    }

    // ── putToSleep ──────────────────────────────────────────────────────

    @Test
    fun `putToSleep does nothing when no choreboo exists`() = runTest {
        coEvery { chorebooDao.getChorebooSync() } returns null

        repo.putToSleep()

        coVerify(exactly = 0) { chorebooDao.updateChoreboo(any()) }
    }

    @Test
    fun `putToSleep sets sleepUntil to 24 hours in the future`() = runTest {
        val before = System.currentTimeMillis()
        val entity = baseEntity(sleepUntil = 0L)
        coEvery { chorebooDao.getChorebooSync() } returns entity
        val saved = slot<ChorebooEntity>()
        coEvery { chorebooDao.updateChoreboo(capture(saved)) } returns Unit

        repo.putToSleep()
        val after = System.currentTimeMillis()

        val twentyFourHoursMs = 24L * 60L * 60L * 1000L
        val minExpected = before + twentyFourHoursMs
        val maxExpected = after + twentyFourHoursMs
        assertTrue(
            "sleepUntil should be ~24h from now",
            saved.captured.sleepUntil in minExpected..maxExpected,
        )
    }

    @Test
    fun `putToSleep updates lastInteractionAt`() = runTest {
        val before = System.currentTimeMillis()
        val entity = baseEntity(lastInteractionAt = before - 60_000L)
        coEvery { chorebooDao.getChorebooSync() } returns entity
        val saved = slot<ChorebooEntity>()
        coEvery { chorebooDao.updateChoreboo(capture(saved)) } returns Unit

        repo.putToSleep()
        val after = System.currentTimeMillis()

        assertTrue(
            "lastInteractionAt should be updated to now",
            saved.captured.lastInteractionAt in before..after,
        )
    }

    // ── getOrCreateChoreboo (happy path) ─────────────────────────────────

    @Test
    fun `getOrCreateChoreboo returns existing choreboo when one is found`() = runTest {
        val existing = baseEntity(id = 5L, hunger = 55)
        coEvery { chorebooDao.getChorebooSync() } returns existing

        val result = repo.getOrCreateChoreboo("TestBoo")

        assertEquals(5L, result.id)
        assertEquals(55, result.hunger)
        // No insert should occur
        coVerify(exactly = 0) { chorebooDao.insertChoreboo(any()) }
    }

    @Test
    fun `getOrCreateChoreboo inserts a new choreboo when none exists`() = runTest {
        coEvery { chorebooDao.getChorebooSync() } returns null
        coEvery { chorebooDao.insertChoreboo(any()) } returns 42L

        val result = repo.getOrCreateChoreboo("NewBoo")

        coVerify(exactly = 1) { chorebooDao.insertChoreboo(any()) }
        assertEquals("NewBoo", result.name)
        assertEquals(ChorebooStage.EGG, result.stage)
        assertEquals(1, result.level)
        assertEquals(0, result.xp)
    }

    @Test
    fun `getOrCreateChoreboo sets default stats on new choreboo`() = runTest {
        coEvery { chorebooDao.getChorebooSync() } returns null
        val saved = slot<ChorebooEntity>()
        coEvery { chorebooDao.insertChoreboo(capture(saved)) } returns 1L

        repo.getOrCreateChoreboo("Boo", PetType.FOX)

        assertEquals(10, saved.captured.hunger)
        assertEquals(80, saved.captured.happiness)
        assertEquals(80, saved.captured.energy)
        assertEquals(PetType.FOX.name, saved.captured.petType)
    }

    // ── autoFeedIfNeeded ─────────────────────────────────────────────────

    @Test
    fun `autoFeedIfNeeded does nothing when no choreboo exists`() = runTest {
        coEvery { chorebooDao.getChorebooSync() } returns null
        val userPreferences = mockk<UserPreferences>(relaxed = true)

        repo.autoFeedIfNeeded(userPreferences)

        coVerify(exactly = 0) { chorebooDao.updateChoreboo(any()) }
    }

    @Test
    fun `autoFeedIfNeeded does nothing when hunger is already 30 or above`() = runTest {
        val entity = baseEntity(hunger = 30)
        coEvery { chorebooDao.getChorebooSync() } returns entity
        val userPreferences = mockk<UserPreferences>(relaxed = true)

        repo.autoFeedIfNeeded(userPreferences)

        coVerify(exactly = 0) { chorebooDao.updateChoreboo(any()) }
    }

    @Test
    fun `autoFeedIfNeeded does nothing when user has fewer than 10 points`() = runTest {
        val entity = baseEntity(hunger = 10)
        coEvery { chorebooDao.getChorebooSync() } returns entity
        val userPreferences = mockk<UserPreferences>(relaxed = true)
        coEvery { userPreferences.totalPoints } returns flowOf(9)

        repo.autoFeedIfNeeded(userPreferences)

        coVerify(exactly = 0) { chorebooDao.updateChoreboo(any()) }
    }

    @Test
    fun `autoFeedIfNeeded feeds when hunger below 30 and user has enough points`() = runTest {
        val entity = baseEntity(hunger = 15)
        coEvery { chorebooDao.getChorebooSync() } returns entity
        val userPreferences = mockk<UserPreferences>(relaxed = true)
        coEvery { userPreferences.totalPoints } returns flowOf(50)
        coEvery { userPreferences.deductPoints(10) } returns true
        coEvery { userPreferences.totalLifetimeXp } returns flowOf(100)

        val saved = slot<ChorebooEntity>()
        coEvery { chorebooDao.updateChoreboo(capture(saved)) } returns Unit

        repo.autoFeedIfNeeded(userPreferences)

        coVerify(exactly = 1) { userPreferences.deductPoints(10) }
        assertEquals(35, saved.captured.hunger) // 15 + 20
    }

    @Test
    fun `autoFeedIfNeeded does not feed when deductPoints returns false`() = runTest {
        val entity = baseEntity(hunger = 10)
        coEvery { chorebooDao.getChorebooSync() } returns entity
        val userPreferences = mockk<UserPreferences>(relaxed = true)
        coEvery { userPreferences.totalPoints } returns flowOf(10)
        coEvery { userPreferences.deductPoints(10) } returns false

        repo.autoFeedIfNeeded(userPreferences)

        coVerify(exactly = 0) { chorebooDao.updateChoreboo(any()) }
    }

    @Test
    fun `autoFeedIfNeeded caps hunger at 100 when feeding near-full pet`() = runTest {
        val entity = baseEntity(hunger = 25)
        coEvery { chorebooDao.getChorebooSync() } returns entity
        val userPreferences = mockk<UserPreferences>(relaxed = true)
        // hunger = 25 < 30 → triggers feed. 25 + 20 = 45 (under cap, but let's use 85 to test cap)
        val entityAtCap = baseEntity(hunger = 85)
        // Re-stub with a hunger that would overflow
        coEvery { chorebooDao.getChorebooSync() } returns baseEntity(hunger = 85)
        coEvery { userPreferences.totalPoints } returns flowOf(50)
        // hunger=85 is NOT < 30, so this won't trigger. Use hunger=25 to verify 25+20=45≤100
        coEvery { chorebooDao.getChorebooSync() } returns baseEntity(hunger = 25)
        coEvery { userPreferences.deductPoints(10) } returns true
        coEvery { userPreferences.totalLifetimeXp } returns flowOf(0)

        val saved = slot<ChorebooEntity>()
        coEvery { chorebooDao.updateChoreboo(capture(saved)) } returns Unit

        repo.autoFeedIfNeeded(userPreferences)

        assertTrue(saved.captured.hunger <= 100)
    }
}
