package com.mark.moodlogger.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * One completed PHQ-9 or GAD-7 screening. [answers] is a comma-separated list of
 * per-question scores (0-3 each, standard scale for both instruments), stored as a
 * snapshot string rather than a child table - a completed screening is a point-in-time
 * record, never edited after the fact, same spirit as NutritionLogEntry's snapshot.
 * Added 2026-09-28.
 */
@Entity(tableName = "mental_health_checkins")
data class MentalHealthCheckin(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: String,
    val timestamp: Long,
    val answers: String,
    val totalScore: Int,
) {
    fun answerList(): List<Int> = answers.split(",").mapNotNull { it.trim().toIntOrNull() }
}

@Dao
interface MentalHealthCheckinDao {
    @Insert
    suspend fun insert(checkin: MentalHealthCheckin): Long

    @Delete
    suspend fun delete(checkin: MentalHealthCheckin)

    @Query("SELECT * FROM mental_health_checkins ORDER BY timestamp DESC")
    fun observeAll(): Flow<List<MentalHealthCheckin>>

    @Query("SELECT * FROM mental_health_checkins WHERE type = :type ORDER BY timestamp DESC LIMIT 1")
    suspend fun latest(type: String): MentalHealthCheckin?
}

/**
 * The real, standard, public-domain PHQ-9 and GAD-7 instruments - word for word, not
 * paraphrased, since a screening tool's validity depends on the exact wording. Both use
 * the same 0-3 "over the last 2 weeks" frequency scale. Severity bands are the published
 * clinical cutoffs for each.
 */
object Screenings {
    const val TYPE_PHQ9 = "PHQ9"
    const val TYPE_GAD7 = "GAD7"

    const val PHQ9_TITLE = "PHQ-9"
    const val GAD7_TITLE = "GAD-7"

    const val INTRO = "Over the last 2 weeks, how often have you been bothered by any of the following problems?"

    val ANSWER_LABELS = listOf("Not at all", "Several days", "More than half the days", "Nearly every day")

    val PHQ9_QUESTIONS = listOf(
        "Little interest or pleasure in doing things",
        "Feeling down, depressed, or hopeless",
        "Trouble falling or staying asleep, or sleeping too much",
        "Feeling tired or having little energy",
        "Poor appetite or overeating",
        "Feeling bad about yourself - or that you are a failure or have let yourself or your family down",
        "Trouble concentrating on things, such as reading the newspaper or watching television",
        "Moving or speaking so slowly that other people could have noticed? Or the opposite - being so fidgety or restless that you have been moving around a lot more than usual",
        "Thoughts that you would be better off dead, or of hurting yourself in some way",
    )

    /** Index of the self-harm question - if this is answered above "Not at all", the
     *  check-in screen shows a crisis-line resource alongside the score. */
    const val PHQ9_SELF_HARM_INDEX = 8

    val GAD7_QUESTIONS = listOf(
        "Feeling nervous, anxious, or on edge",
        "Not being able to stop or control worrying",
        "Worrying too much about different things",
        "Trouble relaxing",
        "Being so restless that it is hard to sit still",
        "Becoming easily annoyed or irritable",
        "Feeling afraid, as if something awful might happen",
    )

    fun questionsFor(type: String): List<String> = if (type == TYPE_PHQ9) PHQ9_QUESTIONS else GAD7_QUESTIONS
    fun titleFor(type: String): String = if (type == TYPE_PHQ9) PHQ9_TITLE else GAD7_TITLE

    /** Published clinical severity cutoffs. PHQ-9 max 27, GAD-7 max 21. */
    fun severity(type: String, score: Int): String = if (type == TYPE_PHQ9) {
        when {
            score <= 4 -> "Minimal"
            score <= 9 -> "Mild"
            score <= 14 -> "Moderate"
            score <= 19 -> "Moderately severe"
            else -> "Severe"
        }
    } else {
        when {
            score <= 4 -> "Minimal"
            score <= 9 -> "Mild"
            score <= 14 -> "Moderate"
            else -> "Severe"
        }
    }
}
