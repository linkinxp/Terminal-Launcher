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
import dev.linkinxp.termlauncher.fx.FxSettings
import dev.linkinxp.termlauncher.theme.Theme
import kotlinx.coroutines.delay
import kotlin.random.Random

/** Scanlines, vignette and flicker over the whole screen. Draws only; it never intercepts touches. */
@Composable
fun EffectsOverlay(settings: FxSettings, theme: Theme, reduceMotion: Boolean, modifier: Modifier = Modifier) {
    val scanlines = settings.scanlinesOn(theme)
    var flash by remember { mutableFloatStateOf(0f) }
    val flicker = settings.flicker && !reduceMotion
    LaunchedEffect(flicker) {
        while (flicker) {
            delay(Random.nextLong(2_000, 6_000))
            flash = 0.04f
            delay(60)
            flash = 0f
        }
    }
    if (!scanlines && flash == 0f) return
    // Own render layer: ~700 scanlines + the vignette are recorded once, not on every invalidation underneath.
    Canvas(modifier.fillMaxSize().graphicsLayer()) {
        if (scanlines) {
            val step = 3.dp.toPx()
            var y = 0f
            while (y < size.height) {
                drawLine(Color.Black.copy(alpha = 0.2f), Offset(0f, y), Offset(size.width, y), strokeWidth = 1.2f)
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
        if (flash > 0f) drawRect(Color.Black.copy(alpha = flash))
    }
}
