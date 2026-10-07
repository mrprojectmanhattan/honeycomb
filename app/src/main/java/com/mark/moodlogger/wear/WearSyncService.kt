package com.mark.moodlogger.wear

import android.content.Context
import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.Wearable
import com.google.android.gms.wearable.WearableListenerService
import com.mark.moodlogger.data.MoodDatabase
import com.mark.moodlogger.data.MoodEntry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

/**
 * Receives moods logged on the watch (v3.27).
 *
 * The watch writes each mood as a Data Layer item at `/mood/<uuid>`. The Data
 * Layer buffers those on the watch while the phone is out of range, so a shift
 * spent with the phone in a locker still ends with every tap arriving here once
 * the two are near each other again. This service is started by the system when
 * an item lands; it is not an always-on service.
 *
 * Idempotent: an item is only ever inserted once, even if Google Play services
 * redelivers it, because processed ids are remembered.
 */
class WearSyncService : WearableListenerService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onDataChanged(events: DataEventBuffer) {
        // The buffer is only valid inside this callback, so copy what we need out.
        val incoming = events
            .filter { it.type == DataEvent.TYPE_CHANGED && it.dataItem.uri.path?.startsWith(PATH_PREFIX) == true }
            .map { event ->
                val map = DataMapItem.fromDataItem(event.dataItem).dataMap
                Incoming(
                    uri = event.dataItem.uri,
                    id = event.dataItem.uri.lastPathSegment ?: "",
                    timestamp = map.getLong(KEY_TS),
                    score = map.getInt(KEY_SCORE),
                    onSchedule = map.getBoolean(KEY_ON_SCHEDULE),
                    promptAt = map.getLong(KEY_PROMPT_AT),
                )
            }
        if (incoming.isEmpty()) return

        val app = applicationContext
        scope.launch {
            val dao = MoodDatabase.get(app).moodDao()
            val seen = SeenIds(app)
            for (item in incoming) {
                if (item.id.isBlank() || item.score !in 1..5 || item.timestamp <= 0L) {
                    // Malformed; drop it so it can't wedge the queue.
                    Wearable.getDataClient(app).deleteDataItems(item.uri).await()
                    continue
                }
                if (!seen.contains(item.id)) {
                    dao.insert(
                        MoodEntry(
                            timestamp = item.timestamp,
                            score = item.score,
                            source = if (item.onSchedule) MoodEntry.SOURCE_REMINDER else MoodEntry.SOURCE_MANUAL,
                        )
                    )
                    // A watch log answers the phone's own silent prompt for that hour,
                    // so a shift with the phone away doesn't read as a run of misses.
                    if (item.onSchedule && item.promptAt > 0L) {
                        dao.markPromptsAnsweredBetween(
                            item.promptAt - PROMPT_MATCH_MS,
                            item.promptAt + PROMPT_MATCH_MS,
                        )
                    }
                    seen.add(item.id)
                }
                // Clean up so the Data Layer doesn't keep a growing pile of moods.
                Wearable.getDataClient(app).deleteDataItems(item.uri).await()
            }
        }
    }

    private data class Incoming(
        val uri: android.net.Uri,
        val id: String,
        val timestamp: Long,
        val score: Int,
        val onSchedule: Boolean,
        val promptAt: Long,
    )

    /** Remembers the most recent processed ids so a redelivery can't double-insert. */
    private class SeenIds(context: Context) {
        private val prefs = context.getSharedPreferences("wear_sync", Context.MODE_PRIVATE)

        fun contains(id: String): Boolean = ids().contains(id)

        fun add(id: String) {
            val list = (ids() + id).takeLast(MAX_REMEMBERED)
            prefs.edit().putString(KEY_IDS, list.joinToString(",")).apply()
        }

        private fun ids(): List<String> =
            prefs.getString(KEY_IDS, "").orEmpty().split(",").filter { it.isNotBlank() }
    }

    companion object {
        const val PATH_PREFIX = "/mood/"
        const val KEY_TS = "ts"
        const val KEY_SCORE = "score"
        const val KEY_ON_SCHEDULE = "onSchedule"
        const val KEY_PROMPT_AT = "promptAt"

        private const val KEY_IDS = "seen_ids"
        private const val MAX_REMEMBERED = 300

        /** The phone's own alarm and the watch's fire within seconds of the same
         *  top of the hour; two minutes either side is generous. */
        private const val PROMPT_MATCH_MS = 2 * 60 * 1000L
    }
}
