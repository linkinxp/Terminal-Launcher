package dev.linkinxp.termlauncher.wall

import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.caverock.androidsvg.SVG
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Draws the generated SVG wallpapers to bitmaps, keeping only the last two so memory stays bounded. */
object SvgRenderer {
    private data class Key(val style: SvgStyle, val seed: Int, val colors: SvgColors, val w: Int, val h: Int)

    private val cache = object : LinkedHashMap<Key, ImageBitmap>(4, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<Key, ImageBitmap>?) = size > 2
    }

    /** The wallpaper at [width] x [height] pixels, or null when the size is unusable or rendering fails. */
    suspend fun render(style: SvgStyle, seed: Int, colors: SvgColors, width: Int, height: Int): ImageBitmap? {
        if (width <= 0 || height <= 0) return null
        val key = Key(style, seed, colors, width, height)
        synchronized(cache) { cache[key] }?.let { return it }
        return withContext(Dispatchers.Default) {
            try {
                val svg = SVG.getFromString(svgText(style, seed, colors, width, height))
                svg.setDocumentWidth(width.toFloat()); svg.setDocumentHeight(height.toFloat())
                val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                svg.renderToCanvas(Canvas(bitmap))
                bitmap.asImageBitmap().also { synchronized(cache) { cache[key] = it } }
            } catch (_: Exception) {
                null
            }
        }
    }
}
