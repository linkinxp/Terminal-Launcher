package dev.linkinxp.termlauncher.icons

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.Typeface
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.Drawable
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import dev.linkinxp.termlauncher.theme.Theme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val CORNER = 0.28f   // tile corner radius, fraction of the size
private const val SAFE = 0.66f     // the glyph lives in this fraction of the tile

private fun withAlpha(argb: Long, alpha: Float): Int {
    val a = (alpha * 255).toInt().coerceIn(0, 255)
    return (a shl 24) or (argb.toInt() and 0x00FFFFFF)
}

/**
 * One themed tile: a rounded square (the theme's dim color, faint, with a thin border) and the app's glyph tinted with
 * the theme's foreground (accent when [selected]). A monochrome layer is tinted flat; any other icon is grayscaled and
 * tinted so it keeps its detail; a null [icon] draws the first letter of [label].
 */
fun drawTile(icon: Drawable?, label: String, theme: Theme, sizePx: Int, selected: Boolean = false): Bitmap {
    val s = sizePx.toFloat()
    val bmp = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bmp)
    val radius = s * CORNER
    val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = withAlpha(theme.dim, 0.18f) }
    canvas.drawRoundRect(0f, 0f, s, s, radius, radius, fill)
    val border = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = 1f; color = withAlpha(theme.dim, 0.4f) }
    canvas.drawRoundRect(0.5f, 0.5f, s - 0.5f, s - 0.5f, radius, radius, border)
    val tint = if (selected) theme.accent else theme.foreground
    val inset = s * (1 - SAFE) / 2
    val mono = (icon as? AdaptiveIconDrawable)?.monochrome
    // Whatever the glyph layer paints stays inside the rounded tile.
    canvas.clipPath(android.graphics.Path().apply { addRoundRect(0f, 0f, s, s, radius, radius, android.graphics.Path.Direction.CW) })
    when {
        mono != null -> {
            // The adaptive canvas is 108 dp of which 72 dp (66%) is visible: scale it so that part fills the safe zone.
            val layer = s / SAFE
            val off = (s - layer) / 2
            val d = mono.mutate()
            d.setBounds(off.toInt(), off.toInt(), (off + layer).toInt(), (off + layer).toInt())
            d.colorFilter = PorterDuffColorFilter(tint.toInt(), PorterDuff.Mode.SRC_IN)
            d.draw(canvas)
        }
        icon != null -> {
            val d = icon.mutate()
            d.setBounds(inset.toInt(), inset.toInt(), (s - inset).toInt(), (s - inset).toInt())
            d.colorFilter = ColorMatrixColorFilter(tintMatrix(tint))
            d.draw(canvas)
        }
        else -> {
            val letter = label.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "?"
            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = tint.toInt(); textAlign = Paint.Align.CENTER; typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD); textSize = s * 0.45f
            }
            canvas.drawText(letter, s / 2, s / 2 - (paint.ascent() + paint.descent()) / 2, paint)
        }
    }
    return bmp
}

/** Renders and caches themed tiles; every call is safe from any thread and never throws. */
class IconRepository(private val context: Context) {
    private val cache = IconCache<ImageBitmap>()

    suspend fun tile(pkg: String, label: String, theme: Theme, sizePx: Int, selected: Boolean = false): ImageBitmap {
        val key = IconKey(pkg, themeKey(theme) + if (selected) "!" else "", sizePx)
        cache.get(key)?.let { return it }
        return withContext(Dispatchers.Default) {
            val icon = try { context.packageManager.getApplicationIcon(pkg) } catch (_: Exception) { null }
            val bitmap = try { drawTile(icon, label, theme, sizePx, selected) } catch (_: Exception) { drawTile(null, label, theme, sizePx, selected) }
            bitmap.asImageBitmap().also { cache.put(key, it) }
        }
    }

    fun invalidate() = cache.clear()

    companion object {
        @Volatile private var instance: IconRepository? = null
        fun get(context: Context): IconRepository =
            instance ?: synchronized(this) { instance ?: IconRepository(context.applicationContext).also { instance = it } }
    }
}
