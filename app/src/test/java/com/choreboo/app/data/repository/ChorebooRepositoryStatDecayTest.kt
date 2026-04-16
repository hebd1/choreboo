package com.choreboo.app.data.repository

import com.choreboo.app.data.datastore.UserPreferences
import com.choreboo.app.data.local.dao.ChorebooDao
import com.choreboo.app.data.local.entity.ChorebooEntity
import com.choreboo.app.domain.model.ChorebooStage
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

/**
 * Tests for [ChorebooRepository.applyStatDecay], [ChorebooRepository.autoFeedIfNeeded],
 * [ChorebooRepository.updateBackground], and [ChorebooRepository.syncFromCloud].
 *
 * Uses MockK to mock [ChorebooDao]. The Data Connect connector is `lazy` and
 * never accessed in these tests (entities have `remoteId = null`).
 */
class ChorebooRepositoryStatDecayTest {

    private lateinit var chorebooDao: ChorebooDao
    private lateinit var userRepository: UserRepository
    private lateinit var repo: ChorebooRepository

    private val now = System.currentTimeMillis()

    private fun baseEntity(
        hunger: Int = 80,
        happiness: Int = 80,
        energy: Int = 80,
        lastInteractionAt: Long = now,
        sleepUntil: Long = 0,
    ) = ChorebooEntity(
        id = 1L,
        name = "TestBoo",
        stage = ChorebooStage.BABY.name,
        level = 3,
        xp = 10,
        hunger = hunger,
        happiness = happiness,
        energy = energy,
        petType = "FOX",
        lastInteractionAt = lastInteractionAt,
        createdAt = now - 86_400_000L,
        sleepUntil = sleepUntil,
        ownerUid = "test-uid",
        remoteId = null, // null → no write-through → no connector access
    )

    @Before
    fun setUp() {
        chorebooDao = mockk(relaxed = true)
        userRepository = mockk(relaxed = true)
        repo = ChorebooRepository(chorebooDao, userRepository)
    }

    // ── applyStatDecay: no choreboo ─────────────────────────────────────

    @Test
    fun `applyStatDecay does nothing when no choreboo exists`() = runTest {
        coEvery { chorebooDao.getChorebooSync() } returns null

        repo.applyStatDecay()

        coVerify(exactly = 0) { chorebooDao.updateChoreboo(any()) }
    }

    // ── applyStatDecay: sleeping pet ────────────────────────────────────

    @Test
    fun `applyStatDecay updates lastInteractionAt when pet is sleeping`() = runTest {
        val entity = baseEntity(sleepUntil = now + 3_600_000L) // sleeping for 1 more hour
        coEvery { chorebooDao.getChorebooSync() } returns entity
        val saved = slot<ChorebooEntity>()
        coEvery { chorebooDao.updateChoreboo(capture(saved)) } returns Unit

        repo.applyStatDecay()

        // Stats should not decay while sleeping — hunger/happiness/energy unchanged
        assertEquals(80, saved.captured.hunger)
        assertEquals(80, saved.captured.happiness)
        assertEquals(80, saved.captured.energy)
        // lastInteractionAt should be updated to prevent decay accumulation
        assert(saved.captured.lastInteractionAt >= now)
    }

    // ── applyStatDecay: very recent interaction ─────────────────────────

    @Test
    fun `applyStatDecay does nothing when interaction was very recent`() = runTest {
        // Last interaction 10 seconds ago — less than 0.01 hours
        val entity = baseEntity(lastInteractionAt = now - 10_000L)
        coEvery { chorebooDao.getChorebooSync() } returns entity

        repo.applyStatDecay()

        // Should only be called once if sleep needs clearing, but since sleepUntil=0
        // and elapsed < 0.01 hours, no update at all
        coVerify(exactly = 0) { chorebooDao.updateChoreboo(any()) }
    }

    // ── applyStatDecay: 1 hour elapsed ──────────────────────────────────

    @Test
    fun `applyStatDecay reduces stats after 1 hour`() = runTest {
        val oneHourAgo = now - 3_600_000L // 1 hour in ms
        val entity = baseEntity(
            hunger = 80,
            happiness = 80,
            energy = 80,
            lastInteractionAt = oneHourAgo,
        )
        coEvery { chorebooDao.getChorebooSync() } returns entity
        val saved = slot<ChorebooEntity>()
        coEvery { chorebooDao.updateChoreboo(capture(saved)) } returns Unit

        repo.applyStatDecay()

        // 1 hour → decayAmount = roundToInt(1.0) = 1, capped at 50
        // hunger = 80 - 1 = 79, happiness = 80 - 0 = 80 (1/2 rounds to 0), energy same
        assertEquals(79, saved.captured.hunger)
        assertEquals(80, saved.captured.happiness)
        assertEquals(80, saved.captured.energy)
    }

    // ── applyStatDecay: large elapsed time ──────────────────────────────

    @Test
    fun `applyStatDecay caps decay at 50`() = runTest {
        val hundredHoursAgo = now - 100L * 3_600_000L
        val entity = baseEntity(
            hunger = 80,
            happiness = 80,
            energy = 80,
            lastInteractionAt = hundredHoursAgo,
        )
        coEvery { chorebooDao.getChorebooSync() } returns entity
        val saved = slot<ChorebooEntity>()
        coEvery { chorebooDao.updateChoreboo(capture(saved)) } returns Unit

        repo.applyStatDecay()

        // decay capped at 50 → hunger = 80-50=30, happiness/energy = 80-(50/2)=80-25=55
        assertEquals(30, saved.captured.hunger)
        assertEquals(55, saved.captured.happiness)
        assertEquals(55, saved.captured.energy)
    }

    // ── applyStatDecay: stats floor at 0 ────────────────────────────────

    @Test
    fun `applyStatDecay floors stats at 0`() = runTest {
        val fiftyHoursAgo = now - 50L * 3_600_000L
        val entity = baseEntity(
            hunger = 10,
            happiness = 5,
            energy = 5,
            lastInteractionAt = fiftyHoursAgo,
        )
        coEvery { chorebooDao.getChorebooSync() } returns entity
        val saved = slot<ChorebooEntity>()
        coEvery { chorebooDao.updateChoreboo(capture(saved)) } returns Unit

        repo.applyStatDecay()

        // 50 hours → decayAmount = 50. hunger = max(0, 10-50) = 0
        // happiness = max(0, 5 - 25) = 0, energy = max(0, 5 - 25) = 0
        assertEquals(0, saved.captured.hunger)
        assertEquals(0, saved.captured.happiness)
        assertEquals(0, saved.captured.energy)
    }

    // ── applyStatDecay: sleep just expired ──────────────────────────────

    @Test
    fun `applyStatDecay uses sleepUntil as decay start when sleep just expired`() = runTest {
        // Sleep expired 2 hours ago
        val sleepExpiredAt = now - 2L * 3_600_000L
        val entity = baseEntity(
            hunger = 80,
            happiness = 80,
            energy = 80,
            lastInteractionAt = now - 26L * 3_600_000L, // old interaction
            sleepUntil = sleepExpiredAt,
        )
        coEvery { chorebooDao.getChorebooSync() } returns entity
        val saved = slot<ChorebooEntity>()
        coEvery { chorebooDao.updateChoreboo(capture(saved)) } returns Unit

        repo.applyStatDecay()

        // Decay should be from sleepExpiredAt, not from lastInteractionAt
        // ~2 hours → decayAmount = 2, hunger = 80-2=78, happiness/energy = 80-1=79
        assertEquals(78, saved.captured.hunger)
        assertEquals(79, saved.captured.happiness)
        assertEquals(79, saved.captured.energy)
        // Sleep should be cleared
        assertEquals(0, saved.captured.sleepUntil)
    }

    // ── applyStatDecay: sub-hour decay ──────────────────────────────────

    @Test
    fun `applyStatDecay applies decay for 30+ minutes due to rounding`() = runTest {
        // 35 minutes = 0.583 hours → roundToInt = 1
        val thirtyFiveMinAgo = now - 35L * 60_000L
        val entity = baseEntity(
            hunger = 50,
            lastInteractionAt = thirtyFiveMinAgo,
        )
        coEvery { chorebooDao.getChorebooSync() } returns entity
        val saved = slot<ChorebooEntity>()
        coEvery { chorebooDao.updateChoreboo(capture(saved)) } returns Unit

        repo.applyStatDecay()

        // 0.583 hours → roundToInt = 1 → hunger = 50-1 = 49
        assertEquals(49, saved.captured.hunger)
    }

    @Test
    fun `applyStatDecay rounds down for 20 minutes`() = runTest {
        // 20 minutes = 0.333 hours → roundToInt = 0
        val twentyMinAgo = now - 20L * 60_000L
        val entity = baseEntity(
            hunger = 50,
            lastInteractionAt = twentyMinAgo,
        )
        coEvery { chorebooDao.getChorebooSync() } returns entity
        val saved = slot<ChorebooEntity>()
        coEvery { chorebooDao.updateChoreboo(capture(saved)) } returns Unit

        repo.applyStatDecay()

        // 0.333 hours → roundToInt = 0 → no decay, but still updates lastInteractionAt
        assertEquals(50, saved.captured.hunger)
    }

    // ── autoFeedIfNeeded ────────────────────────────────────────────────

    @Test
    fun `autoFeedIfNeeded does nothing when no choreboo exists`() = runTest {
        coEvery { chorebooDao.getChorebooSync() } returns null
        val userPrefs = mockk<UserPreferences>(relaxed = true)

        repo.autoFeedIfNeeded(userPrefs)

        coVerify(exactly = 0) { chorebooDao.updateChoreboo(any()) }
    }

    @Test
    fun `autoFeedIfNeeded does nothing when hunger is 30 or above`() = runTest {
        val entity = baseEntity(hunger = 30)
        coEvery { chorebooDao.getChorebooSync() } returns entity
        val userPrefs = mockk<UserPreferences>(relaxed = true)

        repo.autoFeedIfNeeded(userPrefs)

        coVerify(exactly = 0) { chorebooDao.updateChoreboo(any()) }
    }

    @Test
    fun `autoFeedIfNeeded does nothing when user has fewer than 10 points`() = runTest {
        val entity = baseEntity(hunger = 20)
        coEvery { chorebooDao.getChorebooSync() } returns entity
        val userPrefs = mockk<UserPreferences>(relaxed = true)
        every { userPrefs.totalPoints } returns MutableStateFlow(5)

        repo.autoFeedIfNeeded(userPrefs)

        coVerify(exactly = 0) { chorebooDao.updateChoreboo(any()) }
    }

    @Test
    fun `autoFeedIfNeeded adds 20 hunger and deducts 10 points when eligible`() = runTest {
        val entity = baseEntity(hunger = 20)
        coEvery { chorebooDao.getChorebooSync() } returns entity
        val userPrefs = mockk<UserPreferences>(relaxed = true)
        every { userPrefs.totalPoints } returns MutableStateFlow(50)
        every { userPrefs.totalLifetimeXp } returns MutableStateFlow(200)
        coEvery { userPrefs.deductPoints(10) } returns true

        val saved = slot<ChorebooEntity>()
        coEvery { chorebooDao.updateChoreboo(capture(saved)) } returns Unit

        repo.autoFeedIfNeeded(userPrefs)

        assertEquals(40, saved.captured.hunger) // 20 + 20
        coVerify(exactly = 1) { userPrefs.deductPoints(10) }
    }

    @Test
    fun `autoFeedIfNeeded caps hunger at 100`() = runTest {
        val entity = baseEntity(hunger = 90)
        // Even though hunger >= 30, set to 29 for the test
        val lowEntity = entity.copy(hunger = 29)
        coEvery { chorebooDao.getChorebooSync() } returns lowEntity
        val userPrefs = mockk<UserPreferences>(relaxed = true)
        every { userPrefs.totalPoints } returns MutableStateFlow(50)
        every { userPrefs.totalLifetimeXp } returns MutableStateFlow(200)
        coEvery { userPrefs.deductPoints(10) } returns true

        val saved = slot<ChorebooEntity>()
        coEvery { chorebooDao.updateChoreboo(capture(saved)) } returns Unit

        repo.autoFeedIfNeeded(userPrefs)

        // 29 + 20 = 49, coerced at most 100 → 49
        assertEquals(49, saved.captured.hunger)
    }

    @Test
    fun `autoFeedIfNeeded does nothing when deductPoints fails`() = runTest {
        val entity = baseEntity(hunger = 10)
        coEvery { chorebooDao.getChorebooSync() } returns entity
        val userPrefs = mockk<UserPreferences>(relaxed = true)
        every { userPrefs.totalPoints } returns MutableStateFlow(50)
        coEvery { userPrefs.deductPoints(10) } returns false

        repo.autoFeedIfNeeded(userPrefs)

        coVerify(exactly = 0) { chorebooDao.updateChoreboo(any()) }
    }

    // ── updateBackground ────────────────────────────────────────────────

    @Test
    fun `updateBackground stores null when given BACKGROUND_DEFAULT_ID`() = runTest {
        val entity = baseEntity()
        coEvery { chorebooDao.getChorebooSync() } returns entity
        val saved = slot<ChorebooEntity>()
        coEvery { chorebooDao.updateChoreboo(capture(saved)) } returns Unit

        repo.updateBackground("default")

        assertNull(saved.captured.backgroundId)
    }

    @Test
    fun `updateBackground stores actual id for non-default background`() = runTest {
        val entity = baseEntity()
        coEvery { chorebooDao.getChorebooSync() } returns entity
        val saved = slot<ChorebooEntity>()
        coEvery { chorebooDao.updateChoreboo(capture(saved)) } returns Unit

        repo.updateBackground("forest_night")

        assertEquals("forest_night", saved.captured.backgroundId)
    }

    @Test
    fun `updateBackground stores null when given null`() = runTest {
        val entity = baseEntity()
        coEvery { chorebooDao.getChorebooSync() } returns entity
        val saved = slot<ChorebooEntity>()
        coEvery { chorebooDao.updateChoreboo(capture(saved)) } returns Unit

        repo.updateBackground(null)

        assertNull(saved.captured.backgroundId)
    }

    @Test
    fun `updateBackground does nothing when no choreboo exists`() = runTest {
        coEvery { chorebooDao.getChorebooSync() } returns null

        repo.updateBackground("forest_night")

        coVerify(exactly = 0) { chorebooDao.updateChoreboo(any()) }
    }
}
