package dev.linkinxp.termlauncher.theme

object ThemeRegistry {
    val all: List<Theme> = listOf(
        Theme("classy-black", 0xFF000000, 0xFFFFFFFF, 0xFF3D7BFF, 0xFF8A8A8A, BorderStyle.ROUNDED, glow = true),
        Theme("matrix", 0xFF000000, 0xFF00FF41, 0xFF008F11, 0xFF006B1F, BorderStyle.SINGLE, glow = true),
        Theme("amber", 0xFF0A0600, 0xFFFFB000, 0xFFFF7A00, 0xFF8C6200, BorderStyle.ASCII, glow = true),
        Theme("solarized-dark", 0xFF002B36, 0xFF93A1A1, 0xFF268BD2, 0xFF586E75, BorderStyle.SINGLE, glow = false),
        Theme("dracula", 0xFF282A36, 0xFFF8F8F2, 0xFFFF79C6, 0xFF6272A4, BorderStyle.DOUBLE, glow = false),
    )

    val default: Theme = all.first()

    fun byName(name: String): Theme? = all.firstOrNull { it.name.equals(name.trim(), ignoreCase = true) }
}
