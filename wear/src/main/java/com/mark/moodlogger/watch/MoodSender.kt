package com.mark.moodlogger.watch

import android.content.Context
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable
import java.util.UUID

/**
 * Sends one mood to the phone.
 *
 * Each mood is written to the Data Layer as its own item, `/mood/<uuid>`. The
 * Data Layer stores it on the watch and delivers it to the phone whenever the
 * two can talk, so a tap made with the phone in a locker is not lost, it waits.
 * A short local copy is also kept on the watch as a safety net.
 */
object MoodSender {

    // Must match the phone's WearSyncService.
    private const val PATH_PREFIX = "/mood/"
    private const val KEY_TS = "ts"
    private const val KEY_SCORE = "score"
    private const val KEY_ON_SCHEDULE = "onSchedule"
    private const val KEY_PROMPT_AT = "promptAt"

    /** Returns whether it counted as on-schedule. */
    fun send(context: Context, score: Int, fromNotification: Boolean): Boolean {
        val app = context.applicationContext
        val now = System.currentTimeMillis()
        val lastFired = WatchPrefs.lastReminderFiredAt(app)
        val withinWindow = lastFired > 0 && now - lastFired <= WatchPrefs.ON_SCHEDULE_WINDOW_MS
        val onSchedule = fromNotification || withinWindow

        WatchPrefs.appendLocalLog(app, now, score, onSchedule)

        val request = PutDataMapRequest.create(PATH_PREFIX + UUID.randomUUID().toString()).apply {
            dataMap.putLong(KEY_TS, now)
            dataMap.putInt(KEY_SCORE, score)
            dataMap.putBoolean(KEY_ON_SCHEDULE, onSchedule)
            // Lets the phone close out its own silent prompt for that hour.
            dataMap.putLong(KEY_PROMPT_AT, if (onSchedule) lastFired else 0L)
        }.asPutDataRequest().setUrgent()

        Wearable.getDataClient(app).putDataItem(request)
        return onSchedule
    }
}
