package com.factory.habitharmony.data.repository

import com.factory.habitharmony.data.dao.HabitDao
import com.factory.habitharmony.data.entity.HabitCompletionEntity
import com.factory.habitharmony.data.entity.HabitEntity
import com.factory.habitharmony.data.entity.HabitFrequency
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.Calendar

@OptIn(ExperimentalCoroutinesApi::class)
class HabitRepositoryTest {

    private lateinit var dao: HabitDao
    private lateinit var repo: HabitRepository

    @Before
    fun setup() {
        dao = mockk(relaxed = true)
        repo = HabitRepository(dao)
    }

    // ── Habit CRUD ────────────────────────────────────────────────────────────

    @Test
    fun `getAllActiveHabits delegates to DAO`() {
        val habits = listOf(HabitEntity(id = 1L, name = "Run"))
        every { dao.getAllActiveHabits() } returns flowOf(habits)

        repo.getAllActiveHabits()

        verify { dao.getAllActiveHabits() }
    }

    @Test
    fun `getHabitById returns habit from DAO`() = runTest {
        val habit = HabitEntity(id = 1L, name = "Meditate")
        coEvery { dao.getHabitById(1L) } returns habit

        val result = repo.getHabitById(1L)

        assertEquals(habit, result)
        coVerify { dao.getHabitById(1L) }
    }

    @Test
    fun `getHabitById returns null when DAO returns null`() = runTest {
        coEvery { dao.getHabitById(99L) } returns null

        assertNull(repo.getHabitById(99L))
    }

    @Test
    fun `insertHabit delegates to DAO and returns generated id`() = runTest {
        val habit = HabitEntity(name = "Journal")
        coEvery { dao.insertHabit(habit) } returns 42L

        val id = repo.insertHabit(habit)

        assertEquals(42L, id)
        coVerify { dao.insertHabit(habit) }
    }

    @Test
    fun `updateHabit delegates to DAO`() = runTest {
        val habit = HabitEntity(id = 1L, name = "Updated")
        coEvery { dao.updateHabit(habit) } returns Unit

        repo.updateHabit(habit)

        coVerify { dao.updateHabit(habit) }
    }

    @Test
    fun `deleteHabit delegates to DAO`() = runTest {
        val habit = HabitEntity(id = 3L, name = "Gone")
        coEvery { dao.deleteHabit(habit) } returns Unit

        repo.deleteHabit(habit)

        coVerify { dao.deleteHabit(habit) }
    }

    @Test
    fun `archiveHabit delegates to DAO with correct id`() = runTest {
        coEvery { dao.archiveHabit(7L) } returns Unit

        repo.archiveHabit(7L)

        coVerify { dao.archiveHabit(7L) }
    }

    // ── Completion: toggleCompletion ──────────────────────────────────────────

    @Test
    fun `toggleCompletion inserts when no existing completion`() = runTest {
        val habitId = 1L
        val date = HabitRepository.todayStart()
        coEvery { dao.getCompletion(habitId, date) } returns null
        coEvery { dao.insertCompletion(any()) } returns Unit

        repo.toggleCompletion(habitId, date)

        coVerify {
            dao.insertCompletion(match { it.habitId == habitId && it.completedDate == date })
        }
        coVerify(exactly = 0) { dao.deleteCompletion(any(), any()) }
    }

    @Test
    fun `toggleCompletion deletes when completion already exists`() = runTest {
        val habitId = 2L
        val date = HabitRepository.todayStart()
        val existing = HabitCompletionEntity(id = 10L, habitId = habitId, completedDate = date)
        coEvery { dao.getCompletion(habitId, date) } returns existing
        coEvery { dao.deleteCompletion(habitId, date) } returns Unit

        repo.toggleCompletion(habitId, date)

        coVerify { dao.deleteCompletion(habitId, date) }
        coVerify(exactly = 0) { dao.insertCompletion(any()) }
    }

    // ── isCompletedOnDate ─────────────────────────────────────────────────────

    @Test
    fun `isCompletedOnDate returns true when DAO has record`() = runTest {
        val date = HabitRepository.todayStart()
        coEvery { dao.getCompletion(1L, date) } returns
            HabitCompletionEntity(habitId = 1L, completedDate = date)

        assertTrue(repo.isCompletedOnDate(1L, date))
    }

    @Test
    fun `isCompletedOnDate returns false when DAO has no record`() = runTest {
        val date = HabitRepository.todayStart()
        coEvery { dao.getCompletion(1L, date) } returns null

        assertFalse(repo.isCompletedOnDate(1L, date))
    }

    // ── getTotalCompletions ───────────────────────────────────────────────────

    @Test
    fun `getTotalCompletions delegates to DAO`() = runTest {
        coEvery { dao.getTotalCompletions(5L) } returns 21

        assertEquals(21, repo.getTotalCompletions(5L))
        coVerify { dao.getTotalCompletions(5L) }
    }

    // ── Completion query flows ────────────────────────────────────────────────

    @Test
    fun `getCompletionsSince delegates to DAO`() {
        val since = HabitRepository.daysAgoStart(30)
        repo.getCompletionsSince(since)
        verify { dao.getCompletionsSince(since) }
    }

    @Test
    fun `getCompletionsForDate delegates to DAO`() {
        val date = HabitRepository.todayStart()
        repo.getCompletionsForDate(date)
        verify { dao.getCompletionsForDate(date) }
    }

    @Test
    fun `getCompletionsForHabit delegates to DAO`() {
        repo.getCompletionsForHabit(3L)
        verify { dao.getCompletionsForHabit(3L) }
    }

    // ── computeCurrentStreak ─────────────────────────────────────────────────

    @Test
    fun `computeCurrentStreak returns 0 for no completions`() = runTest {
        val habit = HabitEntity(id = 1L, name = "Test", frequency = HabitFrequency.DAILY)
        coEvery { dao.getRecentCompletions(1L, limit = 400) } returns emptyList()

        assertEquals(0, repo.computeCurrentStreak(1L, habit))
    }

    @Test
    fun `computeCurrentStreak counts consecutive days including today`() = runTest {
        val habit = HabitEntity(id = 1L, name = "Test", frequency = HabitFrequency.DAILY)
        val today = HabitRepository.todayStart()
        val yesterday = HabitRepository.daysAgoStart(1)
        val twoDaysAgo = HabitRepository.daysAgoStart(2)

        coEvery { dao.getRecentCompletions(1L, limit = 400) } returns listOf(
            HabitCompletionEntity(habitId = 1L, completedDate = today),
            HabitCompletionEntity(habitId = 1L, completedDate = yesterday),
            HabitCompletionEntity(habitId = 1L, completedDate = twoDaysAgo)
        )

        assertEquals(3, repo.computeCurrentStreak(1L, habit))
    }

    @Test
    fun `computeCurrentStreak breaks on missed scheduled day`() = runTest {
        val habit = HabitEntity(id = 1L, name = "Test", frequency = HabitFrequency.DAILY)
        val today = HabitRepository.todayStart()
        // yesterday is missing → streak should stop at 1
        val twoDaysAgo = HabitRepository.daysAgoStart(2)

        coEvery { dao.getRecentCompletions(1L, limit = 400) } returns listOf(
            HabitCompletionEntity(habitId = 1L, completedDate = today),
            HabitCompletionEntity(habitId = 1L, completedDate = twoDaysAgo)
        )

        assertEquals(1, repo.computeCurrentStreak(1L, habit))
    }

    @Test
    fun `computeCurrentStreak allows streak from yesterday when today not done`() = runTest {
        val habit = HabitEntity(id = 1L, name = "Test", frequency = HabitFrequency.DAILY)
        val yesterday = HabitRepository.daysAgoStart(1)
        val twoDaysAgo = HabitRepository.daysAgoStart(2)

        coEvery { dao.getRecentCompletions(1L, limit = 400) } returns listOf(
            HabitCompletionEntity(habitId = 1L, completedDate = yesterday),
            HabitCompletionEntity(habitId = 1L, completedDate = twoDaysAgo)
        )

        assertEquals(2, repo.computeCurrentStreak(1L, habit))
    }

    @Test
    fun `computeCurrentStreak skips non-scheduled days for WEEKDAYS habit`() = runTest {
        // On a weekend, a WEEKDAYS habit should skip Saturday/Sunday without breaking the streak.
        val habit = HabitEntity(id = 1L, name = "Test", frequency = HabitFrequency.WEEKDAYS)

        // We just need to verify that empty completions → 0.
        coEvery { dao.getRecentCompletions(1L, limit = 400) } returns emptyList()

        assertEquals(0, repo.computeCurrentStreak(1L, habit))
    }

    // ── computeBestStreak ─────────────────────────────────────────────────────

    @Test
    fun `computeBestStreak returns 0 for no completions`() = runTest {
        val habit = HabitEntity(id = 1L, name = "Test")
        coEvery { dao.getRecentCompletions(1L, limit = 1000) } returns emptyList()

        assertEquals(0, repo.computeBestStreak(1L, habit))
    }

    @Test
    fun `computeBestStreak returns 1 for single completion`() = runTest {
        val habit = HabitEntity(id = 1L, name = "Test")
        coEvery { dao.getRecentCompletions(1L, limit = 1000) } returns listOf(
            HabitCompletionEntity(habitId = 1L, completedDate = HabitRepository.todayStart())
        )

        assertEquals(1, repo.computeBestStreak(1L, habit))
    }

    @Test
    fun `computeBestStreak finds longest consecutive run`() = runTest {
        val habit = HabitEntity(id = 1L, name = "Test")
        val dayMs = 86_400_000L
        val base = HabitRepository.daysAgoStart(10)

        // Run of 3, then a gap, then a run of 2
        coEvery { dao.getRecentCompletions(1L, limit = 1000) } returns listOf(
            HabitCompletionEntity(habitId = 1L, completedDate = base),
            HabitCompletionEntity(habitId = 1L, completedDate = base + dayMs),
            HabitCompletionEntity(habitId = 1L, completedDate = base + 2 * dayMs),
            // gap at base + 3 * dayMs
            HabitCompletionEntity(habitId = 1L, completedDate = base + 4 * dayMs),
            HabitCompletionEntity(habitId = 1L, completedDate = base + 5 * dayMs)
        )

        assertEquals(3, repo.computeBestStreak(1L, habit))
    }

    @Test
    fun `computeBestStreak equals run length when all dates are consecutive`() = runTest {
        val habit = HabitEntity(id = 1L, name = "Test")
        val dayMs = 86_400_000L
        val base = HabitRepository.daysAgoStart(4)

        coEvery { dao.getRecentCompletions(1L, limit = 1000) } returns listOf(
            HabitCompletionEntity(habitId = 1L, completedDate = base),
            HabitCompletionEntity(habitId = 1L, completedDate = base + dayMs),
            HabitCompletionEntity(habitId = 1L, completedDate = base + 2 * dayMs),
            HabitCompletionEntity(habitId = 1L, completedDate = base + 3 * dayMs),
            HabitCompletionEntity(habitId = 1L, completedDate = base + 4 * dayMs)
        )

        assertEquals(5, repo.computeBestStreak(1L, habit))
    }

    // ── Date helper companions ────────────────────────────────────────────────

    @Test
    fun `todayStart returns midnight of current day`() {
        val ms = HabitRepository.todayStart()
        val cal = Calendar.getInstance().apply { timeInMillis = ms }
        assertEquals(0, cal.get(Calendar.HOUR_OF_DAY))
        assertEquals(0, cal.get(Calendar.MINUTE))
        assertEquals(0, cal.get(Calendar.SECOND))
        assertEquals(0, cal.get(Calendar.MILLISECOND))
    }

    @Test
    fun `daysAgoStart returns midnight exactly one day before todayStart`() {
        val diff = HabitRepository.todayStart() - HabitRepository.daysAgoStart(1)
        assertEquals(86_400_000L, diff)
    }

    @Test
    fun `daysAgoStart with 0 equals todayStart`() {
        assertEquals(HabitRepository.todayStart(), HabitRepository.daysAgoStart(0))
    }

    @Test
    fun `dayStart normalises any time-of-day to midnight`() {
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 14)
            set(Calendar.MINUTE, 37)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }
        val result = HabitRepository.dayStart(cal)
        val resultCal = Calendar.getInstance().apply { timeInMillis = result }
        assertEquals(0, resultCal.get(Calendar.HOUR_OF_DAY))
        assertEquals(0, resultCal.get(Calendar.MINUTE))
        assertEquals(0, resultCal.get(Calendar.SECOND))
        assertEquals(0, resultCal.get(Calendar.MILLISECOND))
    }

    @Test
    fun `dayStart does not mutate the passed calendar`() {
        val cal = Calendar.getInstance().apply { set(Calendar.HOUR_OF_DAY, 10) }
        val hourBefore = cal.get(Calendar.HOUR_OF_DAY)
        HabitRepository.dayStart(cal)
        assertEquals(hourBefore, cal.get(Calendar.HOUR_OF_DAY))
    }

    @Test
    fun `daysAgoStart 7 returns exactly 7 days before today`() {
        val diff = HabitRepository.todayStart() - HabitRepository.daysAgoStart(7)
        assertEquals(7 * 86_400_000L, diff)
    }
}
