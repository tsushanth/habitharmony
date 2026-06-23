package com.factory.habitharmony.data.dao

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.factory.habitharmony.data.database.HabitDatabase
import com.factory.habitharmony.data.entity.HabitCategory
import com.factory.habitharmony.data.entity.HabitCompletionEntity
import com.factory.habitharmony.data.entity.HabitEntity
import com.factory.habitharmony.data.entity.HabitFrequency
import com.factory.habitharmony.data.repository.HabitRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class HabitDaoTest {

    private lateinit var db: HabitDatabase
    private lateinit var dao: HabitDao

    @Before
    fun setup() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            HabitDatabase::class.java
        ).allowMainThreadQueries().build()
        dao = db.habitDao()
    }

    @After
    fun tearDown() {
        db.close()
    }

    // ── insertHabit / getHabitById ────────────────────────────────────────────

    @Test
    fun insertHabit_returnsGeneratedId() = runTest {
        val id = dao.insertHabit(HabitEntity(name = "Run"))
        assertTrue(id > 0)
    }

    @Test
    fun insertHabit_canBeRetrievedById() = runTest {
        val id = dao.insertHabit(HabitEntity(name = "Meditate", emoji = "🧘"))
        val retrieved = dao.getHabitById(id)

        assertNotNull(retrieved)
        assertEquals("Meditate", retrieved!!.name)
        assertEquals("🧘", retrieved.emoji)
    }

    @Test
    fun getHabitById_returnsNull_forNonExistentId() = runTest {
        assertNull(dao.getHabitById(9999L))
    }

    @Test
    fun insertHabit_preservesAllFields() = runTest {
        val habit = HabitEntity(
            name = "Read",
            emoji = "📚",
            description = "Read 30 minutes",
            colorHex = 0xFF4ECDC4,
            category = HabitCategory.LEARNING,
            frequency = HabitFrequency.WEEKDAYS,
            targetDays = "1,2,3,4,5",
            isArchived = false,
            createdAt = 1_000_000L
        )
        val id = dao.insertHabit(habit)
        val retrieved = dao.getHabitById(id)

        assertNotNull(retrieved)
        assertEquals("Read", retrieved!!.name)
        assertEquals("📚", retrieved.emoji)
        assertEquals("Read 30 minutes", retrieved.description)
        assertEquals(0xFF4ECDC4, retrieved.colorHex)
        assertEquals(HabitCategory.LEARNING, retrieved.category)
        assertEquals(HabitFrequency.WEEKDAYS, retrieved.frequency)
        assertEquals("1,2,3,4,5", retrieved.targetDays)
        assertFalse(retrieved.isArchived)
        assertEquals(1_000_000L, retrieved.createdAt)
    }

    // ── getAllActiveHabits ─────────────────────────────────────────────────────

    @Test
    fun getAllActiveHabits_excludesArchivedHabits() = runTest {
        dao.insertHabit(HabitEntity(name = "Active One"))
        dao.insertHabit(HabitEntity(name = "Active Two"))
        dao.insertHabit(HabitEntity(name = "Archived", isArchived = true))

        val result = dao.getAllActiveHabits().first()

        assertEquals(2, result.size)
        assertTrue(result.all { !it.isArchived })
    }

    @Test
    fun getAllActiveHabits_returnsEmpty_whenAllArchived() = runTest {
        dao.insertHabit(HabitEntity(name = "Archived", isArchived = true))

        val result = dao.getAllActiveHabits().first()

        assertTrue(result.isEmpty())
    }

    @Test
    fun getAllActiveHabits_orderedByCreatedAtAscending() = runTest {
        dao.insertHabit(HabitEntity(name = "Later",  createdAt = 2000L))
        dao.insertHabit(HabitEntity(name = "Earlier", createdAt = 1000L))

        val result = dao.getAllActiveHabits().first()

        assertEquals("Earlier", result[0].name)
        assertEquals("Later", result[1].name)
    }

    // ── getAllHabits ───────────────────────────────────────────────────────────

    @Test
    fun getAllHabits_includesArchivedHabits() = runTest {
        dao.insertHabit(HabitEntity(name = "Active"))
        dao.insertHabit(HabitEntity(name = "Archived", isArchived = true))

        val result = dao.getAllHabits().first()

        assertEquals(2, result.size)
    }

    // ── updateHabit ───────────────────────────────────────────────────────────

    @Test
    fun updateHabit_changesStoredFields() = runTest {
        val id = dao.insertHabit(HabitEntity(name = "Original"))
        val existing = dao.getHabitById(id)!!
        dao.updateHabit(existing.copy(name = "Updated", emoji = "🔥"))

        val updated = dao.getHabitById(id)
        assertEquals("Updated", updated!!.name)
        assertEquals("🔥", updated.emoji)
    }

    // ── deleteHabit ───────────────────────────────────────────────────────────

    @Test
    fun deleteHabit_removesRecord() = runTest {
        val id = dao.insertHabit(HabitEntity(name = "Delete Me"))
        val habit = dao.getHabitById(id)!!
        dao.deleteHabit(habit)

        assertNull(dao.getHabitById(id))
    }

    @Test
    fun deleteHabit_cascadesCompletionDeletion() = runTest {
        val habitId = dao.insertHabit(HabitEntity(name = "Test"))
        val date = HabitRepository.todayStart()
        dao.insertCompletion(HabitCompletionEntity(habitId = habitId, completedDate = date))

        val habit = dao.getHabitById(habitId)!!
        dao.deleteHabit(habit)

        // Cascade should have removed the completion
        assertNull(dao.getCompletion(habitId, date))
    }

    // ── archiveHabit ──────────────────────────────────────────────────────────

    @Test
    fun archiveHabit_setsIsArchivedTrue() = runTest {
        val id = dao.insertHabit(HabitEntity(name = "Archive Me"))
        dao.archiveHabit(id)

        val habit = dao.getHabitById(id)
        assertNotNull(habit)
        assertTrue(habit!!.isArchived)
    }

    @Test
    fun archiveHabit_removesFromActiveList() = runTest {
        val id = dao.insertHabit(HabitEntity(name = "Archive Me"))
        dao.archiveHabit(id)

        val activeHabits = dao.getAllActiveHabits().first()
        assertTrue(activeHabits.none { it.id == id })
    }

    // ── insertCompletion / getCompletion ──────────────────────────────────────

    @Test
    fun insertCompletion_canBeRetrievedByHabitIdAndDate() = runTest {
        val habitId = dao.insertHabit(HabitEntity(name = "Test"))
        val date = HabitRepository.todayStart()
        dao.insertCompletion(HabitCompletionEntity(habitId = habitId, completedDate = date))

        val result = dao.getCompletion(habitId, date)
        assertNotNull(result)
        assertEquals(habitId, result!!.habitId)
        assertEquals(date, result.completedDate)
    }

    @Test
    fun getCompletion_returnsNull_whenNotExists() = runTest {
        assertNull(dao.getCompletion(999L, HabitRepository.todayStart()))
    }

    @Test
    fun insertCompletion_ignoreDuplicate_uniqueConstraint() = runTest {
        val habitId = dao.insertHabit(HabitEntity(name = "Test"))
        val date = HabitRepository.todayStart()

        dao.insertCompletion(HabitCompletionEntity(habitId = habitId, completedDate = date))
        dao.insertCompletion(HabitCompletionEntity(habitId = habitId, completedDate = date))

        // Only one record should exist
        assertEquals(1, dao.getTotalCompletions(habitId))
    }

    // ── deleteCompletion ──────────────────────────────────────────────────────

    @Test
    fun deleteCompletion_removesRecord() = runTest {
        val habitId = dao.insertHabit(HabitEntity(name = "Test"))
        val date = HabitRepository.todayStart()
        dao.insertCompletion(HabitCompletionEntity(habitId = habitId, completedDate = date))

        dao.deleteCompletion(habitId, date)

        assertNull(dao.getCompletion(habitId, date))
    }

    @Test
    fun deleteCompletion_doesNothing_whenRecordMissing() = runTest {
        val habitId = dao.insertHabit(HabitEntity(name = "Test"))
        // Should not throw
        dao.deleteCompletion(habitId, HabitRepository.todayStart())
    }

    // ── getTotalCompletions ───────────────────────────────────────────────────

    @Test
    fun getTotalCompletions_returnsCorrectCount() = runTest {
        val habitId = dao.insertHabit(HabitEntity(name = "Test"))
        (0..4).forEach { daysAgo ->
            dao.insertCompletion(
                HabitCompletionEntity(
                    habitId = habitId,
                    completedDate = HabitRepository.daysAgoStart(daysAgo)
                )
            )
        }

        assertEquals(5, dao.getTotalCompletions(habitId))
    }

    @Test
    fun getTotalCompletions_returnsZero_whenNoneExist() = runTest {
        val habitId = dao.insertHabit(HabitEntity(name = "Test"))
        assertEquals(0, dao.getTotalCompletions(habitId))
    }

    @Test
    fun getTotalCompletions_isolatedPerHabit() = runTest {
        val h1 = dao.insertHabit(HabitEntity(name = "A"))
        val h2 = dao.insertHabit(HabitEntity(name = "B"))
        val date = HabitRepository.todayStart()

        dao.insertCompletion(HabitCompletionEntity(habitId = h1, completedDate = date))
        dao.insertCompletion(HabitCompletionEntity(habitId = h1, completedDate = HabitRepository.daysAgoStart(1)))

        assertEquals(2, dao.getTotalCompletions(h1))
        assertEquals(0, dao.getTotalCompletions(h2))
    }

    // ── getCompletionsForDate ─────────────────────────────────────────────────

    @Test
    fun getCompletionsForDate_returnsOnlyMatchingDate() = runTest {
        val h1 = dao.insertHabit(HabitEntity(name = "A"))
        val h2 = dao.insertHabit(HabitEntity(name = "B"))
        val today = HabitRepository.todayStart()
        val yesterday = HabitRepository.daysAgoStart(1)

        dao.insertCompletion(HabitCompletionEntity(habitId = h1, completedDate = today))
        dao.insertCompletion(HabitCompletionEntity(habitId = h2, completedDate = today))
        dao.insertCompletion(HabitCompletionEntity(habitId = h1, completedDate = yesterday))

        val result = dao.getCompletionsForDate(today).first()
        assertEquals(2, result.size)
        assertTrue(result.all { it.completedDate == today })
    }

    @Test
    fun getCompletionsForDate_returnsEmpty_whenNoneOnDate() = runTest {
        val habitId = dao.insertHabit(HabitEntity(name = "Test"))
        dao.insertCompletion(
            HabitCompletionEntity(habitId = habitId, completedDate = HabitRepository.daysAgoStart(1))
        )

        val result = dao.getCompletionsForDate(HabitRepository.todayStart()).first()
        assertTrue(result.isEmpty())
    }

    // ── getCompletionsSince ───────────────────────────────────────────────────

    @Test
    fun getCompletionsSince_returnsCompletionsOnOrAfterDate() = runTest {
        val habitId = dao.insertHabit(HabitEntity(name = "Test"))
        val today = HabitRepository.todayStart()
        val yesterday = HabitRepository.daysAgoStart(1)
        val threeDaysAgo = HabitRepository.daysAgoStart(3)

        dao.insertCompletion(HabitCompletionEntity(habitId = habitId, completedDate = today))
        dao.insertCompletion(HabitCompletionEntity(habitId = habitId, completedDate = yesterday))
        dao.insertCompletion(HabitCompletionEntity(habitId = habitId, completedDate = threeDaysAgo))

        // Query since 2 days ago: should return today and yesterday only
        val since = HabitRepository.daysAgoStart(2)
        val result = dao.getCompletionsSince(since).first()
        assertEquals(2, result.size)
        assertTrue(result.all { it.completedDate >= since })
    }

    @Test
    fun getCompletionsSince_returnsAll_whenSinceIsVeryOld() = runTest {
        val habitId = dao.insertHabit(HabitEntity(name = "Test"))
        (0..2).forEach { daysAgo ->
            dao.insertCompletion(
                HabitCompletionEntity(habitId = habitId, completedDate = HabitRepository.daysAgoStart(daysAgo))
            )
        }

        val result = dao.getCompletionsSince(0L).first()
        assertEquals(3, result.size)
    }

    // ── getCompletionsForHabit ────────────────────────────────────────────────

    @Test
    fun getCompletionsForHabit_returnsOnlyThatHabitsCompletions() = runTest {
        val h1 = dao.insertHabit(HabitEntity(name = "A"))
        val h2 = dao.insertHabit(HabitEntity(name = "B"))
        val today = HabitRepository.todayStart()
        val yesterday = HabitRepository.daysAgoStart(1)

        dao.insertCompletion(HabitCompletionEntity(habitId = h1, completedDate = today))
        dao.insertCompletion(HabitCompletionEntity(habitId = h1, completedDate = yesterday))
        dao.insertCompletion(HabitCompletionEntity(habitId = h2, completedDate = today))

        val result = dao.getCompletionsForHabit(h1).first()
        assertEquals(2, result.size)
        assertTrue(result.all { it.habitId == h1 })
    }

    @Test
    fun getCompletionsForHabit_orderedByDateDescending() = runTest {
        val habitId = dao.insertHabit(HabitEntity(name = "Test"))
        val today = HabitRepository.todayStart()
        val yesterday = HabitRepository.daysAgoStart(1)
        val twoDaysAgo = HabitRepository.daysAgoStart(2)

        dao.insertCompletion(HabitCompletionEntity(habitId = habitId, completedDate = yesterday))
        dao.insertCompletion(HabitCompletionEntity(habitId = habitId, completedDate = twoDaysAgo))
        dao.insertCompletion(HabitCompletionEntity(habitId = habitId, completedDate = today))

        val result = dao.getCompletionsForHabit(habitId).first()
        // Most recent first
        assertEquals(today, result[0].completedDate)
        assertEquals(yesterday, result[1].completedDate)
        assertEquals(twoDaysAgo, result[2].completedDate)
    }

    // ── getRecentCompletions ──────────────────────────────────────────────────

    @Test
    fun getRecentCompletions_respectsLimit() = runTest {
        val habitId = dao.insertHabit(HabitEntity(name = "Test"))
        (0..4).forEach { daysAgo ->
            dao.insertCompletion(
                HabitCompletionEntity(habitId = habitId, completedDate = HabitRepository.daysAgoStart(daysAgo))
            )
        }

        val result = dao.getRecentCompletions(habitId, limit = 3)
        assertEquals(3, result.size)
    }

    @Test
    fun getRecentCompletions_returnsEmpty_whenNone() = runTest {
        val habitId = dao.insertHabit(HabitEntity(name = "Test"))
        assertTrue(dao.getRecentCompletions(habitId, limit = 100).isEmpty())
    }

    @Test
    fun getRecentCompletions_returnsMostRecentFirst() = runTest {
        val habitId = dao.insertHabit(HabitEntity(name = "Test"))
        val today = HabitRepository.todayStart()
        val yesterday = HabitRepository.daysAgoStart(1)

        dao.insertCompletion(HabitCompletionEntity(habitId = habitId, completedDate = yesterday))
        dao.insertCompletion(HabitCompletionEntity(habitId = habitId, completedDate = today))

        val result = dao.getRecentCompletions(habitId, limit = 10)
        assertEquals(today, result[0].completedDate)
        assertEquals(yesterday, result[1].completedDate)
    }
}
