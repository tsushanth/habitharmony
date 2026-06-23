package com.factory.habitharmony.data.repository

import com.factory.habitharmony.data.dao.HabitDao
import com.factory.habitharmony.data.entity.HabitCompletionEntity
import com.factory.habitharmony.data.entity.HabitEntity
import kotlinx.coroutines.flow.Flow
import java.util.Calendar

class HabitRepository(private val dao: HabitDao) {

    // ── Habits ────────────────────────────────────────────────────────────────

    fun getAllActiveHabits(): Flow<List<HabitEntity>> = dao.getAllActiveHabits()

    suspend fun getHabitById(id: Long): HabitEntity? = dao.getHabitById(id)

    suspend fun insertHabit(habit: HabitEntity): Long = dao.insertHabit(habit)

    suspend fun updateHabit(habit: HabitEntity) = dao.updateHabit(habit)

    suspend fun deleteHabit(habit: HabitEntity) = dao.deleteHabit(habit)

    suspend fun archiveHabit(habitId: Long) = dao.archiveHabit(habitId)

    // ── Completions ───────────────────────────────────────────────────────────

    fun getCompletionsSince(sinceDate: Long): Flow<List<HabitCompletionEntity>> =
        dao.getCompletionsSince(sinceDate)

    fun getCompletionsForDate(date: Long): Flow<List<HabitCompletionEntity>> =
        dao.getCompletionsForDate(date)

    fun getCompletionsForHabit(habitId: Long): Flow<List<HabitCompletionEntity>> =
        dao.getCompletionsForHabit(habitId)

    suspend fun toggleCompletion(habitId: Long, date: Long) {
        if (dao.getCompletion(habitId, date) != null) {
            dao.deleteCompletion(habitId, date)
        } else {
            dao.insertCompletion(HabitCompletionEntity(habitId = habitId, completedDate = date))
        }
    }

    suspend fun isCompletedOnDate(habitId: Long, date: Long): Boolean =
        dao.getCompletion(habitId, date) != null

    suspend fun getTotalCompletions(habitId: Long): Int = dao.getTotalCompletions(habitId)

    // ── Streak ────────────────────────────────────────────────────────────────

    /**
     * Computes the current consecutive-day streak for a habit.
     * Looks backwards from today; gives benefit-of-the-doubt for today
     * (i.e. if today is not yet completed, the streak from yesterday still counts).
     */
    suspend fun computeCurrentStreak(habitId: Long, habit: HabitEntity): Int {
        val completions = dao.getRecentCompletions(habitId, limit = 400)
        if (completions.isEmpty()) return 0
        val completedDates = completions.map { it.completedDate }.toSet()

        val cal = Calendar.getInstance()
        val today = dayStart(cal)

        var streak = 0
        cal.timeInMillis = today

        // Walk backwards; if today isn't done yet, start checking from yesterday
        val startFromYesterday = !completedDates.contains(today)
        if (startFromYesterday) cal.add(Calendar.DAY_OF_YEAR, -1)

        repeat(400) {
            val dayMs = cal.timeInMillis
            val scheduled = habit.isScheduledForDay(cal.get(Calendar.DAY_OF_WEEK))
            when {
                !scheduled -> { /* skip non-scheduled days, don't break streak */ }
                completedDates.contains(dayMs) -> streak++
                else -> return streak
            }
            cal.add(Calendar.DAY_OF_YEAR, -1)
        }
        return streak
    }

    /**
     * Computes the best (longest) streak ever achieved for a habit.
     */
    suspend fun computeBestStreak(habitId: Long, habit: HabitEntity): Int {
        val completions = dao.getRecentCompletions(habitId, limit = 1000)
        if (completions.isEmpty()) return 0
        val completedDates = completions.map { it.completedDate }.toSet()
        if (completedDates.isEmpty()) return 0

        val sortedDates = completedDates.sorted()
        var best = 1
        var current = 1

        for (i in 1 until sortedDates.size) {
            val diff = sortedDates[i] - sortedDates[i - 1]
            if (diff == 86_400_000L) { // exactly one day apart
                current++
                if (current > best) best = current
            } else if (diff > 86_400_000L) {
                current = 1
            }
        }
        return best
    }

    // ── Date helpers ──────────────────────────────────────────────────────────

    companion object {
        fun dayStart(cal: Calendar = Calendar.getInstance()): Long {
            val c = cal.clone() as Calendar
            c.set(Calendar.HOUR_OF_DAY, 0)
            c.set(Calendar.MINUTE, 0)
            c.set(Calendar.SECOND, 0)
            c.set(Calendar.MILLISECOND, 0)
            return c.timeInMillis
        }

        fun todayStart(): Long = dayStart()

        /** Returns epoch-millis for midnight of [daysAgo] days before today. */
        fun daysAgoStart(daysAgo: Int): Long {
            val cal = Calendar.getInstance()
            cal.add(Calendar.DAY_OF_YEAR, -daysAgo)
            return dayStart(cal)
        }
    }
}
