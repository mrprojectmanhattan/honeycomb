package com.mark.moodlogger.watch

import android.content.Context
import androidx.wear.phone.interactions.notifications.BridgingConfig
import androidx.wear.phone.interactions.notifications.BridgingManager

/**
 * Stops the phone's notifications from being auto-bridged to this watch app.
 *
 * The watch and phone apps share one applicationId (required for the Data Layer
 * to work at all). Android's side effect of that: it treats them as "the same
 * app" and auto-bridges the phone's own reminder notification over to the
 * watch, on top of the watch's own locally-posted one. Any bridged notification
 * automatically gets an "Open on phone" action tacked on by the system — that's
 * what was showing up instead of the watch's own mood picker, even though the
 * watch's own notification code was correct.
 *
 * The manifest meta-data flag for this (NO_BRIDGING) takes effect only from
 * install time onward and can lose notifications before the watch app is first
 * opened, so Google recommends the runtime API instead — called here on every
 * app open and every boot, same pattern as [ReminderScheduler].
 */
object BridgingSetup {
    fun disablePhoneBridging(context: Context) {
        BridgingManager.fromContext(context.applicationContext)
            .setConfig(
                BridgingConfig.Builder(context.applicationContext, /* bridgingEnabled= */ false)
                    .build()
            )
    }
}
