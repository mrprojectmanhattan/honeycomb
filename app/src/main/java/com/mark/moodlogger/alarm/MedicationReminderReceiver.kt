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
        CoroutineScope(SupervisorJob() + Dispatchers.Default).launch {
            try {
                val dao = MoodDatabase.get(app).moodDao()
                val meds = dao.activeMedicationReminders()

                val cal = Calendar.getInstance()
                val nowMinute = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
                // A couple minutes of tolerance either side, in case the alarm fired a
                // little early/late (setAndAllowWhileIdle on the inexact-alarm path can
                // drift on some devices).
                val due = meds.filter { m ->
                    m.reminderMinuteList().any { minute -> kotlin.math.abs(minute - nowMinute) <= 2 }
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
