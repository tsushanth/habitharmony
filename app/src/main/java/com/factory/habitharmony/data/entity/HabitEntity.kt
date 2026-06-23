package com.factory.habitharmony.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class HabitCategory(val displayName: String, val emoji: String) {
    HEALTH("Health", "❤️"),
    FITNESS("Fitness", "💪"),
    LEARNING("Learning", "📚"),
    MINDFULNESS("Mindfulness", "🧘"),
    PRODUCTIVITY("Productivity", "⚡"),
    SOCIAL("Social", "👥"),
    NUTRITION("Nutrition", "🍎"),
    SLEEP("Sleep", "😴"),
    FINANCE("Finance", "💰"),
    OTHER("Other", "✨")
}

enum class HabitFrequency(val displayName: String) {
    DAILY("Every day"),
    WEEKDAYS("Weekdays"),
    WEEKENDS("Weekends"),
    SPECIFIC_DAYS("Specific days")
}

@Entity(tableName = "habits")
data class HabitEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val emoji: String = "⭐",
    val description: String = "",
    val colorHex: Long = 0xFF6C63FF,
    val category: HabitCategory = HabitCategory.OTHER,
    val frequency: HabitFrequency = HabitFrequency.DAILY,
    // Comma-separated day indices: 0=Sun, 1=Mon, 2=Tue, 3=Wed, 4=Thu, 5=Fri, 6=Sat
    val targetDays: String = "0,1,2,3,4,5,6",
    val isArchived: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
) {
    fun getTargetDaysList(): List<Int> =
        targetDays.split(",").mapNotNull { it.trim().toIntOrNull() }

    fun isScheduledForDay(calDayOfWeek: Int): Boolean {
        // calDayOfWeek: Calendar.DAY_OF_WEEK (1=Sun, 2=Mon, ..., 7=Sat)
        val idx = calDayOfWeek - 1 // 0=Sun … 6=Sat
        return when (frequency) {
            HabitFrequency.DAILY -> true
            HabitFrequency.WEEKDAYS -> idx in 1..5 // Mon-Fri
            HabitFrequency.WEEKENDS -> idx == 0 || idx == 6 // Sun or Sat
            HabitFrequency.SPECIFIC_DAYS -> getTargetDaysList().contains(idx)
        }
    }

    fun frequencyLabel(): String = when (frequency) {
        HabitFrequency.DAILY -> "Every day"
        HabitFrequency.WEEKDAYS -> "Mon – Fri"
        HabitFrequency.WEEKENDS -> "Sat & Sun"
        HabitFrequency.SPECIFIC_DAYS -> {
            val dayNames = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
            getTargetDaysList().mapNotNull { dayNames.getOrNull(it) }.joinToString(", ")
        }
    }
}

val HABIT_COLORS = listOf(
    0xFF6C63FF, // Indigo
    0xFF4ECDC4, // Teal
    0xFFFF6B6B, // Red
    0xFFFFCA28, // Amber
    0xFF66BB6A, // Green
    0xFFFF7043, // Deep Orange
    0xFFEC407A, // Pink
    0xFF42A5F5, // Blue
    0xFFAB47BC, // Purple
    0xFF78909C  // Blue Grey
)

val EMOJI_OPTIONS = listOf(
    "⭐", "💪", "🧘", "📚", "🏃", "💧", "🍎", "😴",
    "🎯", "✍️", "🎵", "🌱", "🏋️", "🧠", "❤️", "🎨",
    "🍳", "☕", "🚴", "🧗", "📖", "💊", "🛌", "🌅",
    "🏊", "🎭", "🌿", "🔥", "✅", "🎲"
)
