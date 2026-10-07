package com.mark.moodlogger.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.mark.moodlogger.data.CareProvider
import com.mark.moodlogger.data.MentalHealthCheckin
import com.mark.moodlogger.data.Medication
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters

private val DAYS = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")

/** Next occurrence of [dayOfWeek] (1=Mon..7=Sun) at [minuteOfDay], or null. */
private fun nextAppointment(dayOfWeek: Int?, minuteOfDay: Int?): LocalDateTime? {
    if (dayOfWeek == null || minuteOfDay == null) return null
    val now = LocalDateTime.now()
    val time = LocalTime.of(minuteOfDay / 60, minuteOfDay % 60)
    val targetDow = java.time.DayOfWeek.of(dayOfWeek)
    var date: LocalDate = now.toLocalDate().with(TemporalAdjusters.nextOrSame(targetDow))
    var dt = LocalDateTime.of(date, time)
    if (dt.isBefore(now)) {
        date = now.toLocalDate().with(TemporalAdjusters.next(targetDow))
        dt = LocalDateTime.of(date, time)
    }
    return dt
}

private fun formatNext(dt: LocalDateTime): String {
    val today = LocalDate.now()
    val dayPart = when (dt.toLocalDate()) {
        today -> "Today"
        today.plusDays(1) -> "Tomorrow"
        else -> dt.format(DateTimeFormatter.ofPattern("EEEE, MMM d"))
    }
    return "$dayPart at " + dt.format(DateTimeFormatter.ofPattern("h:mm a"))
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CareScreen(
    providers: List<CareProvider>,
    medications: List<Medication>,
    allergies: String,
    checkins: List<MentalHealthCheckin>,
    onSaveProvider: (CareProvider) -> Unit,
    onDeleteProvider: (Long) -> Unit,
    onSaveMedication: (Medication) -> Unit,
    onDeleteMedication: (Long) -> Unit,
    onSetAllergies: (String) -> Unit,
    onSubmitCheckin: (type: String, answers: List<Int>) -> Unit,
    onDeleteCheckin: (MentalHealthCheckin) -> Unit,
    onBack: () -> Unit,
) {
    var tab by remember { mutableIntStateOf(0) } // 0 Team, 1 Meds, 2 Summary, 3 Check-ins
    var editingProvider by remember { mutableStateOf<CareProvider?>(null) }
    var editingMed by remember { mutableStateOf<Medication?>(null) }
    var editingAllergies by remember { mutableStateOf(false) }
    var showLockedSummary by remember { mutableStateOf(false) }

    if (showLockedSummary) {
        LockedHealthSummaryDialog(
            providers = providers,
            medications = medications,
            allergies = allergies,
            onClose = { showLockedSummary = false },
        )
    }

    editingProvider?.let { p ->
        ProviderDialog(
            initial = p,
            onDismiss = { editingProvider = null },
            onSave = { onSaveProvider(it); editingProvider = null },
            onDelete = { if (p.id != 0L) onDeleteProvider(p.id); editingProvider = null },
        )
    }
    editingMed?.let { m ->
        MedicationDialog(
            initial = m,
            providers = providers.filter { it.active },
            onDismiss = { editingMed = null },
            onSave = { onSaveMedication(it); editingMed = null },
            onDelete = { if (m.id != 0L) onDeleteMedication(m.id); editingMed = null },
        )
    }
    if (editingAllergies) {
        AllergiesDialog(
            initial = allergies,
            onDismiss = { editingAllergies = false },
            onSave = { onSetAllergies(it); editingAllergies = false },
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Care") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
        floatingActionButton = {
            when (tab) {
                0 -> FloatingActionButton(onClick = { editingProvider = CareProvider() }) {
                    Icon(Icons.Filled.Add, "Add provider")
                }
                1 -> FloatingActionButton(onClick = { editingMed = Medication() }) {
                    Icon(Icons.Filled.Add, "Add medication")
                }
                else -> Unit
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
                listOf("Team", "Meds", "Summary", "Check-ins").forEachIndexed { i, label ->
                    SegmentedButton(
                        selected = tab == i,
                        onClick = { tab = i },
                        shape = SegmentedButtonDefaults.itemShape(i, 4),
                    ) { Text(label, fontSize = 12.sp) }
                }
            }

            when (tab) {
                0 -> TeamTab(providers) { editingProvider = it }
                1 -> MedsTab(
                    medications = medications,
                    providers = providers,
                    allergies = allergies,
                    onEditMed = { editingMed = it },
                    onEditAllergies = { editingAllergies = true },
                )
                2 -> SummaryTab(providers, medications, allergies) { showLockedSummary = true }
                else -> CheckinScreen(
                    checkins = checkins,
                    onSubmit = onSubmitCheckin,
                    onDelete = onDeleteCheckin,
                )
            }
        }
    }
}

// ---- Team tab -----------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TeamTab(providers: List<CareProvider>, onEdit: (CareProvider) -> Unit) {
    if (providers.isEmpty()) {
        EmptyNote(
            "No one added yet. Tap + to add a provider — therapist, primary care, " +
                "specialist, dentist — with the usual appointment day and time.",
        )
        return
    }
    LazyColumn(
        Modifier.fillMaxSize().padding(horizontal = 12.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(providers, key = { it.id }) { p ->
            Card(onClick = { onEdit(p) }, modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text(
                        CareProvider.kindLabel(p.kind) + if (!p.active) "  (inactive)" else "",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        p.name.ifBlank { "(no name)" },
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                    val next = nextAppointment(p.dayOfWeek, p.minuteOfDay)
                    Spacer(Modifier.size(4.dp))
                    Text(
                        if (next != null) "Next: ${formatNext(next)}" else "No recurring appointment set",
                        fontSize = 13.sp,
                        color = if (next != null) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (p.phone.isNotBlank()) {
                        Text(p.phone, fontSize = 13.sp)
                    }
                    if (p.notes.isNotBlank()) {
                        Spacer(Modifier.size(6.dp))
                        Text(p.notes, fontSize = 13.sp)
                    }
                }
            }
        }
    }
}

// ---- Meds tab ---------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MedsTab(
    medications: List<Medication>,
    providers: List<CareProvider>,
    allergies: String,
    onEditMed: (Medication) -> Unit,
    onEditAllergies: () -> Unit,
) {
    LazyColumn(
        Modifier.fillMaxSize().padding(horizontal = 12.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            Card(onClick = onEditAllergies, modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text(
                        "Drug allergies",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.size(2.dp))
                    Text(
                        allergies.ifBlank { "None recorded — tap to add" },
                        fontSize = 14.sp,
                        color = if (allergies.isBlank()) MaterialTheme.colorScheme.onSurfaceVariant
                        else MaterialTheme.colorScheme.error,
                        fontWeight = if (allergies.isBlank()) FontWeight.Normal else FontWeight.Medium,
                    )
                }
            }
        }
        if (medications.isEmpty()) {
            item {
                Text(
                    "No medications added. Tap + to add one — name, dose, and when you take it.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(20.dp),
                )
            }
        }
        items(medications, key = { it.id }) { m ->
            val prescriber = providers.firstOrNull { it.id == m.prescriberId }
            Card(onClick = { onEditMed(m) }, modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            m.name,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (m.active) MaterialTheme.colorScheme.onSurface
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f),
                        )
                        if (m.asNeeded) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = MaterialTheme.colorScheme.secondaryContainer,
                            ) {
                                Text(
                                    "as needed",
                                    fontSize = 10.sp,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                )
                            }
                        }
                        if (!m.active) {
                            Spacer(Modifier.size(6.dp))
                            Text("inactive", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    val line = m.summaryLine()
                    if (line.isNotBlank()) {
                        Spacer(Modifier.size(2.dp))
                        Text(line, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    if (prescriber != null) {
                        Text(
                            "prescribed by ${prescriber.name}",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    if (m.notes.isNotBlank()) {
                        Spacer(Modifier.size(4.dp))
                        Text(m.notes, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

// ---- Summary tab ----------------------------------------------------

@Composable
private fun SummaryTab(
    providers: List<CareProvider>,
    medications: List<Medication>,
    allergies: String,
    onShowLocked: () -> Unit,
) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
    ) {
        Text("Health summary", fontWeight = FontWeight.Bold, fontSize = 20.sp)
        Text(
            "For handing to a doctor or nurse.",
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.size(12.dp))
        Button(onClick = onShowLocked, modifier = Modifier.fillMaxWidth()) {
            Text("Show full-screen (locked) for a doctor")
        }

        HealthSummaryContent(providers, medications, allergies)
    }
}

/** The actual summary body - shared between the in-app Summary tab and the locked
 *  full-screen modal, so they can never quietly drift apart. */
@Composable
private fun HealthSummaryContent(
    providers: List<CareProvider>,
    medications: List<Medication>,
    allergies: String,
) {
    Column {
        Spacer(Modifier.size(20.dp))
        Text("Medications", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
        Spacer(Modifier.size(6.dp))
        val activeMeds = medications.filter { it.active }
        if (activeMeds.isEmpty()) {
            Text("None recorded.", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            activeMeds.forEach { m ->
                Column(Modifier.padding(vertical = 6.dp)) {
                    Text(
                        m.name + if (m.asNeeded) "  (as needed)" else "",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                    )
                    val line = m.summaryLine()
                    if (line.isNotBlank()) Text(line, fontSize = 14.sp)
                    val prescriber = providers.firstOrNull { it.id == m.prescriberId }
                    if (prescriber != null) {
                        Text(
                            "prescribed by ${prescriber.name}",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }

        Spacer(Modifier.size(18.dp))
        HorizontalDivider()
        Spacer(Modifier.size(18.dp))
        Text("Drug allergies", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
        Spacer(Modifier.size(6.dp))
        Text(
            allergies.ifBlank { "None recorded." },
            fontSize = 15.sp,
            color = if (allergies.isBlank()) MaterialTheme.colorScheme.onSurfaceVariant
            else MaterialTheme.colorScheme.error,
            fontWeight = if (allergies.isBlank()) FontWeight.Normal else FontWeight.Medium,
        )

        Spacer(Modifier.size(18.dp))
        HorizontalDivider()
        Spacer(Modifier.size(18.dp))
        Text("Care team", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
        Spacer(Modifier.size(6.dp))
        val activeProviders = providers.filter { it.active }
        if (activeProviders.isEmpty()) {
            Text("None recorded.", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            activeProviders.forEach { p ->
                Column(Modifier.padding(vertical = 6.dp)) {
                    Text(
                        "${CareProvider.kindLabel(p.kind)} — ${p.name.ifBlank { "(no name)" }}",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                    )
                    if (p.phone.isNotBlank()) Text(p.phone, fontSize = 14.sp)
                }
            }
        }
        Spacer(Modifier.size(24.dp))
    }
}

/**
 * The "hand to a doctor" view, locked down. `dismissOnBackPress = false` and
 * `dismissOnClickOutside = false` mean neither the system back gesture nor a stray tap
 * outside the card can close it - only the explicit X. So a doctor holding the phone
 * can't accidentally swipe or back-button into anything else in the app. Added 2026-09-28.
 */
@Composable
private fun LockedHealthSummaryDialog(
    providers: List<CareProvider>,
    medications: List<Medication>,
    allergies: String,
    onClose: () -> Unit,
) {
    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false,
        ),
    ) {
        Surface(color = MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxSize()) {
            Column(Modifier.fillMaxSize()) {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        "Health summary",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        modifier = Modifier.weight(1f),
                    )
                    IconButton(onClick = onClose) {
                        Icon(Icons.Filled.Close, contentDescription = "Close")
                    }
                }
                HorizontalDivider()
                Column(
                    Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(20.dp),
                ) {
                    HealthSummaryContent(providers, medications, allergies)
                }
            }
        }
    }
}

@Composable
private fun EmptyNote(text: String) {
    Box(Modifier.fillMaxSize().padding(28.dp), contentAlignment = Alignment.Center) {
        Text(text, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
    }
}

// ---- dialogs -------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun ProviderDialog(
    initial: CareProvider,
    onDismiss: () -> Unit,
    onSave: (CareProvider) -> Unit,
    onDelete: () -> Unit,
) {
    var kind by remember { mutableStateOf(initial.kind) }
    var name by remember { mutableStateOf(initial.name) }
    var phone by remember { mutableStateOf(initial.phone) }
    var hasDay by remember { mutableStateOf(initial.dayOfWeek != null) }
    var dayIdx by remember { mutableIntStateOf((initial.dayOfWeek ?: 5) - 1) }
    var minute by remember { mutableIntStateOf(initial.minuteOfDay ?: 14 * 60) }
    var notes by remember { mutableStateOf(initial.notes) }
    var active by remember { mutableStateOf(initial.active) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial.id == 0L) "Add provider" else "Edit provider") },
        text = {
            Column(
                Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    CareProvider.KINDS.forEach { k ->
                        FilterChip(
                            selected = kind == k,
                            onClick = { kind = k },
                            label = { Text(CareProvider.kindLabel(k), fontSize = 12.sp) },
                        )
                    }
                }
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Phone (optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    FilterChip(
                        selected = hasDay,
                        onClick = { hasDay = !hasDay },
                        label = { Text(if (hasDay) "Recurring appointment" else "No set appointment") },
                    )
                }
                if (hasDay) {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        DAYS.forEachIndexed { i, d ->
                            FilterChip(
                                selected = dayIdx == i,
                                onClick = { dayIdx = i },
                                label = { Text(d) },
                            )
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Time", modifier = Modifier.size(width = 56.dp, height = 24.dp), fontSize = 14.sp)
                        IconButton(onClick = { minute = (minute - 15 + 1440) % 1440 }) {
                            Icon(Icons.Filled.KeyboardArrowLeft, "Earlier")
                        }
                        Text(
                            LocalTime.of(minute / 60, minute % 60)
                                .format(DateTimeFormatter.ofPattern("h:mm a")),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                        )
                        IconButton(onClick = { minute = (minute + 15) % 1440 }) {
                            Icon(Icons.Filled.KeyboardArrowRight, "Later")
                        }
                    }
                }
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes (optional)") },
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Active", modifier = Modifier.weight(1f), fontSize = 14.sp)
                    Switch(checked = active, onCheckedChange = { active = it })
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onSave(
                        initial.copy(
                            kind = kind,
                            name = name,
                            phone = phone.trim(),
                            dayOfWeek = if (hasDay) dayIdx + 1 else null,
                            minuteOfDay = if (hasDay) minute else null,
                            notes = notes.trim(),
                            active = active,
                        )
                    )
                },
                enabled = name.isNotBlank(),
            ) { Text("Save") }
        },
        dismissButton = {
            Row {
                if (initial.id != 0L) {
                    TextButton(onClick = onDelete) {
                        Icon(Icons.Filled.Delete, null, Modifier.size(18.dp))
                        Spacer(Modifier.size(4.dp))
                        Text("Delete")
                    }
                }
                TextButton(onClick = onDismiss) { Text("Cancel") }
            }
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun MedicationDialog(
    initial: Medication,
    providers: List<CareProvider>,
    onDismiss: () -> Unit,
    onSave: (Medication) -> Unit,
    onDelete: () -> Unit,
) {
    var name by remember { mutableStateOf(initial.name) }
    var dose by remember { mutableStateOf(initial.dose) }
    var amount by remember { mutableStateOf(initial.amount) }
    var schedule by remember { mutableStateOf(initial.schedule) }
    var prescriberId by remember { mutableStateOf(initial.prescriberId) }
    var asNeeded by remember { mutableStateOf(initial.asNeeded) }
    var active by remember { mutableStateOf(initial.active) }
    var notes by remember { mutableStateOf(initial.notes) }
    var reminderEnabled by remember { mutableStateOf(initial.reminderEnabled) }
    var reminderMinutesList by remember { mutableStateOf(initial.reminderMinuteList()) }
    var stepperMinute by remember { mutableIntStateOf(8 * 60) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial.id == 0L) "Add medication" else "Edit medication") },
        text = {
            Column(
                Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = dose,
                    onValueChange = { dose = it },
                    label = { Text("Dose (e.g. 50 mg)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = { Text("How many per dose (e.g. 1 tablet)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = schedule,
                    onValueChange = { schedule = it },
                    label = { Text("When (e.g. 8am and 8pm)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Take as needed", modifier = Modifier.weight(1f), fontSize = 14.sp)
                    Switch(checked = asNeeded, onCheckedChange = { asNeeded = it })
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Remind me", modifier = Modifier.weight(1f), fontSize = 14.sp)
                    Switch(checked = reminderEnabled, onCheckedChange = { reminderEnabled = it })
                }
                if (reminderEnabled) {
                    if (reminderMinutesList.isNotEmpty()) {
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            reminderMinutesList.forEach { m ->
                                FilterChip(
                                    selected = true,
                                    onClick = { reminderMinutesList = reminderMinutesList - m },
                                    label = {
                                        Text(
                                            LocalTime.of(m / 60, m % 60)
                                                .format(DateTimeFormatter.ofPattern("h:mm a")),
                                        )
                                    },
                                    trailingIcon = { Icon(Icons.Filled.Close, contentDescription = "Remove", modifier = Modifier.size(14.dp)) },
                                )
                            }
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Add a time", modifier = Modifier.size(width = 80.dp, height = 24.dp), fontSize = 13.sp)
                        IconButton(onClick = { stepperMinute = (stepperMinute - 15 + 1440) % 1440 }) {
                            Icon(Icons.Filled.KeyboardArrowLeft, "Earlier")
                        }
                        Text(
                            LocalTime.of(stepperMinute / 60, stepperMinute % 60)
                                .format(DateTimeFormatter.ofPattern("h:mm a")),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium,
                        )
                        IconButton(onClick = { stepperMinute = (stepperMinute + 15) % 1440 }) {
                            Icon(Icons.Filled.KeyboardArrowRight, "Later")
                        }
                        Spacer(Modifier.size(4.dp))
                        OutlinedButton(onClick = {
                            if (stepperMinute !in reminderMinutesList) {
                                reminderMinutesList = (reminderMinutesList + stepperMinute).sorted()
                            }
                        }) { Text("Add") }
                    }
                }
                if (providers.isNotEmpty()) {
                    Text("Prescribed by", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        FilterChip(
                            selected = prescriberId == null,
                            onClick = { prescriberId = null },
                            label = { Text("None") },
                        )
                        providers.forEach { p ->
                            FilterChip(
                                selected = prescriberId == p.id,
                                onClick = { prescriberId = p.id },
                                label = { Text(p.name.ifBlank { CareProvider.kindLabel(p.kind) }, fontSize = 12.sp) },
                            )
                        }
                    }
                }
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes (optional)") },
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Active", modifier = Modifier.weight(1f), fontSize = 14.sp)
                    Switch(checked = active, onCheckedChange = { active = it })
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onSave(
                        initial.copy(
                            name = name,
                            dose = dose,
                            amount = amount,
                            schedule = schedule,
                            prescriberId = prescriberId,
                            asNeeded = asNeeded,
                            active = active,
                            notes = notes,
                            reminderEnabled = reminderEnabled,
                            reminderMinutes = Medication.formatMinutes(reminderMinutesList),
                        )
                    )
                },
                enabled = name.isNotBlank(),
            ) { Text("Save") }
        },
        dismissButton = {
            Row {
                if (initial.id != 0L) {
                    TextButton(onClick = onDelete) {
                        Icon(Icons.Filled.Delete, null, Modifier.size(18.dp))
                        Spacer(Modifier.size(4.dp))
                        Text("Delete")
                    }
                }
                TextButton(onClick = onDismiss) { Text("Cancel") }
            }
        },
    )
}

@Composable
private fun AllergiesDialog(
    initial: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit,
) {
    var text by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Drug allergies") },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                label = { Text("e.g. Penicillin, sulfa drugs") },
                minLines = 2,
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = { TextButton(onClick = { onSave(text) }) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
