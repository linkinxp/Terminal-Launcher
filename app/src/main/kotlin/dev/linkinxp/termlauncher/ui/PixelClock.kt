package dev.linkinxp.termlauncher.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.dp
import java.time.LocalTime

// 3x5 bitmap digits, one string of 5 rows x 3 columns per digit.
private val DIGITS = listOf(
    "111101101101111", "010110010010111", "111001111100111", "111001111001111", "101101111001001",
    "111100111001111", "111100111101111", "111001001001001", "111101111101111", "111101111001111",
)

/** Large blocky HH MM clock drawn from a bitmap font; glows when the theme asks for it. */
@Composable
fun PixelClock(time: LocalTime, modifier: Modifier = Modifier) {
    val theme = LocalTheme.current
    val digits = clockDigits(time.hour, time.minute)
    Canvas(modifier.fillMaxWidth().height(120.dp)) {
        // 4 digits of 3 cells, 1-cell gaps between digits, 2-cell gap between the pairs.
        val totalCells = 4 * 3 + 2 + 2
        val cell = minOf(size.width / totalCells, size.height / 5)
        val left = (size.width - cell * totalCells) / 2
        val top = (size.height - cell * 5) / 2
        val color = theme.foreground.color()
        digits.forEachIndexed { i, d ->
            val x0 = left + cell * (i * 4 + if (i >= 2) 1 else 0)
            DIGITS[d].forEachIndexed { bit, ch ->
                if (ch == '1') {
                    val o = Offset(x0 + (bit % 3) * cell, top + (bit / 3) * cell)
                    if (theme.glow) drawRect(color.copy(alpha = 0.18f), o - Offset(cell * 0.12f, cell * 0.12f), Size(cell * 1.24f, cell * 1.24f))
                    drawRect(color, o, Size(cell * 0.96f, cell * 0.96f))
                }
            }
        }
    }
}

/** The four clock digits as 0-9, computed arithmetically so a non-Latin-digit locale cannot break them. */
fun clockDigits(hour: Int, minute: Int): List<Int> = listOf(hour / 10, hour % 10, minute / 10, minute % 10)
