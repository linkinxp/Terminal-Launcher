package dev.linkinxp.termlauncher.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.linkinxp.termlauncher.LauncherUi
import dev.linkinxp.termlauncher.LauncherViewModel
import dev.linkinxp.termlauncher.apps.DrawerSort
import dev.linkinxp.termlauncher.data.IconMode
import kotlinx.coroutines.launch

private val SORT_LABEL = mapOf(DrawerSort.AZ to "a-z", DrawerSort.RECENTLY_INSTALLED to "new", DrawerSort.MOST_USED to "used")
private val ICON_LABEL = mapOf(IconMode.TEXT to "txt", IconMode.GLYPH to "gly", IconMode.ICON to "ico")

/** Full-screen app list: filter, sort and icon toggles, recent section, letter sections, fast-scroll rail. */
@Composable
fun AppDrawer(
    ui: LauncherUi,
    vm: LauncherViewModel,
    onAppLongPress: (String) -> Unit,
    onFolderTap: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val theme = LocalTheme.current
    val d = ui.drawer
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    BackHandler { vm.closeDrawer() }

    var filterValue by remember { mutableStateOf(TextFieldValue(ui.filter)) }
    val folders = ui.layout.folders()
    val showFolders = ui.filter.isBlank() && !ui.showHidden

    // Item positions of each section header, for the fast-scroll rail.
    val headerIndex = remember(d, folders.size, showFolders) {
        val map = LinkedHashMap<Char, Int>()
        var i = (if (showFolders) folders.size else 0) + (if (d.recent.isEmpty()) 0 else 1 + d.recent.size)
        d.sections.forEach { map[it.letter] = i; i += 1 + it.apps.size }
        map
    }

    Box(
        modifier.fillMaxSize().background(theme.background.color())
            // Swallow taps/long-presses the rows do not handle, so they cannot reach the home screen underneath.
            .pointerInput(Unit) { detectTapGestures(onTap = {}, onLongPress = {}) }
            .statusBarsPadding().navigationBarsPadding().imePadding(),
    ) {
        Column(Modifier.fillMaxSize().padding(start = 16.dp, end = 32.dp)) {
            Row(
                Modifier.fillMaxWidth().padding(vertical = 8.dp)
                    .pointerInput(Unit) {
                        var total = 0f
                        detectVerticalDragGestures(
                            onDragStart = { total = 0f },
                            onDragEnd = { if (total > 120f) vm.closeDrawer() },
                        ) { _, dy -> total += dy }
                    },
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                TermText(if (ui.showHidden) "ls -a" else "ls", color = theme.accent.color(), bold = true)
                Row {
                    TermText("sort:${SORT_LABEL.getValue(ui.sort)}", Modifier.clickable {
                        vm.setSort(DrawerSort.entries[(ui.sort.ordinal + 1) % DrawerSort.entries.size])
                    }.padding(horizontal = 8.dp))
                    TermText("icons:${ICON_LABEL.getValue(ui.iconMode)}", Modifier.clickable {
                        vm.setIconMode(IconMode.entries[(ui.iconMode.ordinal + 1) % IconMode.entries.size])
                    }.padding(horizontal = 8.dp))
                    TermText("[x]", Modifier.clickable { vm.closeDrawer() }.padding(start = 8.dp), dim = true)
                }
            }
            Row(Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                TermText("| grep ", dim = true)
                BasicTextField(
                    value = filterValue,
                    onValueChange = { filterValue = it; vm.setFilter(it.text) },
                    singleLine = true,
                    textStyle = TextStyle(color = theme.foreground.color(), fontFamily = TermFont, fontSize = 14.sp),
                    cursorBrush = SolidColor(theme.accent.color()),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    modifier = Modifier.weight(1f),
                )
            }
            LazyColumn(Modifier.weight(1f), state = listState) {
                if (showFolders) items(folders.size, key = { "f-${folders[it].name}" }) { i ->
                    val f = folders[i]
                    TermText("~/${f.name} (${f.apps.size})", Modifier.fillMaxWidth().clickable { onFolderTap(f.name) }.padding(vertical = 8.dp), color = theme.accent.color())
                }
                if (d.recent.isNotEmpty()) {
                    item(key = "h-recent") { TermText("recent", color = theme.accent.color(), bold = true, modifier = Modifier.padding(top = 4.dp)) }
                    items(d.recent.size, key = { "r-${d.recent[it].pkg}" }) { i ->
                        val app = d.recent[i]
                        AppRow(app, ui.iconMode, { vm.launch(app.pkg); vm.closeDrawer() }, { onAppLongPress(app.pkg) })
                    }
                }
                d.sections.forEach { section ->
                    item(key = "h-${section.letter}") {
                        TermText("-- ${section.letter} --", color = theme.accent.color(), bold = true, modifier = Modifier.padding(top = 8.dp))
                    }
                    items(section.apps.size, key = { "a-${section.letter}-${section.apps[it].pkg}" }) { i ->
                        val app = section.apps[i]
                        AppRow(app, ui.iconMode, { vm.launch(app.pkg); vm.closeDrawer() }, { onAppLongPress(app.pkg) })
                    }
                }
                if (d.sections.isEmpty()) item { TermText("no match", dim = true) }
            }
        }
        AlphaIndex(
            letters = headerIndex.keys.toList(),
            onSelect = { c -> headerIndex[c]?.let { scope.launch { listState.scrollToItem(it) } } },
            modifier = Modifier.align(androidx.compose.ui.Alignment.CenterEnd).padding(vertical = 64.dp),
        )
    }
}
