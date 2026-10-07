package com.mark.moodlogger.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.EventBusy
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mark.moodlogger.MoodScale
import com.mark.moodlogger.data.CareProvider
import com.mark.moodlogger.data.EntryWithKeywords
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
import java.util.Calendar
import kotlin.math.roundToInt

/**
 * The user-choice widget in the home screen's right column, below "Working
 * toward". Tap it to cycle to the next option; a row of dots shows which of the
 * five is showing. All five read off data the app already has - nothing new to
 * set up. The choice is persisted in Settings ([SettingsStore] "home_widget").
 */
enum class HomeWidget(val id: String, val label: String) {
    SNAPSHOT("snapshot", "Today"),
    APPOINTMENT("appointment", "Next appt"),
    STEPS("steps", "Steps"),
    WORKOUT("workout", "Workout"),
    INSIGHT("insight", "Insight");

    companion object {
        val order: List<HomeWidget> = listOf(SNAPSHOT, APPOINTMENT, STEPS, WORKOUT, INSIGHT)
        fun from(id: String): HomeWidget = order.firstOrNull { it.id == id } ?: SNAPSHOT
    }
}

@Composable
fun HomeWidgetSlot(
    widgetId: String,
    entries: List<EntryWithKeywords>,
    stepsToday: Int,
    workoutThisWeek: Set<String>,
    careProviders: List<CareProvider>,
    onSetWidget: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val widget = HomeWidget.from(widgetId)

    Column(
        modifier = modifier.clickable {
            val next = HomeWidget.order[(widget.ordinal + 1) % HomeWidget.order.size]
            onSetWidget(next.id)
        },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            widget.label,
            fontWeight = FontWeight.SemiBold,
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(Modifier.size(6.dp))
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp, horizontal = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                when (widget) {
                    HomeWidget.SNAPSHOT -> SnapshotContent(entries)
                    HomeWidget.APPOINTMENT -> AppointmentContent(careProviders)
                    HomeWidget.STEPS -> StepsContent(stepsToday)
                    HomeWidget.WORKOUT -> WorkoutContent(workoutThisWeek)
                    HomeWidget.INSIGHT -> InsightContent(entries)
                }
            }
        }
        Spacer(Modifier.size(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            HomeWidget.order.forEach { w ->
                Box(
                    Modifier
                        .size(6.dp)
                        .background(
                            if (w == widget) MaterialTheme.colorScheme.primary
                            else Color(0x44808080),
                            CircleShape,
                        )
                )
            }
        }
    }
}

// ---- individual widgets --------------------------------------------------

@Composable
private fun SnapshotContent(entries: List<EntryWithKeywords>) {
    val today = remember(entries) { todayEntries(entries) }
    if (today.isEmpty()) {
        Text(
            "No check-ins yet today",
            fontSize = 12.sp,
            color = Color.Gray,
            textAlign = TextAlign.Center,
        )
        return
    }
    val avg = today.map { it.entry.score }.average().toFloat()
    Surface(
        shape = HexagonShape,
        color = MoodScale.colorForAvg(avg),
        modifier = Modifier.size(width = 52.dp, height = 46.dp),
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                "${today.size}",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = MoodScale.inkForAvg(avg),
            )
        }
    }
    Spacer(Modifier.size(6.dp))
    Text(
        "Today · ${MoodScale.label(avg.roundToInt())}",
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
    )
    Text(
        "${today.size} check-in${if (today.size == 1) "" else "s"}",
        fontSize = 11.sp,
        color = Color.Gray,
    )
    Spacer(Modifier.size(6.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
        today.sortedBy { it.entry.timestamp }.takeLast(12).forEach { e ->
            Box(
                Modifier
                    .size(7.dp)
                    .background(MoodScale.color(e.entry.score), CircleShape)
            )
        }
    }
}

@Composable
private fun AppointmentContent(providers: List<CareProvider>) {
    val next = remember(providers) {
        providers
            .filter { it.active && it.dayOfWeek != null && it.minuteOfDay != null }
            .map { p -> p to nextAppt(p.dayOfWeek!!, p.minuteOfDay!!) }
            .minByOrNull { it.second }
    }
    if (next == null) {
        Icon(
            Icons.Default.EventBusy,
            contentDescription = null,
            tint = Color.Gray,
            modifier = Modifier.size(26.dp),
        )
        Spacer(Modifier.size(6.dp))
        Text(
            "No recurring appointment set",
            fontSize = 11.sp,
            color = Color.Gray,
            textAlign = TextAlign.Center,
        )
        Text(
            "Add one on the Care tab",
            fontSize = 10.sp,
            color = Color.Gray,
            textAlign = TextAlign.Center,
        )
        return
    }
    val (provider, dt) = next
    Icon(
        Icons.Default.Event,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.primary,
        modifier = Modifier.size(26.dp),
    )
    Spacer(Modifier.size(6.dp))
    Text(
        CareProvider.kindLabel(provider.kind),
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
    )
    Text(
        formatNextAppt(dt),
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        textAlign = TextAlign.Center,
    )
    if (provider.name.isNotBlank()) {
        Text(
            provider.name,
            fontSize = 10.sp,
            color = Color.Gray,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun StepsContent(steps: Int) {
    Icon(
        Icons.Default.DirectionsWalk,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.primary,
        modifier = Modifier.size(28.dp),
    )
    Spacer(Modifier.size(4.dp))
    Text(
        "%,d".format(steps),
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp,
    )
    Text(
        if (steps == 0) "steps today — nothing yet" else "steps today",
        fontSize = 11.sp,
        color = Color.Gray,
        textAlign = TextAlign.Center,
    )
}

@Composable
private fun WorkoutContent(regions: Set<String>) {
    Icon(
        Icons.Default.FitnessCenter,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.primary,
        modifier = Modifier.size(28.dp),
    )
    Spacer(Modifier.size(4.dp))
    if (regions.isEmpty()) {
        Text(
            "Nothing logged this week",
            fontSize = 11.sp,
            color = Color.Gray,
            textAlign = TextAlign.Center,
        )
        return
    }
    Text(
        "${regions.size}",
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp,
    )
    Text(
        "area${if (regions.size == 1) "" else "s"} this week",
        fontSize = 11.sp,
        color = Color.Gray,
        textAlign = TextAlign.Center,
    )
    Spacer(Modifier.size(4.dp))
    Text(
        regions.sorted().joinToString(" · ") { it.replaceFirstChar(Char::uppercase) },
        fontSize = 10.sp,
        color = Color.Gray,
        textAlign = TextAlign.Center,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
    )
}

@Composable
private fun InsightContent(entries: List<EntryWithKeywords>) {
    val insight = remember(entries) { weeklyInsight(entries) }
    Icon(
        Icons.Default.Insights,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.primary,
        modifier = Modifier.size(26.dp),
    )
    Spacer(Modifier.size(6.dp))
    Text(
        insight,
        fontSize = 11.sp,
        color = MaterialTheme.colorScheme.onSurface,
        textAlign = TextAlign.Center,
        lineHeight = 15.sp,
    )
}

// ---- helpers -----------------------------------------------------------

private fun todayEntries(entries: List<EntryWithKeywords>): List<EntryWithKeywords> {
    val start = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis
    return entries.filter { it.entry.timestamp >= start }
}

/**
 * Mirrors CareScreen's private nextAppointment/formatNext. Kept local. TODO:
 * dedupe into a shared CareSchedule helper.
 */
private fun nextAppt(dayOfWeek: Int, minuteOfDay: Int): LocalDateTime {
    val now = LocalDateTime.now()
    val time = LocalTime.of(minuteOfDay / 60, minuteOfDay % 60)
    val dow = DayOfWeek.of(dayOfWeek)
    var date = now.toLocalDate().with(TemporalAdjusters.nextOrSame(dow))
    var dt = LocalDateTime.of(date, time)
    if (dt.isBefore(now)) {
        date = now.toLocalDate().with(TemporalAdjusters.next(dow))
        dt = LocalDateTime.of(date, time)
    }
    return dt
}

private fun formatNextAppt(dt: LocalDateTime): String {
    val today = LocalDate.now()
    val dayPart = when (dt.toLocalDate()) {
        today -> "Today"
        today.plusDays(1) -> "Tomorrow"
        else -> dt.format(DateTimeFormatter.ofPattern("EEEE"))
    }
    return "$dayPart at " + dt.format(DateTimeFormatter.ofPattern("h:mm a"))
}

private fun weeklyInsight(entries: List<EntryWithKeywords>): String {
    val now = System.currentTimeMillis()
    val cutoff = now - 14L * 24 * 3600 * 1000
    val recent = entries.filter { it.entry.timestamp >= cutoff }
    if (recent.size < 8) {
        return "Keep logging — insights need about a week of check-ins."
    }
    val zone = ZoneId.systemDefault()
    val byHour = recent
        .groupBy { Instant.ofEpochMilli(it.entry.timestamp).atZone(zone).hour }
        .filterValues { it.size >= 3 }
    if (byHour.size >= 2) {
        val low = byHour.minByOrNull { (_, v) -> v.map { it.entry.score }.average() }!!
        val high = byHour.maxByOrNull { (_, v) -> v.map { it.entry.score }.average() }!!
        val lowAvg = low.value.map { it.entry.score }.average()
        val highAvg = high.value.map { it.entry.score }.average()
        if (highAvg - lowAvg >= 0.6) {
            return "Your mood tends to run lowest around ${hourLabel(low.key)}, " +
                "and best around ${hourLabel(high.key)}."
        }
    }
    val weekMs = 7L * 24 * 3600 * 1000
    val thisWeek = recent.filter { it.entry.timestamp >= now - weekMs }.map { it.entry.score }
    val lastWeek = recent.filter { it.entry.timestamp < now - weekMs }.map { it.entry.score }
    if (thisWeek.size >= 3 && lastWeek.size >= 3) {
        val d = thisWeek.average() - lastWeek.average()
        return when {
            d >= 0.4 -> "This week is running better than last week. Keep it going."
            d <= -0.4 -> "This week is running lower than last week. Worth a gentle check-in with yourself."
            else -> "This week looks steady, close to last week's average."
        }
    }
    return "You're averaging %.1f out of 5 lately.".format(recent.map { it.entry.score }.average())
}

private fun hourLabel(h: Int): String {
    val ampm = if (h < 12) "AM" else "PM"
    val h12 = when {
        h == 0 -> 12
        h > 12 -> h - 12
        else -> h
    }
    return "$h12 $ampm"
}
