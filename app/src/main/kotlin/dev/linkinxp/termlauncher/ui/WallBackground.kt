package dev.linkinxp.termlauncher.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.linkinxp.termlauncher.theme.Theme
import dev.linkinxp.termlauncher.wall.WallMode
import dev.linkinxp.termlauncher.wall.WallSettings
import dev.linkinxp.termlauncher.wall.driftLines
import dev.linkinxp.termlauncher.wall.generateAscii
import kotlinx.coroutines.delay

private const val ART_SP = 11f
private const val ART_LINE_DP = 13f
private const val MONO_ADVANCE = 0.6f // JetBrains Mono advance width in em
private const val DRIFT_MS = 250L

/**
 * The launcher background: opaque theme color (SOLID), a theme-colored dim layer over the transparent
 * window so the system wallpaper shows through (SYSTEM), or generated ASCII art (ASCII).
 */
@Composable
fun WallBackground(wall: WallSettings, theme: Theme, reduceMotion: Boolean, modifier: Modifier = Modifier) {
    val bg = theme.background.color()
    when (wall.mode) {
        WallMode.SOLID -> Box(modifier.fillMaxSize().background(bg))
        WallMode.SYSTEM -> Box(modifier.fillMaxSize().background(bg.copy(alpha = wall.dim.coerceIn(0, 100) / 100f)))
        WallMode.ASCII -> AsciiBackground(wall, theme, reduceMotion, modifier)
    }
}

@Composable
private fun AsciiBackground(wall: WallSettings, theme: Theme, reduceMotion: Boolean, modifier: Modifier) {
    val density = LocalDensity.current
    val measurer = rememberTextMeasurer()
    val drifting = wall.animate && !reduceMotion
    var step by remember { mutableIntStateOf(0) }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(drifting, lifecycle) {
        step = 0
        // Only tick while the launcher is resumed: no wakeups while another app is in front.
        if (drifting) lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) { while (true) { delay(DRIFT_MS); step++ } }
    }
    BoxWithConstraints(modifier.fillMaxSize().background(theme.background.color())) {
        val cellW = with(density) { ART_SP.sp.toPx() * MONO_ADVANCE }
        val cellH = with(density) { ART_LINE_DP.dp.toPx() }
        val cols = (constraints.maxWidth / cellW).toInt()
        val rows = (constraints.maxHeight / cellH).toInt()
        if (cols <= 0 || rows <= 0) return@BoxWithConstraints
        val lines = remember(wall.style, wall.seed, cols, rows) { generateAscii(wall.style, cols, rows, wall.seed) }
        val text = remember(lines, step, drifting) { (if (drifting) driftLines(lines, step) else lines).joinToString("\n") }
        val style = TextStyle(
            fontFamily = TermFont,
            fontSize = ART_SP.sp,
            lineHeight = with(density) { ART_LINE_DP.dp.toSp() },
            color = theme.dim.color().copy(alpha = 0.35f),
        )
        val layout = remember(text, style, measurer) {
            measurer.measure(text, style, overflow = TextOverflow.Clip, softWrap = false)
        }
        Canvas(Modifier.fillMaxSize()) { drawText(layout) }
    }
}
