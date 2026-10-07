package com.mark.moodlogger.watch

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** Alarms don't survive a reboot or an app update, so re-arm the reminder chain
 *  and re-apply the notification-bridging opt-out (also runtime-only, not
 *  guaranteed to survive an update on its own). */
class WatchBootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED -> {
                ReminderScheduler.scheduleNextTopOfHour(context)
                BridgingSetup.disablePhoneBridging(context)
            }
        }
    }
}
