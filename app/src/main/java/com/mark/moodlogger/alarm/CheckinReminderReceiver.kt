package com.mark.moodlogger.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.mark.moodlogger.data.SettingsStore
import com.mark.moodlogger.notification.Notifications
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class CheckinReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val pending = goAsync()
        val app = context.applicationContext
        CoroutineScope(SupervisorJob() + Dispatchers.Default).launch {
            try {
                val store = SettingsStore(app)
                val s = store.settings.first()
                if (s.checkinRemindersEnabled) {
                    Notifications.showCheckinReminder(app)
                }
                // Reschedule regardless, so turning it back on later doesn't need a
                // fresh boot/settings-change to re-arm the chain.
                CheckinReminderScheduler.scheduleNextSunday(app)
            } finally {
                pending.finish()
            }
        }
    }
}
