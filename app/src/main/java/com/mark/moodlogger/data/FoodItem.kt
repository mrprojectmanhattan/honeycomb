package com.mark.moodlogger.data

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import android.content.Context

/**
 * One food, one row, values per 100g (the USDA standard, so any serving size scales
 * cleanly). Read-only reference data - it never gets written to on-device, only
 * replaced wholesale when the bundled database is updated. See NutritionReferenceDatabase.
 */
@Entity(tableName = "food_items")
data class FoodItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val fdcId: String?,
    val name: String,
    val category: String?,
    /** "usda_foundation" or "usda_sr_legacy" today; future sources (e.g. a barcode
     *  database) get their own tag so results can show where a number came from. */
    val source: String,
    val caloriesPer100g: Double?,
    val proteinPer100g: Double?,
    val fatPer100g: Double?,
    val carbsPer100g: Double?,
    val fiberPer100g: Double?,
    val sugarPer100g: Double?,
    val sodiumMgPer100g: Double?,
)

@Dao
interface FoodItemDao {
    @Query(
        "SELECT * FROM food_items WHERE name LIKE '%' || :query || '%' " +
            "ORDER BY LENGTH(name) ASC LIMIT 40"
    )
    suspend fun search(query: String): List<FoodItem>

    @Query("SELECT * FROM food_items WHERE id = :id")
    suspend fun get(id: Long): FoodItem?

    @Query("SELECT COUNT(*) FROM food_items")
    suspend fun count(): Int
}

/**
 * A separate, read-only database from the bundled USDA data (nutrition.db in assets/).
 * Kept apart from MoodDatabase on purpose: this one has no migrations to write and no
 * personal data in it, so updating it is just swapping the asset file, never a risky
 * schema migration against the user's mood/journal history. See NutritionLogEntry for
 * where logged meals live instead.
 */
@Database(entities = [FoodItem::class], version = 1, exportSchema = false)
abstract class NutritionReferenceDatabase : RoomDatabase() {
    abstract fun foodItemDao(): FoodItemDao

    companion object {
        @Volatile
        private var instance: NutritionReferenceDatabase? = null

        fun get(context: Context): NutritionReferenceDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    NutritionReferenceDatabase::class.java,
                    "nutrition_reference.db",
                )
                    .createFromAsset("nutrition.db")
                    // The bundled asset is the source of truth; never write here.
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { instance = it }
            }
    }
}
