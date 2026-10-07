package com.mark.moodlogger.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * One medication Mark takes. Free-text [dose] ("50 mg"), [amount] ("1 tablet"),
 * and [schedule] ("8am and 8pm" / "twice daily") — v1 keeps these as strings;
 * structured scheduling comes with medication reminders (v3.24). [prescriberId]
 * is a soft link to a [CareProvider] (nullable, no FK cascade). [asNeeded] = PRN.
 */
@Entity(tableName = "medications")
data class Medication(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String = "",
    @ColumnInfo(defaultValue = "") val dose: String = "",
    @ColumnInfo(defaultValue = "") val amount: String = "",
    @ColumnInfo(defaultValue = "") val schedule: String = "",
    val prescriberId: Long? = null,
    @ColumnInfo(defaultValue = "0") val asNeeded: Boolean = false,
    @ColumnInfo(defaultValue = "1") val active: Boolean = true,
    @ColumnInfo(defaultValue = "") val notes: String = "",
    /** Added 2026-09-28 (med reminders). Structured, unlike [schedule]'s free text -
     *  an alarm needs real minute-of-day values, not "8am and 8pm" as a string. */
    @ColumnInfo(defaultValue = "0") val reminderEnabled: Boolean = false,
    /** Comma-separated minute-of-day values (0..1439), e.g. "480,1200" for 8am and 8pm. */
    @ColumnInfo(defaultValue = "") val reminderMinutes: String = "",
) {
    /** "50 mg · 1 tablet · twice daily" — the parts that are filled in. */
    fun summaryLine(): String = listOf(dose, amount, schedule)
        .filter { it.isNotBlank() }
        .joinToString("  ·  ")

    /** Parsed, sorted, invalid values dropped. */
    fun reminderMinuteList(): List<Int> = reminderMinutes.split(",")
        .mapNotNull { it.trim().toIntOrNull() }
        .filter { it in 0..1439 }
        .sorted()

    companion object {
        fun formatMinutes(minutes: List<Int>): String = minutes.sorted().joinToString(",")
    }
}
