package com.mark.moodlogger.ui

import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mark.moodlogger.data.ShoppingItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShoppingScreen(
    items: List<ShoppingItem>,
    remindersEnabled: Boolean,
    reminderHour: Int,
    escalateAfterDays: Int,
    onAdd: (String) -> Unit,
    onSetPurchased: (Long, Boolean) -> Unit,
    onDelete: (Long) -> Unit,
    onClearPurchased: () -> Unit,
    onRemindersEnabled: (Boolean) -> Unit,
    onReminderHour: (Int) -> Unit,
    onEscalateAfterDays: (Int) -> Unit,
    onBack: () -> Unit,
) {
    val now = remember(items) { System.currentTimeMillis() }
    val outstanding = items.filter { it.outstanding }
    val bought = items.filter { !it.outstanding }

    var newItem by remember { mutableStateOf("") }
    var showBought by remember { mutableStateOf(false) }
    val submit = {
        onAdd(newItem)
        newItem = ""
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Shopping list") },
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
                .padding(horizontal = 16.dp),
        ) {
            Spacer(Modifier.size(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = newItem,
                    onValueChange = { newItem = it },
                    label = { Text("Add an item") },
                    singleLine = true,
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        imeAction = ImeAction.Done,
                    ),
                    keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                        onDone = { submit() },
                    ),
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.size(8.dp))
                IconButton(onClick = submit, enabled = newItem.isNotBlank()) {
                    Icon(Icons.Filled.Add, contentDescription = "Add")
                }
            }

            Spacer(Modifier.size(8.dp))

            if (outstanding.isEmpty() && bought.isEmpty()) {
                Text(
                    "Nothing on the list. Add what you need above and Honeycomb will nudge " +
                        "you about it once a day until it's checked off.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(vertical = 12.dp),
                )
            }

            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                items(outstanding, key = { it.id }) { item ->
                    ShoppingRow(
                        item = item,
                        now = now,
                        stale = item.ageDays(now) >= escalateAfterDays,
                        onSetPurchased = onSetPurchased,
                        onDelete = onDelete,
                    )
                }

                if (bought.isNotEmpty()) {
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp)
                                .clickable { showBought = !showBought },
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                (if (showBought) "Hide bought" else "Bought") + " (${bought.size})",
                                fontWeight = FontWeight.Medium,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.weight(1f),
                            )
                            TextButton(onClick = onClearPurchased) { Text("Clear") }
                        }
                    }
                    if (showBought) {
                        items(bought, key = { it.id }) { item ->
                            ShoppingRow(
                                item = item,
                                now = now,
                                stale = false,
                                onSetPurchased = onSetPurchased,
                                onDelete = onDelete,
                            )
                        }
                    }
                }
            }

            RemindersCard(
                enabled = remindersEnabled,
                hour = reminderHour,
                escalateAfterDays = escalateAfterDays,
                onEnabled = onRemindersEnabled,
                onHour = onReminderHour,
                onEscalateAfterDays = onEscalateAfterDays,
            )
            Spacer(Modifier.size(12.dp))
        }
    }
}

@Composable
private fun ShoppingRow(
    item: ShoppingItem,
    now: Long,
    stale: Boolean,
    onSetPurchased: (Long, Boolean) -> Unit,
    onDelete: (Long) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(
            checked = !item.outstanding,
            onCheckedChange = { onSetPurchased(item.id, it) },
        )
        Column(Modifier.weight(1f)) {
            Text(
                item.name,
                fontSize = 15.sp,
                textDecoration = if (item.outstanding) TextDecoration.None else TextDecoration.LineThrough,
                color = if (item.outstanding) MaterialTheme.colorScheme.onSurface
                else MaterialTheme.colorScheme.onSurfaceVariant,
            )
            val sub = if (item.outstanding) addedText(item.ageDays(now)) else "bought"
            Text(
                sub,
                fontSize = 11.sp,
                color = if (stale) MaterialTheme.colorScheme.error
                else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        IconButton(onClick = { onDelete(item.id) }) {
            Icon(
                Icons.Filled.Delete,
                contentDescription = "Remove",
                modifier = Modifier.size(18.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun RemindersCard(
    enabled: Boolean,
    hour: Int,
    escalateAfterDays: Int,
    onEnabled: (Boolean) -> Unit,
    onHour: (Int) -> Unit,
    onEscalateAfterDays: (Int) -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Daily reminder",
                    fontWeight = FontWeight.Medium,
                    fontSize = 15.sp,
                    modifier = Modifier.weight(1f),
                )
                Switch(
                    checked = enabled,
                    onCheckedChange = onEnabled,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = androidx.compose.ui.graphics.Color.White,
                        checkedTrackColor = MaterialTheme.colorScheme.primary,
                    ),
                )
            }
            if (enabled) {
                Spacer(Modifier.size(4.dp))
                StepperRow(
                    label = "Remind at",
                    value = formatHour(hour),
                    onDown = { onHour((hour + 23) % 24) },
                    onUp = { onHour((hour + 1) % 24) },
                )
                StepperRow(
                    label = "Nag harder after",
                    value = "$escalateAfterDays " + if (escalateAfterDays == 1) "day" else "days",
                    onDown = { onEscalateAfterDays(escalateAfterDays - 1) },
                    onUp = { onEscalateAfterDays(escalateAfterDays + 1) },
                )
                Text(
                    "Once something has sat that long, the reminder gets louder and fires " +
                        "twice a day until you check it off.",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun StepperRow(
    label: String,
    value: String,
    onDown: () -> Unit,
    onUp: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, modifier = Modifier.weight(1f), fontSize = 14.sp)
        OutlinedButton(onClick = onDown) { Text("-") }
        Spacer(Modifier.size(8.dp))
        Text(value, fontSize = 14.sp, fontWeight = FontWeight.Medium)
        Spacer(Modifier.size(8.dp))
        OutlinedButton(onClick = onUp) { Text("+") }
    }
    HorizontalDivider()
}

private fun addedText(days: Int): String = when (days) {
    0 -> "added today"
    1 -> "added yesterday"
    else -> "added $days days ago"
}

private fun formatHour(hour: Int): String {
    val h12 = when (hour % 12) {
        0 -> 12
        else -> hour % 12
    }
    val suffix = if (hour < 12) "AM" else "PM"
    return "$h12:00 $suffix"
}
