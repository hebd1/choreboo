package com.choreboo.app.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Tests for [HouseholdPet.mood].
 *
 * The mood logic in [HouseholdPet] is duplicated from [ChorebooStats.mood].
 * These tests verify the HouseholdPet copy behaves identically at all
 * boundary values, so a future refactor (extracting shared logic) can
 * use these tests as a safety net.
 */
class HouseholdPetTest {

    // ── helper ──────────────────────────────────────────────────────────

    private fun pet(
        hunger: Int = 80,
        happiness: Int = 80,
        energy: Int = 80,
    ) = HouseholdPet(
        chorebooId = "test-id",
        name = "TestPet",
        stage = ChorebooStage.BABY,
        level = 1,
        xp = 0,
        hunger = hunger,
        happiness = happiness,
        energy = energy,
        petType = PetType.FOX,
        ownerName = "Owner",
        ownerUid = "uid-1",
    )

    // ── priority 1: HUNGRY (hunger < 20) ────────────────────────────────

    @Test
    fun `HUNGRY when hunger below 20`() {
        assertEquals(ChorebooMood.HUNGRY, pet(hunger = 19, energy = 100).mood)
    }

    @Test
    fun `HUNGRY at hunger 0`() {
        assertEquals(ChorebooMood.HUNGRY, pet(hunger = 0, happiness = 0, energy = 0).mood)
    }

    @Test
    fun `not HUNGRY at hunger exactly 20`() {
        val mood = pet(hunger = 20).mood
        assertTrue(mood != ChorebooMood.HUNGRY)
    }

    // ── priority 2: TIRED (energy < 20) ─────────────────────────────────

    @Test
    fun `TIRED when energy below 20 and hunger at least 20`() {
        assertEquals(ChorebooMood.TIRED, pet(hunger = 20, energy = 19).mood)
    }

    @Test
    fun `not TIRED at energy exactly 20`() {
        val mood = pet(energy = 20).mood
        assertTrue(mood != ChorebooMood.TIRED)
    }

    @Test
    fun `HUNGRY beats TIRED when both below 20`() {
        assertEquals(ChorebooMood.HUNGRY, pet(hunger = 10, energy = 10).mood)
    }

    // ── SAD (overallMood < 30) ──────────────────────────────────────────

    @Test
    fun `SAD when overallMood below 30`() {
        assertEquals(ChorebooMood.SAD, pet(hunger = 20, happiness = 20, energy = 20).mood)
    }

    @Test
    fun `not SAD at overallMood exactly 30`() {
        assertEquals(ChorebooMood.IDLE, pet(hunger = 30, happiness = 30, energy = 30).mood)
    }

    // ── IDLE (overallMood 30..49) ───────────────────────────────────────

    @Test
    fun `IDLE at overallMood 49`() {
        assertEquals(ChorebooMood.IDLE, pet(hunger = 49, happiness = 49, energy = 49).mood)
    }

    // ── CONTENT (overallMood 50..69) ────────────────────────────────────

    @Test
    fun `CONTENT at overallMood 50`() {
        assertEquals(ChorebooMood.CONTENT, pet(hunger = 50, happiness = 50, energy = 50).mood)
    }

    @Test
    fun `CONTENT at overallMood 69`() {
        assertEquals(ChorebooMood.CONTENT, pet(hunger = 69, happiness = 69, energy = 69).mood)
    }

    // ── HAPPY (overallMood >= 70) ───────────────────────────────────────

    @Test
    fun `HAPPY at overallMood 70`() {
        assertEquals(ChorebooMood.HAPPY, pet(hunger = 70, happiness = 70, energy = 70).mood)
    }

    @Test
    fun `HAPPY at max stats`() {
        assertEquals(ChorebooMood.HAPPY, pet(hunger = 100, happiness = 100, energy = 100).mood)
    }

    // ── parity with ChorebooStats ───────────────────────────────────────

    @Test
    fun `mood matches ChorebooStats mood for same inputs`() {
        val testCases = listOf(
            Triple(0, 0, 0),       // HUNGRY (hunger < 20)
            Triple(19, 100, 100),  // HUNGRY
            Triple(20, 50, 19),    // TIRED
            Triple(20, 20, 20),    // SAD (overall 20)
            Triple(40, 40, 40),    // IDLE (overall 40)
            Triple(60, 60, 60),    // CONTENT (overall 60)
            Triple(80, 80, 80),    // HAPPY (overall 80)
        )
        for ((hunger, happiness, energy) in testCases) {
            val petMood = pet(hunger = hunger, happiness = happiness, energy = energy).mood
            val statsMood = ChorebooStats(
                hunger = hunger,
                happiness = happiness,
                energy = energy,
            ).mood
            assertEquals(
                "Mismatch at hunger=$hunger, happiness=$happiness, energy=$energy",
                statsMood,
                petMood,
            )
        }
    }

    // ── JUnit4-compatible assertTrue helper (avoids ambiguous overload) ─

    private fun assertTrue(condition: Boolean) {
        org.junit.Assert.assertTrue(condition)
    }
}
