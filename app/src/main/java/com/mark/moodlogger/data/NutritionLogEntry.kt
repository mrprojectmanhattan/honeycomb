package com.mark.moodlogger.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * One "I ate this" entry - the user's own food log, not reference data. `servings`
 * scales the per-100g numbers copied in at log time (a snapshot, so editing the
 * reference database later never silently rewrites what was actually logged).
 * `foodItemId` links back to the FoodItem it was searched from when there is one;
 * free-text entries (nothing matched, or custom numbers) leave it null.
 */
@Entity(tableName = "nutrition_log")
data class NutritionLogEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long,
    val foodItemId: Long?,
    val name: String,
    val servings: Double,
    /** Grams per serving, so "servings" stays meaningful even for a custom entry. */
    val gramsPerServing: Double,
    val calories: Double,
    val protein: Double?,
    val fat: Double?,
    val carbs: Double?,
)
