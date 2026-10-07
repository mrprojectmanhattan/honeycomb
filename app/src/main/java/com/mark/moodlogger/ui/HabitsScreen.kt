package com.mark.moodlogger.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mark.moodlogger.data.Habit
import com.mark.moodlogger.data.elapsedSince
import com.mark.moodlogger.data.totalDays
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HabitsScreen(
    habits: List<Habit>,
    onBack: () -> Unit,
    onAdd: (name: String, kind: String, epochDay: Long) -> Unit,
    onUpdate: (id: Long, name: String, kind: String, epochDay: Long) -> Unit,
    onDelete: (Long) -> Unit,
) {
    var editing by remember { mutableStateOf<Habit?>(null) }
    var showAdd by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Habits") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAdd = true },
                containerColor = androidx.compose.material3.MaterialTheme.colorScheme.primary,
                contentColor = androidx.compose.material3.MaterialTheme.colorScheme.onPrimary,
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add habit")
            }
        },
    ) { pad ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(pad)
                .honeycombBackground(androidx.compose.material3.MaterialTheme.colorScheme.background),
        ) {
            if (habits.isEmpty()) {
                Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                    Text(
                        "Track something you quit or started. Tap + to add one.",
                        color = Color.Gray,
                    )
                }
            } else {
                LazyColumn(
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(habits, key = { it.id }) { habit ->
                        HabitCard(habit) { editing = habit }
                    }
                }
            }
        }
    }

    if (showAdd) {
        HabitDialog(
            existing = null,
            onDismiss = { showAdd = false },
            onSave = { name, kind, day ->
                onAdd(name, kind, day)
                showAdd = false
            },
            onDelete = null,
        )
    }
    editing?.let { h ->
        HabitDialog(
            existing = h,
            onDismiss = { editing = null },
            onSave = { name, kind, day ->
                onUpdate(h.id, name, kind, day)
                editing = null
            },
            onDelete = {
                onDelete(h.id)
                editing = null
            },
        )
    }
}

@Composable
fun HabitCard(habit: Habit, onClick: () -> Unit) {
    val dateFmt = remember { DateTimeFormatter.ofPattern("MMM d, yyyy") }
    val verb = if (habit.kind == Habit.KIND_QUIT) "Quit" else "Started"
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                shape = HexagonShape,
                color = com.mark.moodlogger.MoodScale.color(if (habit.kind == Habit.KIND_QUIT) 4 else 3),
                modifier = Modifier.size(width = 40.dp, height = 34.dp),
            ) {}
            Spacer(Modifier.size(14.dp))
            Column(Modifier.weight(1f)) {
                Text(habit.name, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                Text(
                    "$verb ${habit.startDate.format(dateFmt)}",
                    fontSize = 12.sp,
                    color = Color.Gray,
                )
                Spacer(Modifier.size(6.dp))
                Text(
                    elapsedSince(habit.startDate),
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp,
                    color = com.mark.moodlogger.MoodScale.color(5),
                )
                Text(
                    "${totalDays(habit.startDate)} days total",
                    fontSize = 11.sp,
                    color = Color.Gray,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HabitDialog(
    existing: Habit?,
    onDismiss: () -> Unit,
    onSave: (name: String, kind: String, epochDay: Long) -> Unit,
    onDelete: (() -> Unit)?,
) {
    var name by remember { mutableStateOf(existing?.name ?: "") }
    var quit by remember { mutableStateOf(existing?.kind != Habit.KIND_STARTED) }
    var epochDay by remember {
        mutableStateOf(existing?.startEpochDay ?: LocalDate.now().toEpochDay())
    }
    var showPicker by remember { mutableStateOf(false) }
    val dateFmt = remember { DateTimeFormatter.ofPattern("MMM d, yyyy") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existing == null) "New habit" else "Edit habit") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name (e.g. Soda)") },
                    singleLine = true,
                )
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    SegmentedButton(
                        selected = quit,
                        onClick = { quit = true },
                        shape = SegmentedButtonDefaults.itemShape(0, 2),
                    ) { Text("Quit") }
                    SegmentedButton(
                        selected = !quit,
                        onClick = { quit = false },
                        shape = SegmentedButtonDefaults.itemShape(1, 2),
                    ) { Text("Started") }
                }
                OutlinedButton(
                    onClick = { showPicker = true },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(LocalDate.ofEpochDay(epochDay).format(dateFmt))
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onSave(name, if (quit) Habit.KIND_QUIT else Habit.KIND_STARTED, epochDay)
                },
                enabled = name.isNotBlank(),
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

    if (showPicker) {
        val state = rememberDatePickerStateSafe(epochDay)
        DatePickerDialog(
            onDismissRequest = { showPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let {
                        epochDay = Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate().toEpochDay()
                    }
                    showPicker = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showPicker = false }) { Text("Cancel") } },
        ) {
            DatePicker(state = state)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun rememberDatePickerStateSafe(epochDay: Long) =
    androidx.compose.material3.rememberDatePickerState(
        initialSelectedDateMillis = LocalDate.ofEpochDay(epochDay)
            .atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
    )
