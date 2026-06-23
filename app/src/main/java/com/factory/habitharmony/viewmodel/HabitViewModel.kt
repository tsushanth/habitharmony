package com.factory.habitharmony.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.factory.habitharmony.data.entity.HabitCompletionEntity
import com.factory.habitharmony.data.entity.HabitEntity
import com.factory.habitharmony.data.repository.HabitRepository
import com.factory.habitharmony.data.repository.HabitRepository.Companion.daysAgoStart
import com.factory.habitharmony.data.repository.HabitRepository.Companion.todayStart
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Calendar

// ── Domain model ──────────────────────────────────────────────────────────────

data class HabitWithStats(
    val habit: HabitEntity,
    val isCompletedToday: Boolean,
    val currentStreak: Int,
    val totalCompletions: Int
)

// ── UI states ─────────────────────────────────────────────────────────────────

data class HabitsUiState(
    val todayHabits: List<HabitWithStats> = emptyList(),
    val allHabits: List<HabitWithStats> = emptyList(),
    val todayCompletedCount: Int = 0,
    val todayTotalCount: Int = 0,
    val isLoading: Boolean = true
)

data class InsightsUiState(
    val currentStreak: Int = 0,   // max streak across all habits
    val bestStreak: Int = 0,      // all-time best across all habits
    val completionPercent: Int = 0,
    val weeklyCompleted: Int = 0,
    val weeklyTotal: Int = 0,
    val weeklyData: List<Float> = emptyList(), // 0=Mon … 6=Sun, ratio 0..1
    val habitStats: List<HabitWithStats> = emptyList()
)

// ── ViewModel ─────────────────────────────────────────────────────────────────

class HabitViewModel(private val repository: HabitRepository) : ViewModel() {

    private val _habitsState = MutableStateFlow(HabitsUiState())
    val habitsState: StateFlow<HabitsUiState> = _habitsState.asStateFlow()

    private val _insightsState = MutableStateFlow(InsightsUiState())
    val insightsState: StateFlow<InsightsUiState> = _insightsState.asStateFlow()

    private val today: Long = todayStart()
    private val thirtyDaysAgo: Long = daysAgoStart(30)

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            combine(
                repository.getAllActiveHabits(),
                repository.getCompletionsForDate(today),
                repository.getCompletionsSince(thirtyDaysAgo)
            ) { habits, todayCompletions, recentCompletions ->
                Triple(habits, todayCompletions, recentCompletions)
            }.collect { (habits, todayCompletions, recentCompletions) ->
                processData(habits, todayCompletions, recentCompletions)
            }
        }
    }

    private suspend fun processData(
        habits: List<HabitEntity>,
        todayCompletions: List<HabitCompletionEntity>,
        recentCompletions: List<HabitCompletionEntity>
    ) {
        val completedIds = todayCompletions.map { it.habitId }.toSet()
        val completionsByHabit = recentCompletions.groupBy { it.habitId }

        val cal = Calendar.getInstance()
        val todayDow = cal.get(Calendar.DAY_OF_WEEK)

        val statsMap = habits.associateWith { habit ->
            val habitCompletions = completionsByHabit[habit.id] ?: emptyList()
            val completedDates = habitCompletions.map { it.completedDate }.toSet()
            val streak = computeStreakFromDates(completedDates, habit)
            val total = habitCompletions.size
            HabitWithStats(
                habit = habit,
                isCompletedToday = completedIds.contains(habit.id),
                currentStreak = streak,
                totalCompletions = total
            )
        }

        val todayHabits = habits
            .filter { it.isScheduledForDay(todayDow) }
            .map { statsMap[it] ?: HabitWithStats(it, false, 0, 0) }

        val allHabitsWithStats = habits.map { statsMap[it] ?: HabitWithStats(it, false, 0, 0) }

        _habitsState.update {
            it.copy(
                todayHabits = todayHabits,
                allHabits = allHabitsWithStats,
                todayCompletedCount = todayHabits.count { h -> h.isCompletedToday },
                todayTotalCount = todayHabits.size,
                isLoading = false
            )
        }

        // Insights
        val weeklyData = buildWeeklyData(habits, recentCompletions)
        val weeklyCompleted = todayHabits.count { it.isCompletedToday }
        val weeklyTotal = todayHabits.size
        val completionPct = if (weeklyTotal > 0) (weeklyCompleted * 100) / weeklyTotal else 0
        val maxCurrent = allHabitsWithStats.maxOfOrNull { it.currentStreak } ?: 0
        val bestStreak = allHabitsWithStats.maxOfOrNull { it.currentStreak } ?: 0

        _insightsState.update {
            it.copy(
                currentStreak = maxCurrent,
                bestStreak = bestStreak,
                completionPercent = completionPct,
                weeklyCompleted = weeklyCompleted,
                weeklyTotal = weeklyTotal,
                weeklyData = weeklyData,
                habitStats = allHabitsWithStats
            )
        }
    }

    private fun computeStreakFromDates(completedDates: Set<Long>, habit: HabitEntity): Int {
        if (completedDates.isEmpty()) return 0
        val cal = Calendar.getInstance()
        val todayMs = todayStart()

        var streak = 0
        cal.timeInMillis = todayMs

        // If not completed today, start checking from yesterday
        if (!completedDates.contains(todayMs)) {
            cal.add(Calendar.DAY_OF_YEAR, -1)
        }

        repeat(100) {
            val dayMs = cal.timeInMillis
            val dayDow = cal.get(Calendar.DAY_OF_WEEK)
            val scheduled = habit.isScheduledForDay(dayDow)
            when {
                !scheduled -> { /* skip */ }
                completedDates.contains(dayMs) -> streak++
                else -> return streak
            }
            cal.add(Calendar.DAY_OF_YEAR, -1)
        }
        return streak
    }

    /**
     * Builds per-day completion ratios for the past 7 days.
     * Index 0 = 6 days ago … index 6 = today.
     */
    private fun buildWeeklyData(
        habits: List<HabitEntity>,
        completions: List<HabitCompletionEntity>
    ): List<Float> {
        val completedByDate = completions.groupBy { it.completedDate }
            .mapValues { (_, v) -> v.map { it.habitId }.toSet() }

        return (6 downTo 0).map { daysAgo ->
            val date = daysAgoStart(daysAgo)
            val cal = Calendar.getInstance().apply { timeInMillis = date }
            val dow = cal.get(Calendar.DAY_OF_WEEK)
            val scheduled = habits.filter { it.isScheduledForDay(dow) }
            if (scheduled.isEmpty()) return@map 0f
            val completedSet = completedByDate[date] ?: emptySet()
            val done = scheduled.count { it.id in completedSet }
            done.toFloat() / scheduled.size
        }
    }

    // ── Actions ───────────────────────────────────────────────────────────────

    fun toggleCompletion(habitId: Long) {
        viewModelScope.launch {
            repository.toggleCompletion(habitId, today)
        }
    }

    fun addHabit(habit: HabitEntity) {
        viewModelScope.launch {
            repository.insertHabit(habit)
        }
    }

    fun updateHabit(habit: HabitEntity) {
        viewModelScope.launch {
            repository.updateHabit(habit)
        }
    }

    fun deleteHabit(habit: HabitEntity) {
        viewModelScope.launch {
            repository.deleteHabit(habit)
        }
    }

    fun archiveHabit(habitId: Long) {
        viewModelScope.launch {
            repository.archiveHabit(habitId)
        }
    }

    suspend fun getHabitById(id: Long): HabitEntity? = repository.getHabitById(id)

    fun getCompletionsForHabit(habitId: Long): Flow<List<HabitCompletionEntity>> =
        repository.getCompletionsForHabit(habitId)

    fun getTodayDate(): Long = today

    // ── Factory ───────────────────────────────────────────────────────────────

    companion object {
        fun factory(repository: HabitRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    HabitViewModel(repository) as T
            }
    }
}
