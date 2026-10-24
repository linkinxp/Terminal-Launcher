package dev.linkinxp.termlauncher.importer

import dev.linkinxp.termlauncher.layout.HomeLayout
import dev.linkinxp.termlauncher.layout.Item
import dev.linkinxp.termlauncher.layout.Page

/** One row of a Launcher3-family `favorites` table (Nova, Lawnchair and others). */
data class FavRow(
    val id: Int,
    val itemType: Int,
    val container: Int,
    val screen: Int,
    val cellX: Int,
    val cellY: Int,
    val title: String?,
    val intent: String?,
)

data class ImportedLayout(
    val layout: HomeLayout,
    val apps: Int,
    val folders: Int,
    val skippedWidgets: Int,
    val missingApps: Int,
)

private const val TYPE_APP = 0
private const val TYPE_FOLDER = 2
private const val TYPE_WIDGET = 4
private const val DESKTOP = -100
private const val HOTSEAT = -101
private const val MAX_FOLDER_NAME = 24
private val COMPONENT = Regex("component=([A-Za-z0-9_.]+)/")
private val PACKAGE = Regex("package=([A-Za-z0-9_.]+)")

/** The package named by an Android intent URI (`component=pkg/cls` first, then `package=pkg`), or null. */
fun packageFromIntent(intent: String?): String? {
    if (intent.isNullOrBlank()) return null
    return COMPONENT.find(intent)?.groupValues?.get(1) ?: PACKAGE.find(intent)?.groupValues?.get(1)
}

/**
 * Turns Launcher3-style rows into our layout: desktop screens become pages (consecutive, at most 5; later
 * screens are appended to the fifth), the hotseat becomes the dock (at most 5; extras go to page 1), folders
 * keep their apps in cell order, widgets are skipped and counted, and apps that are not installed here are
 * dropped and counted. A package is placed once (dock first, then pages in order).
 */
fun mapFavorites(rows: List<FavRow>, installed: Set<String>): ImportedLayout {
    val byPosition = compareBy<FavRow>({ it.cellY }, { it.cellX }, { it.id })
    val childrenOf = rows.filter { it.itemType == TYPE_APP && it.container >= 0 }.groupBy { it.container }
    val folderRows = rows.filter { it.itemType == TYPE_FOLDER }.associateBy { it.id }

    val placed = HashSet<String>()
    val missing = HashSet<String>()
    val usedNames = HashSet<String>()
    var folders = 0
    var skippedWidgets = 0

    fun appItem(row: FavRow): Item.App? {
        val pkg = packageFromIntent(row.intent) ?: return null
        if (pkg !in installed) { missing += pkg; return null }
        return if (placed.add(pkg)) Item.App(pkg) else null
    }

    fun folderItem(row: FavRow): Item.Folder? {
        val apps = (childrenOf[row.id] ?: emptyList()).sortedWith(byPosition).mapNotNull { appItem(it)?.pkg }
        if (apps.isEmpty()) return null
        folders++
        return Item.Folder(uniqueName(cleanFolderName(row.title), usedNames), apps)
    }

    fun item(row: FavRow): Item? = when (row.itemType) {
        TYPE_APP -> appItem(row)
        TYPE_FOLDER -> folderItem(folderRows.getValue(row.id))
        TYPE_WIDGET -> { skippedWidgets++; null }
        else -> null
    }

    // Hotseat first so its apps keep the dock, then the desktop screens in order.
    val hotseatRows = rows.filter { it.container == HOTSEAT && it.itemType in setOf(TYPE_APP, TYPE_FOLDER, TYPE_WIDGET) }
        .sortedWith(compareBy({ it.cellX }, { it.id }))
    val hotseat = hotseatRows.mapNotNull { item(it) }
    val dock = hotseat.take(HomeLayout.MAX_DOCK)

    val desktop = rows.filter { it.container == DESKTOP && it.itemType in setOf(TYPE_APP, TYPE_FOLDER, TYPE_WIDGET) }
        .groupBy { it.screen }.toSortedMap()
    val pages = desktop.values.map { screenRows -> screenRows.sortedWith(byPosition).mapNotNull { item(it) }.toMutableList() }
        .toMutableList()
    while (pages.size > HomeLayout.MAX_PAGES) pages[HomeLayout.MAX_PAGES - 1].addAll(pages.removeAt(HomeLayout.MAX_PAGES))
    if (pages.isEmpty()) pages += mutableListOf<Item>()
    pages[0].addAll(hotseat.drop(HomeLayout.MAX_DOCK))

    return ImportedLayout(
        layout = HomeLayout(dock = dock, pages = pages.map { Page(it) }),
        apps = placed.size,
        folders = folders,
        skippedWidgets = skippedWidgets,
        missingApps = missing.size,
    )
}

/** Whitespace -> `-`, `/` removed, at most 24 characters, never blank, never a command word. */
private fun cleanFolderName(title: String?): String {
    val cleaned = (title ?: "").trim().replace(Regex("\\s+"), "-").replace("/", "").take(MAX_FOLDER_NAME)
    val name = cleaned.ifEmpty { "folder" }
    return if (name.lowercase() in HomeLayout.RESERVED_WORDS) "${name}_" else name
}

/** Case-insensitively unique: a clash gets a numeric suffix (`games2`), kept within 24 characters. */
private fun uniqueName(base: String, used: MutableSet<String>): String {
    var candidate = base
    var n = 2
    while (!used.add(candidate.lowercase())) {
        val suffix = n++.toString()
        candidate = base.take(MAX_FOLDER_NAME - suffix.length) + suffix
    }
    return candidate
}
