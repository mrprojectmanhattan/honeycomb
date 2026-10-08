package com.mark.moodlogger.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.mark.moodlogger.data.MoodDatabase
import java.util.Calendar

/**
 * Schedules medication reminders. Unlike the other reminders here, the fire times come
 * from data (each active [com.mark.moodlogger.data.Medication] with reminders on can
 * have several times a day), not a fixed hour - so this reads the database itself
 * rather than taking hours/minutes as a parameter. One alarm at a time for the single
 * soonest minute across every medication, same "each fire books the next one" shape as
 * the others; the receiver re-reads the database to know which medication(s) matched.
 * Added 2026-09-28.
 */
object MedicationReminderScheduler {

    private const val REQUEST_CODE = 4716

    /** Carries which reminder-minute this specific alarm firing is for, so the receiver
     *  can match on that directly instead of comparing against the live clock. A plain
     *  now-relative window (the original fix) still silently drops a reminder delivered
     *  more than a couple minutes late - real on Android under Doze/battery optimization,
     *  not just a midnight-wrap edge case. Flagged by an outside reviewer. */
    const val EXTRA_TARGET_MINUTE = "target_minute"

    suspend fun reschedule(context: Context) {
        val app = context.applicationContext
        val meds = MoodDatabase.get(app).moodDao().activeMedicationReminders()
        val minutes = meds.flatMap { it.reminderMinuteList() }.toSet()
        val am = app.getSystemService(AlarmManager::class.java) ?: return

        val next = nextFire(minutes)
        if (next == null) {
            val pi = PendingIntent.getBroadcast(
                app,
                REQUEST_CODE,
                Intent(app, MedicationReminderReceiver::class.java),
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            )
            if (pi != null) {
                am.cancel(pi)
                pi.cancel()
            }
            return
        }

        val intent = Intent(app, MedicationReminderReceiver::class.java)
            .putExtra(EXTRA_TARGET_MINUTE, next.minute)
        val pi = PendingIntent.getBroadcast(
            app,
            REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val canExact =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) am.canScheduleExactAlarms() else true
        if (canExact) {
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, next.millis, pi)
        } else {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, next.millis, pi)
        }
    }

    fun cancel(context: Context) {
        val am = context.getSystemService(AlarmManager::class.java) ?: return
        val pi = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE,
            Intent(context, MedicationReminderReceiver::class.java),
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pi != null) {
            am.cancel(pi)
            pi.cancel()
        }
    }

    private data class NextFire(val millis: Long, val minute: Int)

    /** Soonest future occurrence of any minute-of-day in [minutes], today or tomorrow,
     *  plus which minute it was - the minute travels with the alarm from here on. */
    private fun nextFire(minutes: Set<Int>): NextFire? {
        if (minutes.isEmpty()) return null
        val now = System.currentTimeMillis()
        return minutes.flatMap { m ->
            val base = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, m / 60)
                set(Calendar.MINUTE, m % 60)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis
            listOf(NextFire(base, m), NextFire(base + 24L * 60 * 60 * 1000, m))
        }.filter { it.millis > now + 1000 }.minByOrNull { it.millis }
    }
}
