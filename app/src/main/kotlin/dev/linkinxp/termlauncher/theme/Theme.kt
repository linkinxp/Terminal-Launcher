package dev.linkinxp.termlauncher.theme

enum class BorderStyle { SINGLE, DOUBLE, ROUNDED, ASCII, BRACKET }

/** Colors are ARGB longs so the core stays free of Android/Compose types. */
data class Theme(
    val name: String,
    val background: Long,
    val foreground: Long,
    val accent: Long,
    val dim: Long,
    val border: BorderStyle,
    val glow: Boolean,
    /** Cyberpunk theme: scanlines default to on. */
    val cyber: Boolean = false,
)
