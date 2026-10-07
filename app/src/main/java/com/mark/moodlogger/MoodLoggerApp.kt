package com.mark.moodlogger

import android.app.Application
import com.mark.moodlogger.notification.Notifications
import com.mark.moodlogger.wear.WatchSettingsPublisher

class MoodLoggerApp : Application() {
    override fun onCreate() {
        super.onCreate()
        Notifications.ensureChannel(this)
        Notifications.ensureShoppingChannel(this)
        Notifications.ensureCheckinChannel(this)
        Notifications.ensureMedChannel(this)
        // v3.27: keep the watch's reminder schedule in step with this phone's settings.
        WatchSettingsPublisher.start(this)
    }
}
