package dev.linkinxp.termlauncher.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import dev.linkinxp.termlauncher.matrix.RAIN_GLYPHS
import dev.linkinxp.termlauncher.matrix.RAIN_TRAIL
import dev.linkinxp.termlauncher.matrix.rainCells
import dev.linkinxp.termlauncher.theme.Theme
import dev.linkinxp.termlauncher.wall.WallSettings
import kotlinx.coroutines.delay

private const val RAIN_TICK_MS = 100L
private const val STILL_TICK = 40L // the one frame shown when animations are off

/**
 * Falling code behind everything, in the theme's foreground color with a bright head on each streak. It only moves while
 * the launcher is resumed, as one cheap layer; with reduced motion it is a single still frame. `dim` sets how strong it is.
 */
@Composable
fun RainBackground(wall: WallSettings, theme: Theme, reduceMotion: Boolean, modifier: Modifier = Modifier) {
    val measurer = rememberTextMeasurer()
    var tick by remember { mutableLongStateOf(STILL_TICK) }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(reduceMotion, lifecycle) {
        if (!reduceMotion) lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            while (true) { delay(RAIN_TICK_MS); tick++ }
        }
    }
    val style = remember { TextStyle(fontFamily = TermFont, fontSize = 13.sp) }
    // Each glyph is laid out once; frames only draw the cached layouts.
    val glyphs = remember(measurer) { RAIN_GLYPHS.map { measurer.measure(it.toString(), style) } }
    val color = theme.foreground.color()
    val head = lerp(color, Color.White, 0.75f)
    val strength = ((100 - wall.dim.coerceIn(0, 100)) / 100f).coerceAtLeast(0.15f)
    Canvas(modifier.fillMaxSize().background(theme.background.color()).graphicsLayer()) {
        val cell = 15.dp.toPx()
        val cols = (size.width / cell).toInt()
        val rows = (size.height / cell).toInt() + 1
        rainCells(cols, rows, tick, wall.seed).forEach { c ->
            val alpha = (if (c.level == 0) 1f else (1f - c.level / RAIN_TRAIL.toFloat()) * 0.8f) * strength
            drawText(glyphs[c.glyph], color = if (c.level == 0) head else color, topLeft = Offset(c.col * cell, c.row * cell), alpha = alpha)
        }
    }
}
