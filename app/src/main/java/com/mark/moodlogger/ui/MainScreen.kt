package com.mark.moodlogger.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mark.moodlogger.MoodScale
import com.mark.moodlogger.data.CareProvider
import com.mark.moodlogger.data.EntryWithKeywords
import com.mark.moodlogger.data.MajorGoal
import com.mark.moodlogger.data.MoodEntry
import com.mark.moodlogger.data.PromptEvent
import com.mark.moodlogger.data.Settings
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    entries: List<EntryWithKeywords>,
    navPosition: String,
    navRailAlign: String,
    waterToday: Int,
    waterGoalOz: Int,
    waterServingOz: Int,
    majorGoals: List<MajorGoal>,
    stepsToday: Int,
    workoutThisWeek: Set<String>,
    careProviders: List<CareProvider>,
    homeWidget: String,
    onSetHomeWidget: (String) -> Unit,
    onLogNow: () -> Unit,
    onLogWater: () -> Unit,
    onUndoWater: () -> Unit,
    onLogSleep: () -> Unit,
    onBreathe: () -> Unit,
    onOpenJournal: () -> Unit,
    onOpenGoals: () -> Unit,
    onOpenTrends: () -> Unit,
    onOpenCare: () -> Unit,
    onOpenShopping: () -> Unit,
    onOpenNutrition: () -> Unit,
    shoppingOpenCount: Int,
    openPromptGroups: List<List<PromptEvent>>,
    onPromptReason: (List<PromptEvent>, String) -> Unit,
    onOpenSettings: () -> Unit,
) {
    val today = rememberTodayEntries(entries)

    val navItems = listOf(
        NavItem("Goals", Icons.Default.Flag, onOpenGoals),
        NavItem("Journal", Icons.Default.Book, onOpenJournal),
        NavItem("Trends", Icons.Default.BarChart, onOpenTrends),
        NavItem("Care", Icons.Default.MedicalServices, onOpenCare),
        NavItem("Settings", Icons.Default.Settings, onOpenSettings),
    )

    val onTop = navPosition == Settings.NAV_TOP
    val onBottom = navPosition == Settings.NAV_BOTTOM
    val onLeft = navPosition == Settings.NAV_LEFT
    val onRight = navPosition == Settings.NAV_RIGHT

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Honeycomb") },
                actions = {
                    if (onTop) {
                        navItems.forEach { item ->
                            IconButton(onClick = item.onClick) {
                                Icon(item.icon, contentDescription = item.label)
                            }
                        }
                    }
                },
            )
        },
        bottomBar = {
            if (onBottom) {
                NavigationBar {
                    navItems.forEach { item ->
                        NavigationBarItem(
                            selected = false,
                            onClick = item.onClick,
                            icon = { Icon(item.icon, contentDescription = item.label) },
                            label = { Text(item.label) },
                        )
                    }
                }
            }
        },
    ) { pad ->
        if (onLeft || onRight) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(pad),
            ) {
                if (onLeft) SideRail(navItems, navRailAlign)
                MainContent(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    entries = entries,
                    today = today,
                    waterToday = waterToday,
                    waterGoalOz = waterGoalOz,
                    waterServingOz = waterServingOz,
                    majorGoals = majorGoals,
                    stepsToday = stepsToday,
                    workoutThisWeek = workoutThisWeek,
                    careProviders = careProviders,
                    homeWidget = homeWidget,
                    onSetHomeWidget = onSetHomeWidget,
                    onLogNow = onLogNow,
                    onLogWater = onLogWater,
                    onUndoWater = onUndoWater,
                    onLogSleep = onLogSleep,
                    onBreathe = onBreathe,
                    shoppingOpenCount = shoppingOpenCount,
                    onOpenShopping = onOpenShopping,
                    onOpenNutrition = onOpenNutrition,
                    openPromptGroups = openPromptGroups,
                    onPromptReason = onPromptReason,
                )
                if (onRight) SideRail(navItems, navRailAlign)
            }
        } else {
            MainContent(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(pad),
                entries = entries,
                today = today,
                waterToday = waterToday,
                waterGoalOz = waterGoalOz,
                waterServingOz = waterServingOz,
                majorGoals = majorGoals,
                stepsToday = stepsToday,
                workoutThisWeek = workoutThisWeek,
                careProviders = careProviders,
                homeWidget = homeWidget,
                onSetHomeWidget = onSetHomeWidget,
                onLogNow = onLogNow,
                onLogWater = onLogWater,
                onUndoWater = onUndoWater,
                onLogSleep = onLogSleep,
                onBreathe = onBreathe,
                shoppingOpenCount = shoppingOpenCount,
                onOpenShopping = onOpenShopping,
                onOpenNutrition = onOpenNutrition,
                openPromptGroups = openPromptGroups,
                onPromptReason = onPromptReason,
            )
        }
    }
}

private data class NavItem(
    val label: String,
    val icon: ImageVector,
    val onClick: () -> Unit,
)

@Composable
private fun SideRail(items: List<NavItem>, align: String) {
    val arrangement = when (align) {
        Settings.RAIL_TOP -> Arrangement.Top
        Settings.RAIL_BOTTOM -> Arrangement.Bottom
        else -> Arrangement.Center
    }
    Column(
        modifier = Modifier
            .fillMaxHeight()
            .width(76.dp)
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
            .padding(vertical = 12.dp),
        verticalArrangement = arrangement,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        items.forEach { item ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = item.onClick)
                    .padding(vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Icon(
                    item.icon,
                    contentDescription = item.label,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.size(4.dp))
                Text(
                    item.label,
                    fontSize = 10.sp,
                    maxLines = 1,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun MainContent(
    modifier: Modifier,
    entries: List<EntryWithKeywords>,
    today: List<EntryWithKeywords>,
    waterToday: Int,
    waterGoalOz: Int,
    waterServingOz: Int,
    majorGoals: List<MajorGoal>,
    stepsToday: Int,
    workoutThisWeek: Set<String>,
    careProviders: List<CareProvider>,
    homeWidget: String,
    onSetHomeWidget: (String) -> Unit,
    onLogNow: () -> Unit,
    onLogWater: () -> Unit,
    onUndoWater: () -> Unit,
    onLogSleep: () -> Unit,
    onBreathe: () -> Unit,
    shoppingOpenCount: Int,
    onOpenShopping: () -> Unit,
    onOpenNutrition: () -> Unit,
    openPromptGroups: List<List<PromptEvent>>,
    onPromptReason: (List<PromptEvent>, String) -> Unit,
) {
    Column(
        modifier = modifier
            .honeycombBackground(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp),
    ) {
        Spacer(Modifier.size(12.dp))
        Button(
            onClick = onLogNow,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(Modifier.size(8.dp))
            Text("Log my mood now", fontSize = 16.sp)
        }

        val openGroup = openPromptGroups.firstOrNull()
        if (openGroup != null) {
            Spacer(Modifier.size(8.dp))
            MissedPromptCard(
                group = openGroup,
                remainingGroups = openPromptGroups.size - 1,
                onReason = { reason -> onPromptReason(openGroup, reason) },
            )
        }

        Spacer(Modifier.size(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            OutlinedButton(
                onClick = onLogSleep,
                shape = RoundedCornerShape(14.dp),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp),
                modifier = Modifier.weight(1f),
            ) {
                Icon(
                    Icons.Default.Bedtime,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                )
                Spacer(Modifier.size(6.dp))
                Text("Log sleep", fontSize = 12.sp, maxLines = 1)
            }
            OutlinedButton(
                onClick = onBreathe,
                shape = RoundedCornerShape(14.dp),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp),
                modifier = Modifier.weight(1f),
            ) {
                Icon(
                    Icons.Default.Air,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                )
                Spacer(Modifier.size(6.dp))
                Text("Take a breath", fontSize = 12.sp, maxLines = 1)
            }
        }

        Spacer(Modifier.size(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            OutlinedButton(
                onClick = onOpenShopping,
                shape = RoundedCornerShape(14.dp),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp),
                modifier = Modifier.weight(1f),
            ) {
                Icon(
                    Icons.Default.ShoppingCart,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                )
                Spacer(Modifier.size(6.dp))
                Text(
                    if (shoppingOpenCount > 0) "Shopping  ·  $shoppingOpenCount" else "Shopping",
                    fontSize = 12.sp,
                    maxLines = 1,
                )
            }
            OutlinedButton(
                onClick = onOpenNutrition,
                shape = RoundedCornerShape(14.dp),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp),
                modifier = Modifier.weight(1f),
            ) {
                Icon(
                    Icons.Default.Restaurant,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                )
                Spacer(Modifier.size(6.dp))
                Text("Nutrition", fontSize = 12.sp, maxLines = 1)
            }
        }

        Spacer(Modifier.size(8.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
        ) {
            // Left: today's mood entries
            Column(
                modifier = Modifier
                    .weight(1.15f)
                    .fillMaxHeight(),
            ) {
                Text("Today", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                Spacer(Modifier.size(8.dp))
                if (today.isEmpty()) {
                    Text(
                        "Nothing logged yet today.",
                        color = Color.Gray,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(vertical = 16.dp),
                    )
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(today, key = { it.entry.id }) { row -> EntryCard(row) }
                    }
                }
            }

            Spacer(Modifier.size(14.dp))

            // Right: the honey jar water tracker, then the major-goal reminder,
            // in the empty space.
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .verticalScroll(rememberScrollState())
                    .padding(top = 2.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                WaterPanel(
                    today = waterToday,
                    goal = waterGoalOz,
                    serving = waterServingOz,
                    onLog = onLogWater,
                    onUndo = onUndoWater,
                    modifier = Modifier.fillMaxWidth(),
                )
                if (majorGoals.isNotEmpty()) {
                    Spacer(Modifier.size(20.dp))
                    MajorGoalStack(
                        goals = majorGoals,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                Spacer(Modifier.size(if (majorGoals.isNotEmpty()) 14.dp else 20.dp))
                HomeWidgetSlot(
                    widgetId = homeWidget,
                    entries = entries,
                    stepsToday = stepsToday,
                    workoutThisWeek = workoutThisWeek,
                    careProviders = careProviders,
                    onSetWidget = onSetHomeWidget,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.size(12.dp))
            }
        }
    }
}

@Composable
private fun WaterPanel(
    today: Int,
    goal: Int,
    serving: Int,
    onLog: () -> Unit,
    onUndo: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("Water", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
        Spacer(Modifier.size(6.dp))
        HoneyJar(
            fraction = if (goal > 0) today.toFloat() / goal else 0f,
            modifier = Modifier
                .fillMaxWidth()
                .height(130.dp),
        )
        Spacer(Modifier.size(8.dp))
        Text(
            "$today / $goal oz",
            fontWeight = FontWeight.Bold,
            fontSize = 17.sp,
        )
        Spacer(Modifier.size(8.dp))
        Button(
            onClick = onLog,
            shape = RoundedCornerShape(12.dp),
        ) {
            Text("+$serving oz")
        }
        if (today > 0) {
            TextButton(onClick = onUndo) { Text("Undo") }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EntryCard(row: EntryWithKeywords) {
    val entry = row.entry
    val time = remember(entry.timestamp) {
        SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(entry.timestamp))
    }
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                shape = HexagonShape,
                color = MoodScale.color(entry.score),
                modifier = Modifier.size(width = 34.dp, height = 30.dp),
            ) {}
            Spacer(Modifier.size(10.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "${MoodScale.emoji(entry.score)}  ${MoodScale.label(entry.score)}",
                        fontWeight = FontWeight.Medium,
                        fontSize = 14.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    Spacer(Modifier.size(6.dp))
                    Text(time, fontSize = 11.sp, color = Color.Gray, maxLines = 1)
                }
                if (entry.source == MoodEntry.SOURCE_MANUAL) {
                    Text("off-schedule", fontSize = 10.sp, color = Color(0xFFF57C00))
                }
                if (entry.note.isNotBlank()) {
                    Text(
                        entry.note,
                        fontSize = 12.sp,
                        color = Color.Gray,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                if (row.keywords.isNotEmpty()) {
                    Spacer(Modifier.size(4.dp))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        row.keywordNames.forEach { name ->
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color(0x22C98A12),
                            ) {
                                Text(
                                    name,
                                    fontSize = 10.sp,
                                    color = Color(0xFFD9A22E),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun rememberTodayEntries(entries: List<EntryWithKeywords>): List<EntryWithKeywords> {
    val startOfDay = remember(entries) {
        Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }
    return entries.filter { it.entry.timestamp >= startOfDay }
}
