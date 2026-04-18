package com.choreboo.app.worker

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

class HabitReminderSchedulerTest {

    @Test
    fun `calculateNextTriggerTime returns later today for weekly habit`() {
        val now = zonedDateTime(2026, 1, 15, 8, 0)

        val result = HabitReminderScheduler.calculateNextTriggerTime(
            reminderTime = LocalTime.of(9, 30),
            scheduledDays = listOf("THU"),
            now = now,
        )

        assertEquals(zonedDateTime(2026, 1, 15, 9, 30), result)
    }

    @Test
    fun `calculateNextTriggerTime returns next weekly occurrence when today's time passed`() {
        val now = zonedDateTime(2026, 1, 15, 10, 0)

        val result = HabitReminderScheduler.calculateNextTriggerTime(
            reminderTime = LocalTime.of(9, 0),
            scheduledDays = listOf("THU", "SAT"),
            now = now,
        )

        assertEquals(zonedDateTime(2026, 1, 17, 9, 0), result)
    }

    @Test
    fun `calculateNextTriggerTime normalizes monthly D31 to last day of short month`() {
        val now = zonedDateTime(2026, 2, 10, 8, 0)

        val result = HabitReminderScheduler.calculateNextTriggerTime(
            reminderTime = LocalTime.of(18, 45),
            scheduledDays = listOf("D31"),
            now = now,
        )

        assertEquals(zonedDateTime(2026, 2, 28, 18, 45), result)
    }

    @Test
    fun `calculateNextTriggerTime rolls monthly habit to next month when today's time passed`() {
        val now = zonedDateTime(2026, 1, 15, 10, 0)

        val result = HabitReminderScheduler.calculateNextTriggerTime(
            reminderTime = LocalTime.of(9, 0),
            scheduledDays = listOf("D15"),
            now = now,
        )

        assertEquals(zonedDateTime(2026, 2, 15, 9, 0), result)
    }

    @Test
    fun `calculateNextTriggerTime returns null when no scheduled days configured`() {
        val now = zonedDateTime(2026, 1, 15, 10, 0)

        val result = HabitReminderScheduler.calculateNextTriggerTime(
            reminderTime = LocalTime.of(9, 0),
            scheduledDays = emptyList(),
            now = now,
        )

        assertNull(result)
    }

    private fun zonedDateTime(
        year: Int,
        month: Int,
        day: Int,
        hour: Int,
        minute: Int,
    ): ZonedDateTime {
        return ZonedDateTime.of(
            year,
            month,
            day,
            hour,
            minute,
            0,
            0,
            ZoneId.systemDefault(),
        )
    }
}
