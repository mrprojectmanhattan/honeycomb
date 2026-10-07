package com.mark.moodlogger.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import java.util.Calendar

/**
 * Schedules the weekly PHQ-9/GAD-7 check-in nudge. Same self-rescheduling pattern as
 * [SessionRecapScheduler]: whichever fire happens, [CheckinReminderReceiver] books the
 * next Sunday before it's done. A plain awareness nudge, not conditioned on how long
 * since his last check-in - it only fires once a week anyway. Added 2026-09-28.
 */
object CheckinReminderScheduler {

    private const val REQUEST_CODE = 4715
    const val HOUR = 18
    const val MINUTE = 0

    fun scheduleNextSunday(context: Context) {
        val am = context.getSystemService(AlarmManager::class.java) ?: return
        val triggerAt = nextSundayMillis()

        val pi = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE,
            Intent(context, CheckinReminderReceiver::class.java),
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
            Intent(context, CheckinReminderReceiver::class.java),
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pi != null) {
            am.cancel(pi)
            pi.cancel()
        }
    }

    private fun nextSundayMillis(): Long {
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, HOUR)
            set(Calendar.MINUTE, MINUTE)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val now = System.currentTimeMillis()
        while (cal.get(Calendar.DAY_OF_WEEK) != Calendar.SUNDAY || cal.timeInMillis <= now) {
            cal.add(Calendar.DAY_OF_YEAR, 1)
        }
        return cal.timeInMillis
    }
}
