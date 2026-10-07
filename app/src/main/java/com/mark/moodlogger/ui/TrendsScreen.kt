package com.mark.moodlogger.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mark.moodlogger.MoodScale
import com.mark.moodlogger.data.EntryWithKeywords
import com.mark.moodlogger.data.Keyword
import com.mark.moodlogger.data.MoodEntry
import com.mark.moodlogger.data.SleepLog
import com.mark.moodlogger.data.WorkoutLog
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.ZoneId
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

private data class Bar(
    val label: String,
    val avg: Float?,
    val count: Int,
    /** non-null on the first day of a month: the short month name, e.g. "Sep" */
    val monthMark: String? = null,
)

private data class KeywordStat(val name: String, val avg: Float, val count: Int)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrendsScreen(
    entries: List<EntryWithKeywords>,
    keywords: List<Keyword>,
    sleepLogs: List<SleepLog>,
    sleepGoalHours: Int,
    workouts: List<WorkoutLog>,
    onEditEntry: (EntryWithKeywords) -> Unit,
    onDeleteEntry: (Long) -> Unit,
    onBack: () -> Unit,
) {
    var view by remember { mutableIntStateOf(0) } // 0 = Chart, 1 = List
    val plain = remember(entries) { entries.map { it.entry } }
    val workoutStats = remember(workouts) { computeWorkout(workouts) }
    val byDay14 = remember(plain) { computeByDay(plain, 14) }
    val combDays = remember(plain) { computeDayAverages(plain, 30) }
    val byHour = remember(plain) { computeByHour(plain) }
    val streak = remember(plain) { computeStreak(plain) }
    val keywordStats = remember(entries) { computeKeywordStats(entries) }
    val offSchedule = remember(plain) { computeOffSchedule(plain) }
    val sleepStats = remember(sleepLogs, plain, sleepGoalHours) {
        computeSleep(sleepLogs, plain, sleepGoalHours)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Trends") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { pad ->
        Column(Modifier.fillMaxSize().padding(pad)) {
            SingleChoiceSegmentedButtonRow(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            ) {
                SegmentedButton(
                    selected = view == 0,
                    onClick = { view = 0 },
                    shape = SegmentedButtonDefaults.itemShape(0, 2),
                ) { Text("Chart") }
                SegmentedButton(
                    selected = view == 1,
                    onClick = { view = 1 },
                    shape = SegmentedButtonDefaults.itemShape(1, 2),
                ) { Text("List") }
            }

            if (view == 1) {
                HistoryContent(
                    entries = entries,
                    workouts = workouts,
                    onEdit = onEditEntry,
                    onDelete = onDeleteEntry,
                    modifier = Modifier.fillMaxSize(),
                )
                return@Column
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .honeycombBackground(MaterialTheme.colorScheme.background)
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
            ) {
                if (plain.isEmpty() && workouts.isEmpty()) {
                    Text("Log a few moods and your trends will show up here.", color = Color.Gray)
                    return@Column
                }

                Text(
                    if (streak > 0) "🍯  Logged $streak ${if (streak == 1) "day" else "days"} in a row"
                    else "No active streak, log today to start one",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    color = if (streak > 0) MoodScale.color(5) else Color.Gray,
                )

                Spacer(Modifier.size(24.dp))
                Text("Last 30 days", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                Spacer(Modifier.size(4.dp))
                val monthsSpan = remember(combDays) { combDays.mapNotNull { it.monthMark } }
                Text(
                    buildString {
                        append("Each cell is a day. Today is bottom-right.")
                        if (monthsSpan.size >= 2) {
                            append("  ")
                            append(monthsSpan.joinToString(" → "))
                            append(" — a ringed cell starts the month.")
                        }
                    },
                    fontSize = 11.sp,
                    color = Color.Gray,
                )
                Spacer(Modifier.size(10.dp))
                HoneycombCalendar(combDays)

                Spacer(Modifier.size(28.dp))
                ChartBlock("Last 14 days", byDay14) { i, _ -> i % 2 == 0 }
                Spacer(Modifier.size(28.dp))
                ChartBlock("By hour of day (all time)", byHour) { _, bar -> bar.label.isNotEmpty() }
                Spacer(Modifier.size(24.dp))
                Legend()

                Spacer(Modifier.size(28.dp))
                HorizontalDivider()
                Spacer(Modifier.size(20.dp))
                SleepBlock(sleepStats, sleepGoalHours)

                Spacer(Modifier.size(28.dp))
                HorizontalDivider()
                Spacer(Modifier.size(20.dp))
                WorkoutBlock(workoutStats)

                Spacer(Modifier.size(28.dp))
                HorizontalDivider()
                Spacer(Modifier.size(20.dp))

                Text("By tag", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                Spacer(Modifier.size(8.dp))
                if (keywordStats.isEmpty()) {
                    Text("No tagged entries yet.", fontSize = 12.sp, color = Color.Gray)
                } else {
                    keywordStats.forEach { s -> KeywordStatRow(s) }
                }

                Spacer(Modifier.size(28.dp))
                HorizontalDivider()
                Spacer(Modifier.size(20.dp))

                Text("Off-schedule logs", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                Spacer(Modifier.size(4.dp))
                Text(
                    "Entries logged more than 5 minutes off the hourly nudge (or during quiet hours).",
                    fontSize = 12.sp,
                    color = Color.Gray,
                )
                Spacer(Modifier.size(10.dp))
                OffScheduleBlock(offSchedule)
                Spacer(Modifier.size(24.dp))
            }
        }
    }
}

private data class DayCell(
    val dayOfMonth: Int,
    val avg: Float?,
    val isToday: Boolean,
    /** non-null on the first day of a month (and the earliest cell): short month name */
    val monthMark: String? = null,
)

@Composable
private fun HoneycombCalendar(cells: List<DayCell>) {
    val cols = 6
    val rows = (cells.size + cols - 1) / cols
    val measurer = rememberTextMeasurer()
    val emptyFill = Color(0x1FC8962B)
    val emptyLine = Color(0x4DC8962B)
    val todayRing = MoodScale.color(5)
    val onEmpty = Color(0x88FFFFFF)
    val monthEdge = Color(0xFF5B8DEF)

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(9.5f / (1.7320508f * (rows + 0.5f))),
    ) {
        val s = size.width / 9.5f
        val h = s * 1.7320508f
        cells.forEachIndexed { i, cell ->
            val col = i % cols
            val row = i / cols
            val cx = s + col * 1.5f * s
            val cy = h / 2f + row * h + if (col % 2 == 1) h / 2f else 0f

            val p = Path().apply {
                moveTo(cx - s, cy)
                lineTo(cx - s / 2f, cy - h / 2f)
                lineTo(cx + s / 2f, cy - h / 2f)
                lineTo(cx + s, cy)
                lineTo(cx + s / 2f, cy + h / 2f)
                lineTo(cx - s / 2f, cy + h / 2f)
                close()
            }
            if (cell.avg != null) {
                drawPath(p, color = MoodScale.colorForAvg(cell.avg))
            } else {
                drawPath(p, color = emptyFill)
                drawPath(p, color = emptyLine, style = Stroke(width = 1.5f))
            }
            if (cell.isToday) {
                drawPath(p, color = todayRing, style = Stroke(width = 3.5f))
            }
            if (cell.monthMark != null) {
                drawPath(p, color = monthEdge, style = Stroke(width = 4f))
            }

            val layout = measurer.measure(
                cell.dayOfMonth.toString(),
                style = TextStyle(
                    fontSize = 9.sp,
                    color = if (cell.avg != null) MoodScale.inkForAvg(cell.avg) else onEmpty,
                    fontWeight = FontWeight.Medium,
                ),
            )
            drawText(
                layout,
                topLeft = Offset(
                    cx - layout.size.width / 2f,
                    cy - layout.size.height / 2f,
                ),
            )
        }
    }
}

@Composable
private fun ChartBlock(
    title: String,
    bars: List<Bar>,
    showLabel: (index: Int, bar: Bar) -> Boolean,
) {
    Text(title, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
    Spacer(Modifier.size(10.dp))

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(140.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        bars.forEach { bar ->
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Bottom,
            ) {
                if (bar.avg != null) {
                    val frac = (((bar.avg - 1f) / 4f) * 0.86f + 0.06f).coerceIn(0.04f, 0.92f)
                    if (bars.size <= 16) {
                        Text("%.1f".format(bar.avg), fontSize = 8.sp, color = Color.Gray)
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(if (bars.size <= 16) 0.62f else 0.78f)
                            .fillMaxHeight(frac)
                            .background(
                                MoodScale.color(bar.avg.roundToInt().coerceIn(1, 5)),
                                RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp),
                            ),
                    )
                }
            }
        }
    }

    Row(modifier = Modifier.fillMaxWidth()) {
        bars.forEachIndexed { i, bar ->
            if (bar.monthMark != null) {
                Text(
                    text = bar.monthMark,
                    modifier = Modifier.weight(1f),
                    fontSize = 8.sp,
                    color = Color(0xFF5B8DEF),
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                )
            } else {
                Text(
                    text = if (showLabel(i, bar)) bar.label else "",
                    modifier = Modifier.weight(1f),
                    fontSize = 8.sp,
                    color = Color.Gray,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@Composable
private fun Legend() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        MoodScale.scores.forEach { score ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(10.dp)
                        .background(MoodScale.color(score), RoundedCornerShape(2.dp)),
                )
                Spacer(Modifier.size(4.dp))
                Text(MoodScale.label(score), fontSize = 10.sp, color = Color.Gray)
            }
        }
    }
}

@Composable
private fun KeywordStatRow(s: KeywordStat) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(s.name, fontSize = 13.sp, modifier = Modifier.width(90.dp))
        Box(
            Modifier
                .weight(1f)
                .height(14.dp)
                .background(Color(0x11808080), RoundedCornerShape(3.dp)),
        ) {
            Box(
                Modifier
                    .fillMaxHeight()
                    .fillMaxWidth((((s.avg - 1f) / 4f)).coerceIn(0.02f, 1f))
                    .background(
                        MoodScale.color(s.avg.roundToInt().coerceIn(1, 5)),
                        RoundedCornerShape(3.dp),
                    ),
            )
        }
        Spacer(Modifier.size(8.dp))
        Text("%.1f".format(s.avg), fontSize = 12.sp, fontWeight = FontWeight.Medium)
        Spacer(Modifier.size(6.dp))
        Text("(${s.count})", fontSize = 11.sp, color = Color.Gray)
    }
}

private data class OffSchedule(
    val manualCount: Int,
    val reminderCount: Int,
    val manualAvg: Float?,
    val reminderAvg: Float?,
    val recentManual: List<MoodEntry>,
)

@Composable
private fun OffScheduleBlock(data: OffSchedule) {
    if (data.manualCount == 0) {
        Text("Nothing logged off-schedule yet.", fontSize = 12.sp, color = Color.Gray)
        return
    }
    Row(Modifier.fillMaxWidth()) {
        Column(Modifier.weight(1f)) {
            Text("Off-schedule", fontSize = 12.sp, color = Color.Gray)
            Text(
                data.manualAvg?.let { "%.1f avg".format(it) } ?: "-",
                fontWeight = FontWeight.SemiBold,
            )
            Text("${data.manualCount} entries", fontSize = 11.sp, color = Color.Gray)
        }
        Column(Modifier.weight(1f)) {
            Text("From the nudge", fontSize = 12.sp, color = Color.Gray)
            Text(
                data.reminderAvg?.let { "%.1f avg".format(it) } ?: "-",
                fontWeight = FontWeight.SemiBold,
            )
            Text("${data.reminderCount} entries", fontSize = 11.sp, color = Color.Gray)
        }
    }
    Spacer(Modifier.size(12.dp))
    Text("Recent off-schedule entries", fontSize = 12.sp, color = Color.Gray)
    Spacer(Modifier.size(4.dp))
    val fmt = remember { SimpleDateFormat("MMM d, h:mm a", Locale.getDefault()) }
    data.recentManual.forEach { e ->
        Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(12.dp)
                    .background(MoodScale.color(e.score), RoundedCornerShape(3.dp)),
            )
            Spacer(Modifier.size(8.dp))
            Text(MoodScale.label(e.score), fontSize = 12.sp, modifier = Modifier.width(48.dp))
            Text(
                e.note.ifBlank { fmt.format(Date(e.timestamp)) },
                fontSize = 12.sp,
                color = Color.Gray,
                maxLines = 1,
            )
        }
    }
}

// ---- sleep -------------------------------------------------------------

private data class SleepStats(
    val nights: List<Pair<Int, Float>>, // (day-of-month, hours), last 14, oldest first
    val avgHours: Float?,
    val atGoal: Int,
    val goalOf: Int,
    val moodAfterFull: Float?,
    val moodAfterShort: Float?,
)

private fun computeSleep(
    sleeps: List<SleepLog>,
    moods: List<MoodEntry>,
    goalHours: Int,
): SleepStats {
    val today = LocalDate.now().toEpochDay()
    val recent = sleeps.filter { it.dateEpochDay > today - 14 }.sortedBy { it.dateEpochDay }
    val nights = recent.map { LocalDate.ofEpochDay(it.dateEpochDay).dayOfMonth to it.hours }
    val avg = if (recent.isEmpty()) null else recent.map { it.hours }.average().toFloat()

    fun moodOnDay(epochDay: Long): Float? {
        val start = LocalDate.ofEpochDay(epochDay)
            .atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val end = start + 24L * 60 * 60 * 1000
        val s = moods.filter { it.timestamp in start until end }
        return if (s.isEmpty()) null else s.map { it.score }.average().toFloat()
    }

    val full = sleeps.filter { it.hours >= goalHours }.mapNotNull { moodOnDay(it.dateEpochDay) }
    val short = sleeps.filter { it.hours < goalHours }.mapNotNull { moodOnDay(it.dateEpochDay) }

    return SleepStats(
        nights = nights,
        avgHours = avg,
        atGoal = recent.count { it.hours >= goalHours },
        goalOf = recent.size,
        moodAfterFull = if (full.isEmpty()) null else full.average().toFloat(),
        moodAfterShort = if (short.isEmpty()) null else short.average().toFloat(),
    )
}

@Composable
private fun SleepBlock(stats: SleepStats, goalHours: Int) {
    Text("Sleep", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
    Spacer(Modifier.size(8.dp))

    if (stats.goalOf == 0) {
        Text(
            "Log a night's sleep from the main screen and it'll chart here.",
            fontSize = 12.sp,
            color = Color.Gray,
        )
        return
    }

    Text(
        "%.1f h average, last %d %s".format(
            stats.avgHours ?: 0f, stats.goalOf, if (stats.goalOf == 1) "night" else "nights",
        ),
        fontWeight = FontWeight.Bold,
        fontSize = 16.sp,
    )
    Text(
        "${stats.atGoal} of ${stats.goalOf} at your ${goalHours}h goal",
        fontSize = 11.sp,
        color = Color.Gray,
    )

    Spacer(Modifier.size(10.dp))
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(90.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        stats.nights.forEach { (dom, hours) ->
            Column(
                modifier = Modifier.weight(1f).fillMaxHeight(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Bottom,
            ) {
                val frac = (hours / 12f).coerceIn(0.04f, 1f)
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.6f)
                        .fillMaxHeight(frac)
                        .background(
                            if (hours >= goalHours) MoodScale.color(4) else MoodScale.color(2),
                            RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp),
                        ),
                )
                Text("$dom", fontSize = 8.sp, color = Color.Gray)
            }
        }
    }

    if (stats.moodAfterFull != null && stats.moodAfterShort != null) {
        Spacer(Modifier.size(10.dp))
        Text(
            "Next-day mood: %.1f after a full night, %.1f after a short one"
                .format(stats.moodAfterFull, stats.moodAfterShort),
            fontSize = 12.sp,
            color = Color.Gray,
        )
    }
}

// ---- workout ----------------------------------------------------------

private data class WorkoutStats(
    val thisWeekRegions: Int,
    val daysTrainedLast30: Int,
    val sessionsLast30: Int,
    val perRegionLast30: List<Pair<String, Int>>, // all 6, display order
)

private fun computeWorkout(logs: List<WorkoutLog>): WorkoutStats {
    val weekStart = startOfWeekMs()
    val cutoff30 = System.currentTimeMillis() - 30L * 24 * 60 * 60 * 1000

    val thisWeek = logs.filter { it.timestamp >= weekStart }.map { it.region }.toSet()
    val last30 = logs.filter { it.timestamp >= cutoff30 }
    val perRegion = WorkoutLog.REGIONS.map { r -> r to last30.count { it.region == r } }
    val daysTrained = last30.map { startOfDayMs(it.timestamp) }.toSet().size

    return WorkoutStats(
        thisWeekRegions = thisWeek.size,
        daysTrainedLast30 = daysTrained,
        sessionsLast30 = last30.size,
        perRegionLast30 = perRegion,
    )
}

@Composable
private fun WorkoutBlock(stats: WorkoutStats) {
    Text("Workout", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
    Spacer(Modifier.size(8.dp))

    if (stats.sessionsLast30 == 0 && stats.thisWeekRegions == 0) {
        Text(
            "Log a workout from the Workout tab and it'll chart here.",
            fontSize = 12.sp,
            color = Color.Gray,
        )
        return
    }

    Text(
        "${stats.thisWeekRegions} of 6 muscle groups this week",
        fontWeight = FontWeight.Bold,
        fontSize = 16.sp,
    )
    Text(
        "Trained on ${stats.daysTrainedLast30} of the last 30 days",
        fontSize = 11.sp,
        color = Color.Gray,
    )

    Spacer(Modifier.size(10.dp))
    val max = (stats.perRegionLast30.maxOfOrNull { it.second } ?: 0).coerceAtLeast(1)
    stats.perRegionLast30.forEach { (region, count) ->
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(WorkoutLog.label(region), fontSize = 13.sp, modifier = Modifier.width(90.dp))
            Box(
                Modifier
                    .weight(1f)
                    .height(14.dp)
                    .background(Color(0x11808080), RoundedCornerShape(3.dp)),
            ) {
                if (count > 0) {
                    Box(
                        Modifier
                            .fillMaxHeight()
                            .fillMaxWidth((count.toFloat() / max).coerceIn(0.04f, 1f))
                            .background(Color(0xFFD9A22E), RoundedCornerShape(3.dp)),
                    )
                }
            }
            Spacer(Modifier.size(8.dp))
            Text("$count", fontSize = 12.sp, fontWeight = FontWeight.Medium)
        }
    }
}

// ---- computations -------------------------------------------------------

private fun startOfDayMs(ts: Long): Long = Calendar.getInstance().apply {
    timeInMillis = ts
    set(Calendar.HOUR_OF_DAY, 0)
    set(Calendar.MINUTE, 0)
    set(Calendar.SECOND, 0)
    set(Calendar.MILLISECOND, 0)
}.timeInMillis

/** Midnight on the Monday of the current week. */
private fun startOfWeekMs(): Long = Calendar.getInstance().apply {
    set(Calendar.HOUR_OF_DAY, 0)
    set(Calendar.MINUTE, 0)
    set(Calendar.SECOND, 0)
    set(Calendar.MILLISECOND, 0)
    val dow = get(Calendar.DAY_OF_WEEK)
    val back = if (dow == Calendar.SUNDAY) 6 else dow - Calendar.MONDAY
    add(Calendar.DAY_OF_YEAR, -back)
}.timeInMillis

private fun startOfDayCal(): Calendar = Calendar.getInstance().apply {
    set(Calendar.HOUR_OF_DAY, 0)
    set(Calendar.MINUTE, 0)
    set(Calendar.SECOND, 0)
    set(Calendar.MILLISECOND, 0)
}

private fun computeByDay(entries: List<MoodEntry>, days: Int): List<Bar> {
    val fmt = SimpleDateFormat(if (days > 20) "d" else "d/M", Locale.getDefault())
    val monthFmt = SimpleDateFormat("MMM", Locale.getDefault())
    val cal = startOfDayCal().apply { add(Calendar.DAY_OF_YEAR, -(days - 1)) }
    val bars = ArrayList<Bar>(days)
    repeat(days) { i ->
        val start = cal.timeInMillis
        val dom = cal.get(Calendar.DAY_OF_MONTH)
        cal.add(Calendar.DAY_OF_YEAR, 1)
        val end = cal.timeInMillis
        val slice = entries.filter { it.timestamp in start until end }
        val avg = if (slice.isEmpty()) null else slice.sumOf { it.score }.toFloat() / slice.size
        val monthMark = if (i == 0 || dom == 1) monthFmt.format(start) else null
        bars.add(Bar(fmt.format(start), avg, slice.size, monthMark))
    }
    return bars
}

private fun computeDayAverages(entries: List<MoodEntry>, days: Int): List<DayCell> {
    val cal = startOfDayCal().apply { add(Calendar.DAY_OF_YEAR, -(days - 1)) }
    val todayStart = startOfDayCal().timeInMillis
    val monthFmt = SimpleDateFormat("MMM", Locale.getDefault())
    val out = ArrayList<DayCell>(days)
    repeat(days) { i ->
        val start = cal.timeInMillis
        val dom = cal.get(Calendar.DAY_OF_MONTH)
        cal.add(Calendar.DAY_OF_YEAR, 1)
        val end = cal.timeInMillis
        val slice = entries.filter { it.timestamp in start until end }
        val avg = if (slice.isEmpty()) null else slice.sumOf { it.score }.toFloat() / slice.size
        val monthMark = if (i == 0 || dom == 1) monthFmt.format(start) else null
        out.add(DayCell(dom, avg, start == todayStart, monthMark))
    }
    return out
}

private fun computeByHour(entries: List<MoodEntry>): List<Bar> {
    val sums = IntArray(24)
    val counts = IntArray(24)
    val cal = Calendar.getInstance()
    for (e in entries) {
        cal.timeInMillis = e.timestamp
        val h = cal.get(Calendar.HOUR_OF_DAY)
        sums[h] += e.score
        counts[h] += 1
    }
    return (0..23).map { h ->
        val avg = if (counts[h] == 0) null else sums[h].toFloat() / counts[h]
        val label = when (h) { 0 -> "12a"; 6 -> "6a"; 12 -> "12p"; 18 -> "6p"; else -> "" }
        Bar(label, avg, counts[h])
    }
}

private fun computeStreak(entries: List<MoodEntry>): Int {
    if (entries.isEmpty()) return 0
    val dayMs = 24L * 60 * 60 * 1000
    val today = startOfDayCal().timeInMillis
    val loggedDays = entries.map {
        Calendar.getInstance().apply {
            timeInMillis = it.timestamp
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }.toSet()

    // streak counts back from today, or from yesterday if today isn't logged yet
    var cursor = if (loggedDays.contains(today)) today else today - dayMs
    if (!loggedDays.contains(cursor)) return 0
    var streak = 0
    while (loggedDays.contains(cursor)) {
        streak++
        cursor -= dayMs
    }
    return streak
}

private fun computeKeywordStats(entries: List<EntryWithKeywords>): List<KeywordStat> {
    val byKw = HashMap<String, MutableList<Int>>()
    for (r in entries) {
        for (name in r.keywordNames) {
            byKw.getOrPut(name) { mutableListOf() }.add(r.entry.score)
        }
    }
    return byKw.map { (name, scores) ->
        KeywordStat(name, scores.sum().toFloat() / scores.size, scores.size)
    }.sortedByDescending { it.count }
}

private fun computeOffSchedule(entries: List<MoodEntry>): OffSchedule {
    val manual = entries.filter { it.source == MoodEntry.SOURCE_MANUAL }
    val reminder = entries.filter { it.source == MoodEntry.SOURCE_REMINDER }
    return OffSchedule(
        manualCount = manual.size,
        reminderCount = reminder.size,
        manualAvg = if (manual.isEmpty()) null else manual.sumOf { it.score }.toFloat() / manual.size,
        reminderAvg = if (reminder.isEmpty()) null else reminder.sumOf { it.score }.toFloat() / reminder.size,
        recentManual = manual.sortedByDescending { it.timestamp }.take(8),
    )
}
