package com.mark.moodlogger.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/** One "I trained this muscle group" tap. Tracked per week (resets Monday). */
@Entity(tableName = "workout_logs")
data class WorkoutLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long,
    val region: String,
) {
    companion object {
        const val SHOULDERS = "shoulders"
        const val CHEST = "chest"
        const val ARMS = "arms"
        const val BACK = "back"
        const val CORE = "core"
        const val LEGS = "legs"

        /** Display order. */
        val REGIONS = listOf(SHOULDERS, CHEST, ARMS, BACK, CORE, LEGS)

        fun label(region: String): String =
            region.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
    }
}
