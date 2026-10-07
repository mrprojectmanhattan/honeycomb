package com.mark.moodlogger.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

data class Settings(
    val remindersEnabled: Boolean = true,
    val quietHoursEnabled: Boolean = true,
    /** hour of day 0..23, inclusive start */
    val quietStartHour: Int = 22,
    /** hour of day 0..23, exclusive end */
    val quietEndHour: Int = 8,
    /** label for the active countdown timer, blank if none */
    val timerLabel: String = "",
    /** epoch millis when the active timer fires, 0 if no timer is running */
    val timerTriggerAt: Long = 0L,
    /** where the main-screen menu sits: one of the NAV_* values below */
    val navPosition: String = NAV_TOP,
    /** for a left/right side menu, where the icons sit vertically: RAIL_* below */
    val navRailAlign: String = RAIL_CENTER,
    /** daily water target, fluid ounces */
    val waterGoalOz: Int = 64,
    /** how many ounces one water tap adds */
    val waterServingOz: Int = 8,
    /** opt-in: guess sleep times from charger connect/disconnect. Off by default. */
    val sleepAssistEnabled: Boolean = false,
    /** nightly sleep target, hours */
    val sleepGoalHours: Int = 8,
    /** last time the charger was connected, epoch millis (0 = never). Assist only. */
    val lastPluggedInAt: Long = 0L,
    /** last time the charger was disconnected, epoch millis (0 = never). Assist only. */
    val lastUnpluggedAt: Long = 0L,
    /** opt-in: read step count from the phone's hardware step sensor. Off by default. */
    val stepCountEnabled: Boolean = false,
    /** epoch millis the hourly reminder last actually fired a notification (0 = never).
     *  A mood logged within 5 min of this counts as on-schedule. */
    val lastReminderFiredAt: Long = 0L,
    /** true once the one-time re-derive of entry sources by timestamp has run. */
    val sourceRederiveDone: Boolean = false,
    /** which option the home-screen user-choice widget shows (ui/HomeWidget ids). */
    val homeWidget: String = "snapshot",
    /** daily nudge about anything still on the shopping list. */
    val shoppingRemindersEnabled: Boolean = true,
    /** hour of day 0..23 the shopping nudge fires. */
    val shoppingReminderHour: Int = 18,
    /** once an item has sat unbought this many days, the nudge escalates. */
    val shoppingEscalateAfterDays: Int = 4,
    /** drug allergies, free text — shown on the Care health-summary screen. */
    val allergies: String = "",
    /** daily calorie target for the Nutrition screen. 2000 is a placeholder, not a
     *  recommendation - Mark sets his own real number. Added 2026-09-28. */
    val dailyCalorieBudget: Int = 2000,
    /** opt-in: require a PIN or biometric to open the app. Off by default so a fresh
     *  install doesn't lock him out before he's set a PIN. See AppLock.kt. Added 2026-09-28. */
    val appLockEnabled: Boolean = false,
    /** SHA-256 hex digest of the PIN, salted with a random per-install value below -
     *  never the plain PIN. Empty means no PIN has been set yet. Added 2026-09-28. */
    val appLockPinHash: String = "",
    /** Random per-install salt, generated once the first time a PIN is set. Added 2026-09-28. */
    val appLockSalt: String = "",
    /** opt-in: also allow fingerprint/face unlock instead of typing the PIN. Only
     *  offered once a PIN exists, as the PIN fallback. Added 2026-09-28. */
    val appLockBiometricEnabled: Boolean = false,
    /** Weekly nudge to do a PHQ-9/GAD-7 check-in. Off by default, new 2026-09-28. */
    val checkinRemindersEnabled: Boolean = false,
) {
    companion object {
        const val NAV_TOP = "top"
        const val NAV_BOTTOM = "bottom"
        const val NAV_LEFT = "left"
        const val NAV_RIGHT = "right"

        const val RAIL_TOP = "top"
        const val RAIL_CENTER = "center"
        const val RAIL_BOTTOM = "bottom"

        const val WATER_GOAL_MIN = 8
        const val WATER_GOAL_MAX = 256
        val WATER_SERVINGS = listOf(8, 16, 32)
    }
}

class SettingsStore(private val context: Context) {

    private object Keys {
        val ENABLED = booleanPreferencesKey("reminders_enabled")
        val QUIET_ENABLED = booleanPreferencesKey("quiet_enabled")
        val QUIET_START = intPreferencesKey("quiet_start")
        val QUIET_END = intPreferencesKey("quiet_end")
        val TIMER_LABEL = stringPreferencesKey("timer_label")
        val TIMER_TRIGGER_AT = longPreferencesKey("timer_trigger_at")
        val NAV_POSITION = stringPreferencesKey("nav_position")
        val NAV_RAIL_ALIGN = stringPreferencesKey("nav_rail_align")
        val WATER_GOAL = intPreferencesKey("water_goal_oz")
        val WATER_SERVING = intPreferencesKey("water_serving_oz")
        val SLEEP_ASSIST = booleanPreferencesKey("sleep_assist_enabled")
        val SLEEP_GOAL = intPreferencesKey("sleep_goal_hours")
        val PLUGGED_IN_AT = longPreferencesKey("last_plugged_in_at")
        val UNPLUGGED_AT = longPreferencesKey("last_unplugged_at")
        val STEP_COUNT = booleanPreferencesKey("step_count_enabled")
        val LAST_REMINDER_FIRED = longPreferencesKey("last_reminder_fired_at")
        val SOURCE_REDERIVE_DONE = booleanPreferencesKey("source_rederive_done")
        val HOME_WIDGET = stringPreferencesKey("home_widget")
        val SHOPPING_ENABLED = booleanPreferencesKey("shopping_reminders_enabled")
        val SHOPPING_HOUR = intPreferencesKey("shopping_reminder_hour")
        val SHOPPING_ESCALATE_DAYS = intPreferencesKey("shopping_escalate_after_days")
        val ALLERGIES = stringPreferencesKey("allergies")
        val DAILY_CALORIE_BUDGET = intPreferencesKey("daily_calorie_budget")
        val APP_LOCK_ENABLED = booleanPreferencesKey("app_lock_enabled")
        val APP_LOCK_PIN_HASH = stringPreferencesKey("app_lock_pin_hash")
        val APP_LOCK_SALT = stringPreferencesKey("app_lock_salt")
        val APP_LOCK_BIOMETRIC = booleanPreferencesKey("app_lock_biometric_enabled")
        val CHECKIN_REMINDERS = booleanPreferencesKey("checkin_reminders_enabled")
    }

    val settings: Flow<Settings> = context.dataStore.data.map { p ->
        Settings(
            remindersEnabled = p[Keys.ENABLED] ?: true,
            quietHoursEnabled = p[Keys.QUIET_ENABLED] ?: true,
            quietStartHour = p[Keys.QUIET_START] ?: 22,
            quietEndHour = p[Keys.QUIET_END] ?: 8,
            timerLabel = p[Keys.TIMER_LABEL] ?: "",
            timerTriggerAt = p[Keys.TIMER_TRIGGER_AT] ?: 0L,
            navPosition = p[Keys.NAV_POSITION] ?: Settings.NAV_TOP,
            navRailAlign = p[Keys.NAV_RAIL_ALIGN] ?: Settings.RAIL_CENTER,
            waterGoalOz = p[Keys.WATER_GOAL] ?: 64,
            waterServingOz = p[Keys.WATER_SERVING] ?: 8,
            sleepAssistEnabled = p[Keys.SLEEP_ASSIST] ?: false,
            sleepGoalHours = p[Keys.SLEEP_GOAL] ?: 8,
            lastPluggedInAt = p[Keys.PLUGGED_IN_AT] ?: 0L,
            lastUnpluggedAt = p[Keys.UNPLUGGED_AT] ?: 0L,
            stepCountEnabled = p[Keys.STEP_COUNT] ?: false,
            lastReminderFiredAt = p[Keys.LAST_REMINDER_FIRED] ?: 0L,
            sourceRederiveDone = p[Keys.SOURCE_REDERIVE_DONE] ?: false,
            homeWidget = p[Keys.HOME_WIDGET] ?: "snapshot",
            shoppingRemindersEnabled = p[Keys.SHOPPING_ENABLED] ?: true,
            shoppingReminderHour = p[Keys.SHOPPING_HOUR] ?: 18,
            shoppingEscalateAfterDays = p[Keys.SHOPPING_ESCALATE_DAYS] ?: 4,
            allergies = p[Keys.ALLERGIES] ?: "",
            dailyCalorieBudget = p[Keys.DAILY_CALORIE_BUDGET] ?: 2000,
            appLockEnabled = p[Keys.APP_LOCK_ENABLED] ?: false,
            appLockPinHash = p[Keys.APP_LOCK_PIN_HASH] ?: "",
            appLockSalt = p[Keys.APP_LOCK_SALT] ?: "",
            appLockBiometricEnabled = p[Keys.APP_LOCK_BIOMETRIC] ?: false,
            checkinRemindersEnabled = p[Keys.CHECKIN_REMINDERS] ?: false,
        )
    }

    suspend fun setRemindersEnabled(value: Boolean) {
        context.dataStore.edit { it[Keys.ENABLED] = value }
    }

    suspend fun setQuietHoursEnabled(value: Boolean) {
        context.dataStore.edit { it[Keys.QUIET_ENABLED] = value }
    }

    suspend fun setQuietStart(hour: Int) {
        context.dataStore.edit { it[Keys.QUIET_START] = hour.coerceIn(0, 23) }
    }

    suspend fun setQuietEnd(hour: Int) {
        context.dataStore.edit { it[Keys.QUIET_END] = hour.coerceIn(0, 23) }
    }

    suspend fun setTimer(label: String, triggerAt: Long) {
        context.dataStore.edit {
            it[Keys.TIMER_LABEL] = label
            it[Keys.TIMER_TRIGGER_AT] = triggerAt
        }
    }

    suspend fun clearTimer() {
        context.dataStore.edit {
            it.remove(Keys.TIMER_LABEL)
            it.remove(Keys.TIMER_TRIGGER_AT)
        }
    }

    suspend fun setNavPosition(value: String) {
        context.dataStore.edit { it[Keys.NAV_POSITION] = value }
    }

    suspend fun setNavRailAlign(value: String) {
        context.dataStore.edit { it[Keys.NAV_RAIL_ALIGN] = value }
    }

    suspend fun setWaterGoal(oz: Int) {
        context.dataStore.edit {
            it[Keys.WATER_GOAL] = oz.coerceIn(Settings.WATER_GOAL_MIN, Settings.WATER_GOAL_MAX)
        }
    }

    suspend fun setWaterServing(oz: Int) {
        context.dataStore.edit { it[Keys.WATER_SERVING] = oz.coerceAtLeast(1) }
    }

    suspend fun setSleepAssist(enabled: Boolean) {
        context.dataStore.edit { it[Keys.SLEEP_ASSIST] = enabled }
    }

    suspend fun setSleepGoal(hours: Int) {
        context.dataStore.edit { it[Keys.SLEEP_GOAL] = hours.coerceIn(3, 14) }
    }

    suspend fun recordPluggedIn(atMillis: Long) {
        context.dataStore.edit { it[Keys.PLUGGED_IN_AT] = atMillis }
    }

    suspend fun recordUnplugged(atMillis: Long) {
        context.dataStore.edit { it[Keys.UNPLUGGED_AT] = atMillis }
    }

    suspend fun setStepCountEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.STEP_COUNT] = enabled }
    }

    suspend fun recordReminderFired(atMillis: Long) {
        context.dataStore.edit { it[Keys.LAST_REMINDER_FIRED] = atMillis }
    }

    suspend fun setSourceRederiveDone() {
        context.dataStore.edit { it[Keys.SOURCE_REDERIVE_DONE] = true }
    }

    suspend fun setHomeWidget(value: String) {
        context.dataStore.edit { it[Keys.HOME_WIDGET] = value }
    }

    suspend fun setShoppingRemindersEnabled(value: Boolean) {
        context.dataStore.edit { it[Keys.SHOPPING_ENABLED] = value }
    }

    suspend fun setShoppingReminderHour(hour: Int) {
        context.dataStore.edit { it[Keys.SHOPPING_HOUR] = hour.coerceIn(0, 23) }
    }

    suspend fun setShoppingEscalateAfterDays(days: Int) {
        context.dataStore.edit { it[Keys.SHOPPING_ESCALATE_DAYS] = days.coerceIn(1, 30) }
    }

    suspend fun setAllergies(value: String) {
        context.dataStore.edit { it[Keys.ALLERGIES] = value }
    }

    suspend fun setDailyCalorieBudget(calories: Int) {
        context.dataStore.edit { it[Keys.DAILY_CALORIE_BUDGET] = calories.coerceIn(800, 8000) }
    }

    /** Sets a new PIN: generates a fresh salt and stores only the salted hash. */
    suspend fun setAppLockPin(pin: String) {
        val salt = AppLock.newSalt()
        context.dataStore.edit {
            it[Keys.APP_LOCK_SALT] = salt
            it[Keys.APP_LOCK_PIN_HASH] = AppLock.hashPin(pin, salt)
            it[Keys.APP_LOCK_ENABLED] = true
        }
    }

    suspend fun clearAppLock() {
        context.dataStore.edit {
            it.remove(Keys.APP_LOCK_PIN_HASH)
            it.remove(Keys.APP_LOCK_SALT)
            it[Keys.APP_LOCK_ENABLED] = false
            it[Keys.APP_LOCK_BIOMETRIC] = false
        }
    }

    suspend fun setAppLockBiometricEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.APP_LOCK_BIOMETRIC] = enabled }
    }

    suspend fun setCheckinRemindersEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.CHECKIN_REMINDERS] = enabled }
    }
}
