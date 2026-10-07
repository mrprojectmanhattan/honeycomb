package com.mark.moodlogger.ui

import androidx.compose.foundation.background
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import kotlin.math.min
import kotlin.math.sqrt

/**
 * A flat-top regular hexagon that fills the given size. Wide shapes get pointy
 * left/right ends; square shapes get a proper hexagon.
 */
object HexagonShape : Shape {
    override fun createOutline(
        size: androidx.compose.ui.geometry.Size,
        layoutDirection: LayoutDirection,
        density: Density,
    ): Outline {
        val w = size.width
        val h = size.height
        // corner inset from the vertical edges = a quarter of the height for a
        // regular hex, but clamp so wide buttons still look hex-ish, not arrow-y.
        val inset = min(h / 2f, w / 4f)
        val path = Path().apply {
            moveTo(inset, 0f)
            lineTo(w - inset, 0f)
            lineTo(w, h / 2f)
            lineTo(w - inset, h)
            lineTo(inset, h)
            lineTo(0f, h / 2f)
            close()
        }
        return Outline.Generic(path)
    }
}

/** A faint tiled honeycomb pattern drawn behind content. */
fun Modifier.honeycombBackground(
    base: Color,
    line: Color = Color(0x14C8962B),
    cell: Float = 46f,
): Modifier = this
    .background(base)
    .drawBehind {
        val r = cell
        val hStep = r * 1.5f
        val vStep = r * sqrt(3f)
        val cols = (size.width / hStep).toInt() + 2
        val rows = (size.height / vStep).toInt() + 2
        for (c in -1..cols) {
            for (rw in -1..rows) {
                val cx = c * hStep
                val cy = rw * vStep + if (c % 2 == 0) 0f else vStep / 2f
                val p = Path()
                for (i in 0..6) {
                    val ang = Math.toRadians((60 * i).toDouble())
                    val x = cx + r * kotlin.math.cos(ang).toFloat()
                    val y = cy + r * kotlin.math.sin(ang).toFloat()
                    if (i == 0) p.moveTo(x, y) else p.lineTo(x, y)
                }
                drawPath(p, color = line, style = Stroke(width = 1.2f))
            }
        }
    }
