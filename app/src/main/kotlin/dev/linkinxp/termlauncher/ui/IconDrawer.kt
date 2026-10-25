package dev.linkinxp.termlauncher.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.linkinxp.termlauncher.LauncherUi
import dev.linkinxp.termlauncher.apps.AppEntry
import dev.linkinxp.termlauncher.apps.DrawerState
import dev.linkinxp.termlauncher.search.ExtraKind
import dev.linkinxp.termlauncher.search.ExtraResult

/** One entry of the icon drawer grid: a full-width header or folder row, or an app cell. */
sealed interface GridEntry {
    val key: String
    data class Header(val text: String) : GridEntry { override val key get() = "h-$text" }
    data class Folder(val name: String, val count: Int) : GridEntry { override val key get() = "f-$name" }
    data class Cell(val app: AppEntry, val group: String) : GridEntry { override val key get() = "c-$group-${app.pkg}" }
    data class Extra(val extra: ExtraResult) : GridEntry { override val key get() = "x-${extra.kind}-${extra.label}" }
}

/** The grid's entries and, for the A-Z rail, the entry index of each letter's header. */
class GridLayout(val entries: List<GridEntry>, val headerIndex: Map<Char, Int>)

/** Folders first; then `recent` and the lettered sections, or while [filtering] one flat best-first list. */
fun gridLayout(d: DrawerState, folders: List<Pair<String, Int>>, filtering: Boolean, extras: List<ExtraResult> = emptyList()): GridLayout {
    val out = ArrayList<GridEntry>()
    val index = LinkedHashMap<Char, Int>()
    folders.forEach { (name, count) -> out += GridEntry.Folder(name, count) }
    if (filtering) {
        d.results.forEach { out += GridEntry.Cell(it, "s") }
    } else {
        if (d.recent.isNotEmpty()) { out += GridEntry.Header("recent"); d.recent.forEach { out += GridEntry.Cell(it, "recent") } }
        d.sections.forEach { section ->
            index[section.letter] = out.size
            out += GridEntry.Header("-- ${section.letter} --")
            section.apps.forEach { out += GridEntry.Cell(it, section.letter.toString()) }
        }
    }
    extras.forEach { out += GridEntry.Extra(it) }
    return GridLayout(out, index)
}

/** The icon grid: 4 columns of themed icons with one-line labels. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun IconDrawerGrid(
    layout: GridLayout,
    ui: LauncherUi,
    state: LazyGridState,
    scrollConnection: NestedScrollConnection,
    onLaunch: (String) -> Unit,
    onAppLongPress: (String) -> Unit,
    onFolderTap: (String) -> Unit,
    onExtra: (ExtraResult) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val theme = LocalTheme.current
    LazyVerticalGrid(
        GridCells.Fixed(ui.density.columns), modifier.fillMaxSize().nestedScroll(scrollConnection), state = state,
        contentPadding = PaddingValues(bottom = 8.dp),
    ) {
        layout.entries.forEach { entry ->
            when (entry) {
                is GridEntry.Header -> item(key = entry.key, span = { GridItemSpan(maxLineSpan) }) {
                    TermText(entry.text, color = theme.accent.color(), bold = true, modifier = Modifier.padding(top = 10.dp, bottom = 2.dp))
                }
                is GridEntry.Folder -> item(key = entry.key, span = { GridItemSpan(maxLineSpan) }) {
                    TermText("~/${entry.name} (${entry.count})", Modifier.fillMaxWidth().clickable { onFolderTap(entry.name) }.padding(vertical = 8.dp), color = theme.accent.color())
                }
                is GridEntry.Extra -> item(key = entry.key, span = { GridItemSpan(maxLineSpan) }) {
                    TermText("> ${entry.extra.label}", Modifier.fillMaxWidth().clickable { onExtra(entry.extra) }.padding(vertical = 10.dp), color = if (entry.extra.kind == ExtraKind.WEB) theme.accent.color() else theme.foreground.color())
                }
                is GridEntry.Cell -> item(key = entry.key) {
                    IconCell(entry.app.label.lowercase(), { onLaunch(entry.app.pkg) }, { onAppLongPress(entry.app.pkg) }, labelSize = ui.density.labelSp.sp) {
                        BadgedIcon(listOf(entry.app.pkg), ui.badges, "badge-${entry.app.pkg}") { IconTile(entry.app.pkg, entry.app.label, ui.density.iconDp.dp) }
                    }
                }
            }
        }
        if (layout.entries.isEmpty()) item(span = { GridItemSpan(maxLineSpan) }) { TermText("no match", dim = true) }
    }
}
