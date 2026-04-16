package com.choreboo.app.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Tests for [ChorebooStage.fromTotalXp].
 *
 * Stage thresholds:
 *   EGG       0
 *   BABY    100
 *   CHILD   500
 *   TEEN   1500
 *   ADULT  5000
 *   LEGENDARY 15000
 *
 * The function picks the highest stage whose threshold is <= totalXp.
 * Negative values are clamped to 0 (B3 fix).
 */
class ChorebooStageTest {

    // ── exact boundaries ────────────────────────────────────────────────

    @Test
    fun `0 xp is EGG`() {
        assertEquals(ChorebooStage.EGG, ChorebooStage.fromTotalXp(0))
    }

    @Test
    fun `99 xp is still EGG`() {
        assertEquals(ChorebooStage.EGG, ChorebooStage.fromTotalXp(99))
    }

    @Test
    fun `100 xp is BABY`() {
        assertEquals(ChorebooStage.BABY, ChorebooStage.fromTotalXp(100))
    }

    @Test
    fun `499 xp is still BABY`() {
        assertEquals(ChorebooStage.BABY, ChorebooStage.fromTotalXp(499))
    }

    @Test
    fun `500 xp is CHILD`() {
        assertEquals(ChorebooStage.CHILD, ChorebooStage.fromTotalXp(500))
    }

    @Test
    fun `1499 xp is still CHILD`() {
        assertEquals(ChorebooStage.CHILD, ChorebooStage.fromTotalXp(1499))
    }

    @Test
    fun `1500 xp is TEEN`() {
        assertEquals(ChorebooStage.TEEN, ChorebooStage.fromTotalXp(1500))
    }

    @Test
    fun `4999 xp is still TEEN`() {
        assertEquals(ChorebooStage.TEEN, ChorebooStage.fromTotalXp(4999))
    }

    @Test
    fun `5000 xp is ADULT`() {
        assertEquals(ChorebooStage.ADULT, ChorebooStage.fromTotalXp(5000))
    }

    @Test
    fun `14999 xp is still ADULT`() {
        assertEquals(ChorebooStage.ADULT, ChorebooStage.fromTotalXp(14999))
    }

    @Test
    fun `15000 xp is LEGENDARY`() {
        assertEquals(ChorebooStage.LEGENDARY, ChorebooStage.fromTotalXp(15000))
    }

    // ── beyond max ──────────────────────────────────────────────────────

    @Test
    fun `very high xp is LEGENDARY`() {
        assertEquals(ChorebooStage.LEGENDARY, ChorebooStage.fromTotalXp(999_999))
    }

    @Test
    fun `Int MAX_VALUE is LEGENDARY`() {
        assertEquals(ChorebooStage.LEGENDARY, ChorebooStage.fromTotalXp(Int.MAX_VALUE))
    }

    // ── negative values (clamped to 0 by B3 fix) ────────────────────────

    @Test
    fun `negative xp is clamped to EGG`() {
        assertEquals(ChorebooStage.EGG, ChorebooStage.fromTotalXp(-1))
    }

    @Test
    fun `large negative xp is clamped to EGG`() {
        assertEquals(ChorebooStage.EGG, ChorebooStage.fromTotalXp(-100_000))
    }

    @Test
    fun `Int MIN_VALUE is clamped to EGG`() {
        assertEquals(ChorebooStage.EGG, ChorebooStage.fromTotalXp(Int.MIN_VALUE))
    }

    // ── enum metadata ───────────────────────────────────────────────────

    @Test
    fun `thresholds are monotonically increasing`() {
        val thresholds = ChorebooStage.entries.map { it.xpThreshold }
        assertEquals(thresholds, thresholds.sorted())
        // Also verify first threshold is 0
        assertEquals(0, thresholds.first())
    }
}
