package com.mark.moodlogger.ui

import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.mark.moodlogger.data.FoodItem
import com.mark.moodlogger.data.NutritionLogEntry
import kotlin.math.roundToInt

/**
 * Search-and-log nutrition screen. Not a full diet-app clone on purpose: type what
 * you're eating, see what it costs against today's budget, log it, done. Numbers only,
 * no judgment built in on purpose - tone matters as much as the math.
 *
 * Stateless, same pattern as the other screens: MainViewModel owns the real data and
 * wires searchQuery/results/todayEntries/dailyBudgetCalories + the callbacks below.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NutritionScreen(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    searchResults: List<FoodItem>,
    onLogFood: (FoodItem, servings: Double) -> Unit,
    /** For anything the database doesn't have: a restaurant dish, a homemade meal,
     *  a specific brand it's missing. Protein/fat/carbs are optional - calories is the
     *  only number that's actually required to log something. */
    onLogCustom: (name: String, calories: Double, protein: Double?, fat: Double?, carbs: Double?) -> Unit,
    todayEntries: List<NutritionLogEntry>,
    onDeleteEntry: (NutritionLogEntry) -> Unit,
    dailyBudgetCalories: Int,
    onBack: () -> Unit,
) {
    val todayCalories = todayEntries.sumOf { it.calories }
    val remaining = dailyBudgetCalories - todayCalories
    val fraction = if (dailyBudgetCalories > 0) (todayCalories / dailyBudgetCalories).toFloat().coerceIn(0f, 1f) else 0f
    var showCustomDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Nutrition") },
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
                .padding(horizontal = 20.dp),
        ) {
            Spacer(Modifier.size(16.dp))

            // ---- today's budget, plain numbers, no commentary ----
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text(
                        "Today: ${todayCalories.roundToInt()} / $dailyBudgetCalories cal",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(Modifier.size(8.dp))
                    LinearProgressIndicator(
                        progress = { fraction },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.size(4.dp))
                    Text(
                        if (remaining >= 0) "${remaining.roundToInt()} left" else "${(-remaining).roundToInt()} over",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }

            Spacer(Modifier.size(16.dp))

            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                label = { Text("Search a food") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )

            Spacer(Modifier.size(8.dp))

            // ---- search results (only shown while actively searching) ----
            if (searchQuery.isNotBlank()) {
                LazyColumn(modifier = Modifier.fillMaxWidth()) {
                    items(searchResults, key = { it.id }) { food ->
                        FoodResultRow(food, onLog = { onLogFood(food, 1.0) })
                    }
                    item {
                        // Always here, not just when search comes up empty - the database
                        // is whole/generic foods only, so branded stuff and homemade meals
                        // are missing on purpose, not a bug. See NUTRITION_MERGE_NOTES.md.
                        Text(
                            "Can't find it? Add it manually",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp)
                                .clickable { showCustomDialog = true },
                        )
                    }
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        "Today's log",
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.padding(vertical = 8.dp),
                    )
                    TextButton(onClick = { showCustomDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.size(4.dp))
                        Text("Add manually")
                    }
                }
                LazyColumn(modifier = Modifier.fillMaxWidth()) {
                    items(todayEntries, key = { it.id }) { entry ->
                        LoggedEntryRow(entry, onDelete = { onDeleteEntry(entry) })
                    }
                }
            }
        }
    }

    if (showCustomDialog) {
        CustomFoodDialog(
            onDismiss = { showCustomDialog = false },
            onSave = { name, cal, protein, fat, carbs ->
                onLogCustom(name, cal, protein, fat, carbs)
                showCustomDialog = false
            },
        )
    }
}

/**
 * The manual-entry fallback for anything the bundled database doesn't have. Only the
 * name and calorie count are required - protein/fat/carbs are a bonus if he knows them
 * (off a label, say) but logging still works without them.
 */
@Composable
private fun CustomFoodDialog(
    onDismiss: () -> Unit,
    onSave: (name: String, calories: Double, protein: Double?, fat: Double?, carbs: Double?) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var calories by remember { mutableStateOf("") }
    var protein by remember { mutableStateOf("") }
    var fat by remember { mutableStateOf("") }
    var carbs by remember { mutableStateOf("") }
    val caloriesValid = calories.toDoubleOrNull() != null
    val canSave = name.isNotBlank() && caloriesValid

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add a food manually") },
        text = {
            Column {
                OutlinedTextField(
                    value = name, onValueChange = { name = it },
                    label = { Text("What is it?") }, singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.size(8.dp))
                OutlinedTextField(
                    value = calories, onValueChange = { calories = it },
                    label = { Text("Calories") }, singleLine = true,
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Number),
                    isError = calories.isNotBlank() && !caloriesValid,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.size(8.dp))
                Text(
                    "Optional, if you know it (grams):",
                    style = MaterialTheme.typography.bodySmall,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = protein, onValueChange = { protein = it },
                        label = { Text("Protein") }, singleLine = true,
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                    )
                    OutlinedTextField(
                        value = fat, onValueChange = { fat = it },
                        label = { Text("Fat") }, singleLine = true,
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                    )
                    OutlinedTextField(
                        value = carbs, onValueChange = { carbs = it },
                        label = { Text("Carbs") }, singleLine = true,
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = canSave,
                onClick = {
                    onSave(
                        name.trim(),
                        calories.toDouble(),
                        protein.toDoubleOrNull(),
                        fat.toDoubleOrNull(),
                        carbs.toDoubleOrNull(),
                    )
                },
            ) { Text("Log it") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun FoodResultRow(food: FoodItem, onLog: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(food.name, style = MaterialTheme.typography.bodyLarge)
            Text(
                "${food.caloriesPer100g?.roundToInt() ?: "?"} cal / 100g",
                style = MaterialTheme.typography.bodySmall,
            )
        }
        IconButton(onClick = onLog) {
            Icon(Icons.Default.Add, contentDescription = "Log this")
        }
    }
}

@Composable
private fun LoggedEntryRow(entry: NutritionLogEntry, onDelete: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(entry.name, style = MaterialTheme.typography.bodyLarge)
            Text("${entry.calories.roundToInt()} cal", style = MaterialTheme.typography.bodySmall)
        }
        IconButton(onClick = onDelete) {
            Icon(Icons.Default.Delete, contentDescription = "Remove")
        }
    }
}
