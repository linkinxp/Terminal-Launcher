package dev.linkinxp.termlauncher.clock

/** How the home clock is drawn. [PIXEL] is the original blocky look. */
enum class ClockStyle {
    PIXEL, SEGMENT, FIGLET, BINARY, HEX, EPOCH, PLAIN;

    companion object {
        fun fromName(name: String?): ClockStyle = entries.firstOrNull { it.name == name } ?: PIXEL
    }
}

private val STYLE_USAGE = "usage: clock style " + ClockStyle.entries.joinToString("|") { it.name.lowercase() }

fun parseClockStyle(arg: String?): Result<ClockStyle> {
    val name = arg?.trim()?.lowercase()
    val style = ClockStyle.entries.firstOrNull { it.name.lowercase() == name }
    return if (style != null) Result.success(style) else Result.failure(IllegalArgumentException(STYLE_USAGE))
}

/** What `clock style [name]` does: the style to save (null = unchanged) and the line to print. */
data class ClockStyleReply(val set: ClockStyle?, val text: String)

fun clockStyleReply(arg: String?, current: ClockStyle): ClockStyleReply =
    if (arg == null) ClockStyleReply(null, "style   ${current.name.lowercase()}")
    else parseClockStyle(arg).fold(
        { ClockStyleReply(it, "clock style: ${it.name.lowercase()}") },
        { ClockStyleReply(null, it.message ?: STYLE_USAGE) },
    )
