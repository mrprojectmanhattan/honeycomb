package com.mark.moodlogger.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mark.moodlogger.MoodScale
import com.mark.moodlogger.data.EntryWithKeywords
import com.mark.moodlogger.data.Keyword

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun LogMoodScreen(
    existing: EntryWithKeywords?,
    allKeywords: List<Keyword>,
    onSave: (score: Int, note: String, keywordIds: Set<Long>) -> Unit,
    onBack: () -> Unit,
) {
    var selected by remember { mutableIntStateOf(existing?.entry?.score ?: 0) }
    var note by remember { mutableStateOf(existing?.entry?.note ?: "") }
    val chosen = remember {
        (existing?.keywords?.map { it.id } ?: emptyList()).toMutableStateList()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (existing == null) "How's your mood?" else "Edit entry") },
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
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text("Right now, I feel...", fontSize = 15.sp)

            MoodScale.scores.forEach { score ->
                val isSelected = selected == score
                val ink = if (isSelected) MoodScale.ink(score) else MaterialTheme.colorScheme.onSurface
                OutlinedButton(
                    onClick = { selected = score },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(58.dp),
                    shape = HexagonShape,
                    border = BorderStroke(
                        width = if (isSelected) 0.dp else 1.5.dp,
                        color = if (isSelected) Color.Transparent
                        else MoodScale.color(score).copy(alpha = 0.55f),
                    ),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = if (isSelected) MoodScale.color(score) else Color.Transparent,
                        contentColor = ink,
                    ),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(MoodScale.emoji(score), fontSize = 20.sp)
                        Spacer(Modifier.size(12.dp))
                        Text(
                            MoodScale.label(score),
                            fontSize = 16.sp,
                            color = ink,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        )
                    }
                }
            }

            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = { Text("Note (optional)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )

            if (allKeywords.isNotEmpty()) {
                Text("Tags", fontSize = 13.sp, color = Color.Gray)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    allKeywords.forEach { kw ->
                        val on = chosen.contains(kw.id)
                        FilterChip(
                            selected = on,
                            onClick = {
                                if (on) chosen.remove(kw.id) else chosen.add(kw.id)
                            },
                            label = { Text(kw.name) },
                            leadingIcon = if (on) {
                                { Icon(Icons.Default.Check, contentDescription = null, Modifier.size(16.dp)) }
                            } else null,
                            colors = FilterChipDefaults.filterChipColors(),
                        )
                    }
                }
            }

            Spacer(Modifier.size(4.dp))
            Button(
                onClick = { if (selected in MoodScale.scores) onSave(selected, note, chosen.toSet()) },
                enabled = selected in MoodScale.scores,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(if (existing == null) "Save" else "Update", fontSize = 16.sp)
            }

            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                OutlinedButton(onClick = onBack) { Text("Cancel") }
            }
        }
    }
}
