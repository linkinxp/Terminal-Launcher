package dev.linkinxp.termlauncher.wall

import kotlin.random.Random

enum class WallMode { SOLID, SYSTEM, ASCII, RAIN, SVG }

data class WallSettings(
    val mode: WallMode = WallMode.SOLID,
    val style: AsciiStyle = AsciiStyle.GLYPH,
    val seed: Int = 1,
    val dim: Int = 55,
    val animate: Boolean = false,
    val svgStyle: SvgStyle = SvgStyle.CIRCUIT,
) {
    /** One `key value` line per setting. */
    fun describe(): String = listOf(
        "mode  ${mode.name.lowercase()}",
        "style ${style.name.lowercase()}",
        "seed  $seed",
        "dim   $dim",
        "anim  ${if (animate) "on" else "off"}",
        "svg   ${svgStyle.name.lowercase()}",
    ).joinToString("\n")
}

private val SVG_USAGE = "usage: wall svg " + SvgStyle.entries.joinToString("|") { it.name.lowercase() } + "|next|random"
private val STYLES = AsciiStyle.entries.joinToString(" ") { it.name.lowercase() }
private val MODES = WallMode.entries.joinToString(" ") { it.name.lowercase() }

/**
 * Applies one `wall` subcommand to [current]: the new settings, or a failure carrying the message to
 * show (the caller then keeps [current]). A bare `wall` ([sub] null) returns [current] unchanged.
 */
fun applyWall(current: WallSettings, sub: String?, arg: String?, random: Random): Result<WallSettings> {
    val a = arg?.trim()
    fun fail(msg: String) = Result.failure<WallSettings>(IllegalArgumentException(msg))
    return when (sub?.lowercase()) {
        null -> Result.success(current)
        "random" -> {
            var next = current.copy(style = AsciiStyle.entries[random.nextInt(AsciiStyle.entries.size)], seed = random.nextInt())
            if (next.style == current.style && next.seed == current.seed) next = next.copy(seed = next.seed + 1)
            Result.success(next)
        }
        "next" -> Result.success(current.copy(style = AsciiStyle.entries[(current.style.ordinal + 1) % AsciiStyle.entries.size]))
        "style" -> {
            val style = a?.let { n -> AsciiStyle.entries.firstOrNull { it.name.equals(n, ignoreCase = true) } }
            if (style != null) Result.success(current.copy(style = style))
            else fail("wall: style: ${a ?: "?"}: no such style ($STYLES)")
        }
        "seed" -> a?.toIntOrNull()?.let { Result.success(current.copy(seed = it)) } ?: fail("usage: wall seed <number>")
        "mode" -> {
            val mode = a?.let { n -> WallMode.entries.firstOrNull { it.name.equals(n, ignoreCase = true) } }
            if (mode != null) Result.success(current.copy(mode = mode))
            else fail("wall: mode: ${a ?: "?"}: no such mode ($MODES)")
        }
        "dim" -> a?.toIntOrNull()?.takeIf { it in 0..100 }?.let { Result.success(current.copy(dim = it)) }
            ?: fail("usage: wall dim <0-100>")
        "animate" -> when (a?.lowercase()) {
            "on" -> Result.success(current.copy(animate = true))
            "off" -> Result.success(current.copy(animate = false))
            else -> fail("usage: wall animate on|off")
        }
        "svg" -> when (a?.lowercase()) {
            null, "" -> Result.success(current)
            "next" -> Result.success(current.copy(svgStyle = SvgStyle.entries[(current.svgStyle.ordinal + 1) % SvgStyle.entries.size]))
            "random" -> {
                var next = current.copy(svgStyle = SvgStyle.entries[random.nextInt(SvgStyle.entries.size)], seed = random.nextInt())
                if (next.svgStyle == current.svgStyle && next.seed == current.seed) next = next.copy(seed = next.seed + 1)
                Result.success(next)
            }
            else -> SvgStyle.entries.firstOrNull { it.name.equals(a, ignoreCase = true) }?.let { Result.success(current.copy(svgStyle = it)) }
                ?: fail(SVG_USAGE)
        }
        else -> fail("wall: $sub: unknown (random next style seed mode dim animate svg)")
    }
}
