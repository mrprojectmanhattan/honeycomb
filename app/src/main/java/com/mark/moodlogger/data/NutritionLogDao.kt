package com.mark.moodlogger.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * NOT WIRED IN YET - see NutritionLogEntry.kt. Once merged, add
 * `abstract fun nutritionLogDao(): NutritionLogDao` to MoodDatabase and
 * NutritionLogEntry::class to its @Database(entities = [...]) list.
 */
@Dao
interface NutritionLogDao {
    @Insert
    suspend fun insert(entry: NutritionLogEntry): Long

    @Delete
    suspend fun delete(entry: NutritionLogEntry)

    @Query("SELECT * FROM nutrition_log WHERE timestamp >= :dayStart AND timestamp < :dayEnd ORDER BY timestamp ASC")
    fun observeForDay(dayStart: Long, dayEnd: Long): Flow<List<NutritionLogEntry>>

    @Query(
        "SELECT COALESCE(SUM(calories), 0.0) FROM nutrition_log " +
            "WHERE timestamp >= :dayStart AND timestamp < :dayEnd"
    )
    fun observeCaloriesForDay(dayStart: Long, dayEnd: Long): Flow<Double>
}
