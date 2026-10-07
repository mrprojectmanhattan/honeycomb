package com.mark.moodlogger.watch

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import java.util.Calendar

/** Fires at the top of each hour: buzzes the wrist unless reminders are off or it's a quiet hour. */
class ReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val app = context.applicationContext
        try {
            val schedule = WatchPrefs.schedule(app)
            val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
            if (schedule.enabled && !schedule.isQuiet(hour)) {
                val firedAt = System.currentTimeMillis()
                // Stamp the fire so a mood logged in the next 5 minutes counts as
                // on-schedule, exactly like the phone.
                WatchPrefs.recordReminderFired(app, firedAt)
                show(app)
            }
        } finally {
            // Always re-arm, even when silent, so the chain never breaks.
            ReminderScheduler.scheduleNextTopOfHour(app)
        }
    }

    companion object {
        // Own channel ID, distinct from the phone's "mood_reminders" — the two apps
        // share a package name (required for the Data Layer), and Wear OS was
        // treating same-named channels as the same notification, defaulting its
        // tap action to "Open on phone" instead of opening locally on the watch.
        const val CHANNEL_ID = "watch_mood_reminders"
        const val EXTRA_FROM_NOTIFICATION = "from_notification"
        private const val NOTIFICATION_ID = 2001

        /** A stale reminder shouldn't sit on the wrist all afternoon. */
        private const val TIMEOUT_MS = 50 * 60 * 1000L

        fun ensureChannel(context: Context) {
            val mgr = context.getSystemService(NotificationManager::class.java) ?: return
            if (mgr.getNotificationChannel(CHANNEL_ID) == null) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    context.getString(R.string.channel_name),
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = context.getString(R.string.channel_desc)
                    enableVibration(true)
                    vibrationPattern = longArrayOf(0, 180, 120, 180)
                }
                mgr.createNotificationChannel(channel)
            }
        }

        private fun show(context: Context) {
            ensureChannel(context)

            val tapIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra(EXTRA_FROM_NOTIFICATION, true)
            }
            val contentPi = PendingIntent.getActivity(
                context,
                0,
                tapIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_stat_mood)
                .setContentTitle(context.getString(R.string.reminder_title))
                .setContentText(context.getString(R.string.reminder_text))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_REMINDER)
                .setAutoCancel(true)
                .setTimeoutAfter(TIMEOUT_MS)
                .setContentIntent(contentPi)
                // Tell Wear OS this notification belongs to the watch only, so it
                // opens the watch's own app instead of offering "Open on phone".
                .setLocalOnly(true)
                .build()

            try {
                NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
            } catch (_: SecurityException) {
                // POST_NOTIFICATIONS not granted on the watch yet. The app asks on
                // first open; nothing to do here.
            }
        }
    }
}
