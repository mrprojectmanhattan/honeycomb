package com.mark.moodlogger.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A "major goal": a big, long-horizon target ("Lose 100 pounds", "Save $1,000",
 * "Get debt-free"). No progress tracking yet - it's a standing reminder. All
 * major goals show as a tap-to-rotate stack on the main screen; you create and
 * edit them in the Major tab of the Goals screen.
 */
@Entity(tableName = "major_goals")
data class MajorGoal(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val detail: String = "",
    val createdAt: Long,
)
