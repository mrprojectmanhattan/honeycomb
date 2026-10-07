package com.mark.moodlogger.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.mark.moodlogger.notification.Notifications
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/** Fires Friday evening, right around when a therapy session lets out, and
 *  nudges a quick voice recap into the Journal while it's still fresh. */
class SessionRecapReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val pending = goAsync()
        val app = context.applicationContext
        CoroutineScope(SupervisorJob() + Dispatchers.Default).launch {
            try {
                Notifications.showSessionRecapReminder(app)
                SessionRecapScheduler.scheduleNextFriday(app)
            } finally {
                pending.finish()
            }
        }
    }
}
