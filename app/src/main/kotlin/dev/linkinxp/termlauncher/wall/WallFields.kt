package dev.linkinxp.termlauncher.wall

/** The wallpaper settings that can matter, so the screen only shows the ones the current mode uses. */
enum class WallField { STYLE, SVG_STYLE, SEED, DIM, ANIMATE }

fun wallFields(mode: WallMode): Set<WallField> = when (mode) {
    WallMode.SOLID -> emptySet()
    WallMode.SYSTEM -> setOf(WallField.DIM)
    WallMode.ASCII -> setOf(WallField.STYLE, WallField.SEED, WallField.DIM, WallField.ANIMATE)
    WallMode.RAIN -> setOf(WallField.SEED, WallField.DIM)
    WallMode.SVG -> setOf(WallField.SVG_STYLE, WallField.SEED, WallField.DIM)
}
