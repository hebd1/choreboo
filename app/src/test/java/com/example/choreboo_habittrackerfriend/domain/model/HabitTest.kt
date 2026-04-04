package com.example.choreboo_habittrackerfriend.domain.model

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/**
 * Tests for [Habit.isScheduledForToday].
 *
 * The method supports two scheduling modes:
 *   1. Weekly day-of-week selectors: 3-letter codes like "MON", "TUE", etc.
 *   2. Monthly day-of-month selectors: "D1", "D15", "D31", etc.
 *
 * Both can coexist — weekly matches return early before monthly is checked.
 * An empty [Habit.customDays] list returns false (no schedule = not scheduled).
 */
class HabitTest {

    // ── helpers ──────────────────────────────────────────────────────────

    private fun habit(customDays: List<String>) = Habit(
        title = "Test",
        customDays = customDays,
    )

    /** 3-letter uppercase abbreviation for today's day-of-week. */
    private val todayWeekday: String =
        LocalDate.now().dayOfWeek.name.take(3).uppercase()

    /** A weekday abbreviation that is definitely NOT today. */
    private val notTodayWeekday: String =
        listOf("MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN")
            .first { it != todayWeekday }

    private val todayDayOfMonth: Int = LocalDate.now().dayOfMonth
    private val lastDayOfMonth: Int = LocalDate.now().lengthOfMonth()

    // ── Weekly scheduling ───────────────────────────────────────────────

    @Test
    fun `weekly - today's weekday is scheduled`() {
        assertTrue(habit(listOf(todayWeekday)).isScheduledForToday())
    }

    @Test
    fun `weekly - today's weekday lowercase is scheduled (case-insensitive)`() {
        assertTrue(habit(listOf(todayWeekday.lowercase())).isScheduledForToday())
    }

    @Test
    fun `weekly - different weekday is not scheduled`() {
        assertFalse(habit(listOf(notTodayWeekday)).isScheduledForToday())
    }

    @Test
    fun `weekly - all 7 days includes today`() {
        val allDays = listOf("MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN")
        assertTrue(habit(allDays).isScheduledForToday())
    }

    @Test
    fun `weekly - multiple days containing today is scheduled`() {
        val days = listOf(todayWeekday, notTodayWeekday)
        assertTrue(habit(days).isScheduledForToday())
    }

    @Test
    fun `weekly - multiple days NOT containing today is not scheduled`() {
        val others = listOf("MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN")
            .filter { it != todayWeekday }
        // others has 6 entries, none of which is today
        assertFalse(habit(others).isScheduledForToday())
    }

    // ── Monthly scheduling ──────────────────────────────────────────────

    @Test
    fun `monthly - today's day-of-month is scheduled`() {
        assertTrue(habit(listOf("D$todayDayOfMonth")).isScheduledForToday())
    }

    @Test
    fun `monthly - different day-of-month is not scheduled`() {
        val otherDay = if (todayDayOfMonth == 1) 2 else 1
        assertFalse(habit(listOf("D$otherDay")).isScheduledForToday())
    }

    @Test
    fun `monthly - D31 matches last day of month even if month has fewer than 31 days`() {
        // This test is only meaningful on the last day of the month.
        // On non-last days it correctly returns false (D31 != todayDayOfMonth).
        val result = habit(listOf("D31")).isScheduledForToday()
        if (todayDayOfMonth == lastDayOfMonth) {
            assertTrue("D31 should match the last day of the month", result)
        } else if (todayDayOfMonth != 31) {
            assertFalse("D31 should not match day $todayDayOfMonth", result)
        }
    }

    @Test
    fun `monthly - multiple monthly days containing today is scheduled`() {
        val otherDay = if (todayDayOfMonth == 15) 1 else 15
        assertTrue(habit(listOf("D$otherDay", "D$todayDayOfMonth")).isScheduledForToday())
    }

    @Test
    fun `monthly - malformed day selector is skipped`() {
        // "Dfoo" can't be parsed to int — should not crash, should return false
        assertFalse(habit(listOf("Dfoo")).isScheduledForToday())
    }

    // ── Mixed weekly + monthly ──────────────────────────────────────────

    @Test
    fun `mixed - weekly match returns true even if monthly wouldn't match`() {
        // Weekly match short-circuits before monthly check
        val otherDay = if (todayDayOfMonth == 1) 2 else 1
        assertTrue(
            habit(listOf(todayWeekday, "D$otherDay")).isScheduledForToday()
        )
    }

    @Test
    fun `mixed - monthly match returns true even if weekly doesn't match`() {
        assertTrue(
            habit(listOf(notTodayWeekday, "D$todayDayOfMonth")).isScheduledForToday()
        )
    }

    // ── Edge cases ──────────────────────────────────────────────────────

    @Test
    fun `empty customDays returns false`() {
        assertFalse(habit(emptyList()).isScheduledForToday())
    }

    @Test
    fun `non-day non-monthly strings are ignored`() {
        // "HELLO" is 5 chars so not a weekday code; no "D" prefix so not monthly
        assertFalse(habit(listOf("HELLO", "12", "")).isScheduledForToday())
    }

    @Test
    fun `blank strings in customDays are ignored`() {
        assertFalse(habit(listOf("", "  ")).isScheduledForToday())
    }
}
