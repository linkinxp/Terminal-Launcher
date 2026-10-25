package dev.linkinxp.termlauncher.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import dev.linkinxp.termlauncher.theme.Theme
import dev.linkinxp.termlauncher.wall.SvgColors
import dev.linkinxp.termlauncher.wall.SvgRenderer
import dev.linkinxp.termlauncher.wall.WallSettings

private const val MAX_WIDTH_PX = 1080 // drawn at most this wide and scaled up: the art is soft-edged vector line work

/**
 * The generated SVG wallpaper: drawn once off the main thread (and cached), then just an image in its own layer.
 * Until it is ready, or if it cannot be drawn, the plain theme background shows. `dim` sets how strong it is.
 */
@Composable
fun SvgBackground(wall: WallSettings, theme: Theme, modifier: Modifier = Modifier) {
    val colors = SvgColors(theme.background, theme.foreground, theme.accent, theme.dim)
    val strength = ((100 - wall.dim.coerceIn(0, 100)) / 100f).coerceAtLeast(0.15f)
    BoxWithConstraints(modifier.fillMaxSize().background(theme.background.color())) {
        val scale = if (constraints.maxWidth > MAX_WIDTH_PX) MAX_WIDTH_PX.toFloat() / constraints.maxWidth else 1f
        val w = (constraints.maxWidth * scale).toInt()
        val h = (constraints.maxHeight * scale).toInt()
        val bitmap by produceState<ImageBitmap?>(null, wall.svgStyle, wall.seed, colors, w, h) {
            value = SvgRenderer.render(wall.svgStyle, wall.seed, colors, w, h)
        }
        bitmap?.let { Image(it, null, Modifier.fillMaxSize().graphicsLayer { alpha = strength }, contentScale = ContentScale.FillBounds) }
    }
}
