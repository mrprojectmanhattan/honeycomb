package com.mark.moodlogger.ui

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings as AndroidSettings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.core.content.ContextCompat
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mark.moodlogger.data.Settings
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    settings: Settings,
    onBack: () -> Unit,
    onOpenKeywords: () -> Unit,
    viewModel: MainViewModel,
    onRemindersEnabled: (Boolean) -> Unit,
    onQuietHoursEnabled: (Boolean) -> Unit,
    onQuietStart: (Int) -> Unit,
    onQuietEnd: (Int) -> Unit,
    onNavPosition: (String) -> Unit,
    onNavRailAlign: (String) -> Unit,
    onWaterGoal: (Int) -> Unit,
    onWaterServing: (Int) -> Unit,
    onSleepAssist: (Boolean) -> Unit,
    onSleepGoal: (Int) -> Unit,
    onStepCountEnabled: (Boolean) -> Unit,
    onOpenTimer: () -> Unit,
    onOpenAbout: () -> Unit,
    onDailyCalorieBudget: (Int) -> Unit,
    onSetAppLockPin: (String) -> Unit,
    onClearAppLock: () -> Unit,
    onSetAppLockBiometric: (Boolean) -> Unit,
    onCheckinRemindersEnabled: (Boolean) -> Unit,
) {
    val context = LocalContext.current
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    var showPinDialog by remember { mutableStateOf(false) }

    val activityPermLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> onStepCountEnabled(granted) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
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
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            NavRow(
                title = "Timer",
                subtitle = "A named countdown timer with an alarm.",
                onClick = onOpenTimer,
            )

            HorizontalDivider()

            ToggleRow(
                title = "Hourly reminders",
                subtitle = "A notification on the hour asking you to log your mood.",
                checked = settings.remindersEnabled,
                onCheckedChange = onRemindersEnabled,
            )

            HorizontalDivider()

            ToggleRow(
                title = "Quiet hours",
                subtitle = "No reminders during the window below.",
                checked = settings.quietHoursEnabled,
                onCheckedChange = onQuietHoursEnabled,
            )

            HourStepper(
                label = "Quiet from",
                hour = settings.quietStartHour,
                enabled = settings.quietHoursEnabled,
                onChange = onQuietStart,
            )
            HourStepper(
                label = "Quiet until",
                hour = settings.quietEndHour,
                enabled = settings.quietHoursEnabled,
                onChange = onQuietEnd,
            )

            HorizontalDivider()

            Column {
                Text("Menu position", fontWeight = FontWeight.Medium, fontSize = 16.sp)
                Text(
                    "Where the menu sits on the main screen.",
                    fontSize = 13.sp,
                    color = Color.Gray,
                )
                Spacer(Modifier.size(8.dp))
                val options = listOf(
                    Settings.NAV_TOP to "Top",
                    Settings.NAV_BOTTOM to "Bottom",
                    Settings.NAV_LEFT to "Left",
                    Settings.NAV_RIGHT to "Right",
                )
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    options.forEachIndexed { i, (value, label) ->
                        SegmentedButton(
                            selected = settings.navPosition == value,
                            onClick = { onNavPosition(value) },
                            shape = SegmentedButtonDefaults.itemShape(i, options.size),
                        ) { Text(label) }
                    }
                }

                val onSide = settings.navPosition == Settings.NAV_LEFT ||
                    settings.navPosition == Settings.NAV_RIGHT
                if (onSide) {
                    Spacer(Modifier.size(16.dp))
                    Text("Icon alignment", fontWeight = FontWeight.Medium, fontSize = 15.sp)
                    Text(
                        "Where the icons sit on the side bar.",
                        fontSize = 13.sp,
                        color = Color.Gray,
                    )
                    Spacer(Modifier.size(8.dp))
                    val aligns = listOf(
                        Settings.RAIL_TOP to "Top",
                        Settings.RAIL_CENTER to "Center",
                        Settings.RAIL_BOTTOM to "Bottom",
                    )
                    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                        aligns.forEachIndexed { i, (value, label) ->
                            SegmentedButton(
                                selected = settings.navRailAlign == value,
                                onClick = { onNavRailAlign(value) },
                                shape = SegmentedButtonDefaults.itemShape(i, aligns.size),
                            ) { Text(label) }
                        }
                    }
                }
            }

            HorizontalDivider()

            Column {
                Text("Water", fontWeight = FontWeight.Medium, fontSize = 16.sp)
                Text(
                    "Your daily target and how much one tap adds.",
                    fontSize = 13.sp,
                    color = Color.Gray,
                )
                Spacer(Modifier.size(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Daily goal", modifier = Modifier.weight(1f), fontSize = 15.sp)
                    OutlinedButton(
                        onClick = { onWaterGoal(settings.waterGoalOz - 8) },
                        enabled = settings.waterGoalOz > Settings.WATER_GOAL_MIN,
                    ) { Text("-") }
                    Spacer(Modifier.size(8.dp))
                    Text(
                        "${settings.waterGoalOz} oz",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                    )
                    Spacer(Modifier.size(8.dp))
                    OutlinedButton(
                        onClick = { onWaterGoal(settings.waterGoalOz + 8) },
                        enabled = settings.waterGoalOz < Settings.WATER_GOAL_MAX,
                    ) { Text("+") }
                }
                Spacer(Modifier.size(10.dp))
                Text("Serving size", fontSize = 15.sp)
                Spacer(Modifier.size(6.dp))
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    Settings.WATER_SERVINGS.forEachIndexed { i, oz ->
                        SegmentedButton(
                            selected = settings.waterServingOz == oz,
                            onClick = { onWaterServing(oz) },
                            shape = SegmentedButtonDefaults.itemShape(i, Settings.WATER_SERVINGS.size),
                        ) { Text("$oz oz") }
                    }
                }
            }

            HorizontalDivider()

            Column {
                Text("Sleep", fontWeight = FontWeight.Medium, fontSize = 16.sp)
                Spacer(Modifier.size(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Nightly goal", modifier = Modifier.weight(1f), fontSize = 15.sp)
                    OutlinedButton(onClick = { onSleepGoal(settings.sleepGoalHours - 1) }) { Text("-") }
                    Spacer(Modifier.size(8.dp))
                    Text(
                        "${settings.sleepGoalHours} h",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                    )
                    Spacer(Modifier.size(8.dp))
                    OutlinedButton(onClick = { onSleepGoal(settings.sleepGoalHours + 1) }) { Text("+") }
                }
                Spacer(Modifier.size(8.dp))
                ToggleRow(
                    title = "Guess my sleep times",
                    subtitle = "Pre-fills the sleep log from when you plug in and unplug " +
                        "your charger at night. Watches nothing else. Off unless you turn it on.",
                    checked = settings.sleepAssistEnabled,
                    onCheckedChange = onSleepAssist,
                )
            }

            HorizontalDivider()

            ToggleRow(
                title = "Count my steps",
                subtitle = "Reads your phone's built-in step counter - just a number, no " +
                    "location or anything else. Off unless you turn it on; steps can always " +
                    "be entered by hand.",
                checked = settings.stepCountEnabled,
                onCheckedChange = { want ->
                    if (!want) {
                        onStepCountEnabled(false)
                    } else if (
                        Build.VERSION.SDK_INT < Build.VERSION_CODES.Q ||
                        ContextCompat.checkSelfPermission(
                            context, android.Manifest.permission.ACTIVITY_RECOGNITION,
                        ) == PackageManager.PERMISSION_GRANTED
                    ) {
                        onStepCountEnabled(true)
                    } else {
                        activityPermLauncher.launch(android.Manifest.permission.ACTIVITY_RECOGNITION)
                    }
                },
            )

            HorizontalDivider()

            Column {
                Text("Nutrition", fontWeight = FontWeight.Medium, fontSize = 16.sp)
                Text(
                    "Your daily calorie target for the Nutrition screen.",
                    fontSize = 13.sp,
                    color = Color.Gray,
                )
                Spacer(Modifier.size(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Daily budget", modifier = Modifier.weight(1f), fontSize = 15.sp)
                    OutlinedButton(onClick = { onDailyCalorieBudget(settings.dailyCalorieBudget - 100) }) { Text("-") }
                    Spacer(Modifier.size(8.dp))
                    Text(
                        "${settings.dailyCalorieBudget} cal",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                    )
                    Spacer(Modifier.size(8.dp))
                    OutlinedButton(onClick = { onDailyCalorieBudget(settings.dailyCalorieBudget + 100) }) { Text("+") }
                }
            }

            HorizontalDivider()

            ToggleRow(
                title = "Weekly check-in reminder",
                subtitle = "A once-a-week nudge (Sunday evening) to do a PHQ-9 or GAD-7 in Care.",
                checked = settings.checkinRemindersEnabled,
                onCheckedChange = onCheckinRemindersEnabled,
            )

            HorizontalDivider()

            Column {
                Text("Privacy", fontWeight = FontWeight.Medium, fontSize = 16.sp)
                Text(
                    "A PIN or biometric lock over the whole app. Off by default.",
                    fontSize = 13.sp,
                    color = Color.Gray,
                )
                Spacer(Modifier.size(8.dp))
                ToggleRow(
                    title = "Require a PIN to open Honeycomb",
                    subtitle = if (settings.appLockEnabled) {
                        "Locked whenever you leave and come back."
                    } else {
                        "Turning this on will ask you to set a PIN."
                    },
                    checked = settings.appLockEnabled,
                    onCheckedChange = { want ->
                        if (want) showPinDialog = true else onClearAppLock()
                    },
                )
                if (settings.appLockEnabled) {
                    Spacer(Modifier.size(8.dp))
                    ToggleRow(
                        title = "Also allow fingerprint",
                        subtitle = "Use a fingerprint instead of typing the PIN.",
                        checked = settings.appLockBiometricEnabled,
                        onCheckedChange = onSetAppLockBiometric,
                    )
                    Spacer(Modifier.size(8.dp))
                    NavRow(
                        title = "Change PIN",
                        subtitle = "Set a new 6-digit PIN.",
                        onClick = { showPinDialog = true },
                    )
                }
            }

            HorizontalDivider()

            NavRow(
                title = "Tags",
                subtitle = "The keywords you can attach to an entry.",
                onClick = onOpenKeywords,
            )

            HorizontalDivider()

            OutlinedButton(
                onClick = {
                    scope.launch {
                        val uri = viewModel.buildCsvUri()
                        val send = Intent(Intent.ACTION_SEND).apply {
                            type = "text/csv"
                            putExtra(Intent.EXTRA_STREAM, uri)
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        context.startActivity(Intent.createChooser(send, "Export mood log"))
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Export all data as CSV")
            }

            HorizontalDivider()

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                Text(
                    "If reminders drift off the hour, allow exact alarms for this app.",
                    fontSize = 13.sp,
                    color = Color.Gray,
                )
                OutlinedButton(onClick = {
                    runCatching {
                        context.startActivity(
                            Intent(AndroidSettings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                        )
                    }
                }) {
                    Text("Open alarm permission settings")
                }
            }

            HorizontalDivider()

            NavRow(
                title = "About Honeycomb",
                subtitle = "What the app does with your data (nothing leaves the phone).",
                onClick = onOpenAbout,
            )

            Spacer(Modifier.size(8.dp))
        }
    }

    if (showPinDialog) {
        SetPinDialog(
            onDismiss = { showPinDialog = false },
            onSave = { pin ->
                onSetAppLockPin(pin)
                showPinDialog = false
            },
        )
    }
}

/**
 * Two 6-digit fields, new PIN + confirm, only calls onSave when they match. Same
 * required-fields-only shape as NutritionScreen's CustomFoodDialog.
 */
@Composable
private fun SetPinDialog(
    onDismiss: () -> Unit,
    onSave: (String) -> Unit,
) {
    var pin by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    val valid = pin.length == 6 && pin.all { it.isDigit() }
    val matches = valid && pin == confirm
    val showMismatch = confirm.isNotEmpty() && confirm.length == 6 && !matches

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Set a PIN") },
        text = {
            Column {
                OutlinedTextField(
                    value = pin,
                    onValueChange = { if (it.length <= 6 && it.all(Char::isDigit)) pin = it },
                    label = { Text("New 6-digit PIN") },
                    singleLine = true,
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.size(8.dp))
                OutlinedTextField(
                    value = confirm,
                    onValueChange = { if (it.length <= 6 && it.all(Char::isDigit)) confirm = it },
                    label = { Text("Confirm PIN") },
                    singleLine = true,
                    isError = showMismatch,
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    modifier = Modifier.fillMaxWidth(),
                )
                if (showMismatch) {
                    Spacer(Modifier.size(4.dp))
                    Text("Doesn't match", color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                }
            }
        },
        confirmButton = {
            TextButton(enabled = matches, onClick = { onSave(pin) }) { Text("Set PIN") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun NavRow(
    title: String,
    subtitle: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.Medium, fontSize = 16.sp)
            Text(subtitle, fontSize = 13.sp, color = Color.Gray)
        }
        Text("›", fontSize = 18.sp, color = Color.Gray)
    }
}

@Composable
private fun ToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.Medium, fontSize = 16.sp)
            Text(subtitle, fontSize = 13.sp, color = Color.Gray)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = MaterialTheme.colorScheme.primary,
            ),
        )
    }
}

@Composable
private fun HourStepper(
    label: String,
    hour: Int,
    enabled: Boolean,
    onChange: (Int) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, modifier = Modifier.weight(1f), fontSize = 15.sp)
        OutlinedButton(
            onClick = { onChange((hour + 23) % 24) },
            enabled = enabled,
        ) { Text("-") }
        Spacer(Modifier.size(8.dp))
        Text(formatHour(hour), fontSize = 15.sp, fontWeight = FontWeight.Medium)
        Spacer(Modifier.size(8.dp))
        OutlinedButton(
            onClick = { onChange((hour + 1) % 24) },
            enabled = enabled,
        ) { Text("+") }
    }
}

private fun formatHour(hour: Int): String {
    val h12 = when (hour % 12) {
        0 -> 12
        else -> hour % 12
    }
    val suffix = if (hour < 12) "AM" else "PM"
    return "$h12:00 $suffix"
}
