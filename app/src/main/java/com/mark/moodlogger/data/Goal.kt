package com.mark.moodlogger.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * A goal: "do X, [targetCount] times per [periodDays]-day window."
 * - auto=false: progress from manual check-ins ([GoalCheckin]).
 * - auto=true: progress computed from another table ([source]). A day "counts"
 *   when: water total >= water goal / sleep hours >= sleep goal / any workout
 *   logged / steps >= [dailyThreshold] / any mood logged.
 */
@Entity(tableName = "goals")
data class Goal(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val auto: Boolean,
    val source: String = "",
    val targetCount: Int,
    val periodDays: Int,
    val dailyThreshold: Int = 0,
    val createdAt: Long,
    val archived: Boolean = false,
) {
    companion object {
        const val SRC_WATER = "water"
        const val SRC_SLEEP = "sleep"
        const val SRC_WORKOUT = "workout"
        const val SRC_STEPS = "steps"
        const val SRC_MOOD = "mood"
    }
}

@Entity(tableName = "goal_checkins", indices = [Index(value = ["goalId"])])
data class GoalCheckin(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val goalId: Long,
    val timestamp: Long,
)

/** One preset goal offered when adding a goal. */
data class GoalPreset(
    val title: String,
    val auto: Boolean,
    val source: String = "",
    val targetCount: Int,
    val periodDays: Int,
    val dailyThreshold: Int = 0,
) {
    companion object {
        val ALL: List<GoalPreset> = listOf(
            GoalPreset("Shower 3x this week", auto = false, targetCount = 3, periodDays = 7),
            GoalPreset("Brush teeth twice a day", auto = false, targetCount = 2, periodDays = 1),
            GoalPreset("Eat a real meal twice a day", auto = false, targetCount = 2, periodDays = 1),
            GoalPreset("Tidy up once a day", auto = false, targetCount = 1, periodDays = 1),
            GoalPreset("Step outside once a day", auto = false, targetCount = 1, periodDays = 1),
            GoalPreset(
                "Log my mood every day", auto = true, source = Goal.SRC_MOOD,
                targetCount = 7, periodDays = 7,
            ),
            GoalPreset(
                "Work out 3x this week", auto = true, source = Goal.SRC_WORKOUT,
                targetCount = 3, periodDays = 7,
            ),
            GoalPreset(
                "Hit my water goal 5 days this week", auto = true, source = Goal.SRC_WATER,
                targetCount = 5, periodDays = 7,
            ),
            GoalPreset(
                "Sleep enough 4 nights this week", auto = true, source = Goal.SRC_SLEEP,
                targetCount = 4, periodDays = 7,
            ),
            GoalPreset(
                "5,000 steps, 4 days this week", auto = true, source = Goal.SRC_STEPS,
                targetCount = 4, periodDays = 7, dailyThreshold = 5000,
            ),
        )
    }
}
