package com.mark.moodlogger.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mark.moodlogger.data.Settings
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimerScreen(
    settings: Settings,
    onBack: () -> Unit,
    onStart: (label: String, durationMillis: Long) -> Unit,
    onCancel: () -> Unit,
) {
    val active = settings.timerTriggerAt > 0L

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Timer") },
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
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (active) {
                RunningTimer(settings = settings, onCancel = onCancel)
            } else {
                TimerSetup(onStart = onStart)
            }
        }
    }
}

@Composable
private fun RunningTimer(settings: Settings, onCancel: () -> Unit) {
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(settings.timerTriggerAt) {
        while (true) {
            now = System.currentTimeMillis()
            delay(500)
        }
    }
    val remaining = (settings.timerTriggerAt - now).coerceAtLeast(0L)
    val endsAt = remember(settings.timerTriggerAt) {
        SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(settings.timerTriggerAt))
    }

    Spacer(Modifier.size(48.dp))
    Text(
        settings.timerLabel.ifBlank { "Timer" },
        fontSize = 20.sp,
        fontWeight = FontWeight.SemiBold,
        textAlign = TextAlign.Center,
    )
    Spacer(Modifier.size(24.dp))
    Text(
        formatRemaining(remaining),
        fontSize = 60.sp,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
    )
    Spacer(Modifier.size(8.dp))
    Text("Ends at $endsAt", fontSize = 13.sp, color = Color.Gray)
    Spacer(Modifier.size(44.dp))
    OutlinedButton(onClick = onCancel, modifier = Modifier.fillMaxWidth()) {
        Text("Cancel timer")
    }
}

@Composable
private fun TimerSetup(onStart: (String, Long) -> Unit) {
    var label by remember { mutableStateOf("") }
    var hours by remember { mutableIntStateOf(0) }
    var minutes by remember { mutableIntStateOf(5) }

    val totalMs = (hours * 3600L + minutes * 60L) * 1000L

    Spacer(Modifier.size(28.dp))
    OutlinedTextField(
        value = label,
        onValueChange = { label = it.take(60) },
        label = { Text("Name (optional)") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
    )
    Spacer(Modifier.size(32.dp))
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        Stepper("Hours", hours, 0, 23) { hours = it }
        Stepper("Minutes", minutes, 0, 59) { minutes = it }
    }
    Spacer(Modifier.size(36.dp))
    Button(
        onClick = { onStart(label, totalMs) },
        enabled = totalMs > 0L,
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text("Start timer", fontSize = 16.sp)
    }
}

@Composable
private fun Stepper(
    label: String,
    value: Int,
    min: Int,
    max: Int,
    onChange: (Int) -> Unit,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, fontSize = 13.sp, color = Color.Gray)
        Spacer(Modifier.size(4.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { if (value > min) onChange(value - 1) }) {
                Icon(Icons.Default.Remove, contentDescription = "minus")
            }
            Text(
                value.toString().padStart(2, '0'),
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
            )
            IconButton(onClick = { if (value < max) onChange(value + 1) }) {
                Icon(Icons.Default.Add, contentDescription = "plus")
            }
        }
    }
}

private fun formatRemaining(ms: Long): String {
    // Round up so a timer with any time left never reads 0:00.
    val totalSec = (ms + 999L) / 1000L
    val h = totalSec / 3600L
    val m = (totalSec % 3600L) / 60L
    val s = totalSec % 60L
    return if (h > 0L) "%d:%02d:%02d".format(h, m, s) else "%d:%02d".format(m, s)
}
