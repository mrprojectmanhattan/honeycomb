package com.mark.moodlogger.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
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
import com.mark.moodlogger.data.EntryWithKeywords
import com.mark.moodlogger.data.MoodEntry
import com.mark.moodlogger.data.WorkoutLog
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private val WORKOUT_TINT = Color(0xFFD9A22E)

private data class DayGroup(
    val dayStart: Long,
    val label: String,
    val entries: List<EntryWithKeywords>,
    val workouts: List<WorkoutLog>,
) {
    val avg: Float? get() =
        if (entries.isEmpty()) null
        else entries.sumOf { it.entry.score }.toFloat() / entries.size
}

/**
 * The day-grouped entry log. Was its own screen (a nav tab) through v3.21; from
 * v3.22 it's the "List" view inside Trends, so this is a bare content composable
 * with no Scaffold — the caller supplies the bar and the [modifier].
 */
@Composable
fun HistoryContent(
    entries: List<EntryWithKeywords>,
    workouts: List<WorkoutLog>,
    onEdit: (EntryWithKeywords) -> Unit,
    onDelete: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val groups = remember(entries, workouts) { groupByDay(entries, workouts) }
    val expanded = remember { mutableStateMapOf<Long, Boolean>() }
    var confirmDelete by remember { mutableStateOf<EntryWithKeywords?>(null) }

    val todayStart = remember { startOfDay(System.currentTimeMillis()) }

    if (groups.isEmpty()) {
        Column(
            modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("No entries yet.", color = Color.Gray)
        }
    } else {
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .honeycombBackground(MaterialTheme.colorScheme.background),
        ) {
            groups.forEach { group ->
                val isOpen = expanded[group.dayStart] ?: (group.dayStart == todayStart)
                item(key = "h-${group.dayStart}") {
                    DayHeader(group, isOpen) { expanded[group.dayStart] = !isOpen }
                    HorizontalDivider(color = Color(0x22808080))
                }
                if (isOpen) {
                    items(group.entries, key = { it.entry.id }) { row ->
                        HistoryRow(
                            row = row,
                            onEdit = { onEdit(row) },
                            onDelete = { confirmDelete = row },
                        )
                    }
                    if (group.workouts.isNotEmpty()) {
                        item(key = "w-${group.dayStart}") {
                            WorkoutHistoryRow(group.workouts)
                        }
                    }
                }
            }
        }
    }

    confirmDelete?.let { row ->
        AlertDialog(
            onDismissRequest = { confirmDelete = null },
            title = { Text("Delete this entry?") },
            text = { Text("${MoodScale.label(row.entry.score)} on " + fullTime(row.entry.timestamp)) },
            confirmButton = {
                TextButton(onClick = {
                    onDelete(row.entry.id)
                    confirmDelete = null
                }) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = null }) { Text("Cancel") }
            },
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DayHeader(group: DayGroup, isOpen: Boolean, onToggle: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onToggle)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            if (isOpen) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
            contentDescription = null,
            tint = Color.Gray,
        )
        Spacer(Modifier.size(8.dp))
        Text(group.label, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))

        if (group.workouts.isNotEmpty()) {
            Icon(
                Icons.Default.FitnessCenter,
                contentDescription = "Workout logged",
                tint = WORKOUT_TINT,
                modifier = Modifier.size(15.dp),
            )
            Spacer(Modifier.size(8.dp))
        }

        val count = group.entries.size
        Text(
            when {
                count > 0 -> "$count " + if (count == 1) "entry" else "entries"
                else -> "workout"
            },
            fontSize = 12.sp,
            color = Color.Gray,
        )

        group.avg?.let { avg ->
            Spacer(Modifier.size(10.dp))
            Surface(
                shape = HexagonShape,
                color = MoodScale.colorForAvg(avg),
                modifier = Modifier.size(width = 22.dp, height = 19.dp),
            ) {}
            Spacer(Modifier.size(6.dp))
            Text("%.1f".format(avg), fontSize = 12.sp, color = Color.Gray)
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun HistoryRow(
    row: EntryWithKeywords,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    var menu by remember { mutableStateOf(false) }
    val e = row.entry
    val time = remember(e.timestamp) {
        SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(e.timestamp))
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = { menu = true }, onLongClick = { menu = true })
            .padding(start = 44.dp, end = 16.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Surface(
            shape = HexagonShape,
            color = MoodScale.color(e.score),
            modifier = Modifier.size(width = 26.dp, height = 22.dp),
        ) {}
        Spacer(Modifier.size(10.dp))
        Column(Modifier.weight(1f)) {
            Text(
                MoodScale.label(e.score) +
                    if (e.source == MoodEntry.SOURCE_MANUAL) "  · off-schedule" else "",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
            )
            if (e.note.isNotBlank()) Text(e.note, fontSize = 12.sp, color = Color.Gray)
            if (row.keywords.isNotEmpty()) {
                Text(row.keywordNames.joinToString(" · "), fontSize = 11.sp, color = Color(0xFFD9A22E))
            }
        }
        Text(time, fontSize = 12.sp, color = Color.Gray)

        DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
            DropdownMenuItem(text = { Text("Edit") }, onClick = { menu = false; onEdit() })
            DropdownMenuItem(text = { Text("Delete") }, onClick = { menu = false; onDelete() })
        }
    }
}

@Composable
private fun WorkoutHistoryRow(workouts: List<WorkoutLog>) {
    val regions = remember(workouts) {
        WorkoutLog.REGIONS.filter { r -> workouts.any { it.region == r } }
            .map { WorkoutLog.label(it) }
    }
    val time = remember(workouts) {
        workouts.minByOrNull { it.timestamp }?.let {
            SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(it.timestamp))
        } ?: ""
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 44.dp, end = 16.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            Icons.Default.FitnessCenter,
            contentDescription = null,
            tint = WORKOUT_TINT,
            modifier = Modifier.size(20.dp),
        )
        Spacer(Modifier.size(12.dp))
        Column(Modifier.weight(1f)) {
            Text("Workout", fontSize = 14.sp, fontWeight = FontWeight.Medium)
            Text(regions.joinToString(" · "), fontSize = 12.sp, color = Color.Gray)
        }
        Text(time, fontSize = 12.sp, color = Color.Gray)
    }
}

private fun startOfDay(millis: Long): Long = Calendar.getInstance().apply {
    timeInMillis = millis
    set(Calendar.HOUR_OF_DAY, 0)
    set(Calendar.MINUTE, 0)
    set(Calendar.SECOND, 0)
    set(Calendar.MILLISECOND, 0)
}.timeInMillis

private fun groupByDay(
    entries: List<EntryWithKeywords>,
    workouts: List<WorkoutLog>,
): List<DayGroup> {
    val today = startOfDay(System.currentTimeMillis())
    val yesterday = today - 24L * 60 * 60 * 1000
    val header = SimpleDateFormat("EEEE, MMM d", Locale.getDefault())

    val moodByDay = entries.groupBy { startOfDay(it.entry.timestamp) }
    val workoutByDay = workouts.groupBy { startOfDay(it.timestamp) }
    val allDays = (moodByDay.keys + workoutByDay.keys).toSortedSet(compareByDescending { it })

    return allDays.map { day ->
        val label = when (day) {
            today -> "Today"
            yesterday -> "Yesterday"
            else -> header.format(Date(day))
        }
        DayGroup(
            dayStart = day,
            label = label,
            entries = (moodByDay[day] ?: emptyList()).sortedByDescending { it.entry.timestamp },
            workouts = (workoutByDay[day] ?: emptyList()).sortedBy { it.timestamp },
        )
    }
}

private fun fullTime(millis: Long): String =
    SimpleDateFormat("MMM d, h:mm a", Locale.getDefault()).format(Date(millis))
