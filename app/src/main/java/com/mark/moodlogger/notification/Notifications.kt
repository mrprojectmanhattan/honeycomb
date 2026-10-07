package com.mark.moodlogger.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.mark.moodlogger.R
import com.mark.moodlogger.ui.MainActivity

object Notifications {

    const val CHANNEL_ID = "mood_reminders"
    const val EXTRA_FROM_NOTIFICATION = "from_notification"
    private const val NOTIFICATION_ID = 1001

    /** Bumped id if the channel's sound/importance ever needs to change. */
    const val TIMER_CHANNEL_ID = "timer_alarm_v1"
    const val EXTRA_OPEN_TIMER = "open_timer"
    private const val TIMER_NOTIFICATION_ID = 1002

    const val SHOPPING_CHANNEL_ID = "shopping_reminders_v1"
    const val EXTRA_OPEN_SHOPPING = "open_shopping"
    private const val SHOPPING_NOTIFICATION_ID = 1003

    const val SESSION_RECAP_CHANNEL_ID = "session_recap_v1"
    const val EXTRA_OPEN_JOURNAL_RECAP = "open_journal_recap"
    private const val SESSION_RECAP_NOTIFICATION_ID = 1004

    const val CHECKIN_CHANNEL_ID = "checkin_reminders_v1"
    private const val CHECKIN_NOTIFICATION_ID = 1005

    const val MED_CHANNEL_ID = "med_reminders_v1"
    const val EXTRA_OPEN_CARE = "open_care"
    private const val MED_NOTIFICATION_ID = 1006

    fun ensureChannel(context: Context) {
        val mgr = context.getSystemService(NotificationManager::class.java) ?: return
        if (mgr.getNotificationChannel(CHANNEL_ID) == null) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.channel_name),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = context.getString(R.string.channel_desc)
            }
            mgr.createNotificationChannel(channel)
        }
    }

    fun ensureTimerChannel(context: Context) {
        val mgr = context.getSystemService(NotificationManager::class.java) ?: return
        if (mgr.getNotificationChannel(TIMER_CHANNEL_ID) == null) {
            val channel = NotificationChannel(
                TIMER_CHANNEL_ID,
                context.getString(R.string.timer_channel_name),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = context.getString(R.string.timer_channel_desc)
                val alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                    ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                val attrs = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
                setSound(alarmSound, attrs)
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 500, 250, 500, 250, 500)
            }
            mgr.createNotificationChannel(channel)
        }
    }

    fun ensureShoppingChannel(context: Context) {
        val mgr = context.getSystemService(NotificationManager::class.java) ?: return
        if (mgr.getNotificationChannel(SHOPPING_CHANNEL_ID) == null) {
            val channel = NotificationChannel(
                SHOPPING_CHANNEL_ID,
                context.getString(R.string.shopping_channel_name),
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = context.getString(R.string.shopping_channel_desc)
            }
            mgr.createNotificationChannel(channel)
        }
    }

    fun ensureSessionRecapChannel(context: Context) {
        val mgr = context.getSystemService(NotificationManager::class.java) ?: return
        if (mgr.getNotificationChannel(SESSION_RECAP_CHANNEL_ID) == null) {
            val channel = NotificationChannel(
                SESSION_RECAP_CHANNEL_ID,
                context.getString(R.string.session_recap_channel_name),
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = context.getString(R.string.session_recap_channel_desc)
            }
            mgr.createNotificationChannel(channel)
        }
    }

    fun ensureCheckinChannel(context: Context) {
        val mgr = context.getSystemService(NotificationManager::class.java) ?: return
        if (mgr.getNotificationChannel(CHECKIN_CHANNEL_ID) == null) {
            val channel = NotificationChannel(
                CHECKIN_CHANNEL_ID,
                context.getString(R.string.checkin_channel_name),
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = context.getString(R.string.checkin_channel_desc)
            }
            mgr.createNotificationChannel(channel)
        }
    }

    fun ensureMedChannel(context: Context) {
        val mgr = context.getSystemService(NotificationManager::class.java) ?: return
        if (mgr.getNotificationChannel(MED_CHANNEL_ID) == null) {
            val channel = NotificationChannel(
                MED_CHANNEL_ID,
                context.getString(R.string.med_channel_name),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = context.getString(R.string.med_channel_desc)
            }
            mgr.createNotificationChannel(channel)
        }
    }

    fun showCheckinReminder(context: Context) {
        ensureCheckinChannel(context)

        val tapIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_OPEN_CARE, true)
        }
        val contentPi = PendingIntent.getActivity(
            context,
            4,
            tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHECKIN_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_mood)
            .setContentTitle(context.getString(R.string.checkin_title))
            .setContentText(context.getString(R.string.checkin_text))
            .setStyle(NotificationCompat.BigTextStyle().bigText(context.getString(R.string.checkin_text)))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setContentIntent(contentPi)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(CHECKIN_NOTIFICATION_ID, notification)
        } catch (_: SecurityException) {
            // POST_NOTIFICATIONS was revoked. Nothing to do.
        }
    }

    /** [medNames] is every medication due at this fired minute (usually one, but two
     *  meds sharing a time land in the same notification rather than stacking). */
    fun showMedReminder(context: Context, medNames: List<String>) {
        if (medNames.isEmpty()) return
        ensureMedChannel(context)

        val tapIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_OPEN_CARE, true)
        }
        val contentPi = PendingIntent.getActivity(
            context,
            5,
            tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = if (medNames.size == 1) "Time for ${medNames[0]}" else "Medications due"
        val body = medNames.joinToString(", ")

        val notification = NotificationCompat.Builder(context, MED_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_mood)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setContentIntent(contentPi)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(MED_NOTIFICATION_ID, notification)
        } catch (_: SecurityException) {
            // POST_NOTIFICATIONS was revoked. Nothing to do.
        }
    }

    /**
     * [itemNames] is every outstanding item, oldest first. When [escalated] the
     * notification leads with [oldestName] and how long it's been sitting, and
     * goes out at high priority.
     */
    fun showShoppingReminder(
        context: Context,
        itemNames: List<String>,
        oldestName: String?,
        oldestDays: Int,
        escalated: Boolean,
    ) {
        if (itemNames.isEmpty()) return
        ensureShoppingChannel(context)

        val tapIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_OPEN_SHOPPING, true)
        }
        val contentPi = PendingIntent.getActivity(
            context,
            2,
            tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val list = summariseItems(itemNames)
        val title: String
        val body: String
        if (escalated && oldestName != null) {
            title = "Still need to grab this"
            val age = when (oldestDays) {
                0, 1 -> "since yesterday"
                else -> "for $oldestDays days"
            }
            body = "\"$oldestName\" has been on your list $age. Still on it: $list"
        } else {
            title = if (itemNames.size == 1) {
                "1 thing on your shopping list"
            } else {
                "${itemNames.size} things on your shopping list"
            }
            body = list
        }

        val notification = NotificationCompat.Builder(context, SHOPPING_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_mood)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(if (escalated) NotificationCompat.PRIORITY_HIGH else NotificationCompat.PRIORITY_DEFAULT)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setContentIntent(contentPi)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(SHOPPING_NOTIFICATION_ID, notification)
        } catch (_: SecurityException) {
            // POST_NOTIFICATIONS was revoked. Nothing to do.
        }
    }

    private fun summariseItems(names: List<String>): String {
        if (names.size <= 4) return names.joinToString(", ")
        return names.take(3).joinToString(", ") + ", and ${names.size - 3} more"
    }

    fun showMoodReminder(context: Context) {
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
            .setContentIntent(contentPi)
            // v3.27: the watch runs its own hourly reminder, so don't also mirror
            // this one to the wrist (it would buzz twice at the top of the hour).
            .setLocalOnly(true)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
        } catch (_: SecurityException) {
            // POST_NOTIFICATIONS was revoked between the check and here. Nothing to do.
        }
    }

    fun showSessionRecapReminder(context: Context) {
        ensureSessionRecapChannel(context)

        val tapIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_OPEN_JOURNAL_RECAP, true)
        }
        val contentPi = PendingIntent.getActivity(
            context,
            3,
            tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, SESSION_RECAP_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_mood)
            .setContentTitle(context.getString(R.string.session_recap_title))
            .setContentText(context.getString(R.string.session_recap_text))
            .setStyle(NotificationCompat.BigTextStyle().bigText(context.getString(R.string.session_recap_text)))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setContentIntent(contentPi)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(SESSION_RECAP_NOTIFICATION_ID, notification)
        } catch (_: SecurityException) {
            // POST_NOTIFICATIONS was revoked. Nothing to do.
        }
    }

    fun showTimerAlarm(context: Context, label: String) {
        ensureTimerChannel(context)

        val tapIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_OPEN_TIMER, true)
        }
        val contentPi = PendingIntent.getActivity(
            context,
            1,
            tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val text = if (label.isBlank()) "Time's up." else label
        val notification = NotificationCompat.Builder(context, TIMER_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_mood)
            .setContentTitle("Timer done")
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setContentIntent(contentPi)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(TIMER_NOTIFICATION_ID, notification)
        } catch (_: SecurityException) {
            // POST_NOTIFICATIONS was revoked. Nothing to do.
        }
    }
}
