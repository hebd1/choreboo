package com.choreboo.app.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

/**
 * Tests for [Habit.isScheduledForToday].
 *
 * The method supports two scheduling modes:
 *   1. Weekly day-of-week selectors: 3-letter codes like "MON", "TUE", etc.
 *   2. Monthly day-of-month selectors: "D1", "D15", "D31", etc.
 *
 * Both can coexist — weekly matches return early before monthly is checked.
 * An empty [Habit.customDays] list returns false (no schedule = not scheduled).
 *
 * All tests use a fixed reference date: 2026-01-15 (Thursday, day 15, January has 31 days)
 * to ensure deterministic results regardless of when the test suite is run.
 */
class HabitTest {

    // ── helpers ──────────────────────────────────────────────────────────

    private fun habit(customDays: List<String>) = Habit(
        title = "Test",
        customDays = customDays,
    )

    /**
     * Fixed reference date: Thursday, 15 January 2026.
     * - dayOfWeek = THURSDAY → todayWeekday = "THU"
     * - dayOfMonth = 15
     * - lengthOfMonth = 31 (January)
     */
    private val testDate: LocalDate = LocalDate.of(2026, 1, 15)

    /** 3-letter uppercase abbreviation for the test date's day-of-week ("THU"). */
    private val todayWeekday: String = testDate.dayOfWeek.name.take(3).uppercase()

    /** A weekday abbreviation that is definitely NOT the test date's weekday. */
    private val notTodayWeekday: String =
        listOf("MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN")
            .first { it != todayWeekday }

    private val todayDayOfMonth: Int = testDate.dayOfMonth   // 15
    private val lastDayOfMonth: Int = testDate.lengthOfMonth() // 31

    // ── Weekly scheduling ───────────────────────────────────────────────

    @Test
    fun `weekly - today's weekday is scheduled`() {
        assertTrue(habit(listOf(todayWeekday)).isScheduledForToday(testDate))
    }

    @Test
    fun `weekly - today's weekday lowercase is scheduled (case-insensitive)`() {
        assertTrue(habit(listOf(todayWeekday.lowercase())).isScheduledForToday(testDate))
    }

    @Test
    fun `weekly - different weekday is not scheduled`() {
        assertFalse(habit(listOf(notTodayWeekday)).isScheduledForToday(testDate))
    }

    @Test
    fun `weekly - all 7 days includes today`() {
        val allDays = listOf("MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN")
        assertTrue(habit(allDays).isScheduledForToday(testDate))
    }

    @Test
    fun `weekly - multiple days containing today is scheduled`() {
        val days = listOf(todayWeekday, notTodayWeekday)
        assertTrue(habit(days).isScheduledForToday(testDate))
    }

    @Test
    fun `weekly - multiple days NOT containing today is not scheduled`() {
        val others = listOf("MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN")
            .filter { it != todayWeekday }
        // others has 6 entries, none of which is today
        assertFalse(habit(others).isScheduledForToday(testDate))
    }

    // ── Monthly scheduling ──────────────────────────────────────────────

    @Test
    fun `monthly - today's day-of-month is scheduled`() {
        assertTrue(habit(listOf("D$todayDayOfMonth")).isScheduledForToday(testDate))
    }

    @Test
    fun `monthly - different day-of-month is not scheduled`() {
        // testDate is day 15, so D1 is a safe "other" day
        assertFalse(habit(listOf("D1")).isScheduledForToday(testDate))
    }

    @Test
    fun `monthly - D31 matches last day of month (Jan 31)`() {
        // testDate is January 15; the last day of January is 31, so test against Jan 31
        val lastDay = LocalDate.of(2026, 1, 31)
        assertTrue("D31 should match Jan 31", habit(listOf("D31")).isScheduledForToday(lastDay))
    }

    @Test
    fun `monthly - D31 does not match mid-month day`() {
        // testDate is January 15, which is not the last day of the month
        assertFalse("D31 should not match Jan 15", habit(listOf("D31")).isScheduledForToday(testDate))
    }

    @Test
    fun `monthly - D31 matches last day of Feb (short month)`() {
        // February 2026 has 28 days; D31 should fire on Feb 28 (last day)
        val feb28 = LocalDate.of(2026, 2, 28)
        assertTrue("D31 should match Feb 28 (last day of Feb)", habit(listOf("D31")).isScheduledForToday(feb28))
    }

    @Test
    fun `monthly - multiple monthly days containing today is scheduled`() {
        assertTrue(habit(listOf("D1", "D$todayDayOfMonth")).isScheduledForToday(testDate))
    }

    @Test
    fun `monthly - malformed day selector is skipped`() {
        // "Dfoo" can't be parsed to int — should not crash, should return false
        assertFalse(habit(listOf("Dfoo")).isScheduledForToday(testDate))
    }

    // ── Mixed weekly + monthly ──────────────────────────────────────────

    @Test
    fun `mixed - weekly match returns true even if monthly wouldn't match`() {
        // Weekly match short-circuits before monthly check; D1 is not today (15)
        assertTrue(
            habit(listOf(todayWeekday, "D1")).isScheduledForToday(testDate)
        )
    }

    @Test
    fun `mixed - monthly match returns true even if weekly doesn't match`() {
        assertTrue(
            habit(listOf(notTodayWeekday, "D$todayDayOfMonth")).isScheduledForToday(testDate)
        )
    }

    // ── Edge cases ──────────────────────────────────────────────────────

    @Test
    fun `empty customDays returns false`() {
        assertFalse(habit(emptyList()).isScheduledForToday(testDate))
    }

    @Test
    fun `non-day non-monthly strings are ignored`() {
        // "HELLO" is 5 chars so not a weekday code; no "D" prefix so not monthly
        assertFalse(habit(listOf("HELLO", "12", "")).isScheduledForToday(testDate))
    }

    @Test
    fun `blank strings in customDays are ignored`() {
        assertFalse(habit(listOf("", "  ")).isScheduledForToday(testDate))
    }

    @Test
    fun `next scheduled date returns next weekly match`() {
        assertEquals(
            LocalDate.of(2026, 1, 19),
            habit(listOf("MON")).nextScheduledDate(testDate),
        )
    }

    @Test
    fun `next scheduled date wraps to next week for weekly schedules`() {
        val monday = LocalDate.of(2026, 1, 19)
        assertEquals(
            LocalDate.of(2026, 1, 26),
            habit(listOf("MON")).nextScheduledDate(monday),
        )
    }

    @Test
    fun `next scheduled date returns next monthly match in same month`() {
        assertEquals(
            LocalDate.of(2026, 1, 31),
            habit(listOf("D31")).nextScheduledDate(testDate),
        )
    }

    @Test
    fun `next scheduled date returns normalized short month date`() {
        val januaryLastDay = LocalDate.of(2026, 1, 31)
        assertEquals(
            LocalDate.of(2026, 2, 28),
            habit(listOf("D31")).nextScheduledDate(januaryLastDay),
        )
    }

    @Test
    fun `next scheduled date returns earliest mixed schedule match`() {
        assertEquals(
            LocalDate.of(2026, 1, 16),
            habit(listOf("MON", "D16")).nextScheduledDate(testDate),
        )
    }

    @Test
    fun `next scheduled date returns null for invalid schedule`() {
        assertNull(habit(listOf("Dfoo", "HELLO")).nextScheduledDate(testDate))
    }

    @Test
    fun `timeUntilNextReminderMinutes returns null when reminder disabled`() {
        val now = LocalDateTime.of(2026, 1, 15, 8, 0)
        val reminderHabit = Habit(
            title = "Test",
            customDays = listOf("THU"),
            reminderEnabled = false,
            reminderTime = LocalTime.of(9, 0),
        )

        assertNull(reminderHabit.timeUntilNextReminderMinutes(now))
    }

    @Test
    fun `timeUntilNextReminderMinutes returns minutes until later today`() {
        val now = LocalDateTime.of(2026, 1, 15, 8, 0)
        val reminderHabit = Habit(
            title = "Test",
            customDays = listOf("THU"),
            reminderEnabled = true,
            reminderTime = LocalTime.of(9, 30),
        )

        assertEquals(90L, reminderHabit.timeUntilNextReminderMinutes(now))
    }

    @Test
    fun `timeUntilNextReminderMinutes skips past reminder today and returns next scheduled day`() {
        val now = LocalDateTime.of(2026, 1, 15, 10, 0)
        val reminderHabit = Habit(
            title = "Test",
            customDays = listOf("THU", "SAT"),
            reminderEnabled = true,
            reminderTime = LocalTime.of(9, 0),
        )

        assertEquals(47L * 60L, reminderHabit.timeUntilNextReminderMinutes(now))
    }

    @Test
    fun `timeUntilNextReminderMinutes normalizes D31 to short month end`() {
        val now = LocalDateTime.of(2026, 2, 10, 8, 0)
        val reminderHabit = Habit(
            title = "Test",
            customDays = listOf("D31"),
            reminderEnabled = true,
            reminderTime = LocalTime.of(18, 45),
        )

        val expectedMinutes = java.time.Duration.between(
            now,
            LocalDateTime.of(2026, 2, 28, 18, 45),
        ).toMinutes()
        assertEquals(expectedMinutes, reminderHabit.timeUntilNextReminderMinutes(now))
    }
}
