package com.mark.moodlogger

import androidx.compose.ui.graphics.Color

/** The 1..5 mood scale shared by the UI, the CSV export, and notifications. */
object MoodScale {

    val scores = 1..5

    val labels = listOf("Awful", "Low", "OK", "Good", "Great")
    val emojis = listOf("😣", "🙁", "😐", "🙂", "😄")

    /**
     * A "honeycomb" ramp: a cold, dead grey at the bottom that warms and
     * brightens step by step into rich honey gold at the top. No reds.
     */
    private val palette = listOf(
        Color(0xFF6E6C66), // Awful  - dim, near-neutral grey
        Color(0xFF9C9068), // Low    - grey warming to khaki
        Color(0xFFCBA544), // OK     - dull gold
        Color(0xFFEEAD26), // Good   - honey amber
        Color(0xFFF88F08), // Great  - deep, saturated honey gold
    )

    fun label(score: Int): String = labels.getOrElse(score - 1) { "?" }
    fun emoji(score: Int): String = emojis.getOrElse(score - 1) { "" }
    fun color(score: Int): Color = palette.getOrElse(score - 1) { Color(0xFF6E6C66) }

    /** Readable text/number colour to sit on top of a mood colour. */
    fun ink(score: Int): Color =
        if (score <= 2) Color(0xFFF2ECE0) else Color(0xFF241A03)

    fun inkForAvg(avg: Float): Color =
        if (avg < 2.5f) Color(0xFFF2ECE0) else Color(0xFF241A03)

    /** Blend the ramp for a fractional average (e.g. 3.4). */
    fun colorForAvg(avg: Float): Color {
        val clamped = avg.coerceIn(1f, 5f)
        val lo = clamped.toInt().coerceIn(1, 5)
        val hi = (lo + 1).coerceAtMost(5)
        val t = clamped - lo
        val a = palette[lo - 1]
        val b = palette[hi - 1]
        return Color(
            red = a.red + (b.red - a.red) * t,
            green = a.green + (b.green - a.green) * t,
            blue = a.blue + (b.blue - a.blue) * t,
        )
    }
}
