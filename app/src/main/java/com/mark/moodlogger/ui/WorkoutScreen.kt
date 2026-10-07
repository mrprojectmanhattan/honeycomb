package com.mark.moodlogger.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mark.moodlogger.data.WorkoutLog

/**
 * The weekly muscle-group tracker. Was its own nav tab through v3.21; from v3.22
 * it's the "Workout" tab inside Goals, so this is a bare content composable.
 */
@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun WorkoutContent(
    doneThisWeek: Set<String>,
    onToggle: (region: String, currentlyDone: Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .honeycombBackground(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.size(16.dp))
        Text("This week", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
        Text(
            "${doneThisWeek.size} of ${WorkoutLog.REGIONS.size} muscle groups trained",
            fontSize = 12.sp,
            color = Color.Gray,
        )

        Spacer(Modifier.size(8.dp))
        HexFigure(
            done = doneThisWeek,
            modifier = Modifier
                .fillMaxWidth(0.72f)
                .height(300.dp),
        )

        Spacer(Modifier.size(16.dp))
        Text("Tap what you trained", fontSize = 12.sp, color = Color.Gray)
        Spacer(Modifier.size(10.dp))
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            WorkoutLog.REGIONS.forEach { region ->
                val on = region in doneThisWeek
                FilterChip(
                    selected = on,
                    onClick = { onToggle(region, on) },
                    label = { Text(WorkoutLog.label(region)) },
                    leadingIcon = if (on) {
                        { Icon(Icons.Default.Check, contentDescription = null) }
                    } else {
                        null
                    },
                )
            }
        }

        Spacer(Modifier.size(20.dp))
        Text("Resets Monday morning", fontSize = 11.sp, color = Color.Gray)
    }
}
