package com.mark.moodlogger.wear

import android.content.Context
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable
import com.mark.moodlogger.data.SettingsStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

/**
 * Tells the watch when to remind (v3.27). The watch runs its own hourly
 * reminder so it works with the phone out of range, but the schedule (on/off and
 * quiet hours) stays the phone's setting, so there's one place to change it.
 *
 * Published as a single Data Layer item that the watch caches. Only re-published
 * when a setting actually changes.
 */
object WatchSettingsPublisher {

    const val PATH = "/settings"
    const val KEY_ENABLED = "remindersEnabled"
    const val KEY_QUIET_ENABLED = "quietHoursEnabled"
    const val KEY_QUIET_START = "quietStartHour"
    const val KEY_QUIET_END = "quietEndHour"

    private data class Snapshot(
        val enabled: Boolean,
        val quietEnabled: Boolean,
        val quietStart: Int,
        val quietEnd: Int,
    )

    /** Call once from Application.onCreate. */
    fun start(context: Context) {
        val app = context.applicationContext
        CoroutineScope(SupervisorJob() + Dispatchers.Default).launch {
            SettingsStore(app).settings
                .map {
                    Snapshot(
                        enabled = it.remindersEnabled,
                        quietEnabled = it.quietHoursEnabled,
                        quietStart = it.quietStartHour,
                        quietEnd = it.quietEndHour,
                    )
                }
                .distinctUntilChanged()
                .collect { snap ->
                    try {
                        val req = PutDataMapRequest.create(PATH).apply {
                            dataMap.putBoolean(KEY_ENABLED, snap.enabled)
                            dataMap.putBoolean(KEY_QUIET_ENABLED, snap.quietEnabled)
                            dataMap.putInt(KEY_QUIET_START, snap.quietStart)
                            dataMap.putInt(KEY_QUIET_END, snap.quietEnd)
                        }.asPutDataRequest().setUrgent()
                        Wearable.getDataClient(app).putDataItem(req).await()
                    } catch (_: Exception) {
                        // No watch paired, or Play services busy: nothing to do, the
                        // next settings change (or app start) will publish again.
                    }
                }
        }
    }
}
