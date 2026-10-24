package dev.linkinxp.termlauncher.theme

import kotlin.math.pow

/** `#RGB`, `#RRGGBB` (hash optional, any case) to an opaque ARGB long, or null for anything else. */
fun parseHexColor(text: String): Long? {
    val hex = text.trim().removePrefix("#")
    if (hex.any { it !in '0'..'9' && it !in 'a'..'f' && it !in 'A'..'F' }) return null
    val full = when (hex.length) {
        3 -> hex.map { "$it$it" }.joinToString("")
        6 -> hex
        else -> return null
    }
    return 0xFF000000L or full.toLong(16)
}

/** WCAG contrast ratio of two colors (1.0 = identical, 21.0 = black on white); alpha is ignored. */
fun contrastRatio(a: Long, b: Long): Double {
    val la = luminance(a); val lb = luminance(b)
    return (maxOf(la, lb) + 0.05) / (minOf(la, lb) + 0.05)
}

private fun luminance(argb: Long): Double {
    fun channel(shift: Int): Double {
        val c = ((argb shr shift) and 0xFF) / 255.0
        return if (c <= 0.03928) c / 12.92 else ((c + 0.055) / 1.055).pow(2.4)
    }
    return 0.2126 * channel(16) + 0.7152 * channel(8) + 0.0722 * channel(0)
}

/** `#RRGGBB` for an ARGB long. */
fun hexString(argb: Long): String = "#" + (argb and 0xFFFFFF).toString(16).padStart(6, '0')
