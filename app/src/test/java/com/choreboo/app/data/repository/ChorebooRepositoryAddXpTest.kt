package com.choreboo.app.data.repository

import com.choreboo.app.data.local.dao.ChorebooDao
import com.choreboo.app.data.local.entity.ChorebooEntity
import com.choreboo.app.data.repository.UserRepository
import com.choreboo.app.domain.model.ChorebooStage
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Tests for [ChorebooRepository.addXp].
 *
 * Uses MockK to mock [ChorebooDao]. The Data Connect connector is `lazy` and
 * never accessed in these tests (no write-through calls fire because the test
 * entity has `remoteId = null`).
 */
class ChorebooRepositoryAddXpTest {

    private lateinit var chorebooDao: ChorebooDao
    private lateinit var userRepository: UserRepository
    private lateinit var repo: ChorebooRepository

    private fun baseEntity(
        level: Int = 1,
        xp: Int = 0,
        stage: String = ChorebooStage.EGG.name,
    ) = ChorebooEntity(
        id = 1L,
        name = "TestBoo",
        stage = stage,
        level = level,
        xp = xp,
        hunger = 80,
        happiness = 80,
        energy = 80,
        petType = "FOX",
        lastInteractionAt = System.currentTimeMillis(),
        createdAt = System.currentTimeMillis(),
        sleepUntil = 0,
        ownerUid = "test-uid",
        remoteId = null, // null → no write-through call → no connector access
    )

    @Before
    fun setUp() {
        chorebooDao = mockk(relaxed = true)
        userRepository = mockk(relaxed = true)
        repo = ChorebooRepository(chorebooDao, userRepository)
    }

    // ── Basic XP addition ───────────────────────────────────────────────

    @Test
    fun `addXp adds XP without leveling up`() = runTest {
        coEvery { chorebooDao.getActiveChorebooSync() } returns baseEntity(level = 1, xp = 0)
        val saved = slot<ChorebooEntity>()
        coEvery { chorebooDao.updateChoreboo(capture(saved)) } returns Unit

        val result = repo.addXp(10)

        assertEquals(0, result.levelsGained)
        assertEquals(1, result.newLevel)
        assertFalse(result.evolved)
        assertNull(result.newStage)
        assertEquals(10, saved.captured.xp)
        assertEquals(1, saved.captured.level)
    }

    @Test
    fun `addXp returns empty result when no choreboo exists`() = runTest {
        coEvery { chorebooDao.getActiveChorebooSync() } returns null

        val result = repo.addXp(10)

        assertEquals(0, result.levelsGained)
        assertEquals(1, result.newLevel)
        assertFalse(result.evolved)
        coVerify(exactly = 0) { chorebooDao.updateChoreboo(any()) }
    }

    // ── Level-up ────────────────────────────────────────────────────────

    @Test
    fun `addXp levels up when XP reaches threshold`() = runTest {
        // level 1: xpNeeded = 50. Add 50 → level 2, xp = 0
        coEvery { chorebooDao.getActiveChorebooSync() } returns baseEntity(level = 1, xp = 0)
        val saved = slot<ChorebooEntity>()
        coEvery { chorebooDao.updateChoreboo(capture(saved)) } returns Unit

        val result = repo.addXp(50)

        assertEquals(1, result.levelsGained)
        assertEquals(2, result.newLevel)
        assertEquals(0, saved.captured.xp)
        assertEquals(2, saved.captured.level)
    }

    @Test
    fun `addXp levels up with overflow XP`() = runTest {
        // level 1: xpNeeded = 50. xp = 40 + 25 = 65. 65 - 50 = 15 remainder at level 2
        coEvery { chorebooDao.getActiveChorebooSync() } returns baseEntity(level = 1, xp = 40)
        val saved = slot<ChorebooEntity>()
        coEvery { chorebooDao.updateChoreboo(capture(saved)) } returns Unit

        val result = repo.addXp(25)

        assertEquals(1, result.levelsGained)
        assertEquals(2, result.newLevel)
        assertEquals(15, saved.captured.xp)
    }

    @Test
    fun `addXp multiple level-ups in one call`() = runTest {
        // level 1 xpNeeded=50, level 2 xpNeeded=100. Add 200:
        // 200 - 50 = 150 (level 2), 150 - 100 = 50 (level 3), 50 < 150 → stop at level 3 xp=50
        coEvery { chorebooDao.getActiveChorebooSync() } returns baseEntity(level = 1, xp = 0)
        val saved = slot<ChorebooEntity>()
        coEvery { chorebooDao.updateChoreboo(capture(saved)) } returns Unit

        val result = repo.addXp(200)

        assertEquals(2, result.levelsGained)
        assertEquals(3, result.newLevel)
        assertEquals(50, saved.captured.xp)
    }

    // ── Stage evolution ─────────────────────────────────────────────────

    @Test
    fun `addXp detects stage evolution from EGG to BABY`() = runTest {
        // EGG→BABY at 100 totalXp. Level 1 needs 50 xp. At level 2, totalXp = 50.
        // At level 3 (xpNeeded for level 2 = 100), totalXp = 50+100 = 150 → BABY (>100)
        // Let's compute: start at level 1 xp=0, add 150.
        // 150 - 50 = 100 (level 2), 100 - 100 = 0 (level 3), totalXp = (1*50 + 2*100) nah let me think.
        // totalXpEarned = sum(1 until newLevel) { it * 50 } + newXp
        // If newLevel=3, newXp=0: totalXpEarned = (1*50)+(2*50)+0 = 50+100+0 = ... wait no.
        // (1 until 3).sumOf { it * 50 } = 1*50 + 2*50 = 150. totalXp = 150 → BABY (>=100)
        coEvery { chorebooDao.getActiveChorebooSync() } returns baseEntity(level = 1, xp = 0, stage = "EGG")
        val saved = slot<ChorebooEntity>()
        coEvery { chorebooDao.updateChoreboo(capture(saved)) } returns Unit

        val result = repo.addXp(150)

        assertTrue(result.evolved)
        assertEquals(ChorebooStage.BABY, result.newStage)
        assertEquals("BABY", saved.captured.stage)
    }

    @Test
    fun `addXp no evolution when stage stays the same`() = runTest {
        // Already BABY (100+), add small amount — stays BABY
        // level=3, xp=0: totalXp = (1*50)+(2*50) = 150 (BABY). Add 10 → level=3, xp=10, totalXp=160 → still BABY
        coEvery { chorebooDao.getActiveChorebooSync() } returns baseEntity(level = 3, xp = 0, stage = "BABY")
        val saved = slot<ChorebooEntity>()
        coEvery { chorebooDao.updateChoreboo(capture(saved)) } returns Unit

        val result = repo.addXp(10)

        assertFalse(result.evolved)
        assertNull(result.newStage)
    }

    // ── Validation ──────────────────────────────────────────────────────

    @Test(expected = IllegalArgumentException::class)
    fun `addXp throws on zero amount`() = runTest {
        repo.addXp(0)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `addXp throws on negative amount`() = runTest {
        repo.addXp(-5)
    }

    // ── Name length validation ───────────────────────────────────────────

    @Test(expected = IllegalArgumentException::class)
    fun `getOrCreateChoreboo throws on name exceeding 20 characters`() = runTest {
        repo.getOrCreateChoreboo(name = "A".repeat(21))
    }

    @Test
    fun `getOrCreateChoreboo accepts name at exactly 20 characters`() = runTest {
        coEvery { chorebooDao.getActiveChorebooSync() } returns baseEntity()
        // Should not throw — existing choreboo returned before any insertion
        repo.getOrCreateChoreboo(name = "A".repeat(20))
    }

    @Test(expected = IllegalArgumentException::class)
    fun `updateName throws on name exceeding 20 characters`() = runTest {
        repo.updateName("A".repeat(21))
    }
}
