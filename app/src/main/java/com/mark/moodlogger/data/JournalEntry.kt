package com.mark.moodlogger.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * A written / spoken reflection. Multiple per day allowed. [day] is
 * `LocalDate.toEpochDay()` for the day the entry belongs to (indexed for the
 * day view); [timestamp] is epoch millis for ordering. [audioPath] is an
 * absolute path into `filesDir/journal_audio/` or null. [title] is optional
 * (blank when not given) and exists mainly to make an entry easy to search for.
 */
@Entity(
    tableName = "journal_entries",
    indices = [Index(value = ["day"])],
)
data class JournalEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val day: Long,
    @ColumnInfo(defaultValue = "")
    val title: String = "",
    val text: String = "",
    val audioPath: String? = null,
    val audioDurationMs: Long = 0,
)
