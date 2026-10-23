package dev.linkinxp.termlauncher.panel

/** Which home-page panels are shown (both off by default). */
data class PanelSettings(val cal: Boolean = false, val wx: Boolean = false) {
    fun describe(): String = listOf(
        "cal  ${if (cal) "on" else "off"}",
        "wx   ${if (wx) "on" else "off"}",
    ).joinToString("\n")
}

/**
 * Applies `panel <name> on|off` to [current]; a bare `panel` ([name] null) returns it unchanged. Failures carry
 * the message to show and leave the settings untouched.
 */
fun applyPanel(current: PanelSettings, name: String?, on: Boolean?): Result<PanelSettings> {
    if (name == null) return Result.success(current)
    if (name.lowercase() !in PANELS) {
        return Result.failure(IllegalArgumentException("panel: $name: no such panel (${PANELS.joinToString(" ")})"))
    }
    if (on == null) return Result.failure(IllegalArgumentException("usage: panel <cal|wx> on|off"))
    return Result.success(if (name.lowercase() == "cal") current.copy(cal = on) else current.copy(wx = on))
}

private val PANELS = listOf("cal", "wx")
