package com.mark.moodlogger.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.mark.moodlogger.data.SettingsStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/** Alarms do not survive a reboot or an app update, so re-arm the chain. */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED -> Unit
            else -> return
        }

        val pending = goAsync()
        val app = context.applicationContext
        CoroutineScope(SupervisorJob() + Dispatchers.Default).launch {
            try {
                val store = SettingsStore(app)
                val s = store.settings.first()
                if (s.remindersEnabled) {
                    AlarmScheduler.scheduleNextTopOfHour(app)
                }
                if (s.shoppingRemindersEnabled) {
                    ShoppingReminderScheduler.schedule(app, listOf(s.shoppingReminderHour))
                }
                SessionRecapScheduler.scheduleNextFriday(app)
                CheckinReminderScheduler.scheduleNextSunday(app)
                MedicationReminderScheduler.reschedule(app)
                if (s.timerTriggerAt > System.currentTimeMillis()) {
                    TimerScheduler.schedule(app, s.timerTriggerAt)
                } else if (s.timerTriggerAt > 0L) {
                    store.clearTimer()
                }
            } finally {
                pending.finish()
            }
        }
    }
}
