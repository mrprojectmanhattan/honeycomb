package com.mark.moodlogger.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/** One row per hourly reminder that actually fired a notification.
 *
 *  A missed prompt and a day with no data used to look identical. This makes the
 *  difference recordable: an unanswered prompt can carry *why* it went
 *  unanswered, so structural gaps (a shift where phones aren't allowed) can be
 *  told apart from avoidance, which is itself worth seeing.
 */
@Entity(tableName = "prompt_events", indices = [Index("firedAt")])
data class PromptEvent(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    /** epoch millis the reminder fired */
    val firedAt: Long,
    /** a mood was logged in response to this prompt */
    val answered: Boolean = false,
    /** one of the REASON_* values, blank until it's been answered or explained */
    val skipReason: String = "",
    /** epoch millis the reason was recorded, null if never */
    val reasonSetAt: Long? = null,
) {
    companion object {
        const val REASON_AT_WORK = "at_work"
        const val REASON_DRIVING = "driving"
        const val REASON_ASLEEP = "asleep"
        const val REASON_BUSY = "busy"
        const val REASON_FORGOT = "forgot"
        const val REASON_DIDNT_WANT = "didnt_want_to"

        /** value to label, in the order the chips are shown */
        val REASONS: List<Pair<String, String>> = listOf(
            REASON_AT_WORK to "At work",
            REASON_DRIVING to "Driving",
            REASON_ASLEEP to "Asleep",
            REASON_BUSY to "Busy",
            REASON_FORGOT to "Forgot",
            REASON_DIDNT_WANT to "Didn't want to",
        )

        /** Reasons where not logging was out of his hands, so they shouldn't
         *  count against adherence the way an avoided prompt does. */
        val STRUCTURAL_REASONS = setOf(REASON_AT_WORK, REASON_DRIVING, REASON_ASLEEP)

        fun label(reason: String): String =
            REASONS.firstOrNull { it.first == reason }?.second ?: reason

        /** Consecutive prompts closer together than this belong to the same run,
         *  so a whole shift can be explained in one tap. */
        const val GROUP_GAP_MS = 90 * 60 * 1000L
    }
}
