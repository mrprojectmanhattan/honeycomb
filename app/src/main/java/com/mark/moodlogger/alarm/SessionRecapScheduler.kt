package com.mark.moodlogger.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import java.util.Calendar

/**
 * Schedules the Friday therapy-recap nudge. Same self-rescheduling pattern as
 * [AlarmScheduler]: whichever fire happens, [SessionRecapReceiver] books the one
 * for next Friday before it's done.
 *
 * Fixed at Friday 6:40 PM — his session runs 'til 6:30 and sometimes later, so
 * this lands about the time he's walking to the car. Change [HOUR]/[MINUTE] here
 * if his session time ever moves.
 */
object SessionRecapScheduler {

    private const val REQUEST_CODE = 4714
    const val HOUR = 18
    const val MINUTE = 40

    fun scheduleNextFriday(context: Context) {
        val am = context.getSystemService(AlarmManager::class.java) ?: return
        val triggerAt = nextFridayMillis()

        val pi = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE,
            Intent(context, SessionRecapReceiver::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val canExact =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) am.canScheduleExactAlarms() else true

        if (canExact) {
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi)
        } else {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi)
        }
    }

    fun cancel(context: Context) {
        val am = context.getSystemService(AlarmManager::class.java) ?: return
        val pi = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE,
            Intent(context, SessionRecapReceiver::class.java),
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pi != null) {
            am.cancel(pi)
            pi.cancel()
        }
    }

    /** The next Friday at [HOUR]:[MINUTE], today if it's Friday and that time
     *  hasn't passed yet, otherwise next week. */
    private fun nextFridayMillis(): Long {
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, HOUR)
            set(Calendar.MINUTE, MINUTE)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val now = System.currentTimeMillis()
        while (cal.get(Calendar.DAY_OF_WEEK) != Calendar.FRIDAY || cal.timeInMillis <= now) {
            cal.add(Calendar.DAY_OF_YEAR, 1)
        }
        return cal.timeInMillis
    }
}
