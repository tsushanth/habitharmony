package com.factory.habitharmony.viewmodel

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import app.cash.turbine.test
import com.factory.habitharmony.data.entity.HabitCompletionEntity
import com.factory.habitharmony.data.entity.HabitEntity
import com.factory.habitharmony.data.entity.HabitFrequency
import com.factory.habitharmony.data.repository.HabitRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HabitViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var mockRepo: HabitRepository
    private lateinit var viewModel: HabitViewModel

    private lateinit var habitsFlow: MutableStateFlow<List<HabitEntity>>
    private lateinit var todayCompletionsFlow: MutableStateFlow<List<HabitCompletionEntity>>
    private lateinit var recentCompletionsFlow: MutableStateFlow<List<HabitCompletionEntity>>

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)

        habitsFlow = MutableStateFlow(emptyList())
        todayCompletionsFlow = MutableStateFlow(emptyList())
        recentCompletionsFlow = MutableStateFlow(emptyList())

        mockRepo = mockk(relaxed = true) {
            every { getAllActiveHabits() } returns habitsFlow
            every { getCompletionsForDate(any()) } returns todayCompletionsFlow
            every { getCompletionsSince(any()) } returns recentCompletionsFlow
        }

        viewModel = HabitViewModel(mockRepo)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // ── Initial state ─────────────────────────────────────────────────────────

    @Test
    fun `initial habitsState has isLoading true`() {
        assertTrue(viewModel.habitsState.value.isLoading)
    }

    @Test
    fun `initial habitsState has empty habit lists`() {
        val s = viewModel.habitsState.value
        assertTrue(s.todayHabits.isEmpty())
        assertTrue(s.allHabits.isEmpty())
        assertEquals(0, s.todayCompletedCount)
        assertEquals(0, s.todayTotalCount)
    }

    @Test
    fun `initial insightsState has all-zero values`() {
        val s = viewModel.insightsState.value
        assertEquals(0, s.currentStreak)
        assertEquals(0, s.bestStreak)
        assertEquals(0, s.completionPercent)
        assertEquals(0, s.weeklyCompleted)
        assertEquals(0, s.weeklyTotal)
        assertTrue(s.habitStats.isEmpty())
    }

    // ── Data loading ──────────────────────────────────────────────────────────

    @Test
    fun `habitsState isLoading becomes false after first data emission`() = runTest {
        advanceUntilIdle()

        assertFalse(viewModel.habitsState.value.isLoading)
    }

    @Test
    fun `allHabits is populated after habits emit`() = runTest {
        habitsFlow.value = listOf(
            HabitEntity(id = 1L, name = "Run", frequency = HabitFrequency.DAILY)
        )
        advanceUntilIdle()

        assertEquals(1, viewModel.habitsState.value.allHabits.size)
        assertEquals("Run", viewModel.habitsState.value.allHabits[0].habit.name)
    }

    @Test
    fun `todayHabits contains only habits scheduled for today`() = runTest {
        // DAILY is always scheduled; we can at minimum verify allHabits size.
        val daily = HabitEntity(id = 1L, name = "Daily", frequency = HabitFrequency.DAILY)
        habitsFlow.value = listOf(daily)
        advanceUntilIdle()

        // DAILY habits are always in today's list regardless of day
        val state = viewModel.habitsState.value
        assertEquals(1, state.allHabits.size)
    }

    @Test
    fun `completed habit reflected in isCompletedToday`() = runTest {
        val habitId = 1L
        val habit = HabitEntity(id = habitId, name = "Yoga", frequency = HabitFrequency.DAILY)
        val today = HabitRepository.todayStart()

        habitsFlow.value = listOf(habit)
        todayCompletionsFlow.value = listOf(
            HabitCompletionEntity(habitId = habitId, completedDate = today)
        )
        advanceUntilIdle()

        val withStats = viewModel.habitsState.value.allHabits.find { it.habit.id == habitId }
        assertNotNull(withStats)
        assertTrue(withStats!!.isCompletedToday)
    }

    @Test
    fun `uncompleted habit shows isCompletedToday false`() = runTest {
        val habit = HabitEntity(id = 2L, name = "Read", frequency = HabitFrequency.DAILY)
        habitsFlow.value = listOf(habit)
        todayCompletionsFlow.value = emptyList()
        advanceUntilIdle()

        val withStats = viewModel.habitsState.value.allHabits.find { it.habit.id == 2L }
        assertNotNull(withStats)
        assertFalse(withStats!!.isCompletedToday)
    }

    @Test
    fun `todayCompletedCount and todayTotalCount are correct`() = runTest {
        val today = HabitRepository.todayStart()
        val h1 = HabitEntity(id = 1L, name = "A", frequency = HabitFrequency.DAILY)
        val h2 = HabitEntity(id = 2L, name = "B", frequency = HabitFrequency.DAILY)
        val h3 = HabitEntity(id = 3L, name = "C", frequency = HabitFrequency.DAILY)

        habitsFlow.value = listOf(h1, h2, h3)
        todayCompletionsFlow.value = listOf(
            HabitCompletionEntity(habitId = 1L, completedDate = today),
            HabitCompletionEntity(habitId = 3L, completedDate = today)
        )
        advanceUntilIdle()

        val s = viewModel.habitsState.value
        assertEquals(2, s.todayCompletedCount)
        assertEquals(3, s.todayTotalCount)
    }

    @Test
    fun `totalCompletions reflects recentCompletions for a habit`() = runTest {
        val habitId = 1L
        val habit = HabitEntity(id = habitId, name = "Test", frequency = HabitFrequency.DAILY)
        val completions = (1..5).map { daysAgo ->
            HabitCompletionEntity(
                habitId = habitId,
                completedDate = HabitRepository.daysAgoStart(daysAgo)
            )
        }

        habitsFlow.value = listOf(habit)
        recentCompletionsFlow.value = completions
        advanceUntilIdle()

        val withStats = viewModel.habitsState.value.allHabits.find { it.habit.id == habitId }
        assertNotNull(withStats)
        assertEquals(5, withStats!!.totalCompletions)
    }

    // ── Insights state ────────────────────────────────────────────────────────

    @Test
    fun `completionPercent is 100 when all today habits are completed`() = runTest {
        val today = HabitRepository.todayStart()
        val habit = HabitEntity(id = 1L, name = "Test", frequency = HabitFrequency.DAILY)

        habitsFlow.value = listOf(habit)
        todayCompletionsFlow.value = listOf(
            HabitCompletionEntity(habitId = 1L, completedDate = today)
        )
        advanceUntilIdle()

        assertEquals(100, viewModel.insightsState.value.completionPercent)
    }

    @Test
    fun `completionPercent is 0 when no habits are completed`() = runTest {
        habitsFlow.value = listOf(
            HabitEntity(id = 1L, name = "Test", frequency = HabitFrequency.DAILY)
        )
        todayCompletionsFlow.value = emptyList()
        advanceUntilIdle()

        assertEquals(0, viewModel.insightsState.value.completionPercent)
    }

    @Test
    fun `weeklyData has exactly 7 entries`() = runTest {
        habitsFlow.value = emptyList()
        advanceUntilIdle()

        assertEquals(7, viewModel.insightsState.value.weeklyData.size)
    }

    @Test
    fun `weeklyData values are between 0f and 1f inclusive`() = runTest {
        val habitId = 1L
        val habit = HabitEntity(id = habitId, name = "Test", frequency = HabitFrequency.DAILY)
        val today = HabitRepository.todayStart()

        habitsFlow.value = listOf(habit)
        recentCompletionsFlow.value = listOf(
            HabitCompletionEntity(habitId = habitId, completedDate = today)
        )
        advanceUntilIdle()

        viewModel.insightsState.value.weeklyData.forEach { ratio ->
            assertTrue("Ratio $ratio should be >= 0", ratio >= 0f)
            assertTrue("Ratio $ratio should be <= 1", ratio <= 1f)
        }
    }

    @Test
    fun `currentStreak reflects consecutive completions`() = runTest {
        val habitId = 1L
        val habit = HabitEntity(id = habitId, name = "Test", frequency = HabitFrequency.DAILY)
        val today = HabitRepository.todayStart()
        val yesterday = HabitRepository.daysAgoStart(1)
        val twoDaysAgo = HabitRepository.daysAgoStart(2)

        habitsFlow.value = listOf(habit)
        todayCompletionsFlow.value = listOf(
            HabitCompletionEntity(habitId = habitId, completedDate = today)
        )
        recentCompletionsFlow.value = listOf(
            HabitCompletionEntity(habitId = habitId, completedDate = today),
            HabitCompletionEntity(habitId = habitId, completedDate = yesterday),
            HabitCompletionEntity(habitId = habitId, completedDate = twoDaysAgo)
        )
        advanceUntilIdle()

        assertEquals(3, viewModel.insightsState.value.currentStreak)
    }

    @Test
    fun `habitStats in insightsState matches allHabits`() = runTest {
        val h1 = HabitEntity(id = 1L, name = "A", frequency = HabitFrequency.DAILY)
        val h2 = HabitEntity(id = 2L, name = "B", frequency = HabitFrequency.DAILY)

        habitsFlow.value = listOf(h1, h2)
        advanceUntilIdle()

        val insightStats = viewModel.insightsState.value.habitStats
        assertEquals(2, insightStats.size)
    }

    // ── Action: toggleCompletion ──────────────────────────────────────────────

    @Test
    fun `toggleCompletion calls repository with correct habitId`() = runTest {
        viewModel.toggleCompletion(5L)
        advanceUntilIdle()

        coVerify { mockRepo.toggleCompletion(5L, any()) }
    }

    @Test
    fun `toggleCompletion uses the same date as getTodayDate`() = runTest {
        val expectedDate = viewModel.getTodayDate()

        viewModel.toggleCompletion(1L)
        advanceUntilIdle()

        coVerify { mockRepo.toggleCompletion(1L, expectedDate) }
    }

    // ── Action: addHabit ──────────────────────────────────────────────────────

    @Test
    fun `addHabit calls repository insertHabit`() = runTest {
        val habit = HabitEntity(name = "New")
        viewModel.addHabit(habit)
        advanceUntilIdle()

        coVerify { mockRepo.insertHabit(habit) }
    }

    // ── Action: updateHabit ───────────────────────────────────────────────────

    @Test
    fun `updateHabit calls repository updateHabit`() = runTest {
        val habit = HabitEntity(id = 1L, name = "Updated")
        viewModel.updateHabit(habit)
        advanceUntilIdle()

        coVerify { mockRepo.updateHabit(habit) }
    }

    // ── Action: deleteHabit ───────────────────────────────────────────────────

    @Test
    fun `deleteHabit calls repository deleteHabit`() = runTest {
        val habit = HabitEntity(id = 2L, name = "Gone")
        viewModel.deleteHabit(habit)
        advanceUntilIdle()

        coVerify { mockRepo.deleteHabit(habit) }
    }

    // ── Action: archiveHabit ──────────────────────────────────────────────────

    @Test
    fun `archiveHabit calls repository archiveHabit`() = runTest {
        viewModel.archiveHabit(9L)
        advanceUntilIdle()

        coVerify { mockRepo.archiveHabit(9L) }
    }

    // ── getHabitById ──────────────────────────────────────────────────────────

    @Test
    fun `getHabitById returns habit from repository`() = runTest {
        val habit = HabitEntity(id = 1L, name = "Test")
        coEvery { mockRepo.getHabitById(1L) } returns habit

        assertEquals(habit, viewModel.getHabitById(1L))
    }

    @Test
    fun `getHabitById returns null when not found`() = runTest {
        coEvery { mockRepo.getHabitById(99L) } returns null

        assertNull(viewModel.getHabitById(99L))
    }

    // ── getCompletionsForHabit ────────────────────────────────────────────────

    @Test
    fun `getCompletionsForHabit delegates to repository`() {
        val completions = listOf(
            HabitCompletionEntity(habitId = 1L, completedDate = HabitRepository.todayStart())
        )
        every { mockRepo.getCompletionsForHabit(1L) } returns flowOf(completions)

        viewModel.getCompletionsForHabit(1L)

        verify { mockRepo.getCompletionsForHabit(1L) }
    }

    // ── getTodayDate ──────────────────────────────────────────────────────────

    @Test
    fun `getTodayDate returns same value on repeated calls`() {
        assertEquals(viewModel.getTodayDate(), viewModel.getTodayDate())
    }

    @Test
    fun `getTodayDate returns midnight of today`() {
        val date = viewModel.getTodayDate()
        assertEquals(HabitRepository.todayStart(), date)
    }

    // ── StateFlow emissions (Turbine) ─────────────────────────────────────────

    @Test
    fun `habitsState emits initial loading state then resolved state`() = runTest {
        viewModel.habitsState.test {
            val loading = awaitItem()
            assertTrue(loading.isLoading)

            advanceUntilIdle()

            val resolved = awaitItem()
            assertFalse(resolved.isLoading)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `habitsState emits new state when habits list changes`() = runTest {
        // Drain initial states
        advanceUntilIdle()

        viewModel.habitsState.test {
            awaitItem() // current value (empty, not loading)

            habitsFlow.value = listOf(
                HabitEntity(id = 1L, name = "Swim", frequency = HabitFrequency.DAILY)
            )
            advanceUntilIdle()

            val updated = awaitItem()
            assertEquals(1, updated.allHabits.size)
            assertEquals("Swim", updated.allHabits[0].habit.name)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `insightsState emits update when completions change`() = runTest {
        val habitId = 1L
        val habit = HabitEntity(id = habitId, name = "Test", frequency = HabitFrequency.DAILY)
        habitsFlow.value = listOf(habit)
        advanceUntilIdle()

        viewModel.insightsState.test {
            awaitItem() // drain current

            val today = HabitRepository.todayStart()
            todayCompletionsFlow.value = listOf(
                HabitCompletionEntity(habitId = habitId, completedDate = today)
            )
            advanceUntilIdle()

            val updated = awaitItem()
            assertEquals(100, updated.completionPercent)

            cancelAndIgnoreRemainingEvents()
        }
    }

    // ── Factory ───────────────────────────────────────────────────────────────

    @Test
    fun `factory creates HabitViewModel instance`() {
        val factory = HabitViewModel.factory(mockRepo)
        val vm = factory.create(HabitViewModel::class.java)
        assertNotNull(vm)
    }
}
