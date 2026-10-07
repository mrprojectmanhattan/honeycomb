package com.mark.moodlogger.ui

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import com.mark.moodlogger.data.WorkoutLog
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/**
 * A human figure built from hexagons, one region per muscle group. Regions in
 * [done] are filled solid honey with their outline colour; the rest are drawn as
 * a faint outline in that region's colour so you can tell them apart.
 */
@Composable
fun HexFigure(
    done: Set<String>,
    modifier: Modifier = Modifier,
    fill: Color = Color(0xFFE9A319),
) {
    Canvas(modifier) {
        // grid spans columns 1..5, rows 0..7
        val gridCols = 6f
        val gridRows = 8.5f
        val cell = min(size.width / gridCols, size.height / gridRows)
        val r = cell * 0.54f
        val originX = size.width / 2f - 3f * cell
        val originY = (size.height - cell * (gridRows - 0.5f)) / 2f + cell / 2f

        for ((col, rowI, region) in FIGURE_CELLS) {
            val cx = originX + col * cell
            val cy = originY + rowI * cell
            val hex = hexAt(cx, cy, r)
            val outline = REGION_OUTLINE[region] ?: Color(0xFF7A8088)
            if (region in done) {
                drawPath(hex, color = fill)
                drawPath(hex, color = outline, style = Stroke(width = 2.5f))
            } else {
                drawPath(hex, color = outline.copy(alpha = 0.16f))
                drawPath(hex, color = outline.copy(alpha = 0.75f), style = Stroke(width = 2.5f))
            }
        }
    }
}

private val REGION_OUTLINE = mapOf(
    WorkoutLog.SHOULDERS to Color(0xFFF2C94C),
    WorkoutLog.CHEST to Color(0xFFF0953F),
    WorkoutLog.ARMS to Color(0xFFE8B84B),
    WorkoutLog.BACK to Color(0xFFC98A3C),
    WorkoutLog.CORE to Color(0xFFF5D774),
    WorkoutLog.LEGS to Color(0xFFD99E3E),
    "head" to Color(0xFF7A8088),
)

// (column, row, region)
private val FIGURE_CELLS = listOf(
    Triple(3, 0, "head"),
    Triple(2, 1, WorkoutLog.SHOULDERS),
    Triple(3, 1, WorkoutLog.CHEST),
    Triple(4, 1, WorkoutLog.SHOULDERS),
    Triple(1, 2, WorkoutLog.ARMS),
    Triple(2, 2, WorkoutLog.BACK),
    Triple(3, 2, WorkoutLog.CHEST),
    Triple(4, 2, WorkoutLog.BACK),
    Triple(5, 2, WorkoutLog.ARMS),
    Triple(1, 3, WorkoutLog.ARMS),
    Triple(2, 3, WorkoutLog.BACK),
    Triple(3, 3, WorkoutLog.CORE),
    Triple(4, 3, WorkoutLog.BACK),
    Triple(5, 3, WorkoutLog.ARMS),
    Triple(1, 4, WorkoutLog.ARMS),
    Triple(3, 4, WorkoutLog.CORE),
    Triple(5, 4, WorkoutLog.ARMS),
    Triple(2, 5, WorkoutLog.LEGS),
    Triple(4, 5, WorkoutLog.LEGS),
    Triple(2, 6, WorkoutLog.LEGS),
    Triple(4, 6, WorkoutLog.LEGS),
    Triple(2, 7, WorkoutLog.LEGS),
    Triple(4, 7, WorkoutLog.LEGS),
)

private fun hexAt(cx: Float, cy: Float, r: Float): Path = Path().apply {
    for (i in 0..6) {
        val a = Math.toRadians(60.0 * i - 30.0)
        val x = cx + r * cos(a).toFloat()
        val y = cy + r * sin(a).toFloat()
        if (i == 0) moveTo(x, y) else lineTo(x, y)
    }
    close()
}
