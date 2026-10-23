package dev.linkinxp.termlauncher.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private const val RAIN_MS = 6_000L
private const val GLYPHS = "01{}<>/\\|=+*#\$%&:;"

/** Full-screen matrix rain for 6 s in the theme's foreground color; tap to leave. Skipped under reduce motion. */
@Composable
fun MatrixRain(onDone: () -> Unit) {
    val theme = LocalTheme.current
    val reduce = rememberReduceMotion()
    var elapsed by remember { mutableLongStateOf(0L) }
    LaunchedEffect(reduce) {
        if (reduce) { onDone(); return@LaunchedEffect }
        val start = withFrameNanosCompat()
        while (elapsed < RAIN_MS) {
            elapsed = (withFrameNanosCompat() - start) / 1_000_000L
        }
        onDone()
    }
    val measurer = rememberTextMeasurer()
    val color = theme.foreground.color()
    val style = TextStyle(fontFamily = TermFont, fontSize = 14.sp)
    Canvas(
        Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.93f))
            .pointerInput(Unit) { detectTapGestures { onDone() } },
    ) {
        val cell = 16.dp.toPx()
        val cols = (size.width / cell).toInt()
        val rows = (size.height / cell).toInt() + 1
        val tick = elapsed / 90
        for (c in 0 until cols) {
            val speed = 1 + (c * 7 % 3)
            val head = ((tick * speed + c * 13) % (rows + 12)).toInt()
            for (trail in 0 until 12) {
                val row = head - trail
                if (row !in 0 until rows) continue
                val ch = GLYPHS[((c * 31 + row * 17 + tick / 3) % GLYPHS.length).toInt().coerceAtLeast(0)].toString()
                val alpha = if (trail == 0) 1f else (1f - trail / 12f) * 0.8f
                drawText(measurer, ch, Offset(c * cell, row * cell), style.copy(color = color.copy(alpha = alpha)))
            }
        }
    }
}

private suspend fun withFrameNanosCompat(): Long = androidx.compose.runtime.withFrameNanos { it }
