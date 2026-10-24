package dev.linkinxp.termlauncher.quick

const val MAX_QUICK = 8
private const val TILE_CHARS = 7

/** The `quick` panel's apps (package names) in order: no duplicates, at most [MAX_QUICK]. */
fun quickAdd(list: List<String>, pkg: String): List<String> =
    if (pkg in list || list.size >= MAX_QUICK) list else list + pkg

fun quickRemove(list: List<String>, pkg: String): List<String> = list.filterNot { it == pkg }

/** Swaps [pkg] with the tile before it; the first tile or an unknown app leaves the list unchanged. */
fun quickMoveUp(list: List<String>, pkg: String): List<String> {
    val i = list.indexOf(pkg)
    if (i <= 0) return list
    return list.toMutableList().also { it[i] = it[i - 1]; it[i - 1] = pkg }
}

fun encodeQuick(list: List<String>): String = list.joinToString("\n")

/** `null` = nothing stored (use the defaults); blank lines, repeats and anything past [MAX_QUICK] are dropped. */
fun decodeQuick(raw: String?): List<String>? =
    raw?.lines()?.map { it.trim() }?.filter { it.isNotEmpty() }?.distinct()?.take(MAX_QUICK)

/** Tile text: the first word of the app name, lowercase, at most 7 characters (`?` when blank), so four tiles fit a row. */
fun tileLabel(label: String): String = label.trim().substringBefore(' ').lowercase().take(TILE_CHARS).ifEmpty { "?" }

fun <T> tileRows(items: List<T>, perRow: Int = 4): List<List<T>> = items.chunked(perRow)

sealed interface QuickAction {
    data object Show : QuickAction
    data class Add(val name: String) : QuickAction
    data class Remove(val name: String) : QuickAction
    data object Clear : QuickAction
    data object Reset : QuickAction
}

/** Parses `quick`, `quick add <app>`, `quick rm <app>`, `quick clear`, `quick reset`; failures carry the message. */
fun parseQuickAction(sub: String?, arg: String?): Result<QuickAction> {
    fun fail(msg: String) = Result.failure<QuickAction>(IllegalArgumentException(msg))
    val name = arg?.trim()?.takeIf { it.isNotEmpty() }
    return when (sub?.lowercase()) {
        null -> Result.success(QuickAction.Show)
        "add" -> name?.let { Result.success(QuickAction.Add(it)) } ?: fail("usage: quick add <app>")
        "rm" -> name?.let { Result.success(QuickAction.Remove(it)) } ?: fail("usage: quick rm <app>")
        "clear" -> Result.success(QuickAction.Clear)
        "reset" -> Result.success(QuickAction.Reset)
        else -> fail("quick: $sub: unknown (add rm clear reset)")
    }
}
