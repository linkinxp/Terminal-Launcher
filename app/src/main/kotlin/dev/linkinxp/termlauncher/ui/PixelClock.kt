package dev.linkinxp.termlauncher.ui

import android.graphics.BlurMaskFilter
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.unit.dp
import java.time.LocalTime

// 3x5 bitmap digits, one string of 5 rows x 3 columns per digit.
private val DIGITS = listOf(
    "111101101101111", "010110010010111", "111001111100111", "111001111001111", "101101111001001",
    "111100111001111", "111100111101111", "111001001001001", "111101111101111", "111101111001111",
)

private const val GLITCH_MS = 400

/**
 * Large blocky HH MM clock drawn from a bitmap font; glows when the theme asks for it. With [glitch],
 * a minute change scrambles the digits for ~400 ms (locking in left to right) with a red/cyan split.
 */
@Composable
fun PixelClock(time: LocalTime, modifier: Modifier = Modifier, glitch: Boolean = false) {
    val theme = LocalTheme.current
    val reduce = rememberReduceMotion()
    val real = clockDigits(time.hour, time.minute)
    val progress = remember { Animatable(1f) }
    val lastMinute = remember { mutableIntStateOf(time.minute) }
    LaunchedEffect(time.hour, time.minute) {
        if (lastMinute.intValue != time.minute) {
            lastMinute.intValue = time.minute
            if (glitch && !reduce) {
                progress.snapTo(0f)
                progress.animateTo(1f, tween(GLITCH_MS, easing = LinearEasing))
            }
        }
    }
    val p = progress.value
    val shown = if (p >= 1f) real else glitchFrame(real, p, time.hour * 60 + time.minute + (p * 14).toInt())
    // Own render layer: the glow (a blur) is recorded once per change, not every time something else on the page redraws.
    Canvas(modifier.fillMaxWidth().height(96.dp).graphicsLayer()) {
        // 4 digits of 3 cells, 1-cell gaps between digits, 2-cell gap between the pairs.
        val totalCells = 4 * 3 + 2 + 2
        val cell = minOf(size.width * 0.72f / totalCells, size.height / 5)
        val left = (size.width - cell * totalCells) / 2
        val top = (size.height - cell * 5) / 2
        val color = theme.foreground.color()

        fun cells(digits: List<Int>, dx: Float): List<Offset> = digits.flatMapIndexed { i, d ->
            val x0 = left + dx + cell * (i * 4 + if (i >= 2) 1 else 0)
            DIGITS[d].mapIndexedNotNull { bit, ch ->
                if (ch == '1') Offset(x0 + (bit % 3) * cell, top + (bit / 3) * cell) else null
            }
        }
        fun DrawScope.fill(on: List<Offset>, c: Color) = on.forEach { drawRect(c, it, Size(cell + 0.5f, cell + 0.5f)) }

        val on = cells(shown, 0f)
        if (theme.glow) {
            // Soft halo: the lit cells re-drawn through a blur mask filter underneath the crisp pixels.
            drawIntoCanvas { canvas ->
                val glow = Paint().apply {
                    this.color = color.copy(alpha = 0.55f)
                    asFrameworkPaint().maskFilter = BlurMaskFilter(cell * 0.7f, BlurMaskFilter.Blur.NORMAL)
                }
                on.forEach { o -> canvas.drawRect(o.x, o.y, o.x + cell, o.y + cell, glow) }
            }
        }
        if (p < 1f) {
            // Channel split while glitching.
            val shift = cell * 0.25f
            fill(cells(shown, -shift), Color(0xFFFF2B2B).copy(alpha = 0.65f))
            fill(cells(shown, shift), Color(0xFF00E5FF).copy(alpha = 0.65f))
        }
        fill(on, color)
    }
}

/** The four clock digits as 0-9, computed arithmetically so a non-Latin-digit locale cannot break them. */
fun clockDigits(hour: Int, minute: Int): List<Int> = listOf(hour / 10, hour % 10, minute / 10, minute % 10)
