package com.mark.moodlogger.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/** One step of a breathing pattern. [target] is the breath fill 0..1 to move to
 *  over [seconds]; null holds the current fill (a "hold" beat). */
private data class Phase(val label: String, val seconds: Int, val target: Float?)

private data class Pattern(val name: String, val note: String, val phases: List<Phase>)

private const val FULL = 1f
private const val EMPTY = 0.24f

private val PATTERNS = listOf(
    Pattern(
        "Box", "Even 4-4-4-4. Steady focus.",
        listOf(
            Phase("Breathe in", 4, FULL),
            Phase("Hold", 4, null),
            Phase("Breathe out", 4, EMPTY),
            Phase("Hold", 4, null),
        ),
    ),
    Pattern(
        "4-7-8", "Long exhale. Winds you down.",
        listOf(
            Phase("Breathe in", 4, FULL),
            Phase("Hold", 7, null),
            Phase("Breathe out", 8, EMPTY),
        ),
    ),
    Pattern(
        "Coherent", "Slow 5-5. Balances you out.",
        listOf(
            Phase("Breathe in", 5, FULL),
            Phase("Breathe out", 5, EMPTY),
        ),
    ),
    Pattern(
        "Quick reset", "Sharp in, long out. Fast down-shift.",
        listOf(
            Phase("Breathe in", 2, FULL),
            Phase("Breathe out", 6, EMPTY),
        ),
    ),
)

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun BreatheScreen(onBack: () -> Unit) {
    var patternIdx by remember { mutableIntStateOf(0) }
    var running by remember { mutableStateOf(false) }
    var phaseLabel by remember { mutableStateOf("Ready") }
    var secLeft by remember { mutableIntStateOf(0) }
    var cycles by remember { mutableIntStateOf(0) }

    val pattern = PATTERNS[patternIdx]
    val fill = remember { Animatable(EMPTY) }

    LaunchedEffect(running, patternIdx) {
        if (!running) {
            phaseLabel = "Ready"
            secLeft = 0
            fill.animateTo(EMPTY, tween(600))
            return@LaunchedEffect
        }
        cycles = 0
        while (isActive && running) {
            for (phase in pattern.phases) {
                phaseLabel = phase.label
                val countdown = launch {
                    for (s in phase.seconds downTo 1) {
                        secLeft = s
                        delay(1000)
                    }
                }
                if (phase.target != null) {
                    fill.animateTo(phase.target, tween(phase.seconds * 1000, easing = LinearEasing))
                } else {
                    delay(phase.seconds * 1000L)
                }
                countdown.join()
            }
            cycles++
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Breathe") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
            )
        },
    ) { pad ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .honeycombBackground(MaterialTheme.colorScheme.background)
                .padding(pad)
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.size(12.dp))
            androidx.compose.foundation.layout.FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                PATTERNS.forEachIndexed { i, p ->
                    FilterChip(
                        selected = i == patternIdx,
                        onClick = { if (!running) patternIdx = i },
                        label = { Text(p.name) },
                    )
                }
            }
            Spacer(Modifier.size(4.dp))
            Text(
                pattern.note,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(Modifier.size(28.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center,
            ) {
                val d = (120 + fill.value * 160).dp
                Box(
                    modifier = Modifier
                        .size(d)
                        .clip(HexagonShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.22f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Box(
                        modifier = Modifier
                            .size(d * 0.72f)
                            .clip(HexagonShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)),
                    )
                    if (running && secLeft > 0) {
                        Text("$secLeft", fontSize = 30.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Text(
                phaseLabel,
                fontSize = 22.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.size(4.dp))
            Text(
                if (cycles == 0) " " else "$cycles ${if (cycles == 1) "cycle" else "cycles"}",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.size(16.dp))

            if (running) {
                OutlinedButton(
                    onClick = { running = false },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                ) { Text("Stop") }
            } else {
                Button(
                    onClick = { running = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                ) { Text("Start", fontSize = 16.sp) }
            }
            Spacer(Modifier.size(20.dp))
        }
    }
}
