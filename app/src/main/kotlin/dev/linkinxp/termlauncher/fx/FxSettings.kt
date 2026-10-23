package dev.linkinxp.termlauncher.fx

import dev.linkinxp.termlauncher.theme.Theme

/** CRT/glitch toggles. [scanlines] `null` means "follow the theme" (on for cyber themes). */
data class FxSettings(
    val scanlines: Boolean? = null,
    val flicker: Boolean = false,
    val glitch: Boolean = true,
    val cursor: Boolean = true,
    val boot: Boolean = true,
) {
    fun with(name: String, on: Boolean): Result<FxSettings> = when (name.lowercase()) {
        "scanlines" -> Result.success(copy(scanlines = on))
        "flicker" -> Result.success(copy(flicker = on))
        "glitch" -> Result.success(copy(glitch = on))
        "cursor" -> Result.success(copy(cursor = on))
        "boot" -> Result.success(copy(boot = on))
        else -> Result.failure(IllegalArgumentException("fx: $name: no such effect (${NAMES.joinToString(" ")})"))
    }

    fun scanlinesOn(theme: Theme): Boolean = scanlines ?: theme.cyber

    /** One `name on|off` line per effect (scanlines notes when it follows the theme). */
    fun describe(theme: Theme): String = listOf(
        "scanlines  ${onOff(scanlinesOn(theme))}${if (scanlines == null) " (theme)" else ""}",
        "flicker    ${onOff(flicker)}",
        "glitch     ${onOff(glitch)}",
        "cursor     ${onOff(cursor)}",
        "boot       ${onOff(boot)}",
    ).joinToString("\n")

    private fun onOff(b: Boolean) = if (b) "on" else "off"

    companion object {
        val NAMES = listOf("scanlines", "flicker", "glitch", "cursor", "boot")
    }
}
