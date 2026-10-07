package com.mark.moodlogger.ui

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mark.moodlogger.data.MentalHealthCheckin
import com.mark.moodlogger.data.Screenings
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * The PHQ-9/GAD-7 hub: pick one, take it, see past scores. Kept as one screen with
 * internal state (picker / form / result) rather than three separate nav routes, same
 * self-contained pattern CareScreen already uses for its own dialogs. Plain content, no
 * Scaffold/TopAppBar of its own - this is embedded as a tab inside CareScreen, which
 * already has one.
 */
private enum class CheckinStage { Picker, Form, Result }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CheckinScreen(
    checkins: List<MentalHealthCheckin>,
    onSubmit: (type: String, answers: List<Int>) -> Unit,
    onDelete: (MentalHealthCheckin) -> Unit,
) {
    var stage by remember { mutableStateOf(CheckinStage.Picker) }
    var activeType by remember { mutableStateOf(Screenings.TYPE_PHQ9) }
    var lastResult by remember { mutableStateOf<Triple<String, Int, Boolean>?>(null) }

    when (stage) {
        CheckinStage.Picker -> PickerStage(
            checkins = checkins,
            onPick = { activeType = it; stage = CheckinStage.Form },
            onDelete = onDelete,
        )
        CheckinStage.Form -> FormStage(
            type = activeType,
            onCancel = { stage = CheckinStage.Picker },
            onDone = { answers ->
                val total = answers.sum()
                val selfHarmFlag = activeType == Screenings.TYPE_PHQ9 &&
                    answers.getOrElse(Screenings.PHQ9_SELF_HARM_INDEX) { 0 } > 0
                onSubmit(activeType, answers)
                lastResult = Triple(activeType, total, selfHarmFlag)
                stage = CheckinStage.Result
            },
        )
        CheckinStage.Result -> lastResult?.let { (type, score, selfHarmFlag) ->
            ResultStage(
                type = type,
                score = score,
                selfHarmFlag = selfHarmFlag,
                onDone = { stage = CheckinStage.Picker },
            )
        }
    }
}

@Composable
private fun PickerStage(
    checkins: List<MentalHealthCheckin>,
    onPick: (String) -> Unit,
    onDelete: (MentalHealthCheckin) -> Unit,
) {
    val fmt = remember { SimpleDateFormat("EEE, MMM d 'at' h:mm a", Locale.US) }
    Column(Modifier.fillMaxSize().padding(20.dp)) {
        Text(
            "A couple minutes, real numbers - for you and for therapy.",
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.size(16.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(onClick = { onPick(Screenings.TYPE_PHQ9) }, modifier = Modifier.weight(1f)) {
                Text("Take PHQ-9")
            }
            Button(onClick = { onPick(Screenings.TYPE_GAD7) }, modifier = Modifier.weight(1f)) {
                Text("Take GAD-7")
            }
        }
        Spacer(Modifier.size(20.dp))
        if (checkins.isEmpty()) {
            Text(
                "No check-ins yet.",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 20.dp),
            )
        } else {
            Text("Past check-ins", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
            Spacer(Modifier.size(8.dp))
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(checkins, key = { it.id }) { c ->
                    Card(Modifier.fillMaxWidth()) {
                        Row(
                            Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(
                                    "${Screenings.titleFor(c.type)} - ${c.totalScore}  (${Screenings.severity(c.type, c.totalScore)})",
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 15.sp,
                                )
                                Text(
                                    fmt.format(Date(c.timestamp)),
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            IconButton(onClick = { onDelete(c) }) {
                                Icon(Icons.Filled.Delete, contentDescription = "Delete")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FormStage(
    type: String,
    onCancel: () -> Unit,
    onDone: (List<Int>) -> Unit,
) {
    val questions = Screenings.questionsFor(type)
    val answers = remember(type) { mutableStateOf(List(questions.size) { -1 }) }
    val allAnswered = answers.value.none { it == -1 }

    Column(Modifier.fillMaxSize()) {
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
        ) {
            Text(Screenings.titleFor(type), fontWeight = FontWeight.Bold, fontSize = 20.sp)
            Spacer(Modifier.size(4.dp))
            Text(Screenings.INTRO, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.size(16.dp))
            questions.forEachIndexed { qi, question ->
                Card(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                    Column(Modifier.padding(14.dp)) {
                        Text(question, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                        Spacer(Modifier.size(8.dp))
                        Screenings.ANSWER_LABELS.forEachIndexed { ai, label ->
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                RadioButton(
                                    selected = answers.value[qi] == ai,
                                    onClick = {
                                        answers.value = answers.value.toMutableList().also { it[qi] = ai }
                                    },
                                )
                                Text(label, fontSize = 14.sp)
                            }
                        }
                    }
                }
            }
        }
        HorizontalDivider()
        Row(
            Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            TextButton(onClick = onCancel, modifier = Modifier.weight(1f)) { Text("Cancel") }
            Button(
                onClick = { onDone(answers.value) },
                enabled = allAnswered,
                modifier = Modifier.weight(1f),
            ) { Text("Submit") }
        }
    }
}

@Composable
private fun ResultStage(
    type: String,
    score: Int,
    selfHarmFlag: Boolean,
    onDone: () -> Unit,
) {
    Column(
        Modifier.fillMaxSize().padding(28.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Text(Screenings.titleFor(type), fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.size(8.dp))
        Text("$score", fontWeight = FontWeight.Bold, fontSize = 48.sp)
        Text(
            Screenings.severity(type, score),
            fontSize = 18.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.primary,
        )
        Spacer(Modifier.size(20.dp))
        Text(
            "Just numbers, not a diagnosis. Worth bringing up in therapy either way.",
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (selfHarmFlag) {
            Spacer(Modifier.size(16.dp))
            Card(Modifier.fillMaxWidth()) {
                Text(
                    "If any of that included thoughts of harming yourself, the 988 Suicide & Crisis Lifeline (call or text 988) is real, free, and available right now.",
                    fontSize = 13.sp,
                    modifier = Modifier.padding(14.dp),
                )
            }
        }
        Spacer(Modifier.size(24.dp))
        Button(onClick = onDone, modifier = Modifier.fillMaxWidth()) { Text("Done") }
    }
}
