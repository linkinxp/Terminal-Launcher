package dev.linkinxp.termlauncher.icons

import dev.linkinxp.termlauncher.theme.Theme

/** Identifies the tile of one app drawn for one set of theme colors at one pixel size. */
data class IconKey(val pkg: String, val themeKey: String, val sizePx: Int)

/** Changes exactly when a color an icon is drawn with changes (not with the theme's name or glow). */
fun themeKey(theme: Theme): String = "%x-%x-%x-%x".format(theme.background, theme.foreground, theme.accent, theme.dim)

/**
 * An Android ColorMatrix (20 floats, row-major, offsets in 0..255) that turns a color into its luminance times [argb]:
 * white becomes the color, black stays black, alpha is kept.
 */
fun tintMatrix(argb: Long): FloatArray {
    val r = ((argb shr 16) and 0xFF) / 255f
    val g = ((argb shr 8) and 0xFF) / 255f
    val b = (argb and 0xFF) / 255f
    val lr = 0.2126f; val lg = 0.7152f; val lb = 0.0722f
    return floatArrayOf(
        r * lr, r * lg, r * lb, 0f, 0f,
        g * lr, g * lg, g * lb, 0f, 0f,
        b * lr, b * lg, b * lb, 0f, 0f,
        0f, 0f, 0f, 1f, 0f,
    )
}

/**
 * A thread-safe LRU bounded by total weight (for bitmaps: bytes), not by entry count: once [maxWeight] is exceeded the
 * least recently read or written entries go first (the newest entry always stays).
 */
class IconCache<V>(private val maxWeight: Long, private val weigh: (V) -> Int) {
    private val map = LinkedHashMap<IconKey, V>(32, 0.75f, true)
    private var total = 0L

    @Synchronized fun get(key: IconKey): V? = map[key]

    @Synchronized fun put(key: IconKey, value: V) {
        map.put(key, value)?.let { total -= weigh(it) }
        total += weigh(value)
        val it = map.entries.iterator()
        while (total > maxWeight && map.size > 1 && it.hasNext()) {
            val eldest = it.next()
            if (eldest.key == key) continue
            total -= weigh(eldest.value); it.remove()
        }
    }

    /** Drops every tile of [pkg] (all sizes and themes), e.g. after the app was updated. */
    @Synchronized fun removePackage(pkg: String) {
        val it = map.entries.iterator()
        while (it.hasNext()) { val e = it.next(); if (e.key.pkg == pkg) { total -= weigh(e.value); it.remove() } }
    }

    @Synchronized fun clear() { map.clear(); total = 0 }
    val size: Int @Synchronized get() = map.size
    val weight: Long @Synchronized get() = total
}
