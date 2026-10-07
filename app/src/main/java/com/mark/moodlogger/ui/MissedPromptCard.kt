package com.mark.moodlogger.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mark.moodlogger.data.PromptEvent
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Offers a one-tap reason for a run of prompts that went unanswered.
 *
 *  Tapping a chip explains the whole run, so a shift where phones aren't allowed
 *  costs one tap instead of one per hour.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MissedPromptCard(
    group: List<PromptEvent>,
    remainingGroups: Int,
    onReason: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (group.isEmpty()) return

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
        ),
    ) {
        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
            Text(
                headline(group),
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
            )
            Spacer(Modifier.size(2.dp))
            Text(
                if (remainingGroups > 0) {
                    "What was going on?  ·  $remainingGroups more to clear"
                } else {
                    "What was going on?"
                },
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.size(10.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                PromptEvent.REASONS.forEach { (value, label) ->
                    SuggestionChip(
                        onClick = { onReason(value) },
                        label = { Text(label, fontSize = 12.sp) },
                    )
                }
            }
        }
    }
}

private fun headline(group: List<PromptEvent>): String {
    val count = group.size
    val noun = if (count == 1) "prompt" else "prompts"
    val first = clock(group.first().firedAt)
    return if (count == 1) {
        "Missed 1 $noun at $first"
    } else {
        "Missed $count $noun, $first to ${clock(group.last().firedAt)}"
    }
}

private fun clock(millis: Long): String =
    SimpleDateFormat("h a", Locale.getDefault()).format(Date(millis))
