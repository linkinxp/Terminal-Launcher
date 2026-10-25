package dev.linkinxp.termlauncher.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import dev.linkinxp.termlauncher.fx.FxSettings
import dev.linkinxp.termlauncher.fx.GlitchBar
import dev.linkinxp.termlauncher.fx.flickerFlashes
import dev.linkinxp.termlauncher.fx.glitchBars
import dev.linkinxp.termlauncher.fx.nextGlitchDelayMs
import dev.linkinxp.termlauncher.fx.scanBarY
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import dev.linkinxp.termlauncher.theme.Theme
import kotlinx.coroutines.delay
import kotlin.random.Random

/**
 * CRT effects over the whole screen, drawing only (it never intercepts touches): scanlines (dark lines plus a faint
 * line in the theme color, so they show on a black screen too) and a vignette; a soft band that sweeps down the screen
 * every few seconds; now and then a burst of glitch bars; and, when flicker is on, quick dark flashes.
 * Scanlines and vignette live in their own cached layer; only the moving parts redraw (about 20 times a second, and only
 * while the launcher is on screen and animations are allowed).
 */
@Composable
fun EffectsOverlay(settings: FxSettings, theme: Theme, reduceMotion: Boolean, modifier: Modifier = Modifier) {
    val scanlines = settings.scanlinesOn(theme)
    val motion = !reduceMotion
    var flash by remember { mutableFloatStateOf(0f) }
    var bars by remember { mutableStateOf<List<GlitchBar>>(emptyList()) }
    var clockMs by remember { mutableLongStateOf(0L) }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val sweeping = scanlines && motion
    LaunchedEffect(sweeping, settings.flicker, settings.glitch, motion, lifecycle) {
        if (!motion) { flash = 0f; bars = emptyList(); return@LaunchedEffect }
        lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            coroutineScope {
                if (sweeping) launch { val t0 = System.nanoTime(); while (true) { clockMs = (System.nanoTime() - t0) / 1_000_000L; delay(50) } }
                if (settings.flicker) launch {
                    while (true) {
                        delay(Random.nextLong(2_000, 6_000))
                        flickerFlashes(Random.nextInt()).forEach { f -> flash = f.alpha; delay(f.durationMs.toLong()); flash = 0f; delay(45) }
                    }
                }
                if (settings.glitch) launch {
                    while (true) {
                        delay(nextGlitchDelayMs(Random.nextInt()))
                        repeat(3) { bars = glitchBars(Random.nextInt(), 5 + Random.nextInt(5), 1f); delay(70) }
                        bars = emptyList()
                    }
                }
            }
        }
    }
    val fg = theme.foreground.color()
    val accent = theme.accent.color()
    if (scanlines) {
        // Own render layer: the lines and the vignette are recorded once, not on every invalidation underneath.
        Canvas(modifier.fillMaxSize().graphicsLayer()) {
            val step = 3.dp.toPx()
            val line = 1.dp.toPx()
            var y = 0f
            while (y < size.height) {
                drawRect(Color.Black.copy(alpha = 0.22f), Offset(0f, y), androidx.compose.ui.geometry.Size(size.width, line))
                drawRect(fg.copy(alpha = 0.05f), Offset(0f, y + line * 1.5f), androidx.compose.ui.geometry.Size(size.width, line * 0.7f))
                y += step
            }
            drawRect(
                Brush.radialGradient(
                    listOf(Color.Transparent, Color.Black.copy(alpha = 0.45f)),
                    center = center,
                    radius = maxOf(size.width, size.height) * 0.75f,
                ),
            )
        }
    }
    if (sweeping || flash > 0f || bars.isNotEmpty()) {
        Canvas(modifier.fillMaxSize().graphicsLayer()) {
            if (sweeping) {
                val bandH = size.height * 0.16f
                val top = scanBarY(clockMs, size.height + bandH, 7_000) - bandH
                drawRect(
                    Brush.verticalGradient(listOf(Color.Transparent, fg.copy(alpha = 0.09f), Color.Transparent), startY = top, endY = top + bandH),
                    topLeft = Offset(0f, top), size = androidx.compose.ui.geometry.Size(size.width, bandH),
                )
            }
            if (bars.isNotEmpty()) {
                val colors = listOf(accent, Color(0xFF00E5FF), Color(0xFFFF2B2B))
                bars.forEach { b ->
                    drawRect(
                        colors[b.colorIndex].copy(alpha = 0.22f),
                        Offset(b.dx.dp.toPx() / 3f, b.y * size.height), androidx.compose.ui.geometry.Size(size.width, (b.height * size.height).coerceAtLeast(2.dp.toPx())),
                    )
                }
            }
            if (flash > 0f) drawRect(Color.Black.copy(alpha = flash))
        }
    }
}
