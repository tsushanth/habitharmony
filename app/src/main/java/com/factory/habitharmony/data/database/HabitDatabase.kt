package com.factory.habitharmony.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.factory.habitharmony.data.dao.HabitDao
import com.factory.habitharmony.data.entity.HabitCategory
import com.factory.habitharmony.data.entity.HabitCompletionEntity
import com.factory.habitharmony.data.entity.HabitEntity
import com.factory.habitharmony.data.entity.HabitFrequency

class HabitConverters {
    @TypeConverter
    fun fromHabitCategory(value: HabitCategory): String = value.name

    @TypeConverter
    fun toHabitCategory(value: String): HabitCategory =
        runCatching { HabitCategory.valueOf(value) }.getOrDefault(HabitCategory.OTHER)

    @TypeConverter
    fun fromHabitFrequency(value: HabitFrequency): String = value.name

    @TypeConverter
    fun toHabitFrequency(value: String): HabitFrequency =
        runCatching { HabitFrequency.valueOf(value) }.getOrDefault(HabitFrequency.DAILY)
}

@Database(
    entities = [HabitEntity::class, HabitCompletionEntity::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(HabitConverters::class)
abstract class HabitDatabase : RoomDatabase() {

    abstract fun habitDao(): HabitDao

    companion object {
        @Volatile
        private var INSTANCE: HabitDatabase? = null

        fun getInstance(context: Context): HabitDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    HabitDatabase::class.java,
                    "habit_harmony.db"
                ).build().also { INSTANCE = it }
            }
    }
}
