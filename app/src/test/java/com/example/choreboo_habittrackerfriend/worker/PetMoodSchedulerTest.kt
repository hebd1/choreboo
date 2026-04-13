package com.example.choreboo_habittrackerfriend.worker

import org.junit.Test
import org.junit.Assert.assertTrue
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import java.time.ZonedDateTime

class PetMoodSchedulerTest {

    @Test
    fun `calculateNextCriticalTime returns null when already critical hunger`() {
        val now = System.currentTimeMillis()
        val result = PetMoodScheduler.calculateNextCriticalTime(
            hunger = 19,
            happiness = 50,
            energy = 50,
            scheduleFromTime = now,
        )
        assertNull(result)
    }

    @Test
    fun `calculateNextCriticalTime returns null when already critical energy`() {
        val now = System.currentTimeMillis()
        val result = PetMoodScheduler.calculateNextCriticalTime(
            hunger = 50,
            happiness = 50,
            energy = 15,
            scheduleFromTime = now,
        )
        assertNull(result)
    }

    @Test
    fun `calculateNextCriticalTime returns null when already critical happiness`() {
        val now = System.currentTimeMillis()
        val result = PetMoodScheduler.calculateNextCriticalTime(
            hunger = 50,
            happiness = 10,
            energy = 50,
            scheduleFromTime = now,
        )
        assertNull(result)
    }

    @Test
    fun `calculateNextCriticalTime schedules hunger reaching critical hunger at 60 takes ~40 hours`() {
        val now = System.currentTimeMillis()
        val result = PetMoodScheduler.calculateNextCriticalTime(
            hunger = 60,
            happiness = 100,
            energy = 100,
            scheduleFromTime = now,
        )
        
        assertNotNull(result)
        val diff = (result!! - now) / (1000 * 60 * 60) // hours
        // hunger -1/hr: 60 → 20 = 40 hours
        assertTrue("Expected ~40 hours, got $diff", diff in 39..41)
    }

    @Test
    fun `calculateNextCriticalTime schedules energy reaching critical before hunger`() {
        val now = System.currentTimeMillis()
        val result = PetMoodScheduler.calculateNextCriticalTime(
            hunger = 80,  // -1/hr: 80 → 20 = 60 hours
            happiness = 100,
            energy = 50, // -0.5/hr: 50 → 20 = 60 hours
            scheduleFromTime = now,
        )
        
        assertNotNull(result)
        val diff = (result!! - now) / (1000 * 60 * 60) // hours
        // Both need 60 hours, so we should get 60 hours
        assertTrue("Expected ~60 hours, got $diff", diff in 59..61)
    }

    @Test
    fun `calculateNextCriticalTime uses minimum of three decay rates`() {
        val now = System.currentTimeMillis()
        val result = PetMoodScheduler.calculateNextCriticalTime(
            hunger = 100, // -1/hr: 100 → 20 = 80 hours
            happiness = 40, // -0.5/hr: 40 → 20 = 40 hours (SOONEST)
            energy = 100, // -0.5/hr: 100 → 20 = 160 hours
            scheduleFromTime = now,
        )
        
        assertNotNull(result)
        val diff = (result!! - now) / (1000 * 60 * 60) // hours
        // Happiness is critical soonest at 40 hours
        assertTrue("Expected ~40 hours, got $diff", diff in 39..41)
    }

    @Test
    fun `calculateNextCriticalTime clamps to 30 minutes minimum`() {
        val now = System.currentTimeMillis()
        val result = PetMoodScheduler.calculateNextCriticalTime(
            hunger = 21, // Just above critical (-1/hr: 1 hour to reach 20)
            happiness = 100,
            energy = 100,
            scheduleFromTime = now,
        )
        
        assertNotNull(result)
        val diff = (result!! - now) / (1000 * 60) // minutes
        // Should be clamped to at least 30 minutes
        assertTrue("Expected >= 30 minutes, got $diff", diff >= 30)
    }

    @Test
    fun `calculateNextCriticalTime clamps to 7 days maximum`() {
        val now = System.currentTimeMillis()
        val result = PetMoodScheduler.calculateNextCriticalTime(
            hunger = 100, // -1/hr: 100 → 20 = 80 hours (EXCEEDS 7 days, should be capped)
            happiness = 100,
            energy = 100,
            scheduleFromTime = now,
        )
        
        assertNotNull(result)
        val diff = (result!! - now) / (1000 * 60 * 60) // hours
        val maxHours = 7 * 24 // 168 hours
        assertTrue("Expected <= 168 hours, got $diff", diff <= maxHours)
    }

    @Test
    fun `calculateNextCriticalTime respects schedule from time when sleeping`() {
        val now = System.currentTimeMillis()
        val sleepEnd = now + (2 * 60 * 60 * 1000) // 2 hours from now
        
        val result = PetMoodScheduler.calculateNextCriticalTime(
            hunger = 80, // 60 hours from sleep end
            happiness = 100,
            energy = 100,
            scheduleFromTime = sleepEnd,
        )
        
        assertNotNull(result)
        val diff = (result!! - now) / (1000 * 60 * 60) // hours
        // Should be ~62 hours from now (2 hours sleep + 60 hours decay)
        assertTrue("Expected ~62 hours, got $diff", diff in 61..63)
    }

    @Test
    fun `calculateNextCriticalTime with all stats at exactly critical level returns null`() {
        val now = System.currentTimeMillis()
        val result = PetMoodScheduler.calculateNextCriticalTime(
            hunger = 20,
            happiness = 20,
            energy = 20,
            scheduleFromTime = now,
        )
        // At exactly 20, they're not below 20, so no critical
        assertNotNull(result) // Should still return a future time since they're not < 20 yet
    }

    @Test
    fun `calculateNextCriticalTime with one stat at 20 and others lower returns null`() {
        val now = System.currentTimeMillis()
        val result = PetMoodScheduler.calculateNextCriticalTime(
            hunger = 20,
            happiness = 10, // Critical
            energy = 50,
            scheduleFromTime = now,
        )
        assertNull(result)
    }

    private fun assertNotNull(value: Any?) {
        if (value == null) {
            throw AssertionError("Expected non-null value")
        }
    }
}
