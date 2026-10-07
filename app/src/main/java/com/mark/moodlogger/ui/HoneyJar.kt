package com.mark.moodlogger.ui

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * A honey jar drawn as a grid of hexagons. Cells below the fill line are solid
 * honey; cells above are faint outlines. [fraction] is 0f..1f (today's water
 * intake / the daily goal). Keeps the app's honeycomb theme even though what it
 * tracks is water.
 */
@Composable
fun HoneyJar(
    fraction: Float,
    modifier: Modifier = Modifier,
    honey: Color = Color(0xFFE0A12A),
    honeyDim: Color = Color(0x2EE0A12A),
    glass: Color = Color(0xFF8A6D3B),
    lid: Color = Color(0xFFB5893A),
) {
    val f = fraction.coerceIn(0f, 1f)
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val cx = w / 2f
        val lidHalf = w * 0.36f
        val neckHalf = w * 0.30f
        val bodyHalf = w * 0.47f
        val lidTop = h * 0.02f
        val lidBot = h * 0.14f
        val neckBot = h * 0.21f
        val bodyTop = h * 0.30f
        val rad = w * 0.13f

        val jar = Path().apply {
            moveTo(cx - neckHalf, lidBot)
            lineTo(cx + neckHalf, lidBot)
            lineTo(cx + neckHalf, neckBot)
            quadraticTo(cx + bodyHalf, neckBot, cx + bodyHalf, bodyTop)
            lineTo(cx + bodyHalf, h - rad)
            quadraticTo(cx + bodyHalf, h, cx + bodyHalf - rad, h)
            lineTo(cx - bodyHalf + rad, h)
            quadraticTo(cx - bodyHalf, h, cx - bodyHalf, h - rad)
            lineTo(cx - bodyHalf, bodyTop)
            quadraticTo(cx - bodyHalf, neckBot, cx - neckHalf, neckBot)
            close()
        }

        val fillLineY = h * (1f - f)

        clipPath(jar) {
            val r = w * 0.13f
            val hStep = r * 1.5f
            val vStep = r * sqrt(3f)
            var col = 0
            var x = 0f
            while (x <= w + hStep) {
                var y = 0f
                while (y <= h + vStep) {
                    val ccy = y + if (col % 2 == 0) 0f else vStep / 2f
                    val cell = hexPath(x, ccy, r * 0.86f)
                    if (ccy >= fillLineY) {
                        drawPath(cell, color = honey)
                    } else {
                        drawPath(cell, color = honeyDim, style = Stroke(width = 1.5f))
                    }
                    y += vStep
                }
                x += hStep
                col++
            }
            if (f > 0.001f && f < 0.999f) {
                drawLine(
                    color = honey,
                    start = Offset(0f, fillLineY),
                    end = Offset(w, fillLineY),
                    strokeWidth = 3f,
                )
            }
        }

        drawPath(jar, color = glass, style = Stroke(width = w * 0.045f))

        // lid
        val lidPath = Path().apply {
            addRoundRect(
                RoundRect(
                    left = cx - lidHalf,
                    top = lidTop,
                    right = cx + lidHalf,
                    bottom = lidBot + h * 0.015f,
                    radiusX = w * 0.06f,
                    radiusY = w * 0.06f,
                )
            )
        }
        drawPath(lidPath, color = lid)
        drawPath(lidPath, color = glass, style = Stroke(width = w * 0.04f))
    }
}

private fun hexPath(cx: Float, cy: Float, r: Float): Path = Path().apply {
    for (i in 0..6) {
        val ang = Math.toRadians(60.0 * i)
        val px = cx + r * cos(ang).toFloat()
        val py = cy + r * sin(ang).toFloat()
        if (i == 0) moveTo(px, py) else lineTo(px, py)
    }
    close()
}
