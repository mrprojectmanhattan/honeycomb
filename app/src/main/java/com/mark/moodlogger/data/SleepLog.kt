package com.mark.moodlogger.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * One night's sleep. [dateEpochDay] is the morning you woke up
 * (`LocalDate.toEpochDay`) and is unique, so re-logging a night replaces it.
 * Times are absolute epoch millis. [quality] is 0 (unrated) or 1..5.
 * [latencyMinutes] is 0 (unrated) or how long it took to fall asleep.
 * [nightWakings] is the number of times woken up during the night.
 * [morningFeeling] is 0 (unrated) or a label from [MORNING_FEELINGS].
 */
@Entity(
    tableName = "sleep_logs",
    indices = [Index(value = ["dateEpochDay"], unique = true)],
)
data class SleepLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val dateEpochDay: Long,
    val bedtimeMillis: Long,
    val wakeMillis: Long,
    val quality: Int = 0,
    val latencyMinutes: Int = 0,
    val nightWakings: Int = 0,
    val morningFeeling: String = "",
) {
    val hours: Float get() = ((wakeMillis - bedtimeMillis).coerceAtLeast(0L)) / 3_600_000f
}

val MORNING_FEELINGS = listOf("Refreshed", "Alert", "Tired", "Groggy")
