package com.example.choreboo_habittrackerfriend.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests for [ChorebooStats] computed properties:
 *   - [ChorebooStats.overallMood] — average of hunger, happiness, energy
 *   - [ChorebooStats.mood] — priority: HUNGRY (hunger<20) > TIRED (energy<20)
 *       > SAD (<30) > IDLE (<50) > CONTENT (<70) > HAPPY (>=70)
 *   - [ChorebooStats.xpToNextLevel] — level * 50
 *   - [ChorebooStats.xpProgressFraction] — xp / xpToNextLevel
 *   - [ChorebooStats.isSleeping] — sleepUntil > now
 *   - [ChorebooStats.isHungry] — hunger < 30
 *   - [ChorebooStats.needsAttention] — any stat < 20
 */
class ChorebooStatsTest {

    // ── helpers ──────────────────────────────────────────────────────────

    private fun stats(
        hunger: Int = 80,
        happiness: Int = 80,
        energy: Int = 80,
        level: Int = 1,
        xp: Int = 0,
        sleepUntil: Long = 0,
    ) = ChorebooStats(
        hunger = hunger,
        happiness = happiness,
        energy = energy,
        level = level,
        xp = xp,
        sleepUntil = sleepUntil,
    )

    // ── overallMood ─────────────────────────────────────────────────────

    @Test
    fun `overallMood is integer average of three stats`() {
        assertEquals(50, stats(hunger = 30, happiness = 60, energy = 60).overallMood)
    }

    @Test
    fun `overallMood truncates (integer division)`() {
        // (10 + 10 + 11) / 3 = 10.33… → 10
        assertEquals(10, stats(hunger = 10, happiness = 10, energy = 11).overallMood)
    }

    @Test
    fun `overallMood all zeros`() {
        assertEquals(0, stats(hunger = 0, happiness = 0, energy = 0).overallMood)
    }

    @Test
    fun `overallMood all 100`() {
        assertEquals(100, stats(hunger = 100, happiness = 100, energy = 100).overallMood)
    }

    // ── mood thresholds ─────────────────────────────────────────────────

    @Test
    fun `mood - HUNGRY when hunger below 20 (priority 1)`() {
        // hunger < 20 takes priority over everything
        assertEquals(
            ChorebooMood.HUNGRY,
            stats(hunger = 19, happiness = 100, energy = 100).mood,
        )
    }

    @Test
    fun `mood - HUNGRY at hunger 0`() {
        assertEquals(ChorebooMood.HUNGRY, stats(hunger = 0, happiness = 0, energy = 0).mood)
    }

    @Test
    fun `mood - not HUNGRY at hunger exactly 20`() {
        // hunger == 20 does NOT trigger HUNGRY (strictly < 20)
        val mood = stats(hunger = 20, happiness = 80, energy = 80).mood
        assertTrue(
            "hunger=20 should not be HUNGRY, was $mood",
            mood != ChorebooMood.HUNGRY,
        )
    }

    @Test
    fun `mood - TIRED when energy below 20 and hunger at least 20 (priority 2)`() {
        assertEquals(
            ChorebooMood.TIRED,
            stats(hunger = 20, happiness = 100, energy = 19).mood,
        )
    }

    @Test
    fun `mood - not TIRED at energy exactly 20`() {
        val mood = stats(hunger = 80, happiness = 80, energy = 20).mood
        assertTrue(
            "energy=20 should not be TIRED, was $mood",
            mood != ChorebooMood.TIRED,
        )
    }

    @Test
    fun `mood - HUNGRY beats TIRED when both below 20`() {
        assertEquals(
            ChorebooMood.HUNGRY,
            stats(hunger = 10, happiness = 50, energy = 10).mood,
        )
    }

    @Test
    fun `mood - SAD when overallMood below 30`() {
        // overallMood = (20 + 20 + 20) / 3 = 20 < 30
        assertEquals(
            ChorebooMood.SAD,
            stats(hunger = 20, happiness = 20, energy = 20).mood,
        )
    }

    @Test
    fun `mood - not SAD at overallMood exactly 30`() {
        // overallMood = (30 + 30 + 30) / 3 = 30 → should be IDLE not SAD
        assertEquals(
            ChorebooMood.IDLE,
            stats(hunger = 30, happiness = 30, energy = 30).mood,
        )
    }

    @Test
    fun `mood - IDLE when overallMood 30 to 49`() {
        // overallMood = (49 + 49 + 49) / 3 = 49 < 50
        assertEquals(
            ChorebooMood.IDLE,
            stats(hunger = 49, happiness = 49, energy = 49).mood,
        )
    }

    @Test
    fun `mood - not IDLE at overallMood exactly 50`() {
        assertEquals(
            ChorebooMood.CONTENT,
            stats(hunger = 50, happiness = 50, energy = 50).mood,
        )
    }

    @Test
    fun `mood - CONTENT when overallMood 50 to 69`() {
        assertEquals(
            ChorebooMood.CONTENT,
            stats(hunger = 69, happiness = 69, energy = 69).mood,
        )
    }

    @Test
    fun `mood - not CONTENT at overallMood exactly 70`() {
        assertEquals(
            ChorebooMood.HAPPY,
            stats(hunger = 70, happiness = 70, energy = 70).mood,
        )
    }

    @Test
    fun `mood - HAPPY when overallMood 70 or above`() {
        assertEquals(
            ChorebooMood.HAPPY,
            stats(hunger = 100, happiness = 100, energy = 100).mood,
        )
    }

    // ── xpToNextLevel ───────────────────────────────────────────────────

    @Test
    fun `xpToNextLevel at level 1 is 50`() {
        assertEquals(50, stats(level = 1).xpToNextLevel)
    }

    @Test
    fun `xpToNextLevel at level 10 is 500`() {
        assertEquals(500, stats(level = 10).xpToNextLevel)
    }

    @Test
    fun `xpToNextLevel at level 100 is 5000`() {
        assertEquals(5000, stats(level = 100).xpToNextLevel)
    }

    // ── xpProgressFraction ──────────────────────────────────────────────

    @Test
    fun `xpProgressFraction zero xp is 0`() {
        assertEquals(0f, stats(level = 1, xp = 0).xpProgressFraction, 0.001f)
    }

    @Test
    fun `xpProgressFraction half xp is 0_5`() {
        // level 1 → xpToNextLevel = 50, xp = 25 → 0.5
        assertEquals(0.5f, stats(level = 1, xp = 25).xpProgressFraction, 0.001f)
    }

    @Test
    fun `xpProgressFraction full xp is 1_0`() {
        assertEquals(1.0f, stats(level = 1, xp = 50).xpProgressFraction, 0.001f)
    }

    @Test
    fun `xpProgressFraction overflow xp exceeds 1_0`() {
        // xp can temporarily exceed xpToNextLevel before level-up is processed
        assertEquals(2.0f, stats(level = 1, xp = 100).xpProgressFraction, 0.001f)
    }

    // ── isSleeping ──────────────────────────────────────────────────────

    @Test
    fun `isSleeping is false when sleepUntil is 0`() {
        assertFalse(stats(sleepUntil = 0).isSleeping)
    }

    @Test
    fun `isSleeping is false when sleepUntil is in the past`() {
        assertFalse(stats(sleepUntil = System.currentTimeMillis() - 60_000).isSleeping)
    }

    @Test
    fun `isSleeping is true when sleepUntil is in the future`() {
        assertTrue(stats(sleepUntil = System.currentTimeMillis() + 60_000).isSleeping)
    }

    // ── isHungry ────────────────────────────────────────────────────────

    @Test
    fun `isHungry is true when hunger below 30`() {
        assertTrue(stats(hunger = 29).isHungry)
    }

    @Test
    fun `isHungry is false when hunger is 30`() {
        assertFalse(stats(hunger = 30).isHungry)
    }

    // ── needsAttention ──────────────────────────────────────────────────

    @Test
    fun `needsAttention is true when hunger below 20`() {
        assertTrue(stats(hunger = 19, happiness = 80, energy = 80).needsAttention)
    }

    @Test
    fun `needsAttention is true when happiness below 20`() {
        assertTrue(stats(hunger = 80, happiness = 19, energy = 80).needsAttention)
    }

    @Test
    fun `needsAttention is true when energy below 20`() {
        assertTrue(stats(hunger = 80, happiness = 80, energy = 19).needsAttention)
    }

    @Test
    fun `needsAttention is false when all stats at 20`() {
        assertFalse(stats(hunger = 20, happiness = 20, energy = 20).needsAttention)
    }
}
