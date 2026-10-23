package dev.linkinxp.termlauncher

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dev.linkinxp.termlauncher.apps.AppEntry
import dev.linkinxp.termlauncher.apps.AppRepository
import dev.linkinxp.termlauncher.apps.AppStat
import dev.linkinxp.termlauncher.apps.DrawerSort
import dev.linkinxp.termlauncher.apps.DrawerState
import dev.linkinxp.termlauncher.apps.buildDrawer
import dev.linkinxp.termlauncher.apps.fuzzyRank
import dev.linkinxp.termlauncher.data.IconMode
import dev.linkinxp.termlauncher.data.Prefs
import dev.linkinxp.termlauncher.layout.HomeLayout
import dev.linkinxp.termlauncher.layout.Item
import dev.linkinxp.termlauncher.prompt.Command
import dev.linkinxp.termlauncher.prompt.parseCommand
import dev.linkinxp.termlauncher.theme.Theme
import dev.linkinxp.termlauncher.theme.ThemeRegistry
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class PromptState(
    val text: String = "",
    val suggestions: List<AppEntry> = emptyList(),
    val message: String? = null,
)

data class LauncherUi(
    val theme: Theme = ThemeRegistry.default,
    val layout: HomeLayout = HomeLayout.default(),
    val apps: Map<String, AppEntry> = emptyMap(),
    val hidden: Set<String> = emptySet(),
    val sort: DrawerSort = DrawerSort.AZ,
    val iconMode: IconMode = IconMode.TEXT,
    val drawer: DrawerState = DrawerState(emptyList(), emptyList()),
    val drawerOpen: Boolean = false,
    val showHidden: Boolean = false,
    val filter: String = "",
    val prompt: PromptState = PromptState(),
)

class LauncherViewModel(application: Application) : AndroidViewModel(application) {
    private val prefs = Prefs(application)
    private val repo = AppRepository(application)

    private class Ephemeral(
        val drawerOpen: Boolean = false,
        val showHidden: Boolean = false,
        val filter: String = "",
        val prompt: PromptState = PromptState(),
    )

    private val eph = MutableStateFlow(Ephemeral())

    private class Persisted(
        val theme: Theme, val layout: HomeLayout, val hidden: Set<String>,
        val sort: DrawerSort, val icons: IconMode, val stats: Map<String, AppStat>,
    )

    private val persisted = combine(
        prefs.theme, prefs.layout, prefs.hidden, prefs.sort, prefs.iconMode,
    ) { t, l, h, s, i -> arrayOf(t, l, h, s, i) }
        .combine(prefs.stats) { a, st ->
            @Suppress("UNCHECKED_CAST")
            Persisted(a[0] as Theme, a[1] as HomeLayout, a[2] as Set<String>, a[3] as DrawerSort, a[4] as IconMode, st)
        }

    val ui: StateFlow<LauncherUi> = combine(persisted, repo.apps, eph) { p, apps, e ->
        val installed = apps.map { it.pkg }.toSet()
        LauncherUi(
            theme = p.theme,
            layout = p.layout.prune(installed),
            apps = apps.associateBy { it.pkg },
            hidden = p.hidden,
            sort = p.sort,
            iconMode = p.icons,
            drawer = buildDrawer(apps, p.hidden, p.stats, p.sort, e.filter, e.showHidden),
            drawerOpen = e.drawerOpen,
            showHidden = e.showHidden,
            filter = e.filter,
            prompt = e.prompt,
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, LauncherUi())

    // ---- prompt ----

    fun onPromptChange(text: String) {
        val cur = ui.value
        val suggestions = if (parseCommand(text) is Command.Query && text.isNotBlank()) rank(text, cur).take(5) else emptyList()
        eph.update { it.withPrompt(PromptState(text, suggestions, null)) }
    }

    fun onPromptSubmit(text: String) {
        val cur = ui.value
        val message: String? = when (val cmd = parseCommand(text)) {
            is Command.Help -> HELP
            is Command.Theme -> ThemeRegistry.byName(cmd.name)?.let { t -> viewModelScope.launch { prefs.setTheme(t.name) }; null }
                ?: "theme: ${cmd.name}: no such theme (${ThemeRegistry.all.joinToString(" ") { it.name }})"
            is Command.Pin -> withApp(cmd.app, cur) { app ->
                val next = cur.layout.pin(app.pkg)
                if (next == cur.layout) "pin: ${app.label}: dock full or already pinned" else { saveLayout(next); null }
            }
            is Command.Hide -> withApp(cmd.app, cur) { app ->
                val set = if (app.pkg in cur.hidden) cur.hidden - app.pkg else cur.hidden + app.pkg
                viewModelScope.launch { prefs.setHidden(set) }; null
            }
            is Command.Mkdir -> layoutResult(cur.layout.mkdir(cmd.folder))
            is Command.Mv -> withApp(cmd.app, cur) { app -> layoutResult(cur.layout.mv(app.pkg, cmd.folder)) }
            is Command.Rmdir -> {
                if (cur.layout.folders().none { it.name.equals(cmd.folder, true) }) "rmdir: ${cmd.folder}: no such folder"
                else { saveLayout(cur.layout.rmdir(cmd.folder)); null }
            }
            is Command.Ls -> { openDrawer(cmd.hidden); null }
            is Command.Query -> when {
                cmd.text.isBlank() -> null
                else -> rank(cmd.text, cur).firstOrNull()?.let { launch(it.pkg); null } ?: "no match: ${cmd.text}"
            }
        }
        eph.update { it.withPrompt(PromptState("", emptyList(), message)) }
    }

    // ---- actions ----

    fun launch(pkg: String) {
        val app = ui.value.apps[pkg]
        if (repo.launch(pkg)) viewModelScope.launch { prefs.recordLaunch(pkg) }
        else eph.update { it.withPrompt(PromptState("", emptyList(), "exec: ${app?.label ?: pkg}: not found")) }
    }

    fun uninstall(pkg: String) = repo.uninstall(pkg)
    fun appInfo(pkg: String) = repo.appInfo(pkg)

    fun openDrawer(showHidden: Boolean = false) =
        eph.update { Ephemeral(drawerOpen = true, showHidden = showHidden, prompt = it.prompt) }
    fun closeDrawer() = eph.update { Ephemeral(prompt = it.prompt) }
    fun setFilter(filter: String) = eph.update {
        Ephemeral(it.drawerOpen, it.showHidden, filter, it.prompt)
    }
    fun setSort(sort: DrawerSort) { viewModelScope.launch { prefs.setSort(sort) } }
    fun setIconMode(mode: IconMode) { viewModelScope.launch { prefs.setIconMode(mode) } }
    fun toggleHidden(pkg: String) {
        val h = ui.value.hidden
        viewModelScope.launch { prefs.setHidden(if (pkg in h) h - pkg else h + pkg) }
    }
    fun dismissMessage() = eph.update { it.withPrompt(it.prompt.copy(message = null)) }

    // ---- layout operations (used by UI directly) ----

    fun pin(pkg: String) = edit { it.pin(pkg) }
    fun unpin(pkg: String) = edit { it.unpin(pkg) }
    fun addPage() = edit { it.addPage() }
    fun removePage(index: Int) = edit { it.removePage(index) }
    fun movePage(from: Int, to: Int) = edit { it.movePage(from, to) }
    fun addToPage(page: Int, item: Item) = edit { it.addToPage(page, item) }
    fun rmdir(name: String) = edit { it.rmdir(name) }
    fun unfile(pkg: String) = edit { it.unfile(pkg) }
    fun pinFolder(name: String) = edit { it.pinFolder(name) }
    fun unpinFolder(name: String) = edit { it.unpinFolder(name) }
    fun mkdirOnPage(name: String, page: Int) {
        val message = layoutResult(ui.value.layout.mkdir(name, page))
        eph.update { it.withPrompt(PromptState("", emptyList(), message)) }
    }
    fun mv(pkg: String, folder: String) {
        val r = ui.value.layout.mv(pkg, folder)
        r.onSuccess { saveLayout(it) }
        r.onFailure { m -> eph.update { it.withPrompt(PromptState("", emptyList(), m.message)) } }
    }

    // ---- helpers ----

    private fun edit(op: (HomeLayout) -> HomeLayout) = saveLayout(op(ui.value.layout))
    private fun saveLayout(l: HomeLayout) { viewModelScope.launch { prefs.setLayout(l) } }

    private fun layoutResult(r: Result<HomeLayout>): String? =
        r.fold(onSuccess = { saveLayout(it); null }, onFailure = { it.message })

    private fun withApp(name: String, cur: LauncherUi, block: (AppEntry) -> String?): String? {
        val all = cur.apps.values.toList()
        val app = all.firstOrNull { it.label.equals(name, ignoreCase = true) }
            ?: fuzzyRank(name, all.map { it.label }).firstOrNull()?.let { all[it] }
        return if (app == null) "no match: $name" else block(app)
    }

    /** Visible (non-hidden) apps best-first for [query]. */
    private fun rank(query: String, cur: LauncherUi): List<AppEntry> {
        val visible = cur.apps.values.filter { it.pkg !in cur.hidden }.sortedBy { it.label.lowercase() }
        return fuzzyRank(query, visible.map { it.label }).map { visible[it] }
    }

    private fun Ephemeral.withPrompt(p: PromptState) = Ephemeral(drawerOpen, showHidden, filter, p)

    private companion object {
        const val HELP = "theme <name> | pin <app> | hide <app> | mkdir <f> | mv <app> <f> | rmdir <f> | ls [-a] | help"
    }
}
