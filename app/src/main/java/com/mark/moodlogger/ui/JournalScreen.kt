package com.mark.moodlogger.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.mark.moodlogger.audio.AudioPlayer
import com.mark.moodlogger.audio.AudioRecorder
import com.mark.moodlogger.data.JournalEntry
import java.io.File
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun JournalScreen(
    entries: List<JournalEntry>,
    onSave: (id: Long, title: String, text: String, audioPath: String?, audioDurationMs: Long) -> Unit,
    onDelete: (Long) -> Unit,
    onBack: () -> Unit,
    startNew: Boolean = false,
    onStartNewConsumed: () -> Unit = {},
) {
    var editing by remember { mutableStateOf<JournalEntry?>(null) }
    var query by remember { mutableStateOf("") }
    // Which day sections are open. Absent = use the default (today open, rest closed).
    val expanded = remember { mutableStateMapOf<Long, Boolean>() }

    // Arriving here from the "want to add a reflection?" nudge: open a fresh
    // entry for today straight away.
    LaunchedEffect(startNew) {
        if (startNew) {
            editing = JournalEntry(day = LocalDate.now().toEpochDay())
            onStartNewConsumed()
        }
    }

    editing?.let { entry ->
        JournalEditor(
            entry = entry,
            onDone = { title, text, path, dur -> onSave(entry.id, title, text, path, dur); editing = null },
            onDelete = { if (entry.id != 0L) onDelete(entry.id); editing = null },
            onBack = { editing = null },
        )
        return
    }

    val q = query.trim().lowercase()
    val searching = q.isNotEmpty()
    val shown = if (!searching) entries else entries.filter {
        it.title.lowercase().contains(q) || it.text.lowercase().contains(q)
    }
    val todayDay = LocalDate.now().toEpochDay()

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            if (entries.isNotEmpty()) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = { Text("Search reflections") },
                    leadingIcon = { Icon(Icons.Filled.Search, null) },
                    trailingIcon = {
                        if (query.isNotEmpty()) {
                            IconButton(onClick = { query = "" }) {
                                Icon(Icons.Filled.Close, "Clear search")
                            }
                        }
                    },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                )
            }

            if (entries.isEmpty()) {
                Column(Modifier.padding(32.dp)) {
                    Text("Journal", style = MaterialTheme.typography.headlineSmall)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Write or speak a reflection. Tap + to start.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else if (shown.isEmpty()) {
                Column(Modifier.padding(32.dp)) {
                    Text(
                        "No reflections match “$query”.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                val byDay = shown.groupBy { it.day }.toSortedMap(compareByDescending { it })
                LazyColumn(Modifier.fillMaxSize().padding(horizontal = 12.dp)) {
                    byDay.forEach { (day, dayEntries) ->
                        val isOpen = searching || (expanded[day] ?: (day == todayDay))
                        item(key = "h$day") {
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .clickable(enabled = !searching) {
                                        expanded[day] = !(expanded[day] ?: (day == todayDay))
                                    }
                                    .padding(vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    dayLabel(day),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                )
                                Spacer(Modifier.size(8.dp))
                                Text(
                                    "${dayEntries.size}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Spacer(Modifier.weight(1f))
                                if (!searching) {
                                    Icon(
                                        if (isOpen) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                                        if (isOpen) "Collapse" else "Expand",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }
                        if (isOpen) {
                            items(dayEntries, key = { it.id }) { e ->
                                Card(
                                    onClick = { editing = e },
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                                ) {
                                    Row(
                                        Modifier.padding(14.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(Modifier.weight(1f)) {
                                            if (e.title.isNotBlank()) {
                                                Text(
                                                    e.title,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis,
                                                    fontWeight = FontWeight.SemiBold,
                                                    style = MaterialTheme.typography.bodyLarge
                                                )
                                            }
                                            if (e.text.isNotBlank()) {
                                                Text(
                                                    e.text,
                                                    maxLines = if (e.title.isNotBlank()) 2 else 3,
                                                    overflow = TextOverflow.Ellipsis,
                                                    style = MaterialTheme.typography.bodyMedium
                                                )
                                            } else if (e.title.isBlank()) {
                                                Text(
                                                    "Voice reflection",
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    style = MaterialTheme.typography.bodyMedium
                                                )
                                            }
                                            Text(
                                                timeLabel(e.timestamp),
                                                color = MaterialTheme.colorScheme.outline,
                                                style = MaterialTheme.typography.bodyMedium
                                            )
                                        }
                                        if (e.audioPath != null) {
                                            Icon(
                                                Icons.Filled.GraphicEq, "Has audio",
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                    item(key = "tail") { Spacer(Modifier.height(88.dp)) }
                }
            }
        }

        FloatingActionButton(
            onClick = { editing = JournalEntry(day = LocalDate.now().toEpochDay()) },
            modifier = Modifier.align(Alignment.BottomEnd).padding(20.dp)
        ) { Icon(Icons.Filled.Add, "New reflection") }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun JournalEditor(
    entry: JournalEntry,
    onDone: (title: String, text: String, audioPath: String?, audioDurationMs: Long) -> Unit,
    onDelete: () -> Unit,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    var title by remember { mutableStateOf(entry.title) }
    var text by remember { mutableStateOf(entry.text) }
    var audioPath by remember { mutableStateOf(entry.audioPath) }
    var audioDur by remember { mutableLongStateOf(entry.audioDurationMs) }

    val recorder = remember { AudioRecorder(context) }
    val player = remember { AudioPlayer() }
    var recording by remember { mutableStateOf(false) }
    var elapsed by remember { mutableLongStateOf(0L) }
    var playing by remember { mutableStateOf(false) }

    var hasPerm by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    val ask = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        hasPerm = it
    }

    fun commit() {
        if (recorder.isRecording) recorder.stop()
        player.stop()
        onDone(title, text, audioPath, audioDur)
    }

    // System back / back-gesture from the editor saves and returns to the list,
    // same as the top-bar arrow and the Save button (takes precedence over
    // AppRoot's BackHandler).
    BackHandler { commit() }

    DisposableEffect(Unit) {
        onDispose {
            if (recorder.isRecording) recorder.stop()
            player.stop()
        }
    }
    LaunchedEffect(recording) {
        val start = System.currentTimeMillis()
        while (recording) {
            elapsed = System.currentTimeMillis() - start
            kotlinx.coroutines.delay(100)
        }
    }
    LaunchedEffect(playing) {
        while (playing) {
            if (!player.isPlaying("clip")) playing = false
            kotlinx.coroutines.delay(150)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (entry.id == 0L) "New reflection" else "Reflection") },
                navigationIcon = {
                    IconButton(onClick = { commit() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Save and back")
                    }
                },
                actions = {
                    IconButton(onClick = { commit() }) { Icon(Icons.Filled.Check, "Save") }
                    if (entry.id != 0L) {
                        IconButton(onClick = onDelete) { Icon(Icons.Filled.Delete, "Delete") }
                    }
                }
            )
        }
    ) { pad ->
        Column(Modifier.fillMaxSize().padding(pad).padding(horizontal = 16.dp)) {
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                placeholder = { Text("Title (optional)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            )

            Spacer(Modifier.height(8.dp))

            TextField(
                value = text,
                onValueChange = { text = it },
                placeholder = { Text("How are things?") },
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent
                ),
                modifier = Modifier.fillMaxWidth().weight(1f)
            )

            Spacer(Modifier.height(8.dp))

            // voice reflection control
            Row(verticalAlignment = Alignment.CenterVertically) {
                FilledIconButton(
                    onClick = {
                        when {
                            recording -> {
                                recording = false
                                recorder.stop()?.let { (f, d) ->
                                    audioPath?.let { old -> runCatching { File(old).delete() } }
                                    audioPath = f.absolutePath
                                    audioDur = d
                                }
                            }
                            !hasPerm -> ask.launch(Manifest.permission.RECORD_AUDIO)
                            else -> { recorder.start(); recording = true }
                        }
                    },
                    modifier = Modifier.size(56.dp),
                    shape = CircleShape,
                    colors = if (recording)
                        IconButtonDefaults.filledIconButtonColors(containerColor = MaterialTheme.colorScheme.error)
                    else IconButtonDefaults.filledIconButtonColors()
                ) {
                    Icon(if (recording) Icons.Filled.Stop else Icons.Filled.Mic, "Record")
                }

                Spacer(Modifier.size(12.dp))

                when {
                    recording -> Text(formatMs(elapsed), fontSize = 18.sp, color = MaterialTheme.colorScheme.primary)
                    audioPath != null -> {
                        IconButton(onClick = {
                            if (player.isPlaying("clip")) { player.pause(); playing = false }
                            else { player.play("clip", audioPath!!) { playing = false }; playing = true }
                        }) {
                            Icon(if (playing) Icons.Filled.Pause else Icons.Filled.PlayArrow, "Play")
                        }
                        Text(formatMs(audioDur), color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.weight(1f))
                        TextButton(onClick = {
                            player.stop(); playing = false
                            audioPath?.let { runCatching { File(it).delete() } }
                            audioPath = null; audioDur = 0
                        }) { Text("Remove clip") }
                    }
                    else -> Text("Add a voice reflection", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            Spacer(Modifier.height(10.dp))

            Button(
                onClick = { commit() },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.Filled.Check, null)
                Spacer(Modifier.size(8.dp))
                Text("Save")
            }

            Spacer(Modifier.height(12.dp))
        }
    }
}

private fun formatMs(ms: Long): String {
    val s = ms / 1000
    return String.format(Locale.getDefault(), "%d:%02d", s / 60, s % 60)
}

private fun dayLabel(epochDay: Long): String {
    val d = LocalDate.ofEpochDay(epochDay)
    val today = LocalDate.now()
    return when (epochDay) {
        today.toEpochDay() -> "Today"
        today.minusDays(1).toEpochDay() -> "Yesterday"
        else -> d.format(DateTimeFormatter.ofPattern("EEEE, MMM d"))
    }
}

private fun timeLabel(ts: Long): String =
    java.text.SimpleDateFormat("h:mm a", Locale.getDefault()).format(java.util.Date(ts))
