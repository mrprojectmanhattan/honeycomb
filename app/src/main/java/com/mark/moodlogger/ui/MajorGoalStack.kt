package com.mark.moodlogger.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mark.moodlogger.data.MajorGoal

/**
 * The major-goals reminder on the main screen: a tap-to-rotate stack. The front
 * card shows one goal; the edges of up to two more peek out below it. Tapping
 * anywhere advances to the next goal and wraps around. Display only - major goals
 * are created and edited in the Goals screen's Major tab. Renders nothing when
 * there are no major goals.
 */
@Composable
fun MajorGoalStack(
    goals: List<MajorGoal>,
    modifier: Modifier = Modifier,
) {
    if (goals.isEmpty()) return

    var index by remember(goals.size) { mutableIntStateOf(0) }
    val safeIndex = index.coerceIn(0, goals.lastIndex)
    val current = goals[safeIndex]
    val peek = (goals.size - 1).coerceIn(0, 2)

    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            "Working toward",
            fontWeight = FontWeight.SemiBold,
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(Modifier.size(6.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(enabled = goals.size > 1) {
                    index = (safeIndex + 1) % goals.size
                },
        ) {
            // Cards behind, same size as the front card, shifted down and inset
            // so only their bottom edge shows. Deepest drawn first.
            for (d in peek downTo 1) {
                Card(
                    modifier = Modifier
                        .matchParentSize()
                        .padding(horizontal = (d * 6).dp)
                        .offset(y = (d * 7).dp)
                        .alpha(if (d == 1) 0.5f else 0.3f),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    ),
                ) {}
            }
            // Front card.
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
            ) {
                Column(Modifier.padding(12.dp)) {
                    Text(
                        current.title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (current.detail.isNotBlank()) {
                        Spacer(Modifier.size(4.dp))
                        Text(
                            current.detail,
                            fontSize = 11.sp,
                            color = Color.Gray,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
        if (goals.size > 1) {
            Spacer(Modifier.size((peek * 7 + 10).dp))
            Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                goals.indices.forEach { i ->
                    Box(
                        Modifier
                            .size(6.dp)
                            .background(
                                if (i == safeIndex) MaterialTheme.colorScheme.primary
                                else Color(0x44808080),
                                CircleShape,
                            )
                    )
                }
            }
        }
    }
}
