package com.mark.moodlogger.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        MoodEntry::class, Keyword::class, EntryKeyword::class, Habit::class,
        WaterLog::class, WorkoutLog::class, SleepLog::class,
        Goal::class, GoalCheckin::class, StepDay::class, MajorGoal::class,
        JournalEntry::class, CareProvider::class, ShoppingItem::class,
        Medication::class, PromptEvent::class, NutritionLogEntry::class,
        MentalHealthCheckin::class,
    ],
    version = 17,
    exportSchema = true,
)
abstract class MoodDatabase : RoomDatabase() {

    abstract fun moodDao(): MoodDao
    abstract fun nutritionLogDao(): NutritionLogDao
    abstract fun mentalHealthCheckinDao(): MentalHealthCheckinDao

    companion object {
        val DEFAULT_KEYWORDS = listOf(
            "work", "sleep", "exercise", "social", "family", "stress", "sick", "caffeine",
        )

        private fun seedKeywords(db: SupportSQLiteDatabase) {
            DEFAULT_KEYWORDS.forEach { name ->
                db.execSQL("INSERT OR IGNORE INTO keywords (name) VALUES (?)", arrayOf<Any>(name))
            }
        }

        /** v1 -> v2: add source column, add the keyword tables, seed defaults. */
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE mood_entries ADD COLUMN source TEXT NOT NULL DEFAULT 'unknown'"
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `keywords` " +
                        "(`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL)"
                )
                db.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS `index_keywords_name` ON `keywords` (`name`)"
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `entry_keywords` " +
                        "(`entryId` INTEGER NOT NULL, `keywordId` INTEGER NOT NULL, " +
                        "PRIMARY KEY(`entryId`, `keywordId`), " +
                        "FOREIGN KEY(`entryId`) REFERENCES `mood_entries`(`id`) " +
                        "ON UPDATE NO ACTION ON DELETE CASCADE , " +
                        "FOREIGN KEY(`keywordId`) REFERENCES `keywords`(`id`) " +
                        "ON UPDATE NO ACTION ON DELETE CASCADE )"
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_entry_keywords_keywordId` " +
                        "ON `entry_keywords` (`keywordId`)"
                )
                seedKeywords(db)
            }
        }

        /** v2 -> v3: add the habits table. */
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `habits` " +
                        "(`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`name` TEXT NOT NULL, `kind` TEXT NOT NULL, " +
                        "`startEpochDay` INTEGER NOT NULL)"
                )
            }
        }

        /** v3 -> v4: add the water_logs table. */
        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `water_logs` " +
                        "(`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`timestamp` INTEGER NOT NULL, `amountOz` INTEGER NOT NULL)"
                )
            }
        }

        /** v4 -> v5: add the workout_logs table. */
        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `workout_logs` " +
                        "(`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`timestamp` INTEGER NOT NULL, `region` TEXT NOT NULL)"
                )
            }
        }

        /** v5 -> v6: add the sleep_logs table + its unique index on dateEpochDay. */
        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `sleep_logs` " +
                        "(`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`dateEpochDay` INTEGER NOT NULL, `bedtimeMillis` INTEGER NOT NULL, " +
                        "`wakeMillis` INTEGER NOT NULL, `quality` INTEGER NOT NULL)"
                )
                db.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS `index_sleep_logs_dateEpochDay` " +
                        "ON `sleep_logs` (`dateEpochDay`)"
                )
            }
        }

        /** v6 -> v7: add goals, goal_checkins, step_days. */
        private val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `goals` " +
                        "(`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `title` TEXT NOT NULL, " +
                        "`auto` INTEGER NOT NULL, `source` TEXT NOT NULL, " +
                        "`targetCount` INTEGER NOT NULL, `periodDays` INTEGER NOT NULL, " +
                        "`dailyThreshold` INTEGER NOT NULL, `createdAt` INTEGER NOT NULL, " +
                        "`archived` INTEGER NOT NULL)"
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `goal_checkins` " +
                        "(`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`goalId` INTEGER NOT NULL, `timestamp` INTEGER NOT NULL)"
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_goal_checkins_goalId` " +
                        "ON `goal_checkins` (`goalId`)"
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `step_days` " +
                        "(`epochDay` INTEGER NOT NULL, `steps` INTEGER NOT NULL, " +
                        "`lastCumulative` INTEGER NOT NULL, PRIMARY KEY(`epochDay`))"
                )
            }
        }

        /** v7 -> v8: add the major_goals table. */
        private val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `major_goals` " +
                        "(`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`title` TEXT NOT NULL, `detail` TEXT NOT NULL, " +
                        "`createdAt` INTEGER NOT NULL)"
                )
            }
        }

        /** v8 -> v9: add the journal_entries table (text + voice reflections). Additive only. */
        private val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `journal_entries` " +
                        "(`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`timestamp` INTEGER NOT NULL, `day` INTEGER NOT NULL, " +
                        "`text` TEXT NOT NULL, `audioPath` TEXT, " +
                        "`audioDurationMs` INTEGER NOT NULL)"
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_journal_entries_day` " +
                        "ON `journal_entries` (`day`)"
                )
            }
        }

        /** v9 -> v10: add an optional `title` column to journal_entries. Additive only. */
        private val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE `journal_entries` ADD COLUMN `title` TEXT NOT NULL DEFAULT ''"
                )
            }
        }

        /** v10 -> v11: add the care_providers table. Additive only. */
        private val MIGRATION_10_11 = object : Migration(10, 11) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `care_providers` " +
                        "(`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`kind` TEXT NOT NULL, `name` TEXT NOT NULL, " +
                        "`dayOfWeek` INTEGER, `minuteOfDay` INTEGER, " +
                        "`notes` TEXT NOT NULL DEFAULT '', " +
                        "`active` INTEGER NOT NULL DEFAULT 1)"
                )
            }
        }

        /** v11 -> v12: add the shopping_items table. Additive only. */
        private val MIGRATION_11_12 = object : Migration(11, 12) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `shopping_items` " +
                        "(`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`name` TEXT NOT NULL, `note` TEXT NOT NULL DEFAULT '', " +
                        "`addedAt` INTEGER NOT NULL, `purchasedAt` INTEGER)"
                )
            }
        }

        /** v12 -> v13: add the medications table + a phone column on care_providers. Additive. */
        private val MIGRATION_12_13 = object : Migration(12, 13) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `medications` " +
                        "(`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`name` TEXT NOT NULL, `dose` TEXT NOT NULL DEFAULT '', " +
                        "`amount` TEXT NOT NULL DEFAULT '', `schedule` TEXT NOT NULL DEFAULT '', " +
                        "`prescriberId` INTEGER, `asNeeded` INTEGER NOT NULL DEFAULT 0, " +
                        "`active` INTEGER NOT NULL DEFAULT 1, `notes` TEXT NOT NULL DEFAULT '')"
                )
                db.execSQL(
                    "ALTER TABLE `care_providers` ADD COLUMN `phone` TEXT NOT NULL DEFAULT ''"
                )
            }
        }

        /** v13 -> v14: add latency/wakings/morning-feeling columns to sleep_logs. Additive. */
        private val MIGRATION_13_14 = object : Migration(13, 14) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE `sleep_logs` ADD COLUMN `latencyMinutes` INTEGER NOT NULL DEFAULT 0"
                )
                db.execSQL(
                    "ALTER TABLE `sleep_logs` ADD COLUMN `nightWakings` INTEGER NOT NULL DEFAULT 0"
                )
                db.execSQL(
                    "ALTER TABLE `sleep_logs` ADD COLUMN `morningFeeling` TEXT NOT NULL DEFAULT ''"
                )
            }
        }

        /** v14 -> v15: add the prompt_events table (missed-prompt reasons). Additive. */
        private val MIGRATION_14_15 = object : Migration(14, 15) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `prompt_events` " +
                        "(`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`firedAt` INTEGER NOT NULL, `answered` INTEGER NOT NULL, " +
                        "`skipReason` TEXT NOT NULL, `reasonSetAt` INTEGER)"
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_prompt_events_firedAt` " +
                        "ON `prompt_events` (`firedAt`)"
                )
            }
        }

        /** v15 -> v16: add the nutrition_log table (Mark's own logged food, snapshotted
         *  values - see NutritionLogEntry.kt). Additive only. */
        private val MIGRATION_15_16 = object : Migration(15, 16) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `nutrition_log` " +
                        "(`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`timestamp` INTEGER NOT NULL, `foodItemId` INTEGER, " +
                        "`name` TEXT NOT NULL, `servings` REAL NOT NULL, " +
                        "`gramsPerServing` REAL NOT NULL, `calories` REAL NOT NULL, " +
                        "`protein` REAL, `fat` REAL, `carbs` REAL)"
                )
            }
        }

        /** v16 -> v17: mental_health_checkins table (PHQ-9/GAD-7), and structured
         *  reminder fields on medications (the old `schedule` column is free text,
         *  not something an alarm can read). Additive only. */
        private val MIGRATION_16_17 = object : Migration(16, 17) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `mental_health_checkins` " +
                        "(`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`type` TEXT NOT NULL, `timestamp` INTEGER NOT NULL, " +
                        "`answers` TEXT NOT NULL, `totalScore` INTEGER NOT NULL)"
                )
                db.execSQL(
                    "ALTER TABLE `medications` ADD COLUMN `reminderEnabled` INTEGER NOT NULL DEFAULT 0"
                )
                db.execSQL(
                    "ALTER TABLE `medications` ADD COLUMN `reminderMinutes` TEXT NOT NULL DEFAULT ''"
                )
            }
        }

        private val SEED_CALLBACK = object : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                seedKeywords(db)
            }
        }

        @Volatile
        private var instance: MoodDatabase? = null

        fun get(context: Context): MoodDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    MoodDatabase::class.java,
                    "mood.db",
                )
                    .addMigrations(
                        MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6,
                        MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9, MIGRATION_9_10,
                        MIGRATION_10_11, MIGRATION_11_12, MIGRATION_12_13, MIGRATION_13_14,
                        MIGRATION_14_15, MIGRATION_15_16, MIGRATION_16_17,
                    )
                    .addCallback(SEED_CALLBACK)
                    .build()
                    .also { instance = it }
            }
    }
}
