package com.factory.habitharmony.data.entity

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class HabitEntityTest {

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun habit(
        frequency: HabitFrequency = HabitFrequency.DAILY,
        targetDays: String = "0,1,2,3,4,5,6"
    ) = HabitEntity(
        id = 1L,
        name = "Test",
        frequency = frequency,
        targetDays = targetDays,
        createdAt = 0L
    )

    // Calendar.DAY_OF_WEEK constants for readability
    private val SUN = Calendar.SUNDAY   // 1
    private val MON = Calendar.MONDAY   // 2
    private val TUE = Calendar.TUESDAY  // 3
    private val WED = Calendar.WEDNESDAY // 4
    private val THU = Calendar.THURSDAY // 5
    private val FRI = Calendar.FRIDAY   // 6
    private val SAT = Calendar.SATURDAY // 7

    // ── isScheduledForDay – DAILY ─────────────────────────────────────────────

    @Test
    fun `DAILY frequency is scheduled every day of the week`() {
        val h = habit(frequency = HabitFrequency.DAILY)
        (1..7).forEach { day ->
            assertTrue("Expected DAILY to be scheduled on day $day", h.isScheduledForDay(day))
        }
    }

    // ── isScheduledForDay – WEEKDAYS ──────────────────────────────────────────

    @Test
    fun `WEEKDAYS frequency is scheduled Monday through Friday`() {
        val h = habit(frequency = HabitFrequency.WEEKDAYS)
        listOf(MON, TUE, WED, THU, FRI).forEach { day ->
            assertTrue("Expected WEEKDAYS on day $day", h.isScheduledForDay(day))
        }
    }

    @Test
    fun `WEEKDAYS frequency is NOT scheduled on Sunday`() {
        assertFalse(habit(frequency = HabitFrequency.WEEKDAYS).isScheduledForDay(SUN))
    }

    @Test
    fun `WEEKDAYS frequency is NOT scheduled on Saturday`() {
        assertFalse(habit(frequency = HabitFrequency.WEEKDAYS).isScheduledForDay(SAT))
    }

    // ── isScheduledForDay – WEEKENDS ──────────────────────────────────────────

    @Test
    fun `WEEKENDS frequency is scheduled on Saturday and Sunday`() {
        val h = habit(frequency = HabitFrequency.WEEKENDS)
        assertTrue(h.isScheduledForDay(SUN))
        assertTrue(h.isScheduledForDay(SAT))
    }

    @Test
    fun `WEEKENDS frequency is NOT scheduled on weekdays`() {
        val h = habit(frequency = HabitFrequency.WEEKENDS)
        listOf(MON, TUE, WED, THU, FRI).forEach { day ->
            assertFalse("Expected WEEKENDS to skip day $day", h.isScheduledForDay(day))
        }
    }

    // ── isScheduledForDay – SPECIFIC_DAYS ────────────────────────────────────

    @Test
    fun `SPECIFIC_DAYS respects targetDays index list`() {
        // targetDays uses 0=Sun,1=Mon,…,6=Sat; calDayOfWeek 1=Sun,2=Mon,…,7=Sat
        val h = habit(frequency = HabitFrequency.SPECIFIC_DAYS, targetDays = "1,3,5") // Mon,Wed,Fri
        assertTrue(h.isScheduledForDay(MON))  // idx 1
        assertTrue(h.isScheduledForDay(WED))  // idx 3
        assertTrue(h.isScheduledForDay(FRI))  // idx 5
        assertFalse(h.isScheduledForDay(SUN)) // idx 0
        assertFalse(h.isScheduledForDay(TUE)) // idx 2
        assertFalse(h.isScheduledForDay(SAT)) // idx 6
    }

    @Test
    fun `SPECIFIC_DAYS with empty targetDays is never scheduled`() {
        val h = habit(frequency = HabitFrequency.SPECIFIC_DAYS, targetDays = "")
        (1..7).forEach { day ->
            assertFalse("Expected empty SPECIFIC_DAYS to skip day $day", h.isScheduledForDay(day))
        }
    }

    @Test
    fun `SPECIFIC_DAYS with Sunday index 0 is scheduled on Sunday`() {
        val h = habit(frequency = HabitFrequency.SPECIFIC_DAYS, targetDays = "0")
        assertTrue(h.isScheduledForDay(SUN))
        assertFalse(h.isScheduledForDay(MON))
    }

    // ── getTargetDaysList ─────────────────────────────────────────────────────

    @Test
    fun `getTargetDaysList parses all seven days`() {
        val h = habit(targetDays = "0,1,2,3,4,5,6")
        assertEquals(listOf(0, 1, 2, 3, 4, 5, 6), h.getTargetDaysList())
    }

    @Test
    fun `getTargetDaysList parses partial day list`() {
        val h = habit(targetDays = "1,3,5")
        assertEquals(listOf(1, 3, 5), h.getTargetDaysList())
    }

    @Test
    fun `getTargetDaysList returns empty list for empty string`() {
        assertTrue(habit(targetDays = "").getTargetDaysList().isEmpty())
    }

    @Test
    fun `getTargetDaysList ignores non-numeric entries`() {
        val h = habit(targetDays = "1,abc,3,,5")
        assertEquals(listOf(1, 3, 5), h.getTargetDaysList())
    }

    @Test
    fun `getTargetDaysList parses single day`() {
        assertEquals(listOf(0), habit(targetDays = "0").getTargetDaysList())
    }

    // ── frequencyLabel ────────────────────────────────────────────────────────

    @Test
    fun `frequencyLabel for DAILY returns Every day`() {
        assertEquals("Every day", habit(frequency = HabitFrequency.DAILY).frequencyLabel())
    }

    @Test
    fun `frequencyLabel for WEEKDAYS returns Mon – Fri`() {
        assertEquals("Mon – Fri", habit(frequency = HabitFrequency.WEEKDAYS).frequencyLabel())
    }

    @Test
    fun `frequencyLabel for WEEKENDS returns Sat and Sun`() {
        assertEquals("Sat & Sun", habit(frequency = HabitFrequency.WEEKENDS).frequencyLabel())
    }

    @Test
    fun `frequencyLabel for SPECIFIC_DAYS shows day abbreviations`() {
        val h = habit(frequency = HabitFrequency.SPECIFIC_DAYS, targetDays = "1,3,5")
        assertEquals("Mon, Wed, Fri", h.frequencyLabel())
    }

    @Test
    fun `frequencyLabel for SPECIFIC_DAYS with empty targetDays returns empty string`() {
        val h = habit(frequency = HabitFrequency.SPECIFIC_DAYS, targetDays = "")
        assertEquals("", h.frequencyLabel())
    }

    @Test
    fun `frequencyLabel for SPECIFIC_DAYS includes Sunday`() {
        val h = habit(frequency = HabitFrequency.SPECIFIC_DAYS, targetDays = "0,6")
        assertEquals("Sun, Sat", h.frequencyLabel())
    }

    // ── HabitCategory ─────────────────────────────────────────────────────────

    @Test
    fun `HabitCategory display names are correct`() {
        assertEquals("Health", HabitCategory.HEALTH.displayName)
        assertEquals("Fitness", HabitCategory.FITNESS.displayName)
        assertEquals("Learning", HabitCategory.LEARNING.displayName)
        assertEquals("Mindfulness", HabitCategory.MINDFULNESS.displayName)
        assertEquals("Productivity", HabitCategory.PRODUCTIVITY.displayName)
        assertEquals("Social", HabitCategory.SOCIAL.displayName)
        assertEquals("Nutrition", HabitCategory.NUTRITION.displayName)
        assertEquals("Sleep", HabitCategory.SLEEP.displayName)
        assertEquals("Finance", HabitCategory.FINANCE.displayName)
        assertEquals("Other", HabitCategory.OTHER.displayName)
    }

    @Test
    fun `HabitCategory emojis are set`() {
        assertEquals("❤️", HabitCategory.HEALTH.emoji)
        assertEquals("💪", HabitCategory.FITNESS.emoji)
        assertEquals("📚", HabitCategory.LEARNING.emoji)
    }

    // ── HabitFrequency ────────────────────────────────────────────────────────

    @Test
    fun `HabitFrequency display names are correct`() {
        assertEquals("Every day", HabitFrequency.DAILY.displayName)
        assertEquals("Weekdays", HabitFrequency.WEEKDAYS.displayName)
        assertEquals("Weekends", HabitFrequency.WEEKENDS.displayName)
        assertEquals("Specific days", HabitFrequency.SPECIFIC_DAYS.displayName)
    }

    // ── HabitEntity data class ────────────────────────────────────────────────

    @Test
    fun `HabitEntity default values are correct`() {
        val h = HabitEntity(name = "Test")
        assertEquals("⭐", h.emoji)
        assertEquals("", h.description)
        assertEquals(0xFF6C63FF, h.colorHex)
        assertEquals(HabitCategory.OTHER, h.category)
        assertEquals(HabitFrequency.DAILY, h.frequency)
        assertEquals("0,1,2,3,4,5,6", h.targetDays)
        assertFalse(h.isArchived)
    }

    @Test
    fun `HabitEntity copy changes only the specified field`() {
        val original = HabitEntity(id = 5L, name = "Original", createdAt = 100L)
        val copy = original.copy(name = "Modified")
        assertEquals("Modified", copy.name)
        assertEquals(5L, copy.id)
        assertEquals(100L, copy.createdAt)
    }

    @Test
    fun `HabitEntity equals and hashCode work as data class`() {
        val a = HabitEntity(id = 1L, name = "Same", createdAt = 123L)
        val b = HabitEntity(id = 1L, name = "Same", createdAt = 123L)
        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
    }

    @Test
    fun `HABIT_COLORS list has 10 entries`() {
        assertEquals(10, HABIT_COLORS.size)
    }

    @Test
    fun `EMOJI_OPTIONS list has 30 entries`() {
        assertEquals(30, EMOJI_OPTIONS.size)
    }
}
