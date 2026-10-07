package com.mark.moodlogger.watch

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.HapticFeedbackConstants
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material.Chip
import androidx.wear.compose.material.ChipDefaults
import androidx.wear.compose.material.Colors
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Scaffold
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.TimeText
import kotlinx.coroutines.delay

/**
 * The whole watch app: one screen, five taps. Open it (from the hourly buzz or the
 * app list), tap how you feel, feel a small confirming buzz, and it closes itself.
 * No typing, no notes, nothing to fiddle with, so it's quick enough to do without
 * being noticed.
 */
class MainActivity : ComponentActivity() {

    private var fromNotification by mutableStateOf(false)
    /** Bumped on every launch so a second buzz-tap starts from a clean picker. */
    private var launchCount by mutableStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ReminderReceiver.ensureChannel(this)
        // Stop the phone's reminder notification from being auto-bridged here —
        // otherwise it shows up on top of the watch's own one with a system
        // "Open on phone" action tacked on. See BridgingSetup for the full story.
        BridgingSetup.disablePhoneBridging(this)
        // Start (or refresh) the hourly reminder chain whenever the app opens.
        ReminderScheduler.scheduleNextTopOfHour(this)
        fromNotification = intent.getBooleanExtra(ReminderReceiver.EXTRA_FROM_NOTIFICATION, false)

        setContent {
            WatchTheme {
                MoodApp(
                    fromNotification = fromNotification,
                    launchKey = launchCount,
                    onDone = { finish() },
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        fromNotification = intent.getBooleanExtra(ReminderReceiver.EXTRA_FROM_NOTIFICATION, false)
        launchCount++
    }
}

@Composable
private fun WatchTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colors = Colors(
            primary = Color(0xFFEEAD26),
            primaryVariant = Color(0xFFCBA544),
            secondary = Color(0xFFF88F08),
            background = Color.Black,
            surface = Color(0xFF2A2118),
            onBackground = Color(0xFFF2ECE0),
            onSurface = Color(0xFFF2ECE0),
        ),
        content = content,
    )
}

@Composable
private fun MoodApp(fromNotification: Boolean, launchKey: Int, onDone: () -> Unit) {
    val context = LocalContext.current
    val view = LocalView.current
    var saved by remember(launchKey) { mutableStateOf<Int?>(null) }

    // Notifications need a runtime OK on the watch too. Ask once, on first open.
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            permission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    LaunchedEffect(saved) {
        if (saved != null) {
            delay(1400)
            onDone()
        }
    }

    Scaffold(timeText = { TimeText() }) {
        val picked = saved
        if (picked == null) {
            Picker { score ->
                MoodSender.send(context, score, fromNotification)
                view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                saved = score
            }
        } else {
            Saved(picked)
        }
    }
}

@Composable
private fun Picker(onPick: (Int) -> Unit) {
    val listState = rememberScalingLazyListState()
    ScalingLazyColumn(
        modifier = Modifier.fillMaxSize(),
        state = listState,
        verticalArrangement = Arrangement.spacedBy(6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        item {
            Text(
                text = "How are you?",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFFF2ECE0),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 2.dp),
            )
        }
        // Best at the top: the first thing the eye lands on is the happy end.
        for (score in 5 downTo 1) {
            item {
                Chip(
                    onClick = { onPick(score) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ChipDefaults.chipColors(
                        backgroundColor = MoodScale.color(score),
                        contentColor = MoodScale.ink(score),
                    ),
                    label = {
                        Text(
                            text = "${MoodScale.emoji(score)}  ${MoodScale.label(score)}",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MoodScale.ink(score),
                        )
                    },
                )
            }
        }
    }
}

@Composable
private fun Saved(score: Int) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = MoodScale.emoji(score), fontSize = 44.sp)
            Text(
                text = "Logged",
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = MoodScale.color(score),
            )
            Text(
                text = MoodScale.label(score),
                fontSize = 13.sp,
                color = Color(0xFFF2ECE0),
            )
        }
    }
}
