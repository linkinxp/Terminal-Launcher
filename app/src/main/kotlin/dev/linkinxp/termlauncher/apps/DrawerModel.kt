package dev.linkinxp.termlauncher.apps

import dev.linkinxp.termlauncher.layout.Item

data class DrawerSection(val letter: Char, val apps: List<AppEntry>)

/** [results] is non-empty only while filtering: the matches in relevance order (best first). */
data class DrawerState(
    val recent: List<AppEntry>,
    val sections: List<DrawerSection>,
    val results: List<AppEntry> = emptyList(),
)

private const val RECENT_COUNT = 4

/** Pure drawer model: visibility, filter, `recent`, and lettered sections sorted per [sort]. */
fun buildDrawer(
    apps: List<AppEntry>,
    hidden: Set<String>,
    stats: Map<String, AppStat>,
    sort: DrawerSort,
    filter: String,
    showHidden: Boolean,
): DrawerState {
    val visible = apps.filter { (it.pkg in hidden) == showHidden }
    val matched = fuzzyRank(filter, visible.map { it.label }).map { visible[it] }
    val byLabel = compareBy<AppEntry>({ it.label.lowercase() }, { it.pkg })
    val order: Comparator<AppEntry> = when (sort) {
        DrawerSort.AZ -> byLabel
        DrawerSort.RECENTLY_INSTALLED -> compareByDescending<AppEntry> { it.installTime }.then(byLabel)
        DrawerSort.MOST_USED -> compareByDescending<AppEntry> { stats[it.pkg]?.launches ?: 0 }.then(byLabel)
    }
    val sections = matched.sortedWith(order)
        .groupBy { a -> a.label.firstOrNull()?.takeIf { it.isLetter() }?.uppercaseChar() ?: '#' }
        .map { (letter, list) -> DrawerSection(letter, list) }
        .sortedWith(compareBy({ it.letter == '#' }, { it.letter }))
    val recent = if (filter.isBlank()) {
        visible.filter { (stats[it.pkg]?.lastUsed ?: 0) > 0 }
            .sortedByDescending { stats.getValue(it.pkg).lastUsed }
            .take(RECENT_COUNT)
    } else emptyList()
    val results = if (filter.isBlank()) emptyList() else matched
    return DrawerState(recent, sections, results)
}

/** Folders whose name fuzzy-matches [filter] (all of them when it is blank), best match first. */
fun filterFolders(filter: String, folders: List<Item.Folder>): List<Item.Folder> =
    if (filter.isBlank()) folders else fuzzyRank(filter, folders.map { it.name }).map { folders[it] }
