package com.mark.moodlogger.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mark.moodlogger.MoodScale
import com.mark.moodlogger.data.Goal
import com.mark.moodlogger.data.GoalCheckin
import com.mark.moodlogger.data.GoalPreset
import com.mark.moodlogger.data.Habit
import com.mark.moodlogger.data.MajorGoal
import com.mark.moodlogger.data.MoodEntry
import com.mark.moodlogger.data.SleepLog
import com.mark.moodlogger.data.StepDay
import com.mark.moodlogger.data.WaterLog
import com.mark.moodlogger.data.WorkoutLog
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoalsScreen(
    goals: List<Goal>,
    checkins: List<GoalCheckin>,
    moods: List<MoodEntry>,
    waterLogs: List<WaterLog>,
    sleepLogs: List<SleepLog>,
    workoutLogs: List<WorkoutLog>,
    stepDays: List<StepDay>,
    waterGoalOz: Int,
    sleepGoalHours: Int,
    habits: List<Habit>,
    majorGoals: List<MajorGoal>,
    workoutThisWeek: Set<String>,
    onToggleWorkout: (region: String, currentlyDone: Boolean) -> Unit,
    onAddGoal: (Goal) -> Unit,
    onDeleteGoal: (Long) -> Unit,
    onCheckIn: (Long) -> Unit,
    onUndoCheckIn: (Long) -> Unit,
    onAddHabit: (name: String, kind: String, epochDay: Long) -> Unit,
    onUpdateHabit: (id: Long, name: String, kind: String, epochDay: Long) -> Unit,
    onDeleteHabit: (Long) -> Unit,
    onAddMajor: (title: String, detail: String) -> Unit,
    onUpdateMajor: (MajorGoal) -> Unit,
    onDeleteMajor: (Long) -> Unit,
    onBack: () -> Unit,
) {
    val zone = remember { ZoneId.systemDefault() }
    fun dayOf(millis: Long) = Instant.ofEpochMilli(millis).atZone(zone).toLocalDate().toEpochDay()

    val moodDays = remember(moods) { moods.map { dayOf(it.timestamp) }.toSet() }
    val workoutDays = remember(workoutLogs) { workoutLogs.map { dayOf(it.timestamp) }.toSet() }
    val waterGoalDays = remember(waterLogs, waterGoalOz) {
        waterLogs.groupBy { dayOf(it.timestamp) }
            .filterValues { rows -> rows.sumOf { it.amountOz } >= waterGoalOz && waterGoalOz > 0 }
            .keys
    }
    val sleepGoalDays = remember(sleepLogs, sleepGoalHours) {
        sleepLogs.filter { it.hours >= sleepGoalHours }.map { it.dateEpochDay }.toSet()
    }
    val stepsToday = remember(stepDays) {
        stepDays.firstOrNull { it.epochDay == LocalDate.now().toEpochDay() }?.steps ?: 0
    }

    var tab by remember { mutableIntStateOf(1) } // 0 = Major, 1 = Goals, 2 = Habits, 3 = Workout
    var showAddGoal by remember { mutableStateOf(false) }
    var showCustom by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf<Goal?>(null) }
    var editingHabit by remember { mutableStateOf<Habit?>(null) }
    var showAddHabit by remember { mutableStateOf(false) }
    var showAddMajor by remember { mutableStateOf(false) }
    var editingMajor by remember { mutableStateOf<MajorGoal?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Goals") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
        floatingActionButton = {
            if (tab != 3) {
                FloatingActionButton(
                    onClick = {
                        when (tab) {
                            0 -> showAddMajor = true
                            1 -> showAddGoal = true
                            else -> showAddHabit = true
                        }
                    },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ) { Icon(Icons.Default.Add, contentDescription = "Add") }
            }
        },
    ) { pad ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(pad)
                .honeycombBackground(MaterialTheme.colorScheme.background),
        ) {
            SingleChoiceSegmentedButtonRow(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
            ) {
                SegmentedButton(
                    selected = tab == 0,
                    onClick = { tab = 0 },
                    shape = SegmentedButtonDefaults.itemShape(0, 4),
                ) { Text("Major") }
                SegmentedButton(
                    selected = tab == 1,
                    onClick = { tab = 1 },
                    shape = SegmentedButtonDefaults.itemShape(1, 4),
                ) { Text("Goals") }
                SegmentedButton(
                    selected = tab == 2,
                    onClick = { tab = 2 },
                    shape = SegmentedButtonDefaults.itemShape(2, 4),
                ) { Text("Habits") }
                SegmentedButton(
                    selected = tab == 3,
                    onClick = { tab = 3 },
                    shape = SegmentedButtonDefaults.itemShape(3, 4),
                ) { Text("Workout") }
            }

            if (tab == 1 && stepsToday > 0) {
                Text(
                    "$stepsToday steps today",
                    fontSize = 12.sp,
                    color = Color.Gray,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 2.dp),
                )
            }

            if (tab == 0) {
                if (majorGoals.isEmpty()) {
                    EmptyHint("No major goals yet. These are the big ones - lose 100 pounds, save \$1,000, get debt-free. Tap + to add one and it shows on your home screen.")
                } else {
                    LazyColumn(
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        items(majorGoals, key = { it.id }) { mg ->
                            MajorGoalCard(mg) { editingMajor = mg }
                        }
                    }
                }
            } else if (tab == 1) {
                if (goals.isEmpty()) {
                    EmptyHint("No goals yet. Tap + to set one - shower a few times a week, hit your water target, whatever you're working on.")
                } else {
                    LazyColumn(
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        items(goals, key = { it.id }) { goal ->
                            val prog = remember(goal, checkins, moodDays, waterGoalDays, sleepGoalDays, workoutDays, stepDays) {
                                progressFor(goal, checkins, moodDays, waterGoalDays, sleepGoalDays, workoutDays, stepDays)
                            }
                            GoalCard(
                                goal = goal,
                                done = prog.first,
                                target = prog.second,
                                onCheckIn = { onCheckIn(goal.id) },
                                onUndo = { onUndoCheckIn(goal.id) },
                                onDelete = { confirmDelete = goal },
                            )
                        }
                    }
                }
            } else if (tab == 2) {
                if (habits.isEmpty()) {
                    EmptyHint("Track something you quit or started. Tap + to add one.")
                } else {
                    LazyColumn(
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        items(habits, key = { it.id }) { habit ->
                            HabitCard(habit) { editingHabit = habit }
                        }
                    }
                }
            } else {
                WorkoutContent(
                    doneThisWeek = workoutThisWeek,
                    onToggle = onToggleWorkout,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }

    if (showAddGoal) {
        AddGoalDialog(
            onPreset = { p ->
                onAddGoal(
                    Goal(
                        title = p.title, auto = p.auto, source = p.source,
                        targetCount = p.targetCount, periodDays = p.periodDays,
                        dailyThreshold = p.dailyThreshold, createdAt = 0L,
                    )
                )
                showAddGoal = false
            },
            onCustom = { showAddGoal = false; showCustom = true },
            onDismiss = { showAddGoal = false },
        )
    }
    if (showCustom) {
        CustomGoalDialog(
            onCreate = { title, count, daily ->
                onAddGoal(
                    Goal(
                        title = title, auto = false, targetCount = count,
                        periodDays = if (daily) 1 else 7, createdAt = 0L,
                    )
                )
                showCustom = false
            },
            onDismiss = { showCustom = false },
        )
    }
    confirmDelete?.let { g ->
        AlertDialog(
            onDismissRequest = { confirmDelete = null },
            title = { Text("Delete goal?") },
            text = { Text("\"${g.title}\" and its check-ins.") },
            confirmButton = {
                TextButton(onClick = { onDeleteGoal(g.id); confirmDelete = null }) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = null }) { Text("Cancel") } },
        )
    }

    if (showAddHabit) {
        HabitDialog(
            existing = null,
            onDismiss = { showAddHabit = false },
            onSave = { name, kind, day -> onAddHabit(name, kind, day); showAddHabit = false },
            onDelete = null,
        )
    }
    editingHabit?.let { h ->
        HabitDialog(
            existing = h,
            onDismiss = { editingHabit = null },
            onSave = { name, kind, day -> onUpdateHabit(h.id, name, kind, day); editingHabit = null },
            onDelete = { onDeleteHabit(h.id); editingHabit = null },
        )
    }

    if (showAddMajor) {
        MajorGoalDialog(
            existing = null,
            onDismiss = { showAddMajor = false },
            onSave = { title, detail -> onAddMajor(title, detail); showAddMajor = false },
            onDelete = null,
        )
    }
    editingMajor?.let { mg ->
        MajorGoalDialog(
            existing = mg,
            onDismiss = { editingMajor = null },
            onSave = { title, detail ->
                onUpdateMajor(mg.copy(title = title, detail = detail)); editingMajor = null
            },
            onDelete = { onDeleteMajor(mg.id); editingMajor = null },
        )
    }
}

@Composable
private fun EmptyHint(text: String) {
    Box(Modifier.fillMaxSize().padding(28.dp), contentAlignment = Alignment.Center) {
        Text(text, color = Color.Gray, fontSize = 14.sp)
    }
}

@Composable
private fun MajorGoalCard(goal: MajorGoal, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
    ) {
        Row(
            Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                shape = HexagonShape,
                color = MoodScale.color(5),
                modifier = Modifier.size(width = 34.dp, height = 30.dp),
            ) {}
            Spacer(Modifier.size(12.dp))
            Column(Modifier.weight(1f)) {
                Text(goal.title, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                if (goal.detail.isNotBlank()) {
                    Spacer(Modifier.size(3.dp))
                    Text(goal.detail, fontSize = 12.sp, color = Color.Gray)
                }
            }
        }
    }
}

@Composable
private fun MajorGoalDialog(
    existing: MajorGoal?,
    onDismiss: () -> Unit,
    onSave: (title: String, detail: String) -> Unit,
    onDelete: (() -> Unit)?,
) {
    var title by remember { mutableStateOf(existing?.title ?: "") }
    var detail by remember { mutableStateOf(existing?.detail ?: "") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existing == null) "New major goal" else "Edit major goal") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it.take(80) },
                    label = { Text("The goal (e.g. Save \$1,000)") },
                    singleLine = true,
                )
                OutlinedTextField(
                    value = detail,
                    onValueChange = { detail = it.take(120) },
                    label = { Text("A note (optional)") },
                    minLines = 2,
                    maxLines = 3,
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(title.trim(), detail.trim()) },
                enabled = title.isNotBlank(),
            ) { Text(if (existing == null) "Add" else "Save") }
        },
        dismissButton = {
            Row {
                if (onDelete != null) {
                    TextButton(onClick = onDelete) { Text("Delete") }
                }
                TextButton(onClick = onDismiss) { Text("Cancel") }
            }
        },
    )
}

@Composable
private fun GoalCard(
    goal: Goal,
    done: Int,
    target: Int,
    onCheckIn: () -> Unit,
    onUndo: () -> Unit,
    onDelete: () -> Unit,
) {
    val frac = if (target > 0) (done.toFloat() / target).coerceIn(0f, 1f) else 0f
    val complete = done >= target && target > 0
    val period = if (goal.periodDays <= 1) "today" else "this week"
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    goal.title,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                    modifier = Modifier.weight(1f),
                )
                if (goal.auto) {
                    Text("auto  ", fontSize = 10.sp, color = Color.Gray)
                }
                Icon(
                    Icons.Default.Close,
                    contentDescription = "Delete goal",
                    tint = Color.Gray,
                    modifier = Modifier
                        .size(18.dp)
                        .clickable(onClick = onDelete),
                )
            }
            Spacer(Modifier.size(6.dp))
            Text(
                "$done / $target $period" + if (complete) "  ✓" else "",
                fontSize = 12.sp,
                color = if (complete) MoodScale.color(5) else Color.Gray,
            )
            Spacer(Modifier.size(8.dp))
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .background(Color(0x22808080), RoundedCornerShape(4.dp)),
            ) {
                Box(
                    Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(frac)
                        .background(
                            if (complete) MoodScale.color(5) else MaterialTheme.colorScheme.primary,
                            RoundedCornerShape(4.dp),
                        ),
                )
            }
            if (!goal.auto) {
                Spacer(Modifier.size(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Button(onClick = onCheckIn, shape = RoundedCornerShape(12.dp)) {
                        Text(if (goal.periodDays <= 1) "Did it" else "Check in")
                    }
                    if (done > 0) {
                        Spacer(Modifier.size(6.dp))
                        TextButton(onClick = onUndo) { Text("Undo") }
                    }
                }
            }
        }
    }
}

@Composable
private fun AddGoalDialog(
    onPreset: (GoalPreset) -> Unit,
    onCustom: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add a goal") },
        text = {
            Column(
                Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                GoalPreset.ALL.forEach { p ->
                    Text(
                        p.title,
                        fontSize = 14.sp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onPreset(p) }
                            .padding(vertical = 12.dp),
                    )
                }
                Text(
                    "Custom goal…",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onCustom() }
                        .padding(vertical = 12.dp),
                )
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun CustomGoalDialog(
    onCreate: (title: String, count: Int, daily: Boolean) -> Unit,
    onDismiss: () -> Unit,
) {
    var title by remember { mutableStateOf("") }
    var count by remember { mutableIntStateOf(3) }
    var daily by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Custom goal") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it.take(60) },
                    label = { Text("What's the goal?") },
                    singleLine = true,
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("How many times", Modifier.weight(1f), fontSize = 14.sp)
                    OutlinedButton(onClick = { if (count > 1) count-- }) { Text("-") }
                    Spacer(Modifier.size(8.dp))
                    Text("$count", fontWeight = FontWeight.Bold)
                    Spacer(Modifier.size(8.dp))
                    OutlinedButton(onClick = { if (count < 20) count++ }) { Text("+") }
                }
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    SegmentedButton(
                        selected = !daily,
                        onClick = { daily = false },
                        shape = SegmentedButtonDefaults.itemShape(0, 2),
                    ) { Text("per week") }
                    SegmentedButton(
                        selected = daily,
                        onClick = { daily = true },
                        shape = SegmentedButtonDefaults.itemShape(1, 2),
                    ) { Text("per day") }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onCreate(title.trim(), count, daily) },
                enabled = title.isNotBlank(),
            ) { Text("Create") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

// ---- progress ---------------------------------------------------------

private fun progressFor(
    goal: Goal,
    checkins: List<GoalCheckin>,
    moodDays: Set<Long>,
    waterGoalDays: Set<Long>,
    sleepGoalDays: Set<Long>,
    workoutDays: Set<Long>,
    stepDays: List<StepDay>,
): Pair<Int, Int> {
    val todayDay = LocalDate.now().toEpochDay()
    val periodStartDay =
        if (goal.periodDays <= 1) todayDay else todayDay - (goal.periodDays - 1)
    val periodStartMillis = LocalDate.ofEpochDay(periodStartDay)
        .atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

    val done = if (!goal.auto) {
        checkins.count { it.goalId == goal.id && it.timestamp >= periodStartMillis }
    } else {
        val range = periodStartDay..todayDay
        when (goal.source) {
            Goal.SRC_MOOD -> range.count { it in moodDays }
            Goal.SRC_WATER -> range.count { it in waterGoalDays }
            Goal.SRC_SLEEP -> range.count { it in sleepGoalDays }
            Goal.SRC_WORKOUT -> range.count { it in workoutDays }
            Goal.SRC_STEPS -> range.count { d ->
                (stepDays.firstOrNull { it.epochDay == d }?.steps ?: 0) >= goal.dailyThreshold
            }
            else -> 0
        }
    }
    return done.coerceIn(0, goal.targetCount) to goal.targetCount
}
