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

/**
 * Records the time the charger was connected / disconnected, and ONLY that, and
 * only while the opt-in sleep assist is switched on. Used to pre-fill a guess at
 * last night's bed / wake times. It watches nothing else - no screen state, no
 * app usage, no location.
 */
class PowerReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (action != Intent.ACTION_POWER_CONNECTED &&
            action != Intent.ACTION_POWER_DISCONNECTED
        ) {
            return
        }

        val pending = goAsync()
        val app = context.applicationContext
        val now = System.currentTimeMillis()
        CoroutineScope(SupervisorJob() + Dispatchers.Default).launch {
            try {
                val store = SettingsStore(app)
                if (!store.settings.first().sleepAssistEnabled) return@launch
                when (action) {
                    Intent.ACTION_POWER_CONNECTED -> store.recordPluggedIn(now)
                    Intent.ACTION_POWER_DISCONNECTED -> store.recordUnplugged(now)
                }
            } finally {
                pending.finish()
            }
        }
    }
}
