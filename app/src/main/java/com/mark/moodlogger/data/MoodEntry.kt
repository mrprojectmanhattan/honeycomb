package com.mark.moodlogger.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "mood_entries")
data class MoodEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    /** epoch milliseconds when the mood was logged */
    val timestamp: Long,
    /** 1..5, see [com.mark.moodlogger.MoodScale] */
    val score: Int,
    val note: String = "",
    /** on-schedule ([SOURCE_REMINDER]) vs off-schedule ([SOURCE_MANUAL]).
     *  As of v3.19 this is decided by *timing*: a mood logged within 5 minutes
     *  of the hourly reminder actually firing is on-schedule; anything else
     *  (including anything logged during quiet hours) is off-schedule. */
    @ColumnInfo(defaultValue = "unknown")
    val source: String = SOURCE_MANUAL,
) {
    companion object {
        /** on-schedule: logged within 5 min of the hourly reminder firing */
        const val SOURCE_REMINDER = "reminder"

        /** off-schedule: logged at any other time */
        const val SOURCE_MANUAL = "manual"

        /** pre-dates source tracking */
        const val SOURCE_UNKNOWN = "unknown"

        /** the on-schedule window after a reminder fires, in millis */
        const val ON_SCHEDULE_WINDOW_MS = 5 * 60 * 1000L
    }
}
