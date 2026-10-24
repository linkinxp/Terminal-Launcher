package dev.linkinxp.termlauncher.theme

object ThemeRegistry {
    val all: List<Theme> = listOf(
        Theme("classy-black", 0xFF000000, 0xFFFFFFFF, 0xFF3D7BFF, 0xFF8A8A8A, BorderStyle.ROUNDED, glow = true),
        Theme("matrix", 0xFF000000, 0xFF00FF41, 0xFF008F11, 0xFF006B1F, BorderStyle.SINGLE, glow = true),
        Theme("amber", 0xFF0A0600, 0xFFFFB000, 0xFFFF7A00, 0xFF8C6200, BorderStyle.ASCII, glow = true),
        Theme("solarized-dark", 0xFF002B36, 0xFF93A1A1, 0xFF268BD2, 0xFF586E75, BorderStyle.SINGLE, glow = false),
        Theme("dracula", 0xFF282A36, 0xFFF8F8F2, 0xFFFF79C6, 0xFF6272A4, BorderStyle.DOUBLE, glow = false),
        Theme("neon-city", 0xFF0B0014, 0xFF00F0FF, 0xFFFF2BD6, 0xFF7A3E8C, BorderStyle.BRACKET, glow = true, cyber = true),
        Theme("synthwave", 0xFF1A0B2E, 0xFFFFE6FF, 0xFFFF6AC1, 0xFF8F6BD1, BorderStyle.ROUNDED, glow = true, cyber = true),
        Theme("blade-runner", 0xFF04090B, 0xFF6FE7D2, 0xFFFFA630, 0xFF3F7F76, BorderStyle.DOUBLE, glow = true, cyber = true),
        Theme("glitch-red", 0xFF000000, 0xFFFF3B3B, 0xFFFFFFFF, 0xFF8C1F1F, BorderStyle.BRACKET, glow = true, cyber = true),
    )

    val default: Theme = all.first()

    fun byName(name: String): Theme? = all.firstOrNull { it.name.equals(name.trim(), ignoreCase = true) }
}

/** One line per theme, `* name` for [current] and `  name` for the others, in registry order. */
fun describeThemes(current: String): String =
    ThemeRegistry.all.joinToString("\n") { (if (it.name.equals(current, ignoreCase = true)) "* " else "  ") + it.name }
