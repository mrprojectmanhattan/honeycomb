package com.mark.moodlogger.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.mark.moodlogger.data.MoodDatabase
import com.mark.moodlogger.notification.Notifications
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.util.Calendar

class MedicationReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val pending = goAsync()
        val app = context.applicationContext
        // Read off the Intent synchronously, here, before handing off to the coroutine -
        // the Intent isn't guaranteed safe to hold onto past onReceive returning.
        val targetMinute = intent.getIntExtra(MedicationReminderScheduler.EXTRA_TARGET_MINUTE, -1)
        CoroutineScope(SupervisorJob() + Dispatchers.Default).launch {
            try {
                val dao = MoodDatabase.get(app).moodDao()
                val meds = dao.activeMedicationReminders()

                // Match on the exact minute this alarm was scheduled for, carried on the
                // Intent itself, not a comparison against the live clock - correct no
                // matter how late Android actually delivers it (Doze, battery
                // optimization, and similar can delay even an exact alarm by more than a
                // couple minutes - the old now-relative window silently dropped the
                // reminder whenever that happened, midnight-wrap or not). Flagged by an
                // outside reviewer.
                val due = if (targetMinute >= 0) {
                    meds.filter { m -> m.reminderMinuteList().contains(targetMinute) }
                } else {
                    // No target minute on the Intent - a stale alarm scheduled before this
                    // upgrade. Fall back to the old now-relative window (already
                    // midnight-safe) just this once; every alarm booked from here on
                    // carries its own minute.
                    val cal = Calendar.getInstance()
                    val nowMinute = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
                    meds.filter { m ->
                        m.reminderMinuteList().any { minute ->
                            val diff = kotlin.math.abs(minute - nowMinute)
                            minOf(diff, 1440 - diff) <= 2
                        }
                    }
                }
                if (due.isNotEmpty()) {
                    Notifications.showMedReminder(app, due.map { it.name })
                }

                MedicationReminderScheduler.reschedule(app)
            } finally {
                pending.finish()
            }
        }
    }
}
