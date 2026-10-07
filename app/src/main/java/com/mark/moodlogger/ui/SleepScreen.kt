package com.mark.moodlogger.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberTimePickerState
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
import com.mark.moodlogger.data.MORNING_FEELINGS
import com.mark.moodlogger.data.Settings
import com.mark.moodlogger.data.SleepLog
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val QUALITY_LABELS = listOf("Rough", "Poor", "OK", "Good", "Great")

/** Latency chip labels to stored-minutes values, in order. */
private val LATENCY_OPTIONS = listOf("< 10 min" to 5, "10-20 min" to 15, "20-40 min" to 30, "40+ min" to 50)

/** Night-wakings chip labels to stored counts, in order. */
private val WAKINGS_OPTIONS = listOf("0" to 0, "1" to 1, "2" to 2, "3+" to 3)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SleepScreen(
    existing: SleepLog?,
    settings: Settings,
    onSave: (
        dateEpochDay: Long, bedtimeMillis: Long, wakeMillis: Long, quality: Int,
        latencyMinutes: Int, nightWakings: Int, morningFeeling: String,
    ) -> Unit,
    onDelete: (dateEpochDay: Long) -> Unit,
    onBack: () -> Unit,
) {
    val zone = remember { ZoneId.systemDefault() }
    val guess = remember(settings) { chargeGuess(settings, zone) }

    val initBed: LocalTime = when {
        existing != null -> Instant.ofEpochMilli(existing.bedtimeMillis).atZone(zone).toLocalTime()
        guess != null -> guess.first
        else -> LocalTime.of(23, 0)
    }
    val initWake: LocalTime = when {
        existing != null -> Instant.ofEpochMilli(existing.wakeMillis).atZone(zone).toLocalTime()
        guess != null -> guess.second
        else -> LocalTime.of(7, 0)
    }

    var bedH by remember { mutableIntStateOf(initBed.hour) }
    var bedM by remember { mutableIntStateOf(initBed.minute) }
    var wakeH by remember { mutableIntStateOf(initWake.hour) }
    var wakeM by remember { mutableIntStateOf(initWake.minute) }
    var quality by remember { mutableIntStateOf(existing?.quality ?: 0) }
    var latencyMinutes by remember { mutableIntStateOf(existing?.latencyMinutes ?: 0) }
    var nightWakings by remember { mutableIntStateOf(existing?.nightWakings ?: 0) }
    var morningFeeling by remember { mutableStateOf(existing?.morningFeeling ?: "") }
    var picking by remember { mutableStateOf<String?>(null) } // "bed" | "wake" | null

    val fmt = remember { DateTimeFormatter.ofPattern("h:mm a") }
    val durationMin = run {
        var d = (wakeH * 60 + wakeM) - (bedH * 60 + bedM)
        if (d <= 0) d += 24 * 60
        d
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Last night's sleep") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { pad ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(pad)
                .honeycombBackground(MaterialTheme.colorScheme.background)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Spacer(Modifier.size(4.dp))

            if (existing == null && guess != null) {
                Card(Modifier.fillMaxWidth()) {
                    Text(
                        "Guessed from your charger: ${guess.first.format(fmt)} to " +
                            "${guess.second.format(fmt)}. Adjust below if it's off.",
                        fontSize = 12.sp,
                        color = Color.Gray,
                        modifier = Modifier.padding(12.dp),
                    )
                }
            }

            TimeRow("Went to bed", LocalTime.of(bedH, bedM).format(fmt)) { picking = "bed" }
            TimeRow("Woke up", LocalTime.of(wakeH, wakeM).format(fmt)) { picking = "wake" }

            Text(
                "%dh %02dm".format(durationMin / 60, durationMin % 60),
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.align(Alignment.CenterHorizontally),
            )

            Text("How did you sleep?", fontSize = 13.sp, color = Color.Gray)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                QUALITY_LABELS.forEachIndexed { i, label ->
                    val value = i + 1
                    FilterChip(
                        selected = quality == value,
                        onClick = { quality = if (quality == value) 0 else value },
                        label = { Text(label) },
                    )
                }
            }

            Text("This morning I feel", fontSize = 13.sp, color = Color.Gray)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MORNING_FEELINGS.forEach { label ->
                    FilterChip(
                        selected = morningFeeling == label,
                        onClick = { morningFeeling = if (morningFeeling == label) "" else label },
                        label = { Text(label) },
                    )
                }
            }

            Text("How long to fall asleep?", fontSize = 13.sp, color = Color.Gray)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                LATENCY_OPTIONS.forEach { (label, value) ->
                    FilterChip(
                        selected = latencyMinutes == value,
                        onClick = { latencyMinutes = if (latencyMinutes == value) 0 else value },
                        label = { Text(label) },
                    )
                }
            }

            Text("Times woken up", fontSize = 13.sp, color = Color.Gray)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                WAKINGS_OPTIONS.forEach { (label, value) ->
                    FilterChip(
                        selected = nightWakings == value,
                        onClick = { nightWakings = value },
                        label = { Text(label) },
                    )
                }
            }

            Spacer(Modifier.size(8.dp))
            Button(
                onClick = {
                    val date = LocalDate.now()
                    val wakeMillis = date.atTime(wakeH, wakeM)
                        .atZone(zone).toInstant().toEpochMilli()
                    var bed = date.atTime(bedH, bedM).atZone(zone).toInstant().toEpochMilli()
                    if (bed >= wakeMillis) bed -= 24L * 60 * 60 * 1000
                    onSave(date.toEpochDay(), bed, wakeMillis, quality, latencyMinutes, nightWakings, morningFeeling)
                },
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(if (existing == null) "Save" else "Update", fontSize = 16.sp)
            }

            if (existing != null) {
                TextButton(
                    onClick = { onDelete(existing.dateEpochDay) },
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                ) { Text("Delete this night") }
            }

            if (!settings.sleepAssistEnabled) {
                Text(
                    "Tip: turn on the sleep guess in Settings and the times will be " +
                        "pre-filled from when you plug in and unplug your charger.",
                    fontSize = 11.sp,
                    color = Color.Gray,
                )
            }
            Spacer(Modifier.size(12.dp))
        }
    }

    picking?.let { which ->
        val h = if (which == "bed") bedH else wakeH
        val m = if (which == "bed") bedM else wakeM
        val state = rememberTimePickerState(initialHour = h, initialMinute = m, is24Hour = false)
        AlertDialog(
            onDismissRequest = { picking = null },
            confirmButton = {
                TextButton(onClick = {
                    if (which == "bed") { bedH = state.hour; bedM = state.minute }
                    else { wakeH = state.hour; wakeM = state.minute }
                    picking = null
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { picking = null }) { Text("Cancel") } },
            text = { TimePicker(state = state) },
        )
    }
}

@Composable
private fun TimeRow(label: String, value: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, modifier = Modifier.weight(1f), fontSize = 15.sp)
        OutlinedButton(onClick = onClick) { Text(value) }
    }
}

/** A bed/wake guess from the charger times, or null if they don't look like a night. */
private fun chargeGuess(settings: Settings, zone: ZoneId): Pair<LocalTime, LocalTime>? {
    if (!settings.sleepAssistEnabled) return null
    val inAt = settings.lastPluggedInAt
    val outAt = settings.lastUnpluggedAt
    if (inAt == 0L || outAt == 0L || outAt <= inAt) return null
    val spanHours = (outAt - inAt) / 3_600_000.0
    if (spanHours < 3.0 || spanHours > 14.0) return null
    val bedT = Instant.ofEpochMilli(inAt).atZone(zone).toLocalTime()
    val wakeT = Instant.ofEpochMilli(outAt).atZone(zone).toLocalTime()
    val bedOk = bedT.hour >= 19 || bedT.hour <= 3
    val wakeOk = wakeT.hour in 3..12
    if (!bedOk || !wakeOk) return null
    return bedT to wakeT
}
