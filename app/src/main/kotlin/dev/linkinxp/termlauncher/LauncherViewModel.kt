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
import dev.linkinxp.termlauncher.fx.FxSettings
import dev.linkinxp.termlauncher.wall.WallSettings
import dev.linkinxp.termlauncher.wall.applyWall
import kotlin.random.Random
import dev.linkinxp.termlauncher.data.Prefs
import dev.linkinxp.termlauncher.layout.HomeLayout
import dev.linkinxp.termlauncher.layout.Item
import android.os.Build
import dev.linkinxp.termlauncher.prompt.DeviceInfo
import dev.linkinxp.termlauncher.prompt.capLines
import dev.linkinxp.termlauncher.prompt.complete as completeInput
import dev.linkinxp.termlauncher.prompt.fortune
import dev.linkinxp.termlauncher.prompt.neofetch
import dev.linkinxp.termlauncher.sys.formatUptime
import dev.linkinxp.termlauncher.sys.readSystemStats
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
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
    /** Last 5 submitted lines, newest first (offered when the prompt is empty). */
    val history: List<String> = emptyList(),
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
    val historyAll: List<String> = emptyList(),
    val matrix: Boolean = false,
    val boot: Boolean = false,
    val fx: FxSettings = FxSettings(),
    val wall: WallSettings = WallSettings(),
)

class LauncherViewModel(application: Application) : AndroidViewModel(application) {
    private val prefs = Prefs(application)
    private val repo = AppRepository(application)

    private data class Ephemeral(
        val drawerOpen: Boolean = false,
        val showHidden: Boolean = false,
        val filter: String = "",
        val prompt: PromptState = PromptState(),
        val matrix: Boolean = false,
        val boot: Boolean = false,
    )

    private val eph = MutableStateFlow(Ephemeral())

    private class Persisted(
        val theme: Theme, val layout: HomeLayout, val hidden: Set<String>,
        val sort: DrawerSort, val icons: IconMode, val stats: Map<String, AppStat>,
        val history: List<String>, val fx: FxSettings, val wall: WallSettings,
    )

    private val persisted = combine(
        prefs.theme, prefs.layout, prefs.hidden, prefs.sort, prefs.iconMode,
    ) { t, l, h, s, i -> arrayOf(t, l, h, s, i) }
        .combine(prefs.stats) { a, st -> a to st }
        .combine(prefs.history) { (a, st), h -> Triple(a, st, h) }
        .combine(prefs.fx) { (a, st, h), fx -> Pair(Triple(a, st, h), fx) }
        .combine(prefs.wall) { (t, fx), wall ->
            val (a, st, h) = t
            @Suppress("UNCHECKED_CAST")
            Persisted(a[0] as Theme, a[1] as HomeLayout, a[2] as Set<String>, a[3] as DrawerSort, a[4] as IconMode, st, h.all(), fx, wall)
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
            prompt = e.prompt.copy(history = p.history.asReversed().take(5)),
            historyAll = p.history,
            matrix = e.matrix,
            boot = e.boot,
            fx = p.fx,
            wall = p.wall,
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
            is Command.Pin -> withApp(cmd.app, cur) { app -> pinApp(app); null }
            is Command.Hide -> withApp(cmd.app, cur) { app ->
                val set = if (app.pkg in cur.hidden) cur.hidden - app.pkg else cur.hidden + app.pkg
                viewModelScope.launch { prefs.setHidden(set) }; null
            }
            is Command.Mkdir -> { applyLayout { it.mkdir(cmd.folder) }; null }
            is Command.Mv -> {
                val isFolder = cur.layout.folders().any { it.name.equals(cmd.app, ignoreCase = true) }
                val isApp = cur.apps.values.any { it.label.equals(cmd.app, ignoreCase = true) }
                if (isFolder && !isApp) { applyLayout { it.renameFolder(cmd.app, cmd.folder) }; null } // mv <folder> <new>
                else withApp(cmd.app, cur) { app -> applyLayout { it.mv(app.pkg, cmd.folder) }; null }
            }
            is Command.Rmdir -> {
                applyLayout { l ->
                    if (l.folders().none { it.name.equals(cmd.folder, true) }) Result.failure(IllegalArgumentException("rmdir: ${cmd.folder}: no such folder"))
                    else Result.success(l.rmdir(cmd.folder))
                }
                null
            }
            is Command.Ls -> { openDrawer(cmd.hidden); null }
            is Command.Fx -> when {
                cmd.name == null -> cur.fx.describe(cur.theme)
                cmd.on == null -> cur.fx.with(cmd.name, true).fold(
                    onSuccess = { "usage: fx <name> on|off" }, onFailure = { it.message },
                )
                else -> cur.fx.with(cmd.name, cmd.on).fold(
                    onSuccess = { next -> viewModelScope.launch { prefs.setFx(next) }; null },
                    onFailure = { it.message },
                )
            }
            is Command.Wall ->
                if (cmd.sub == null) cur.wall.describe()
                else applyWall(cur.wall, cmd.sub, cmd.arg, Random.Default).fold(
                    onSuccess = { next -> viewModelScope.launch { prefs.setWall(next) }; null },
                    onFailure = { it.message },
                )
            is Command.Sudo -> "user is not in the sudoers file. This incident will be reported."
            is Command.History ->
                if (cur.historyAll.isEmpty()) "history: empty"
                else cur.historyAll.mapIndexed { i, l -> "${i + 1}  $l" }.joinToString("\n")
            is Command.Neofetch -> neofetch(deviceInfo(cur))
            is Command.Whoami -> "user"
            is Command.Uptime -> "up ${formatUptime(readSystemStats(getApplication()).uptimeMs)}"
            is Command.Date -> DATE_FORMAT.format(LocalDateTime.now()).lowercase()
            is Command.Fortune -> fortune(System.nanoTime().toInt())
            is Command.Clear -> null
            is Command.Matrix -> { startMatrix(); null }
            is Command.Query -> when {
                cmd.text.isBlank() -> null
                else -> rank(cmd.text, cur).firstOrNull()?.let { launch(it.pkg); null } ?: "no match: ${cmd.text}"
            }
        }
        if (text.isNotBlank()) viewModelScope.launch { prefs.addHistory(text) }
        eph.update { it.withPrompt(PromptState("", emptyList(), message?.let { m -> capLines(m) }?.ifEmpty { null })) }
    }

    /** Tab-completion over command words and lowercase app labels (never shortens the input). */
    fun complete(input: String): String {
        val cur = ui.value
        return completeInput(input, COMMANDS + cur.apps.values.filter { it.pkg !in cur.hidden }.map { it.label.lowercase() })
    }

    fun startBoot() = eph.update { it.copy(boot = true) }
    fun endBoot() = eph.update { it.copy(boot = false) }
    fun startMatrix() = eph.update { it.copy(matrix = true) }
    fun stopMatrix() = eph.update { it.copy(matrix = false) }

    private fun deviceInfo(cur: LauncherUi): DeviceInfo {
        val stats = readSystemStats(getApplication())
        return DeviceInfo(
            model = Build.MODEL, android = Build.VERSION.RELEASE, launcher = LAUNCHER_VERSION,
            uptime = formatUptime(stats.uptimeMs), theme = cur.theme.name, apps = cur.apps.size, ramPct = stats.ramPct,
        )
    }

    // ---- actions ----

    fun launch(pkg: String) {
        val app = ui.value.apps[pkg]
        if (repo.launch(pkg)) viewModelScope.launch { prefs.recordLaunch(pkg) }
        else {
            repo.refresh() // the app may be gone: rescan so stale entries disappear
            eph.update { it.withPrompt(PromptState("", emptyList(), "exec: ${app?.label ?: pkg}: not found")) }
        }
    }

    fun uninstall(pkg: String) = repo.uninstall(pkg)
    fun appInfo(pkg: String) = repo.appInfo(pkg)

    fun openDrawer(showHidden: Boolean = false) =
        eph.update { it.copy(drawerOpen = true, showHidden = showHidden, filter = "") }
    fun closeDrawer() = eph.update { it.copy(drawerOpen = false, showHidden = false, filter = "") }
    fun setFilter(filter: String) = eph.update { it.copy(filter = filter) }
    fun setSort(sort: DrawerSort) { viewModelScope.launch { prefs.setSort(sort) } }
    fun setIconMode(mode: IconMode) { viewModelScope.launch { prefs.setIconMode(mode) } }
    fun toggleHidden(pkg: String) {
        val h = ui.value.hidden
        viewModelScope.launch { prefs.setHidden(if (pkg in h) h - pkg else h + pkg) }
    }
    fun dismissMessage() = eph.update { it.withPrompt(it.prompt.copy(message = null)) }

    // ---- layout operations (used by UI directly) ----

    fun pin(pkg: String) { ui.value.apps[pkg]?.let(::pinApp) }
    fun unpin(pkg: String) = edit { it.unpin(pkg) }
    fun addPage() = edit { it.addPage() }
    fun removePage(index: Int) = edit { it.removePage(index) }
    fun movePage(from: Int, to: Int) = edit { it.movePage(from, to) }
    fun addToPage(page: Int, item: Item) = edit { it.addToPage(page, item) }
    fun rmdir(name: String) = edit { it.rmdir(name) }
    fun unfile(pkg: String) = edit { it.unfile(pkg) }
    fun pinFolder(name: String) = edit { it.pinFolder(name) }
    fun unpinFolder(name: String) = edit { it.unpinFolder(name) }
    fun mkdirOnPage(name: String, page: Int) = applyLayout { it.mkdir(name, page) }
    fun removeShortcut(pkg: String) = edit { it.removeShortcut(pkg) }
    fun mv(pkg: String, folder: String) = applyLayout { it.mv(pkg, folder) }

    // ---- helpers ----

    /** Applies [op] to the stored layout atomically; a failure's message is shown in the prompt area. */
    private fun applyLayout(op: (HomeLayout) -> Result<HomeLayout>) {
        viewModelScope.launch {
            prefs.editLayout(op).exceptionOrNull()?.message?.let { m ->
                eph.update { it.withPrompt(it.prompt.copy(message = m)) }
            }
        }
    }

    /** Pins [app] to the dock; a full dock or an already pinned app is reported in the prompt area. */
    private fun pinApp(app: AppEntry) = applyLayout { l ->
        val next = l.pin(app.pkg)
        if (next == l) Result.failure(IllegalStateException("pin: ${app.label.lowercase()}: dock full or already pinned"))
        else Result.success(next)
    }

    private fun edit(op: (HomeLayout) -> HomeLayout) = applyLayout { Result.success(op(it)) }

    private fun withApp(name: String, cur: LauncherUi, block: (AppEntry) -> String?): String? {
        val all = cur.apps.values.sortedWith(compareBy({ it.label.lowercase() }, { it.pkg }))
        val app = all.firstOrNull { it.label.equals(name, ignoreCase = true) }
            ?: fuzzyRank(name, all.map { it.label }).firstOrNull()?.let { all[it] }
        return if (app == null) "no match: $name" else block(app)
    }

    /** Visible (non-hidden) apps best-first for [query]. */
    private fun rank(query: String, cur: LauncherUi): List<AppEntry> {
        val visible = cur.apps.values.filter { it.pkg !in cur.hidden }.sortedWith(compareBy({ it.label.lowercase() }, { it.pkg }))
        return fuzzyRank(query, visible.map { it.label }).map { visible[it] }
    }

    private fun Ephemeral.withPrompt(p: PromptState) = copy(prompt = p)

    companion object {
        const val LAUNCHER_VERSION = "0.3.0"
        val DATE_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("EEE d MMM yyyy HH:mm:ss", Locale.ENGLISH)
        val COMMANDS = listOf(
            "theme", "pin", "hide", "mkdir", "mv", "rmdir", "ls", "help", "fx", "history",
            "neofetch", "whoami", "uptime", "date", "fortune", "clear", "sudo", "matrix", "wall",
        )
        val HELP = listOf(
            "theme <name>       change theme",
            "pin <app>          pin to dock",
            "hide <app>         hide / unhide",
            "mkdir <f>          new folder",
            "mv <app> <f>       file app",
            "mv <f> <new>       rename folder",
            "rmdir <f>          remove folder",
            "ls [-a]            drawer / hidden",
            "fx [name on|off]   effects",
            "wall <sub> [arg]   wallpaper",
            "history neofetch whoami uptime date",
            "fortune clear sudo matrix help",
        ).joinToString("\n")
    }
}
