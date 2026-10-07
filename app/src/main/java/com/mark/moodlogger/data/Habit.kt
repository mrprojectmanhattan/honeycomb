package com.mark.moodlogger.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDate
import java.time.Period

@Entity(tableName = "habits")
data class Habit(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    /** [KIND_QUIT] or [KIND_STARTED] */
    val kind: String,
    /** the day it started, as [LocalDate.toEpochDay] */
    val startEpochDay: Long,
) {
    val startDate: LocalDate get() = LocalDate.ofEpochDay(startEpochDay)

    companion object {
        const val KIND_QUIT = "quit"
        const val KIND_STARTED = "started"
    }
}

/** "5 months, 29 days", "2 years, 1 month", "3 days", "today". */
fun elapsedSince(start: LocalDate, today: LocalDate = LocalDate.now()): String {
    if (!today.isAfter(start)) return if (today == start) "today" else "starts in the future"
    val p = Period.between(start, today)
    val parts = buildList {
        if (p.years > 0) add("${p.years} " + plural(p.years, "year"))
        if (p.months > 0) add("${p.months} " + plural(p.months, "month"))
        if (p.days > 0 || isEmpty()) add("${p.days} " + plural(p.days, "day"))
    }
    return parts.joinToString(", ")
}

fun totalDays(start: LocalDate, today: LocalDate = LocalDate.now()): Long =
    java.time.temporal.ChronoUnit.DAYS.between(start, today).coerceAtLeast(0)

private fun plural(n: Int, unit: String) = if (n == 1) unit else "${unit}s"
