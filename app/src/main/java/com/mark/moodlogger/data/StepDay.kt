package com.mark.moodlogger.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Steps for one calendar day. The phone's hardware step counter reports a running
 * total since boot, so we track [lastCumulative] and accumulate the deltas into
 * [steps]. A negative delta means the phone rebooted (counter reset to 0); we
 * treat the new cumulative as fresh steps for the day.
 */
@Entity(tableName = "step_days")
data class StepDay(
    @PrimaryKey val epochDay: Long,
    val steps: Int,
    val lastCumulative: Long,
)
