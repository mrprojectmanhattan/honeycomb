package com.mark.moodlogger.watch

import androidx.compose.ui.graphics.Color

/** The same 1..5 honeycomb mood scale as the phone: cold grey at the bottom warming to honey gold. */
object MoodScale {
    val labels = listOf("Awful", "Low", "OK", "Good", "Great")
    val emojis = listOf("😣", "🙁", "😐", "🙂", "😄")

    private val palette = listOf(
        Color(0xFF6E6C66), // Awful
        Color(0xFF9C9068), // Low
        Color(0xFFCBA544), // OK
        Color(0xFFEEAD26), // Good
        Color(0xFFF88F08), // Great
    )

    fun label(score: Int): String = labels.getOrElse(score - 1) { "?" }
    fun emoji(score: Int): String = emojis.getOrElse(score - 1) { "" }
    fun color(score: Int): Color = palette.getOrElse(score - 1) { Color(0xFF6E6C66) }
    fun ink(score: Int): Color = if (score <= 2) Color(0xFFF2ECE0) else Color(0xFF241A03)
}
