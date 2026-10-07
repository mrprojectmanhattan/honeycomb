package com.mark.moodlogger.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.mark.moodlogger.data.MoodDatabase
import com.mark.moodlogger.data.PromptEvent
import com.mark.moodlogger.data.SettingsStore
import com.mark.moodlogger.notification.Notifications
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Calendar

class ReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val pending = goAsync()
        val app = context.applicationContext
        CoroutineScope(SupervisorJob() + Dispatchers.Default).launch {
            try {
                val store = SettingsStore(app)
                val settings = store.settings.first()
                if (!settings.remindersEnabled) return@launch

                val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
                val silenced = settings.quietHoursEnabled &&
                    inQuietWindow(hour, settings.quietStartHour, settings.quietEndHour)

                if (!silenced) {
                    Notifications.showMoodReminder(app)
                    val firedAt = System.currentTimeMillis()
                    // Timestamp the fire so a mood logged in the next 5 min counts
                    // as on-schedule (see MoodEntry.SOURCE_*).
                    store.recordReminderFired(firedAt)
                    MoodDatabase.get(app).moodDao()
                        .insertPromptEvent(PromptEvent(firedAt = firedAt))
                }
                AlarmScheduler.scheduleNextTopOfHour(app)
            } finally {
                pending.finish()
            }
        }
    }

    private fun inQuietWindow(hour: Int, start: Int, end: Int): Boolean = when {
        start == end -> false
        start < end -> hour in start until end
        else -> hour >= start || hour < end // window wraps past midnight
    }
}
