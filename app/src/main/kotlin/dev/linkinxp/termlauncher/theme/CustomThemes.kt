package dev.linkinxp.termlauncher.theme

import dev.linkinxp.termlauncher.layout.HomeLayout
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonArray

data class CustomThemeSpec(
    val name: String, val bg: Long, val fg: Long, val accent: Long, val dim: Long, val border: BorderStyle,
)

private val NAME = Regex("[a-z0-9_-]{1,24}")
private const val ADD_USAGE = "usage: theme add <name> <bg> <fg> <accent> <dim> [single|double|rounded|ascii|bracket]"
private const val BORDERS = "single|double|rounded|ascii|bracket"

/** The reason [name] cannot be a new custom theme, or null when it is fine (an existing custom name means replace). */
fun validateCustomTheme(name: String, existingCustom: List<String>, builtIn: List<String>): String? = when {
    name.isEmpty() || name.length > 24 -> "theme: name must be 1-24 characters"
    !NAME.matches(name) -> "theme: name may use a-z 0-9 - _ only"
    builtIn.any { it.equals(name, ignoreCase = true) } -> "theme: $name is a built-in theme"
    name in HomeLayout.RESERVED_WORDS || name == "add" || name == "rm" -> "theme: $name is a reserved word"
    existingCustom.none { it.equals(name, ignoreCase = true) } && existingCustom.size >= CustomThemes.MAX ->
        "theme: too many custom themes (${CustomThemes.MAX} max)"
    else -> null
}

/** The words after `theme add`: name, four colors and an optional border (default single). */
fun parseThemeAdd(args: List<String>): Result<CustomThemeSpec> {
    if (args.size !in 5..6) return Result.failure(IllegalArgumentException(ADD_USAGE))
    val colors = args.subList(1, 5).map { parseHexColor(it) ?: return Result.failure(IllegalArgumentException("theme: bad color '$it' (use #RRGGBB)")) }
    val border = args.getOrNull(5)?.let { word ->
        BorderStyle.entries.firstOrNull { it.name.equals(word, ignoreCase = true) }
            ?: return Result.failure(IllegalArgumentException("theme: bad border '$word' ($BORDERS)"))
    } ?: BorderStyle.SINGLE
    return Result.success(CustomThemeSpec(args[0], colors[0], colors[1], colors[2], colors[3], border))
}

fun buildCustomTheme(name: String, bg: Long, fg: Long, accent: Long, dim: Long, border: BorderStyle): Theme =
    Theme(name, bg, fg, accent, dim, border, glow = false, cyber = false)

class AddThemeResult(val list: List<Theme>?, val message: String)
class RemoveThemeResult(val list: List<Theme>?, val wasActive: Boolean, val message: String)

/** `theme add ...`: the new custom list (null when nothing changes) and the line to print. */
fun addCustomTheme(args: List<String>, existing: List<Theme>): AddThemeResult {
    val spec = parseThemeAdd(args).getOrElse { return AddThemeResult(null, it.message ?: ADD_USAGE) }
    validateCustomTheme(spec.name, existing.map { it.name }, ThemeRegistry.all.map { it.name })?.let { return AddThemeResult(null, it) }
    val theme = buildCustomTheme(spec.name, spec.bg, spec.fg, spec.accent, spec.dim, spec.border)
    val list = existing.filterNot { it.name.equals(spec.name, ignoreCase = true) } + theme
    val warn = if (contrastRatio(spec.fg, spec.bg) < 3.0) "\ntheme: low contrast" else ""
    return AddThemeResult(list, "theme: added ${spec.name} (theme ${spec.name} to use it)$warn")
}

/** `theme rm <name>`: built-in themes cannot be removed; [wasActive] says the removed theme was the one in use. */
fun removeCustomTheme(name: String, existing: List<Theme>, active: String): RemoveThemeResult {
    val found = existing.firstOrNull { it.name.equals(name.trim(), ignoreCase = true) }
        ?: return RemoveThemeResult(null, false, if (ThemeRegistry.byName(name) != null) "theme: ${name.trim().lowercase()} is built-in" else "theme: $name: no such custom theme")
    return RemoveThemeResult(existing - found, found.name.equals(active, ignoreCase = true), "theme: removed ${found.name}")
}

/** Stored form of the user's own themes. */
object CustomThemes {
    const val MAX = 8

    @Serializable
    private class Dto(val name: String, val bg: String, val fg: String, val accent: String, val dim: String, val border: String)

    private val json = Json { ignoreUnknownKeys = true }

    fun encode(list: List<Theme>): String = json.encodeToString(
        ListSerializer(Dto.serializer()),
        list.map { Dto(it.name, hexString(it.background), hexString(it.foreground), hexString(it.accent), hexString(it.dim), it.border.name) },
    )

    /** Bad data gives an empty list; entries with a bad name, color or border are skipped; at most [MAX] are kept. */
    fun decode(raw: String?): List<Theme> {
        if (raw.isNullOrBlank()) return emptyList()
        val elements = try { json.parseToJsonElement(raw).jsonArray } catch (_: Exception) { return emptyList() }
        // One damaged entry is skipped, the others stay.
        val dtos = elements.mapNotNull { e -> try { json.decodeFromJsonElement(Dto.serializer(), e) } catch (_: Exception) { null } }
        return dtos.mapNotNull { d ->
            if (!NAME.matches(d.name)) return@mapNotNull null
            val border = BorderStyle.entries.firstOrNull { it.name == d.border } ?: return@mapNotNull null
            val c = listOf(d.bg, d.fg, d.accent, d.dim).map { parseHexColor(it) ?: return@mapNotNull null }
            buildCustomTheme(d.name, c[0], c[1], c[2], c[3], border)
        }.take(MAX)
    }
}

/** The built-in themes followed by the user's own; a custom theme can never shadow a built-in one. */
class ThemeBook(val custom: List<Theme>) {
    val all: List<Theme> = ThemeRegistry.all + custom.filter { c -> ThemeRegistry.byName(c.name) == null }

    fun byName(name: String): Theme? = all.firstOrNull { it.name.equals(name.trim(), ignoreCase = true) }
}
