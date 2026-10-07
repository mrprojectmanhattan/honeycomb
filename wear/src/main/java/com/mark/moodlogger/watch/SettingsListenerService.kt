package com.mark.moodlogger.watch

import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.WearableListenerService

/**
 * Receives the reminder schedule from the phone (on/off and quiet hours) and
 * caches it, so the watch's own reminder follows the phone's settings while the
 * phone is out of range.
 */
class SettingsListenerService : WearableListenerService() {

    override fun onDataChanged(events: DataEventBuffer) {
        for (event in events) {
            if (event.type != DataEvent.TYPE_CHANGED) continue
            if (event.dataItem.uri.path != PATH) continue
            val map = DataMapItem.fromDataItem(event.dataItem).dataMap
            val current = WatchPrefs.schedule(this)
            WatchPrefs.saveSchedule(
                this,
                WatchPrefs.Schedule(
                    enabled = map.getBoolean(KEY_ENABLED, current.enabled),
                    quietEnabled = map.getBoolean(KEY_QUIET_ENABLED, current.quietEnabled),
                    quietStart = map.getInt(KEY_QUIET_START, current.quietStart),
                    quietEnd = map.getInt(KEY_QUIET_END, current.quietEnd),
                )
            )
            // Make sure the alarm chain is running with the new schedule.
            ReminderScheduler.scheduleNextTopOfHour(this)
        }
    }

    companion object {
        // Must match the phone's WatchSettingsPublisher.
        const val PATH = "/settings"
        const val KEY_ENABLED = "remindersEnabled"
        const val KEY_QUIET_ENABLED = "quietHoursEnabled"
        const val KEY_QUIET_START = "quietStartHour"
        const val KEY_QUIET_END = "quietEndHour"
    }
}
