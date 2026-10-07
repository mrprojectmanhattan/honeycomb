package com.mark.moodlogger.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/** One "I drank some water" tap. Amount is in fluid ounces. */
@Entity(tableName = "water_logs")
data class WaterLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long,
    val amountOz: Int,
)
