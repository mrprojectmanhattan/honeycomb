package com.mark.moodlogger.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
abstract class MoodDao {

    // ---- entries ---------------------------------------------------------

    @Insert
    abstract suspend fun insert(entry: MoodEntry): Long

    @Update
    abstract suspend fun update(entry: MoodEntry)

    @Query("DELETE FROM mood_entries WHERE id = :id")
    abstract suspend fun deleteEntry(id: Long)

    @Transaction
    @Query("SELECT * FROM mood_entries ORDER BY timestamp DESC")
    abstract fun observeAllWithKeywords(): Flow<List<EntryWithKeywords>>

    @Transaction
    @Query("SELECT * FROM mood_entries ORDER BY timestamp ASC")
    abstract suspend fun getAllWithKeywordsChronological(): List<EntryWithKeywords>

    // ---- keywords -------------------------------------------------------

    @Query("SELECT * FROM keywords ORDER BY name COLLATE NOCASE ASC")
    abstract fun observeKeywords(): Flow<List<Keyword>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    abstract suspend fun insertKeyword(keyword: Keyword): Long

    @Update
    abstract suspend fun updateKeyword(keyword: Keyword)

    @Query("DELETE FROM keywords WHERE id = :id")
    abstract suspend fun deleteKeyword(id: Long)

    // ---- join table ----------------------------------------------------

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    abstract suspend fun addEntryKeyword(row: EntryKeyword)

    @Query("DELETE FROM entry_keywords WHERE entryId = :entryId")
    abstract suspend fun clearEntryKeywords(entryId: Long)

    // ---- combined operations -----------------------------------------

    @Transaction
    open suspend fun insertWithKeywords(entry: MoodEntry, keywordIds: Set<Long>): Long {
        val id = insert(entry)
        keywordIds.forEach { addEntryKeyword(EntryKeyword(id, it)) }
        return id
    }

    @Transaction
    open suspend fun updateWithKeywords(entry: MoodEntry, keywordIds: Set<Long>) {
        update(entry)
        clearEntryKeywords(entry.id)
        keywordIds.forEach { addEntryKeyword(EntryKeyword(entry.id, it)) }
    }

    // ---- habits --------------------------------------------------------

    @Query("SELECT * FROM habits ORDER BY startEpochDay ASC")
    abstract fun observeHabits(): Flow<List<Habit>>

    @Insert
    abstract suspend fun insertHabit(habit: Habit): Long

    @Update
    abstract suspend fun updateHabit(habit: Habit)

    @Query("DELETE FROM habits WHERE id = :id")
    abstract suspend fun deleteHabit(id: Long)

    // ---- water --------------------------------------------------------

    @Query("SELECT * FROM water_logs ORDER BY timestamp DESC")
    abstract fun observeWaterLogs(): Flow<List<WaterLog>>

    @Insert
    abstract suspend fun insertWater(log: WaterLog): Long

    @Query("DELETE FROM water_logs WHERE id = :id")
    abstract suspend fun deleteWater(id: Long)

    @Query("SELECT * FROM water_logs WHERE timestamp >= :sinceMillis ORDER BY timestamp DESC LIMIT 1")
    abstract suspend fun latestWaterSince(sinceMillis: Long): WaterLog?

    // ---- workouts ----------------------------------------------------

    @Query("SELECT * FROM workout_logs ORDER BY timestamp DESC")
    abstract fun observeWorkoutLogs(): Flow<List<WorkoutLog>>

    @Insert
    abstract suspend fun insertWorkout(log: WorkoutLog): Long

    @Query("DELETE FROM workout_logs WHERE region = :region AND timestamp >= :sinceMillis")
    abstract suspend fun deleteWorkoutRegionSince(region: String, sinceMillis: Long)

    // ---- sleep ------------------------------------------------------

    @Query("SELECT * FROM sleep_logs ORDER BY dateEpochDay DESC")
    abstract fun observeSleepLogs(): Flow<List<SleepLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun upsertSleep(log: SleepLog)

    @Query("SELECT * FROM sleep_logs WHERE dateEpochDay = :epochDay LIMIT 1")
    abstract suspend fun sleepForDay(epochDay: Long): SleepLog?

    @Query("DELETE FROM sleep_logs WHERE dateEpochDay = :epochDay")
    abstract suspend fun deleteSleepForDay(epochDay: Long)

    // ---- goals -----------------------------------------------------

    @Query("SELECT * FROM goals WHERE archived = 0 ORDER BY createdAt ASC")
    abstract fun observeGoals(): Flow<List<Goal>>

    @Insert
    abstract suspend fun insertGoal(goal: Goal): Long

    @Query("UPDATE goals SET archived = 1 WHERE id = :id")
    abstract suspend fun archiveGoal(id: Long)

    @Query("DELETE FROM goals WHERE id = :id")
    abstract suspend fun deleteGoal(id: Long)

    @Query("SELECT * FROM goal_checkins")
    abstract fun observeCheckins(): Flow<List<GoalCheckin>>

    @Insert
    abstract suspend fun addCheckin(checkin: GoalCheckin)

    @Query(
        "DELETE FROM goal_checkins WHERE id = (" +
            "SELECT id FROM goal_checkins WHERE goalId = :goalId ORDER BY timestamp DESC LIMIT 1)"
    )
    abstract suspend fun undoLatestCheckin(goalId: Long)

    @Query("DELETE FROM goal_checkins WHERE goalId = :goalId")
    abstract suspend fun clearCheckins(goalId: Long)

    // ---- steps -----------------------------------------------------

    @Query("SELECT * FROM step_days ORDER BY epochDay DESC")
    abstract fun observeStepDays(): Flow<List<StepDay>>

    @Query("SELECT * FROM step_days WHERE epochDay = :epochDay LIMIT 1")
    abstract suspend fun stepDay(epochDay: Long): StepDay?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun upsertStepDay(day: StepDay)

    // ---- major goals ---------------------------------------------------

    @Query("SELECT * FROM major_goals ORDER BY createdAt DESC")
    abstract fun observeMajorGoals(): Flow<List<MajorGoal>>

    @Insert
    abstract suspend fun insertMajorGoal(goal: MajorGoal): Long

    @Update
    abstract suspend fun updateMajorGoal(goal: MajorGoal)

    @Query("DELETE FROM major_goals WHERE id = :id")
    abstract suspend fun deleteMajorGoal(id: Long)

    // ---- journal -----------------------------------------------------

    @Query("SELECT * FROM journal_entries ORDER BY timestamp DESC")
    abstract fun observeJournal(): Flow<List<JournalEntry>>

    @Query("SELECT * FROM journal_entries WHERE day = :day ORDER BY timestamp ASC")
    abstract suspend fun journalForDay(day: Long): List<JournalEntry>

    @Insert
    abstract suspend fun insertJournal(entry: JournalEntry): Long

    @Update
    abstract suspend fun updateJournal(entry: JournalEntry)

    @Query("DELETE FROM journal_entries WHERE id = :id")
    abstract suspend fun deleteJournal(id: Long)

    // ---- care team ----------------------------------------------------

    @Query("SELECT * FROM care_providers ORDER BY active DESC, kind ASC, name ASC")
    abstract fun observeCareProviders(): Flow<List<CareProvider>>

    @Insert
    abstract suspend fun insertCareProvider(p: CareProvider): Long

    @Update
    abstract suspend fun updateCareProvider(p: CareProvider)

    @Query("DELETE FROM care_providers WHERE id = :id")
    abstract suspend fun deleteCareProvider(id: Long)

    // ---- shopping list -----------------------------------------------

    /** Outstanding items first (purchasedAt IS NULL sorts as 0), then oldest-added first. */
    @Query("SELECT * FROM shopping_items ORDER BY (purchasedAt IS NOT NULL), addedAt ASC")
    abstract fun observeShoppingItems(): Flow<List<ShoppingItem>>

    @Query("SELECT * FROM shopping_items WHERE purchasedAt IS NULL ORDER BY addedAt ASC")
    abstract suspend fun outstandingShoppingItems(): List<ShoppingItem>

    @Insert
    abstract suspend fun insertShoppingItem(item: ShoppingItem): Long

    @Update
    abstract suspend fun updateShoppingItem(item: ShoppingItem)

    @Query("DELETE FROM shopping_items WHERE id = :id")
    abstract suspend fun deleteShoppingItem(id: Long)

    @Query("DELETE FROM shopping_items WHERE purchasedAt IS NOT NULL")
    abstract suspend fun clearPurchasedShoppingItems()

    // ---- medications ------------------------------------------------

    @Query("SELECT * FROM medications ORDER BY active DESC, name COLLATE NOCASE ASC")
    abstract fun observeMedications(): Flow<List<Medication>>

    @Insert
    abstract suspend fun insertMedication(m: Medication): Long

    @Update
    abstract suspend fun updateMedication(m: Medication)

    @Query("DELETE FROM medications WHERE id = :id")
    abstract suspend fun deleteMedication(id: Long)

    @Query("SELECT * FROM medications WHERE active = 1 AND reminderEnabled = 1")
    abstract suspend fun activeMedicationReminders(): List<Medication>

    // ---- prompt events (v3.25) --------------------------------------

    @Insert
    abstract suspend fun insertPromptEvent(event: PromptEvent): Long

    /** Prompts that were neither answered nor explained, oldest first. */
    @Query(
        "SELECT * FROM prompt_events WHERE answered = 0 AND skipReason = '' " +
            "ORDER BY firedAt ASC"
    )
    abstract fun observeOpenPrompts(): Flow<List<PromptEvent>>

    @Query("SELECT * FROM prompt_events ORDER BY firedAt DESC")
    abstract fun observePromptEvents(): Flow<List<PromptEvent>>

    /** Marks the newest unanswered prompt that fired at or after [since]. */
    @Query(
        "UPDATE prompt_events SET answered = 1 WHERE id = (" +
            "SELECT id FROM prompt_events WHERE answered = 0 AND firedAt >= :since " +
            "ORDER BY firedAt DESC LIMIT 1)"
    )
    abstract suspend fun markPromptAnswered(since: Long)

    /** Marks every unanswered prompt that fired in [from]..[to] as answered.
     *  Used for moods that arrive late from the watch, where "the latest prompt"
     *  would be the wrong one. */
    @Query(
        "UPDATE prompt_events SET answered = 1 " +
            "WHERE answered = 0 AND firedAt >= :from AND firedAt <= :to"
    )
    abstract suspend fun markPromptsAnsweredBetween(from: Long, to: Long)

    @Query(
        "UPDATE prompt_events SET skipReason = :reason, reasonSetAt = :atMillis " +
            "WHERE id IN (:ids)"
    )
    abstract suspend fun setPromptReason(ids: List<Long>, reason: String, atMillis: Long)

    // ---- one-time source re-derive (v3.19) --------------------------

    @Query("SELECT id, timestamp FROM mood_entries")
    abstract suspend fun allMoodTimes(): List<MoodIdTime>

    @Query("UPDATE mood_entries SET source = :source WHERE id IN (:ids)")
    abstract suspend fun setSourceForIds(source: String, ids: List<Long>)
}

data class MoodIdTime(val id: Long, val timestamp: Long)
