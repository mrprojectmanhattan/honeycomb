package com.mark.moodlogger.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A member of Mark's care team (therapist / psychiatrist / other) and, if it
 * recurs, the weekly appointment slot. [dayOfWeek] is java.time's 1=Mon..7=Sun,
 * null when there's no set recurrence; [minuteOfDay] is 0..1439, null when no
 * time is set.
 */
@Entity(tableName = "care_providers")
data class CareProvider(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val kind: String = KIND_THERAPIST,
    val name: String = "",
    val dayOfWeek: Int? = null,
    val minuteOfDay: Int? = null,
    @ColumnInfo(defaultValue = "")
    val notes: String = "",
    @ColumnInfo(defaultValue = "1")
    val active: Boolean = true,
    @ColumnInfo(defaultValue = "")
    val phone: String = "",
) {
    companion object {
        const val KIND_THERAPIST = "therapist"
        const val KIND_PSYCHIATRIST = "psychiatrist"
        const val KIND_PRIMARY = "primary"
        const val KIND_SPECIALIST = "specialist"
        const val KIND_DENTIST = "dentist"
        const val KIND_OTHER = "other"

        /** Display order in the kind picker. */
        val KINDS = listOf(
            KIND_PRIMARY, KIND_THERAPIST, KIND_PSYCHIATRIST,
            KIND_SPECIALIST, KIND_DENTIST, KIND_OTHER,
        )

        fun kindLabel(kind: String): String = when (kind) {
            KIND_THERAPIST -> "Therapist"
            KIND_PSYCHIATRIST -> "Psychiatrist"
            KIND_PRIMARY -> "Primary care"
            KIND_SPECIALIST -> "Specialist"
            KIND_DENTIST -> "Dentist"
            else -> "Other"
        }
    }
}
