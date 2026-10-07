package com.mark.moodlogger.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.mark.moodlogger.data.EntryWithKeywords
import com.mark.moodlogger.data.Habit
import com.mark.moodlogger.data.Keyword
import com.mark.moodlogger.data.MoodEntry
import com.mark.moodlogger.data.Settings
import java.time.LocalDate
import kotlin.math.roundToInt

enum class Screen { Main, Log, Trends, Settings, Keywords, Goals, Timer, Sleep, About, Journal, Breathe, Care, Shopping, Nutrition }

/** The sections you can swipe between, in order. Main, Log, Keywords, Timer, About are not in here. */
private val SWIPE_SCREENS = listOf(
    Screen.Goals, Screen.Journal, Screen.Trends, Screen.Care, Screen.Settings,
)

private fun sibling(current: Screen, delta: Int): Screen? {
    val i = SWIPE_SCREENS.indexOf(current)
    if (i < 0) return null
    return SWIPE_SCREENS.getOrNull(i + delta)
}

@Composable
fun AppRoot(
    vm: MainViewModel,
    startOnLog: Boolean,
    startOnTimer: Boolean = false,
    startOnShopping: Boolean = false,
    startOnJournalRecap: Boolean = false,
    startOnCare: Boolean = false,
) {
    val context = LocalContext.current

    var screen by remember {
        mutableStateOf(
            when {
                startOnLog -> Screen.Log
                startOnTimer -> Screen.Timer
                startOnShopping -> Screen.Shopping
                startOnJournalRecap -> Screen.Journal
                startOnCare -> Screen.Care
                else -> Screen.Main
            }
        )
    }
    var logSource by remember { mutableStateOf(if (startOnLog) MoodEntry.SOURCE_REMINDER else MoodEntry.SOURCE_MANUAL) }
    var editing by remember { mutableStateOf<EntryWithKeywords?>(null) }

    val notifPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        vm.ensureScheduled()
        vm.sampleSteps()
        vm.rederiveSourcesIfNeeded()
    }

    val entries by vm.entries.collectAsState()
    val keywords by vm.keywords.collectAsState()
    val habits by vm.habits.collectAsState()
    val settings by vm.settings.collectAsState()
    val waterToday by vm.waterToday.collectAsState()
    val workoutThisWeek by vm.workoutThisWeek.collectAsState()
    val stepsToday by vm.stepsToday.collectAsState()
    val sleepLogs by vm.sleepLogs.collectAsState()
    val waterLogs by vm.waterLogs.collectAsState()
    val workoutLogs by vm.workoutLogs.collectAsState()
    val goals by vm.goals.collectAsState()
    val goalCheckins by vm.goalCheckins.collectAsState()
    val stepDays by vm.stepDays.collectAsState()
    val majorGoals by vm.majorGoals.collectAsState()
    val journal by vm.journal.collectAsState()
    val careProviders by vm.careProviders.collectAsState()
    val medications by vm.medications.collectAsState()
    val shoppingItems by vm.shoppingItems.collectAsState()
    val openPromptGroups by vm.openPromptGroups.collectAsState()
    val nutritionSearchQuery by vm.nutritionSearchQuery.collectAsState()
    val nutritionSearchResults by vm.nutritionSearchResults.collectAsState()
    val nutritionToday by vm.nutritionToday.collectAsState()
    val checkins by vm.checkins.collectAsState()

    val backToMain = { screen = Screen.Main }
    val openKeywords = { screen = Screen.Keywords }

    // "Want to add a reflection?" nudge after logging a mood — at most once a day,
    // and only if there's no journal entry for today yet.
    var promptReflection by remember { mutableStateOf(false) }
    var startJournalNew by remember { mutableStateOf(startOnJournalRecap) }
    var lastPromptedDay by remember { mutableStateOf(-1L) }

    // System back button + edge-swipe-back gesture. Without this, back from any
    // sub-screen finishes the Activity and closes the app.
    BackHandler(enabled = screen != Screen.Main) {
        when (screen) {
            Screen.Log -> {
                val wasEditing = editing != null
                editing = null
                screen = if (wasEditing) Screen.Trends else Screen.Main
            }
            Screen.Keywords, Screen.Timer, Screen.About -> screen = Screen.Settings
            else -> screen = Screen.Main
        }
    }

    val editEntry: (EntryWithKeywords) -> Unit = { entry ->
        editing = entry
        screen = Screen.Log
    }

    when (screen) {
        Screen.Main -> MainScreen(
            entries = entries,
            navPosition = settings.navPosition,
            navRailAlign = settings.navRailAlign,
            waterToday = waterToday,
            waterGoalOz = settings.waterGoalOz,
            waterServingOz = settings.waterServingOz,
            majorGoals = majorGoals,
            stepsToday = stepsToday,
            workoutThisWeek = workoutThisWeek,
            careProviders = careProviders,
            homeWidget = settings.homeWidget,
            onSetHomeWidget = vm::setHomeWidget,
            onLogNow = {
                editing = null
                logSource = MoodEntry.SOURCE_MANUAL
                screen = Screen.Log
            },
            onLogWater = { vm.logWater(settings.waterServingOz) },
            onUndoWater = vm::undoLastWater,
            onLogSleep = { screen = Screen.Sleep },
            onBreathe = { screen = Screen.Breathe },
            onOpenJournal = { screen = Screen.Journal },
            onOpenGoals = { screen = Screen.Goals },
            onOpenTrends = { screen = Screen.Trends },
            onOpenCare = { screen = Screen.Care },
            onOpenShopping = { screen = Screen.Shopping },
            onOpenNutrition = { screen = Screen.Nutrition },
            shoppingOpenCount = shoppingItems.count { it.outstanding },
            openPromptGroups = openPromptGroups,
            onPromptReason = vm::setPromptReason,
            onOpenSettings = { screen = Screen.Settings },
        )

        Screen.Goals, Screen.Trends, Screen.Journal, Screen.Care, Screen.Settings -> {
            val current = screen
            val prev = sibling(current, -1)
            val next = sibling(current, +1)
            SwipeNav(
                sectionKey = current,
                canPrev = prev != null,
                canNext = next != null,
                onPrev = { prev?.let { screen = it } },
                onNext = { next?.let { screen = it } },
            ) {
                SectionScreen(
                    target = current,
                    vm = vm,
                    entries = entries,
                    keywords = keywords,
                    habits = habits,
                    settings = settings,
                    workoutThisWeek = workoutThisWeek,
                    sleepLogs = sleepLogs,
                    waterLogs = waterLogs,
                    workoutLogs = workoutLogs,
                    goals = goals,
                    goalCheckins = goalCheckins,
                    stepDays = stepDays,
                    majorGoals = majorGoals,
                    journal = journal,
                    careProviders = careProviders,
                    medications = medications,
                    allergies = settings.allergies,
                    checkins = checkins,
                    startJournalNew = startJournalNew,
                    onConsumeStartJournalNew = { startJournalNew = false },
                    onToggleWorkout = vm::toggleWorkout,
                    onBack = backToMain,
                    onOpenKeywords = openKeywords,
                    onOpenTimer = { screen = Screen.Timer },
                    onOpenAbout = { screen = Screen.About },
                    onEditEntry = editEntry,
                )
            }
        }

        Screen.Sleep -> {
            val todayDay = LocalDate.now().toEpochDay()
            SleepScreen(
                existing = sleepLogs.firstOrNull { it.dateEpochDay == todayDay },
                settings = settings,
                onSave = { d, b, w, q, lat, wakings, feeling ->
                    vm.logSleep(d, b, w, q, lat, wakings, feeling)
                    screen = Screen.Main
                },
                onDelete = { d ->
                    vm.deleteSleep(d)
                    screen = Screen.Main
                },
                onBack = { screen = Screen.Main },
            )
        }

        Screen.Timer -> TimerScreen(
            settings = settings,
            onBack = { screen = Screen.Settings },
            onStart = vm::startTimer,
            onCancel = vm::cancelTimer,
        )

        Screen.About -> AboutScreen(onBack = { screen = Screen.Settings })

        Screen.Breathe -> BreatheScreen(onBack = { screen = Screen.Main })

        Screen.Shopping -> ShoppingScreen(
            items = shoppingItems,
            remindersEnabled = settings.shoppingRemindersEnabled,
            reminderHour = settings.shoppingReminderHour,
            escalateAfterDays = settings.shoppingEscalateAfterDays,
            onAdd = vm::addShoppingItem,
            onSetPurchased = vm::setShoppingPurchased,
            onDelete = vm::deleteShoppingItem,
            onClearPurchased = vm::clearPurchasedShopping,
            onRemindersEnabled = vm::setShoppingRemindersEnabled,
            onReminderHour = vm::setShoppingReminderHour,
            onEscalateAfterDays = vm::setShoppingEscalateAfterDays,
            onBack = { screen = Screen.Main },
        )

        Screen.Nutrition -> NutritionScreen(
            searchQuery = nutritionSearchQuery,
            onSearchQueryChange = vm::setNutritionSearchQuery,
            searchResults = nutritionSearchResults,
            onLogFood = vm::logFood,
            onLogCustom = vm::logCustomFood,
            todayEntries = nutritionToday,
            onDeleteEntry = vm::deleteNutritionEntry,
            dailyBudgetCalories = settings.dailyCalorieBudget,
            onBack = { screen = Screen.Main },
        )

        Screen.Log -> LogMoodScreen(
            existing = editing,
            allKeywords = keywords,
            onSave = { score, note, keywordIds ->
                val e = editing
                if (e == null) {
                    vm.logMood(score, note, keywordIds, fromNotification = logSource == MoodEntry.SOURCE_REMINDER)
                    val today = LocalDate.now().toEpochDay()
                    if (lastPromptedDay != today && journal.none { it.day == today }) {
                        promptReflection = true
                        lastPromptedDay = today
                    }
                    screen = Screen.Main
                } else {
                    vm.updateEntry(e.entry.id, e.entry.timestamp, e.entry.source, score, note, keywordIds)
                    editing = null
                    screen = Screen.Trends
                }
            },
            onBack = {
                val wasEditing = editing != null
                editing = null
                screen = if (wasEditing) Screen.Trends else Screen.Main
            },
        )

        Screen.Keywords -> KeywordManagerScreen(
            keywords = keywords,
            onBack = { screen = Screen.Settings },
            onAdd = vm::addKeyword,
            onRename = vm::renameKeyword,
            onDelete = vm::deleteKeyword,
        )
    }

    if (promptReflection) {
        AlertDialog(
            onDismissRequest = { promptReflection = false },
            title = { Text("Mood logged") },
            text = { Text("Want to add a reflection for today?") },
            confirmButton = {
                TextButton(onClick = {
                    promptReflection = false
                    startJournalNew = true
                    screen = Screen.Journal
                }) { Text("Write one") }
            },
            dismissButton = {
                TextButton(onClick = { promptReflection = false }) { Text("Not now") }
            },
        )
    }
}

/**
 * Wraps a section so a clearly-horizontal swipe moves to the previous / next
 * section. Uses [detectHorizontalDragGestures], which only fires once movement is
 * sideways-dominant, so the vertical lists inside Trends / Journal keep their own
 * scrolling. The content nudges with the finger and snaps back on release.
 */
@Composable
private fun SwipeNav(
    sectionKey: Any,
    canPrev: Boolean,
    canNext: Boolean,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    content: @Composable () -> Unit,
) {
    val density = LocalDensity.current
    val thresholdPx = with(density) { 64.dp.toPx() }
    var dragX by remember(sectionKey) { mutableFloatStateOf(0f) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(sectionKey) {
                var acc = 0f
                detectHorizontalDragGestures(
                    onDragStart = { acc = 0f },
                    onDragEnd = {
                        when {
                            acc <= -thresholdPx && canNext -> onNext()
                            acc >= thresholdPx && canPrev -> onPrev()
                        }
                        acc = 0f
                        dragX = 0f
                    },
                    onDragCancel = {
                        acc = 0f
                        dragX = 0f
                    },
                ) { change, dragAmount ->
                    acc += dragAmount
                    dragX = (acc * 0.35f).coerceIn(-140f, 140f)
                    change.consume()
                }
            }
            .offset { IntOffset(dragX.roundToInt(), 0) },
    ) {
        content()
    }
}

@Composable
private fun SectionScreen(
    target: Screen,
    vm: MainViewModel,
    entries: List<EntryWithKeywords>,
    keywords: List<Keyword>,
    habits: List<Habit>,
    settings: Settings,
    workoutThisWeek: Set<String>,
    sleepLogs: List<com.mark.moodlogger.data.SleepLog>,
    waterLogs: List<com.mark.moodlogger.data.WaterLog>,
    workoutLogs: List<com.mark.moodlogger.data.WorkoutLog>,
    goals: List<com.mark.moodlogger.data.Goal>,
    goalCheckins: List<com.mark.moodlogger.data.GoalCheckin>,
    stepDays: List<com.mark.moodlogger.data.StepDay>,
    majorGoals: List<com.mark.moodlogger.data.MajorGoal>,
    journal: List<com.mark.moodlogger.data.JournalEntry>,
    careProviders: List<com.mark.moodlogger.data.CareProvider>,
    medications: List<com.mark.moodlogger.data.Medication>,
    allergies: String,
    checkins: List<com.mark.moodlogger.data.MentalHealthCheckin>,
    startJournalNew: Boolean,
    onConsumeStartJournalNew: () -> Unit,
    onToggleWorkout: (region: String, currentlyDone: Boolean) -> Unit,
    onBack: () -> Unit,
    onOpenKeywords: () -> Unit,
    onOpenTimer: () -> Unit,
    onOpenAbout: () -> Unit,
    onEditEntry: (EntryWithKeywords) -> Unit,
) {
    when (target) {
        Screen.Goals -> GoalsScreen(
            goals = goals,
            checkins = goalCheckins,
            moods = entries.map { it.entry },
            waterLogs = waterLogs,
            sleepLogs = sleepLogs,
            workoutLogs = workoutLogs,
            stepDays = stepDays,
            waterGoalOz = settings.waterGoalOz,
            sleepGoalHours = settings.sleepGoalHours,
            habits = habits,
            majorGoals = majorGoals,
            workoutThisWeek = workoutThisWeek,
            onToggleWorkout = onToggleWorkout,
            onAddGoal = vm::addGoal,
            onDeleteGoal = vm::deleteGoal,
            onCheckIn = vm::checkInGoal,
            onUndoCheckIn = vm::undoGoalCheckin,
            onAddHabit = vm::addHabit,
            onUpdateHabit = vm::updateHabit,
            onDeleteHabit = vm::deleteHabit,
            onAddMajor = vm::addMajorGoal,
            onUpdateMajor = vm::updateMajorGoal,
            onDeleteMajor = vm::deleteMajorGoal,
            onBack = onBack,
        )

        Screen.Trends -> TrendsScreen(
            entries = entries,
            keywords = keywords,
            sleepLogs = sleepLogs,
            sleepGoalHours = settings.sleepGoalHours,
            workouts = workoutLogs,
            onEditEntry = onEditEntry,
            onDeleteEntry = { id -> vm.deleteEntry(id) },
            onBack = onBack,
        )

        Screen.Journal -> JournalScreen(
            entries = journal,
            startNew = startJournalNew,
            onStartNewConsumed = onConsumeStartJournalNew,
            onSave = { id, title, text, path, dur -> vm.saveJournal(id, title, text, path, dur) },
            onDelete = vm::deleteJournal,
            onBack = onBack,
        )

        Screen.Care -> CareScreen(
            providers = careProviders,
            medications = medications,
            allergies = allergies,
            checkins = checkins,
            onSaveProvider = vm::saveCareProvider,
            onDeleteProvider = vm::deleteCareProvider,
            onSaveMedication = vm::saveMedication,
            onDeleteMedication = vm::deleteMedication,
            onSubmitCheckin = vm::submitCheckin,
            onDeleteCheckin = vm::deleteCheckin,
            onSetAllergies = vm::setAllergies,
            onBack = onBack,
        )

        Screen.Settings -> SettingsScreen(
            settings = settings,
            onBack = onBack,
            onOpenKeywords = onOpenKeywords,
            viewModel = vm,
            onRemindersEnabled = vm::setRemindersEnabled,
            onQuietHoursEnabled = vm::setQuietHoursEnabled,
            onQuietStart = vm::setQuietStart,
            onQuietEnd = vm::setQuietEnd,
            onNavPosition = vm::setNavPosition,
            onNavRailAlign = vm::setNavRailAlign,
            onWaterGoal = vm::setWaterGoal,
            onWaterServing = vm::setWaterServing,
            onSleepAssist = vm::setSleepAssist,
            onSleepGoal = vm::setSleepGoal,
            onStepCountEnabled = vm::setStepCountEnabled,
            onOpenTimer = onOpenTimer,
            onOpenAbout = onOpenAbout,
            onDailyCalorieBudget = vm::setDailyCalorieBudget,
            onSetAppLockPin = vm::setAppLockPin,
            onClearAppLock = vm::clearAppLock,
            onSetAppLockBiometric = vm::setAppLockBiometricEnabled,
            onCheckinRemindersEnabled = vm::setCheckinRemindersEnabled,
        )

        else -> Unit
    }
}
