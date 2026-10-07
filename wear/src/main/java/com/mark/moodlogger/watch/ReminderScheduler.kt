package com.mark.moodlogger.watch

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import java.util.Calendar

/**
 * The watch's own hourly reminder, on the same top-of-the-hour rhythm as the
 * phone. It runs on the watch so it still buzzes when the phone is in a locker
 * or a car. Same pattern as the phone: each fire schedules the next.
 */
object ReminderScheduler {

    private const val REQUEST_CODE = 4712

    fun scheduleNextTopOfHour(context: Context) {
        val app = context.applicationContext
        val am = app.getSystemService(AlarmManager::class.java) ?: return

        val triggerAt = Calendar.getInstance().apply {
            timeInMillis = System.currentTimeMillis()
            add(Calendar.HOUR_OF_DAY, 1)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val pi = PendingIntent.getBroadcast(
            app,
            REQUEST_CODE,
            Intent(app, ReminderReceiver::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val canExact =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) am.canScheduleExactAlarms() else true

        if (canExact) {
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi)
        } else {
            // Still fires, just not guaranteed to the minute.
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi)
        }
    }
}
