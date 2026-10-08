package com.mark.moodlogger.ui

import android.app.Application
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.mark.moodlogger.MoodScale
import com.mark.moodlogger.alarm.AlarmScheduler
import com.mark.moodlogger.alarm.SessionRecapScheduler
import com.mark.moodlogger.alarm.ShoppingReminderScheduler
import com.mark.moodlogger.alarm.TimerScheduler
import com.mark.moodlogger.data.EntryWithKeywords
import com.mark.moodlogger.data.FoodItem
import com.mark.moodlogger.data.Habit
import com.mark.moodlogger.data.Keyword
import com.mark.moodlogger.alarm.MedicationReminderScheduler
import com.mark.moodlogger.data.MentalHealthCheckin
import com.mark.moodlogger.data.MoodDatabase
import com.mark.moodlogger.data.MoodEntry
import com.mark.moodlogger.data.NutritionLogEntry
import com.mark.moodlogger.data.NutritionReferenceDatabase
import com.mark.moodlogger.data.PromptEvent
import com.mark.moodlogger.data.Settings
import com.mark.moodlogger.data.SettingsStore
import com.mark.moodlogger.data.CareProvider
import com.mark.moodlogger.data.Goal
import com.mark.moodlogger.data.GoalCheckin
import com.mark.moodlogger.data.JournalEntry
import com.mark.moodlogger.data.MajorGoal
import com.mark.moodlogger.data.Medication
import com.mark.moodlogger.data.ShoppingItem
import com.mark.moodlogger.data.SleepLog
import com.mark.moodlogger.data.StepDay
import com.mark.moodlogger.data.WaterLog
import com.mark.moodlogger.data.WorkoutLog
import com.mark.moodlogger.sensor.StepCounter
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class MainViewModel(app: Application) : AndroidViewModel(app) {

    private val dao = MoodDatabase.get(app).moodDao()
    private val settingsStore = SettingsStore(app)
    private val nutritionDao = MoodDatabase.get(app).nutritionLogDao()
    private val foodDao by lazy { NutritionReferenceDatabase.get(app).foodItemDao() }
    private val checkinDao = MoodDatabase.get(app).mentalHealthCheckinDao()

    val entries: StateFlow<List<EntryWithKeywords>> =
        dao.observeAllWithKeywords()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val keywords: StateFlow<List<Keyword>> =
        dao.observeKeywords()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val habits: StateFlow<List<Habit>> =
        dao.observeHabits()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val settings: StateFlow<Settings> =
        settingsStore.settings.stateIn(
            viewModelScope, SharingStarted.WhileSubscribed(5_000), Settings(),
        )

    /** Settings plus whether they've genuinely loaded from disk yet, as ONE value.
     *  [settings] above starts as the plain Settings() placeholder (appLockEnabled =
     *  false) until the real DataStore value loads - the first version of this fix
     *  (2026-10-07) exposed that as a second, separate StateFlow, but two independent
     *  StateFlows collected via two separate collectAsState() calls can still update a
     *  recomposition apart from each other, so "ready" could arrive one frame before
     *  the real settings did and the gate would briefly trust the stale placeholder
     *  anyway - reopening the exact cold-start exposure gap it was meant to close.
     *  AppLockGate must read both fields from this single flow, never [settings]
     *  directly, so there is only one thing to observe and no way for them to be seen
     *  out of sync. Flagged by an outside reviewer. */
    data class AppLockReadiness(val settings: Settings, val ready: Boolean)

    val appLockReadiness: StateFlow<AppLockReadiness> =
        settingsStore.settings.map { AppLockReadiness(it, ready = true) }
            .stateIn(
                viewModelScope, SharingStarted.WhileSubscribed(5_000),
                AppLockReadiness(Settings(), ready = false),
            )

    /** Ounces of water logged since midnight today. Recomputed on every change. */
    val waterToday: StateFlow<Int> =
        dao.observeWaterLogs()
            .map { logs ->
                val start = startOfTodayMillis()
                logs.filter { it.timestamp >= start }.sumOf { it.amountOz }
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    /** Muscle groups trained since Monday this week. */
    val workoutThisWeek: StateFlow<Set<String>> =
        dao.observeWorkoutLogs()
            .map { logs ->
                val start = startOfWeekMillis()
                logs.filter { it.timestamp >= start }.map { it.region }.toSet()
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())

    val sleepLogs: StateFlow<List<SleepLog>> =
        dao.observeSleepLogs()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val waterLogs: StateFlow<List<WaterLog>> =
        dao.observeWaterLogs()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val workoutLogs: StateFlow<List<WorkoutLog>> =
        dao.observeWorkoutLogs()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val goals: StateFlow<List<Goal>> =
        dao.observeGoals()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val goalCheckins: StateFlow<List<GoalCheckin>> =
        dao.observeCheckins()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val stepDays: StateFlow<List<StepDay>> =
        dao.observeStepDays()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val majorGoals: StateFlow<List<MajorGoal>> =
        dao.observeMajorGoals()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val journal: StateFlow<List<JournalEntry>> =
        dao.observeJournal()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val careProviders: StateFlow<List<CareProvider>> =
        dao.observeCareProviders()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val medications: StateFlow<List<Medication>> =
        dao.observeMedications()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val shoppingItems: StateFlow<List<ShoppingItem>> =
        dao.observeShoppingItems()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val promptEvents: StateFlow<List<PromptEvent>> =
        dao.observePromptEvents()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Unanswered, unexplained prompts, bundled into runs so a whole shift can be
     *  explained in one tap. A prompt still inside its on-schedule window is left
     *  out, since he may be about to answer it. */
    val openPromptGroups: StateFlow<List<List<PromptEvent>>> =
        dao.observeOpenPrompts()
            .map { open ->
                val cutoff = System.currentTimeMillis() - MoodEntry.ON_SCHEDULE_WINDOW_MS
                groupPrompts(open.filter { it.firedAt <= cutoff })
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun setPromptReason(group: List<PromptEvent>, reason: String) {
        if (group.isEmpty()) return
        viewModelScope.launch {
            dao.setPromptReason(group.map { it.id }, reason, System.currentTimeMillis())
        }
    }

    private fun groupPrompts(open: List<PromptEvent>): List<List<PromptEvent>> {
        if (open.isEmpty()) return emptyList()
        val groups = mutableListOf<List<PromptEvent>>()
        var current = mutableListOf(open.first())
        for (event in open.drop(1)) {
            if (event.firedAt - current.last().firedAt <= PromptEvent.GROUP_GAP_MS) {
                current.add(event)
            } else {
                groups.add(current)
                current = mutableListOf(event)
            }
        }
        groups.add(current)
        return groups
    }

    // ---- logging -------------------------------------------------------

    /** [fromNotification] = the log screen was opened by tapping the reminder.
     *  Source is on-schedule if that's true or if the reminder fired within the
     *  last 5 minutes; off-schedule otherwise. */
    fun logMood(score: Int, note: String, keywordIds: Set<Long>, fromNotification: Boolean) {
        viewModelScope.launch {
            val firedAt = settingsStore.settings.first().lastReminderFiredAt
            val withinWindow = firedAt > 0 &&
                System.currentTimeMillis() - firedAt <= MoodEntry.ON_SCHEDULE_WINDOW_MS
            val onSchedule = fromNotification || withinWindow
            val source = if (onSchedule) {
                MoodEntry.SOURCE_REMINDER
            } else {
                MoodEntry.SOURCE_MANUAL
            }
            dao.insertWithKeywords(
                MoodEntry(
                    timestamp = System.currentTimeMillis(),
                    score = score,
                    note = note.trim(),
                    source = source,
                ),
                keywordIds,
            )
            // Close out the prompt this log answers so it never shows as missed.
            // A notification tap answers the latest open prompt whenever it
            // fired; otherwise only the one still inside its window counts.
            if (onSchedule) {
                dao.markPromptAnswered(if (withinWindow) firedAt else 0L)
            }
        }
    }

    fun updateEntry(id: Long, timestamp: Long, source: String, score: Int, note: String, keywordIds: Set<Long>) {
        viewModelScope.launch {
            dao.updateWithKeywords(
                MoodEntry(
                    id = id,
                    timestamp = timestamp,
                    score = score,
                    note = note.trim(),
                    source = source,
                ),
                keywordIds,
            )
        }
    }

    fun deleteEntry(id: Long) {
        viewModelScope.launch { dao.deleteEntry(id) }
    }

    // ---- keyword management -------------------------------------------

    fun addKeyword(name: String) {
        val clean = name.trim().lowercase()
        if (clean.isBlank()) return
        viewModelScope.launch { dao.insertKeyword(Keyword(name = clean)) }
    }

    fun renameKeyword(id: Long, name: String) {
        val clean = name.trim().lowercase()
        if (clean.isBlank()) return
        viewModelScope.launch { dao.updateKeyword(Keyword(id = id, name = clean)) }
    }

    fun deleteKeyword(id: Long) {
        viewModelScope.launch { dao.deleteKeyword(id) }
    }

    // ---- habits ----------------------------------------------------

    fun addHabit(name: String, kind: String, startEpochDay: Long) {
        val clean = name.trim()
        if (clean.isBlank()) return
        viewModelScope.launch {
            dao.insertHabit(Habit(name = clean, kind = kind, startEpochDay = startEpochDay))
        }
    }

    fun updateHabit(id: Long, name: String, kind: String, startEpochDay: Long) {
        val clean = name.trim()
        if (clean.isBlank()) return
        viewModelScope.launch {
            dao.updateHabit(Habit(id = id, name = clean, kind = kind, startEpochDay = startEpochDay))
        }
    }

    fun deleteHabit(id: Long) {
        viewModelScope.launch { dao.deleteHabit(id) }
    }

    // ---- reminders ---------------------------------------------------

    fun setRemindersEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsStore.setRemindersEnabled(enabled)
            val ctx = getApplication<Application>()
            if (enabled) AlarmScheduler.scheduleNextTopOfHour(ctx) else AlarmScheduler.cancel(ctx)
        }
    }

    fun setQuietHoursEnabled(enabled: Boolean) {
        viewModelScope.launch { settingsStore.setQuietHoursEnabled(enabled) }
    }

    fun setQuietStart(hour: Int) {
        viewModelScope.launch { settingsStore.setQuietStart(hour) }
    }

    fun setQuietEnd(hour: Int) {
        viewModelScope.launch { settingsStore.setQuietEnd(hour) }
    }

    fun setNavPosition(value: String) {
        viewModelScope.launch { settingsStore.setNavPosition(value) }
    }

    fun setNavRailAlign(value: String) {
        viewModelScope.launch { settingsStore.setNavRailAlign(value) }
    }

    fun setHomeWidget(value: String) {
        viewModelScope.launch { settingsStore.setHomeWidget(value) }
    }

    // ---- water --------------------------------------------------------

    fun logWater(oz: Int) {
        if (oz <= 0) return
        viewModelScope.launch {
            dao.insertWater(WaterLog(timestamp = System.currentTimeMillis(), amountOz = oz))
        }
    }

    fun undoLastWater() {
        viewModelScope.launch {
            dao.latestWaterSince(startOfTodayMillis())?.let { dao.deleteWater(it.id) }
        }
    }

    fun setWaterGoal(oz: Int) {
        viewModelScope.launch { settingsStore.setWaterGoal(oz) }
    }

    fun setWaterServing(oz: Int) {
        viewModelScope.launch { settingsStore.setWaterServing(oz) }
    }

    // ---- workouts ----------------------------------------------------

    fun toggleWorkout(region: String, currentlyDone: Boolean) {
        viewModelScope.launch {
            if (currentlyDone) {
                dao.deleteWorkoutRegionSince(region, startOfWeekMillis())
            } else {
                dao.insertWorkout(
                    WorkoutLog(timestamp = System.currentTimeMillis(), region = region)
                )
            }
        }
    }

    // ---- sleep -------------------------------------------------------

    fun logSleep(
        dateEpochDay: Long,
        bedtimeMillis: Long,
        wakeMillis: Long,
        quality: Int,
        latencyMinutes: Int,
        nightWakings: Int,
        morningFeeling: String,
    ) {
        viewModelScope.launch {
            dao.upsertSleep(
                SleepLog(
                    dateEpochDay = dateEpochDay,
                    bedtimeMillis = bedtimeMillis,
                    wakeMillis = wakeMillis,
                    quality = quality,
                    latencyMinutes = latencyMinutes,
                    nightWakings = nightWakings,
                    morningFeeling = morningFeeling,
                )
            )
        }
    }

    fun deleteSleep(dateEpochDay: Long) {
        viewModelScope.launch { dao.deleteSleepForDay(dateEpochDay) }
    }

    fun setSleepAssist(enabled: Boolean) {
        viewModelScope.launch { settingsStore.setSleepAssist(enabled) }
    }

    fun setSleepGoal(hours: Int) {
        viewModelScope.launch { settingsStore.setSleepGoal(hours) }
    }

    // ---- goals -----------------------------------------------------

    fun addGoal(goal: Goal) {
        viewModelScope.launch { dao.insertGoal(goal.copy(createdAt = System.currentTimeMillis())) }
    }

    fun deleteGoal(id: Long) {
        viewModelScope.launch {
            dao.clearCheckins(id)
            dao.deleteGoal(id)
        }
    }

    fun checkInGoal(goalId: Long) {
        viewModelScope.launch {
            dao.addCheckin(GoalCheckin(goalId = goalId, timestamp = System.currentTimeMillis()))
        }
    }

    fun undoGoalCheckin(goalId: Long) {
        viewModelScope.launch { dao.undoLatestCheckin(goalId) }
    }

    // ---- major goals ---------------------------------------------------

    fun addMajorGoal(title: String, detail: String) {
        val t = title.trim()
        if (t.isBlank()) return
        viewModelScope.launch {
            dao.insertMajorGoal(
                MajorGoal(title = t, detail = detail.trim(), createdAt = System.currentTimeMillis())
            )
        }
    }

    fun updateMajorGoal(goal: MajorGoal) {
        val t = goal.title.trim()
        if (t.isBlank()) return
        viewModelScope.launch {
            dao.updateMajorGoal(goal.copy(title = t, detail = goal.detail.trim()))
        }
    }

    fun deleteMajorGoal(id: Long) {
        viewModelScope.launch { dao.deleteMajorGoal(id) }
    }

    // ---- journal -------------------------------------------------------

    /** id == 0 -> new entry. [day] is LocalDate.toEpochDay(); defaults to today. */
    fun saveJournal(
        id: Long,
        title: String,
        text: String,
        audioPath: String?,
        audioDurationMs: Long,
        day: Long = java.time.LocalDate.now().toEpochDay(),
    ) {
        val trimmed = text.trim()
        val trimmedTitle = title.trim()
        if (trimmed.isBlank() && trimmedTitle.isBlank() && audioPath == null) {
            if (id != 0L) deleteJournal(id)
            return
        }
        viewModelScope.launch {
            if (id == 0L) {
                dao.insertJournal(
                    JournalEntry(
                        timestamp = System.currentTimeMillis(),
                        day = day,
                        title = trimmedTitle,
                        text = trimmed,
                        audioPath = audioPath,
                        audioDurationMs = audioDurationMs,
                    )
                )
            } else {
                val existing = journal.value.firstOrNull { it.id == id } ?: return@launch
                dao.updateJournal(
                    existing.copy(
                        title = trimmedTitle,
                        text = trimmed,
                        audioPath = audioPath,
                        audioDurationMs = audioDurationMs,
                    )
                )
            }
        }
    }

    fun deleteJournal(id: Long) {
        viewModelScope.launch {
            journal.value.firstOrNull { it.id == id }?.audioPath?.let {
                runCatching { java.io.File(it).delete() }
            }
            dao.deleteJournal(id)
        }
    }

    // ---- care team ----------------------------------------------------

    fun saveCareProvider(provider: CareProvider) {
        if (provider.name.isBlank()) return
        viewModelScope.launch {
            if (provider.id == 0L) dao.insertCareProvider(provider.copy(name = provider.name.trim()))
            else dao.updateCareProvider(provider.copy(name = provider.name.trim()))
        }
    }

    fun deleteCareProvider(id: Long) {
        viewModelScope.launch { dao.deleteCareProvider(id) }
    }

    // ---- medications ------------------------------------------------

    fun saveMedication(med: Medication) {
        if (med.name.isBlank()) return
        viewModelScope.launch {
            val clean = med.copy(
                name = med.name.trim(),
                dose = med.dose.trim(),
                amount = med.amount.trim(),
                schedule = med.schedule.trim(),
                notes = med.notes.trim(),
            )
            if (clean.id == 0L) dao.insertMedication(clean) else dao.updateMedication(clean)
            MedicationReminderScheduler.reschedule(getApplication())
        }
    }

    fun deleteMedication(id: Long) {
        viewModelScope.launch {
            dao.deleteMedication(id)
            MedicationReminderScheduler.reschedule(getApplication())
        }
    }

    // ---- mental health check-ins (2026-09-28) --------------------------

    val checkins: StateFlow<List<MentalHealthCheckin>> =
        checkinDao.observeAll()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun submitCheckin(type: String, answers: List<Int>) {
        viewModelScope.launch {
            checkinDao.insert(
                MentalHealthCheckin(
                    type = type,
                    timestamp = System.currentTimeMillis(),
                    answers = answers.joinToString(","),
                    totalScore = answers.sum(),
                )
            )
        }
    }

    fun deleteCheckin(checkin: MentalHealthCheckin) {
        viewModelScope.launch { checkinDao.delete(checkin) }
    }

    fun setCheckinRemindersEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsStore.setCheckinRemindersEnabled(enabled)
            val ctx = getApplication<Application>()
            if (enabled) {
                com.mark.moodlogger.alarm.CheckinReminderScheduler.scheduleNextSunday(ctx)
            } else {
                com.mark.moodlogger.alarm.CheckinReminderScheduler.cancel(ctx)
            }
        }
    }

    fun setAllergies(value: String) {
        viewModelScope.launch { settingsStore.setAllergies(value.trim()) }
    }

    // ---- shopping list -----------------------------------------------

    fun addShoppingItem(name: String, note: String = "") {
        val clean = name.trim()
        if (clean.isBlank()) return
        viewModelScope.launch {
            dao.insertShoppingItem(
                ShoppingItem(name = clean, note = note.trim(), addedAt = System.currentTimeMillis())
            )
        }
    }

    fun setShoppingPurchased(id: Long, purchased: Boolean) {
        viewModelScope.launch {
            val item = shoppingItems.value.firstOrNull { it.id == id } ?: return@launch
            dao.updateShoppingItem(
                item.copy(purchasedAt = if (purchased) System.currentTimeMillis() else null)
            )
        }
    }

    fun deleteShoppingItem(id: Long) {
        viewModelScope.launch { dao.deleteShoppingItem(id) }
    }

    fun clearPurchasedShopping() {
        viewModelScope.launch { dao.clearPurchasedShoppingItems() }
    }

    fun setShoppingRemindersEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsStore.setShoppingRemindersEnabled(enabled)
            val ctx = getApplication<Application>()
            if (enabled) {
                ShoppingReminderScheduler.schedule(
                    ctx, listOf(settingsStore.settings.first().shoppingReminderHour)
                )
            } else {
                ShoppingReminderScheduler.cancel(ctx)
            }
        }
    }

    fun setShoppingReminderHour(hour: Int) {
        viewModelScope.launch {
            settingsStore.setShoppingReminderHour(hour)
            val s = settingsStore.settings.first()
            if (s.shoppingRemindersEnabled) {
                ShoppingReminderScheduler.schedule(getApplication(), listOf(s.shoppingReminderHour))
            }
        }
    }

    fun setShoppingEscalateAfterDays(days: Int) {
        viewModelScope.launch { settingsStore.setShoppingEscalateAfterDays(days) }
    }

    // ---- nutrition (2026-09-28) -----------------------------------

    private val _nutritionSearchQuery = MutableStateFlow("")
    val nutritionSearchQuery: StateFlow<String> = _nutritionSearchQuery

    private val _nutritionSearchResults = MutableStateFlow<List<FoodItem>>(emptyList())
    val nutritionSearchResults: StateFlow<List<FoodItem>> = _nutritionSearchResults

    private var nutritionSearchJob: Job? = null

    val nutritionToday: StateFlow<List<NutritionLogEntry>> =
        nutritionDao.observeForDay(startOfTodayMillis(), startOfTodayMillis() + 86_400_000L)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun setNutritionSearchQuery(query: String) {
        _nutritionSearchQuery.value = query
        nutritionSearchJob?.cancel()
        if (query.isBlank()) {
            _nutritionSearchResults.value = emptyList()
            return
        }
        nutritionSearchJob = viewModelScope.launch {
            delay(250) // debounce so every keystroke doesn't hit the database
            _nutritionSearchResults.value = foodDao.search(query.trim())
        }
    }

    /** [servings] is a multiple of 100g, since FoodItem's numbers are per-100g. */
    fun logFood(food: FoodItem, servings: Double) {
        viewModelScope.launch {
            nutritionDao.insert(
                NutritionLogEntry(
                    timestamp = System.currentTimeMillis(),
                    foodItemId = food.id,
                    name = food.name,
                    servings = servings,
                    gramsPerServing = 100.0,
                    calories = (food.caloriesPer100g ?: 0.0) * servings,
                    protein = food.proteinPer100g?.let { it * servings },
                    fat = food.fatPer100g?.let { it * servings },
                    carbs = food.carbsPer100g?.let { it * servings },
                )
            )
        }
    }

    fun logCustomFood(name: String, calories: Double, protein: Double?, fat: Double?, carbs: Double?) {
        val clean = name.trim()
        if (clean.isBlank()) return
        viewModelScope.launch {
            nutritionDao.insert(
                NutritionLogEntry(
                    timestamp = System.currentTimeMillis(),
                    foodItemId = null,
                    name = clean,
                    servings = 1.0,
                    gramsPerServing = 0.0,
                    calories = calories,
                    protein = protein,
                    fat = fat,
                    carbs = carbs,
                )
            )
        }
    }

    fun deleteNutritionEntry(entry: NutritionLogEntry) {
        viewModelScope.launch { nutritionDao.delete(entry) }
    }

    fun setDailyCalorieBudget(calories: Int) {
        viewModelScope.launch { settingsStore.setDailyCalorieBudget(calories) }
    }

    // ---- app lock (2026-09-28) --------------------------------------

    fun setAppLockPin(pin: String) {
        viewModelScope.launch { settingsStore.setAppLockPin(pin) }
    }

    /** Called right after an already-verified PIN turns out to be stored under the
     *  pre-2026-10-07 single-round SHA-256 scheme (AppLock.verifyLegacyPin matched, not
     *  verifyPin) - transparently re-hashes it under the current PBKDF2 scheme so this
     *  fallback never has to fire again for this PIN. The caller must have already
     *  confirmed the PIN is correct; this never prompts or re-checks anything itself. */
    fun upgradeLegacyPin(pin: String) {
        viewModelScope.launch { settingsStore.setAppLockPin(pin) }
    }

    fun clearAppLock() {
        viewModelScope.launch { settingsStore.clearAppLock() }
    }

    fun setAppLockBiometricEnabled(enabled: Boolean) {
        viewModelScope.launch { settingsStore.setAppLockBiometricEnabled(enabled) }
    }

    // ---- one-time source re-derive (v3.19) --------------------------

    /**
     * Re-tags every existing mood entry's [MoodEntry.source] from its timestamp:
     * on-schedule if logged in the first 5 minutes of an hour AND outside quiet
     * hours, off-schedule otherwise. Historical fire times aren't recorded, so
     * this is the best available reconstruction. Runs once, guarded by a flag.
     */
    fun rederiveSourcesIfNeeded() {
        viewModelScope.launch {
            val s = settingsStore.settings.first()
            if (s.sourceRederiveDone) return@launch
            val rows = dao.allMoodTimes()
            if (rows.isEmpty()) {
                settingsStore.setSourceRederiveDone()
                return@launch
            }
            val zone = java.time.ZoneId.systemDefault()
            val onSchedule = ArrayList<Long>()
            val offSchedule = ArrayList<Long>()
            for (r in rows) {
                val t = java.time.Instant.ofEpochMilli(r.timestamp).atZone(zone)
                val quiet = s.quietHoursEnabled && inQuietWindow(t.hour, s.quietStartHour, s.quietEndHour)
                if (t.minute < 5 && !quiet) onSchedule.add(r.id) else offSchedule.add(r.id)
            }
            if (onSchedule.isNotEmpty()) dao.setSourceForIds(MoodEntry.SOURCE_REMINDER, onSchedule)
            if (offSchedule.isNotEmpty()) dao.setSourceForIds(MoodEntry.SOURCE_MANUAL, offSchedule)
            settingsStore.setSourceRederiveDone()
        }
    }

    private fun inQuietWindow(hour: Int, start: Int, end: Int): Boolean = when {
        start == end -> false
        start < end -> hour in start until end
        else -> hour >= start || hour < end
    }

    // ---- steps -----------------------------------------------------

    val stepsToday: StateFlow<Int> =
        dao.observeStepDays()
            .map { list ->
                val today = startOfTodayEpochDay()
                list.firstOrNull { it.epochDay == today }?.steps ?: 0
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    fun setStepCountEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsStore.setStepCountEnabled(enabled)
            if (enabled) sampleStepsNow()
        }
    }

    fun sampleSteps() {
        viewModelScope.launch {
            if (settingsStore.settings.first().stepCountEnabled) sampleStepsNow()
        }
    }

    private suspend fun sampleStepsNow() {
        val ctx = getApplication<Application>()
        val cumulative = withTimeoutOrNull(3_000L) { StepCounter.readCumulative(ctx) } ?: return
        val today = startOfTodayEpochDay()
        val row = dao.stepDay(today)
        if (row == null) {
            dao.upsertStepDay(StepDay(today, steps = 0, lastCumulative = cumulative))
        } else {
            val delta = cumulative - row.lastCumulative
            val add = if (delta < 0) cumulative.coerceAtLeast(0L).toInt() else delta.toInt()
            dao.upsertStepDay(
                StepDay(today, steps = (row.steps + add).coerceAtLeast(0), lastCumulative = cumulative)
            )
        }
    }

    fun setStepsManually(count: Int) {
        viewModelScope.launch {
            val today = startOfTodayEpochDay()
            val row = dao.stepDay(today)
            dao.upsertStepDay(
                StepDay(today, steps = count.coerceAtLeast(0), lastCumulative = row?.lastCumulative ?: 0L)
            )
        }
    }

    fun ensureScheduled() {
        viewModelScope.launch {
            val s = settingsStore.settings.first()
            if (s.remindersEnabled) {
                AlarmScheduler.scheduleNextTopOfHour(getApplication())
            }
            if (s.shoppingRemindersEnabled) {
                ShoppingReminderScheduler.schedule(getApplication(), listOf(s.shoppingReminderHour))
            }
            SessionRecapScheduler.scheduleNextFriday(getApplication())
            if (s.checkinRemindersEnabled) {
                com.mark.moodlogger.alarm.CheckinReminderScheduler.scheduleNextSunday(getApplication())
            }
            MedicationReminderScheduler.reschedule(getApplication())
            // Re-arm or tidy up the countdown timer (alarms are lost on force-stop).
            val trigger = s.timerTriggerAt
            if (trigger > 0L) {
                if (trigger > System.currentTimeMillis()) {
                    TimerScheduler.schedule(getApplication(), trigger)
                } else {
                    settingsStore.clearTimer()
                }
            }
        }
    }

    // ---- countdown timer ------------------------------------------------

    fun startTimer(label: String, durationMillis: Long) {
        if (durationMillis <= 0L) return
        viewModelScope.launch {
            val triggerAt = System.currentTimeMillis() + durationMillis
            settingsStore.setTimer(label.trim(), triggerAt)
            TimerScheduler.schedule(getApplication(), triggerAt)
        }
    }

    fun cancelTimer() {
        viewModelScope.launch {
            TimerScheduler.cancel(getApplication())
            settingsStore.clearTimer()
        }
    }

    // ---- export -----------------------------------------------------

    suspend fun buildCsvUri(): Uri {
        val ctx = getApplication<Application>()
        val rows = dao.getAllWithKeywordsChronological()
        val stamp = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US)

        val csv = buildString {
            append("date_time,score,label,source,keywords,note\n")
            for (r in rows) {
                val e = r.entry
                val note = e.note.replace("\"", "\"\"")
                // Keywords are free-text (see Keyword.kt, no character restrictions) -
                // same embedded-quote escaping the note field already gets, or a keyword
                // like `bad"day` breaks the quoted CSV field and corrupts the row.
                // Flagged by an outside reviewer.
                val kw = r.keywordNames.joinToString(";").replace("\"", "\"\"")
                append("\"${stamp.format(Date(e.timestamp))}\",")
                append("${e.score},")
                append("${MoodScale.label(e.score)},")
                append("${e.source},")
                append("\"$kw\",")
                append("\"$note\"\n")
            }
        }

        val dir = File(ctx.cacheDir, "exports").apply { mkdirs() }
        val file = File(dir, "mood-log.csv")
        file.writeText(csv)
        return FileProvider.getUriForFile(ctx, "${ctx.packageName}.fileprovider", file)
    }

    class Factory(private val app: Application) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = MainViewModel(app) as T
    }
}

private fun startOfTodayMillis(): Long = Calendar.getInstance().apply {
    set(Calendar.HOUR_OF_DAY, 0)
    set(Calendar.MINUTE, 0)
    set(Calendar.SECOND, 0)
    set(Calendar.MILLISECOND, 0)
}.timeInMillis

private fun startOfTodayEpochDay(): Long = java.time.LocalDate.now().toEpochDay()

/** Midnight on the Monday of the current week. */
private fun startOfWeekMillis(): Long {
    val c = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    val daysSinceMonday = (c.get(Calendar.DAY_OF_WEEK) - Calendar.MONDAY + 7) % 7
    c.add(Calendar.DAY_OF_YEAR, -daysSinceMonday)
    return c.timeInMillis
}
