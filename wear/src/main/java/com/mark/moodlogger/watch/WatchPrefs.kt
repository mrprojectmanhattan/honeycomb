package com.mark.moodlogger.watch

import android.content.Context

/**
 * Everything the watch remembers, in one small SharedPreferences file.
 *
 * The phone stays the source of truth for moods; the watch only holds the
 * reminder schedule the phone last told it, when its own reminder last fired,
 * and a short local safety log of what was logged here.
 */
object WatchPrefs {

    private const val FILE = "watch_prefs"

    // Same defaults as the phone's SettingsStore, so a watch that has never heard
    // from the phone still behaves sensibly.
    private const val KEY_ENABLED = "remindersEnabled"
    private const val KEY_QUIET_ENABLED = "quietHoursEnabled"
    private const val KEY_QUIET_START = "quietStartHour"
    private const val KEY_QUIET_END = "quietEndHour"
    private const val KEY_LAST_FIRED = "lastReminderFiredAt"
    private const val KEY_LOCAL_LOG = "local_log"

    private const val MAX_LOG = 60

    /** A mood logged within this long of a reminder firing counts as on-schedule
     *  (matches the phone's MoodEntry.ON_SCHEDULE_WINDOW_MS). */
    const val ON_SCHEDULE_WINDOW_MS = 5 * 60 * 1000L

    data class Schedule(
        val enabled: Boolean,
        val quietEnabled: Boolean,
        val quietStart: Int,
        val quietEnd: Int,
    ) {
        fun isQuiet(hour: Int): Boolean {
            if (!quietEnabled) return false
            return when {
                quietStart == quietEnd -> false
                quietStart < quietEnd -> hour in quietStart until quietEnd
                else -> hour >= quietStart || hour < quietEnd // wraps past midnight
            }
        }
    }

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    fun schedule(context: Context): Schedule {
        val p = prefs(context)
        return Schedule(
            enabled = p.getBoolean(KEY_ENABLED, true),
            quietEnabled = p.getBoolean(KEY_QUIET_ENABLED, true),
            quietStart = p.getInt(KEY_QUIET_START, 22),
            quietEnd = p.getInt(KEY_QUIET_END, 8),
        )
    }

    fun saveSchedule(context: Context, s: Schedule) {
        prefs(context).edit()
            .putBoolean(KEY_ENABLED, s.enabled)
            .putBoolean(KEY_QUIET_ENABLED, s.quietEnabled)
            .putInt(KEY_QUIET_START, s.quietStart)
            .putInt(KEY_QUIET_END, s.quietEnd)
            .apply()
    }

    fun lastReminderFiredAt(context: Context): Long = prefs(context).getLong(KEY_LAST_FIRED, 0L)

    fun recordReminderFired(context: Context, atMillis: Long) {
        prefs(context).edit().putLong(KEY_LAST_FIRED, atMillis).apply()
    }

    /** Local safety copy of the last moods logged on the watch: "epochMillis:score:onSchedule". */
    fun appendLocalLog(context: Context, timestamp: Long, score: Int, onSchedule: Boolean) {
        val p = prefs(context)
        val entries = p.getString(KEY_LOCAL_LOG, "").orEmpty().split(";").filter { it.isNotBlank() }
        val next = (entries + "$timestamp:$score:${if (onSchedule) 1 else 0}").takeLast(MAX_LOG)
        p.edit().putString(KEY_LOCAL_LOG, next.joinToString(";")).apply()
    }
}
