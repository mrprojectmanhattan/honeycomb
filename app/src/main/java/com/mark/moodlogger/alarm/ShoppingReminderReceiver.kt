package com.mark.moodlogger.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.mark.moodlogger.data.MoodDatabase
import com.mark.moodlogger.data.SettingsStore
import com.mark.moodlogger.notification.Notifications
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class ShoppingReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val pending = goAsync()
        val app = context.applicationContext
        CoroutineScope(SupervisorJob() + Dispatchers.Default).launch {
            try {
                val store = SettingsStore(app)
                val s = store.settings.first()
                if (!s.shoppingRemindersEnabled) return@launch

                val dao = MoodDatabase.get(app).moodDao()
                val outstanding = dao.outstandingShoppingItems()

                if (outstanding.isEmpty()) {
                    ShoppingReminderScheduler.schedule(app, listOf(s.shoppingReminderHour))
                    return@launch
                }

                val now = System.currentTimeMillis()
                val oldest = outstanding.maxByOrNull { now - it.addedAt }
                val oldestDays = oldest?.ageDays(now) ?: 0
                val escalated = oldestDays >= s.shoppingEscalateAfterDays

                Notifications.showShoppingReminder(
                    context = app,
                    itemNames = outstanding.map { it.name },
                    oldestName = oldest?.name,
                    oldestDays = oldestDays,
                    escalated = escalated,
                )

                val hours = if (escalated) {
                    listOf(
                        s.shoppingReminderHour,
                        ShoppingReminderScheduler.secondHour(s.shoppingReminderHour),
                    )
                } else {
                    listOf(s.shoppingReminderHour)
                }
                ShoppingReminderScheduler.schedule(app, hours)
            } finally {
                pending.finish()
            }
        }
    }
}
