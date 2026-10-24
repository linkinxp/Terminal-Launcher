package dev.linkinxp.termlauncher.panel

/** Which home-page panels are shown: `cal` and `wx` are off by default, the `search` bar is on. */
data class PanelSettings(val cal: Boolean = false, val wx: Boolean = false, val search: Boolean = true) {
    fun describe(): String = listOf(
        "cal    ${if (cal) "on" else "off"}",
        "wx     ${if (wx) "on" else "off"}",
        "search ${if (search) "on" else "off"}",
    ).joinToString("\n")
}

/**
 * Applies `panel <name> on|off` to [current]; a bare `panel` ([name] null) returns it unchanged. Failures carry
 * the message to show and leave the settings untouched.
 */
fun applyPanel(current: PanelSettings, name: String?, on: Boolean?): Result<PanelSettings> {
    if (name == null) return Result.success(current)
    val key = name.lowercase()
    if (key !in PANELS) {
        return Result.failure(IllegalArgumentException("panel: $name: no such panel (${PANELS.joinToString(" ")})"))
    }
    if (on == null) return Result.failure(IllegalArgumentException("usage: panel <cal|wx|search> on|off"))
    return Result.success(
        when (key) {
            "cal" -> current.copy(cal = on)
            "wx" -> current.copy(wx = on)
            else -> current.copy(search = on)
        },
    )
}

private val PANELS = listOf("cal", "wx", "search")
