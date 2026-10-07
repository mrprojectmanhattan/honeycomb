package com.mark.moodlogger.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import java.util.Calendar

/**
 * Schedules the shopping-list nudge. Like [AlarmScheduler] it holds no repeating
 * alarm: each fire of [ShoppingReminderReceiver] schedules the next one. Its own
 * request code, so it never clashes with the hourly mood reminder or the timer.
 *
 * [schedule] takes the list of hours-of-day the nudge should fire at today. On a
 * normal day that's one hour; when the list has escalated it's two, and the
 * receiver passes both so the next fire lands on whichever comes first.
 */
object ShoppingReminderScheduler {

    private const val REQUEST_CODE = 4713

    fun schedule(context: Context, hours: List<Int>) {
        val am = context.getSystemService(AlarmManager::class.java) ?: return
        val triggerAt = nextFireMillis(hours) ?: return

        val pi = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE,
            Intent(context, ShoppingReminderReceiver::class.java),
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
            Intent(context, ShoppingReminderReceiver::class.java),
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pi != null) {
            am.cancel(pi)
            pi.cancel()
        }
    }

    /** The second, earlier nudge used once the list has escalated. Kept in daytime. */
    fun secondHour(primary: Int): Int {
        val later = primary + 5
        return if (later <= 21) later else ((primary - 5) + 24) % 24
    }

    /** Soonest future occurrence of any hour in [hours], today or tomorrow. */
    private fun nextFireMillis(hours: List<Int>): Long? {
        if (hours.isEmpty()) return null
        val now = System.currentTimeMillis()
        return hours.flatMap { h ->
            val base = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, h.coerceIn(0, 23))
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis
            listOf(base, base + 24L * 60 * 60 * 1000)
        }.filter { it > now + 1000 }.minOrNull()
    }
}
