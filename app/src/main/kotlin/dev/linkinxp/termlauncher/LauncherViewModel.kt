package dev.linkinxp.termlauncher

import android.text.format.DateFormat
import dev.linkinxp.termlauncher.clock.ClockFormat
import dev.linkinxp.termlauncher.clock.ClockStyle
import dev.linkinxp.termlauncher.clock.clockFormatReply
import dev.linkinxp.termlauncher.clock.clockStyleCommand
import dev.linkinxp.termlauncher.clock.clockStyleReply
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
import dev.linkinxp.termlauncher.layout.Density
import dev.linkinxp.termlauncher.search.ContactsRepository
import dev.linkinxp.termlauncher.search.ExtraResult
import dev.linkinxp.termlauncher.search.buildExtras
import dev.linkinxp.termlauncher.layout.HomeStyle
import dev.linkinxp.termlauncher.layout.densityPlan
import dev.linkinxp.termlauncher.layout.PanelKind
import dev.linkinxp.termlauncher.layout.Restructure
import dev.linkinxp.termlauncher.layout.layoutPlan
import android.Manifest
import android.os.Build
import dev.linkinxp.termlauncher.backup.BackupRepository
import dev.linkinxp.termlauncher.backup.decodeBackup
import dev.linkinxp.termlauncher.backup.describeBackup
import dev.linkinxp.termlauncher.backup.encodeBackup
import dev.linkinxp.termlauncher.calendar.CalendarRepository
import dev.linkinxp.termlauncher.importer.FavRow
import dev.linkinxp.termlauncher.importer.mapFavorites
import dev.linkinxp.termlauncher.calendar.calendarQueryWindow
import dev.linkinxp.termlauncher.calendar.formatAgenda
import dev.linkinxp.termlauncher.panel.PanelData
import dev.linkinxp.termlauncher.panel.PanelSettings
import dev.linkinxp.termlauncher.panel.applyPanel
import dev.linkinxp.termlauncher.weather.WeatherRepository
import dev.linkinxp.termlauncher.weather.WxSettings
import dev.linkinxp.termlauncher.weather.WxAction
import dev.linkinxp.termlauncher.weather.formatWeather
import dev.linkinxp.termlauncher.weather.weatherSummary
import dev.linkinxp.termlauncher.weather.parseForecast
import dev.linkinxp.termlauncher.weather.shouldFetchWeather
import dev.linkinxp.termlauncher.weather.parseWxAction
import dev.linkinxp.termlauncher.layout.WidgetSize
import dev.linkinxp.termlauncher.widgets.WidgetHostController
import java.time.ZoneId
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
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
import dev.linkinxp.termlauncher.quick.MAX_QUICK
import dev.linkinxp.termlauncher.quick.QuickAction
import dev.linkinxp.termlauncher.quick.QuickDefaults
import dev.linkinxp.termlauncher.quick.parseQuickAction
import dev.linkinxp.termlauncher.quick.quickAdd
import dev.linkinxp.termlauncher.quick.quickMoveUp
import dev.linkinxp.termlauncher.quick.quickRemove
import dev.linkinxp.termlauncher.prompt.Command
import dev.linkinxp.termlauncher.prompt.parseCommand
import dev.linkinxp.termlauncher.theme.Theme
import dev.linkinxp.termlauncher.matrix.RABBIT
import dev.linkinxp.termlauncher.matrix.SPOON
import dev.linkinxp.termlauncher.matrix.pillPlan
import dev.linkinxp.termlauncher.theme.ThemeBook
import dev.linkinxp.termlauncher.theme.ThemeRegistry
import dev.linkinxp.termlauncher.theme.addCustomTheme
import dev.linkinxp.termlauncher.theme.removeCustomTheme
import dev.linkinxp.termlauncher.theme.describeThemes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** What an import would do: a title, summary lines and the action behind `[replace]`. */
data class ImportPreview(val title: String, val lines: List<String>, val onConfirm: suspend () -> Unit)

/** A widget the user is adding: where, which provider, and the host id allocated for it. */
data class PendingWidget(val page: Int, val provider: String, val id: Int)

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
    val wake: Boolean = false,
    val boot: Boolean = false,
    val fx: FxSettings = FxSettings(),
    val wall: WallSettings = WallSettings(),
    val panels: PanelSettings = PanelSettings(),
    val calendar: PanelData = PanelData.Off,
    val weather: PanelData = PanelData.Off,
    /** One-line summary shown under the date (null without a forecast or with the weather panel switched off). */
    val weatherLine: String? = null,
    val wx: WxSettings = WxSettings(),
    /** The quick tiles as package names, already limited to apps installed here. */
    val quick: List<String> = emptyList(),
    val drawerKeyboard: Boolean = true,
    val clockFormat: ClockFormat = ClockFormat.AUTO,
    val clockStyle: ClockStyle = ClockStyle.PIXEL,
    val customThemes: List<Theme> = emptyList(),
    val homeStyle: HomeStyle = HomeStyle.CLASSIC,
    val doubleTapLock: Boolean = true,
    val density: Density = Density.NORMAL,
    val badges: Boolean = true,
    /** Contacts, settings pages and a web fallback for the drawer search (empty without a query). */
    val extras: List<ExtraResult> = emptyList(),
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
        val wake: Boolean = false,
        val boot: Boolean = false,
        val calendar: PanelData = PanelData.Off,
        val weather: PanelData = PanelData.Off,
        val weatherLine: String? = null,
        val extras: List<ExtraResult> = emptyList(),
    )

    private val eph = MutableStateFlow(Ephemeral())

    private class Persisted(
        val theme: Theme, val layout: HomeLayout, val hidden: Set<String>,
        val sort: DrawerSort, val icons: IconMode, val stats: Map<String, AppStat>,
        val history: List<String>, val fx: FxSettings, val wall: WallSettings,
        val panels: PanelSettings,
    )

    private val persisted = combine(
        prefs.theme, prefs.layout, prefs.hidden, prefs.sort, prefs.iconMode,
    ) { t, l, h, s, i -> arrayOf(t, l, h, s, i) }
        .combine(prefs.stats) { a, st -> a to st }
        .combine(prefs.history) { (a, st), h -> Triple(a, st, h) }
        .combine(prefs.fx) { (a, st, h), fx -> Pair(Triple(a, st, h), fx) }
        .combine(prefs.wall) { (t, fx), wall -> Triple(t, fx, wall) }
        .combine(prefs.panels) { (t, fx, wall), panels ->
            val (a, st, h) = t
            @Suppress("UNCHECKED_CAST")
            Persisted(a[0] as Theme, a[1] as HomeLayout, a[2] as Set<String>, a[3] as DrawerSort, a[4] as IconMode, st, h.all(), fx, wall, panels)
        }

    private class Extras(val wx: WxSettings, val quick: List<String>?, val drawerKeyboard: Boolean, val clockFormat: ClockFormat, val clockStyle: ClockStyle, val customThemes: List<Theme>, val homeStyle: HomeStyle, val doubleTapLock: Boolean, val density: Density, val badges: Boolean)

    private val extras = combine(prefs.wx, prefs.quick, prefs.drawerKeyboard, prefs.clockFormat, prefs.clockStyle, prefs.customThemes, prefs.homeStyle, prefs.doubleTapLock, prefs.density, prefs.badges) { a ->
        @Suppress("UNCHECKED_CAST")
        Extras(a[0] as WxSettings, a[1] as List<String>?, a[2] as Boolean, a[3] as ClockFormat, a[4] as ClockStyle, a[5] as List<Theme>, a[6] as HomeStyle, a[7] as Boolean, a[8] as Density, a[9] as Boolean)
    }

    /** Dialer, messages, camera and maps apps of this phone: the tiles until the user changes the list. */
    private val quickDefaults: List<String> by lazy { QuickDefaults(application).packages() }

    /**
     * The drawer rows depend only on the app list, hidden apps, usage stats, sort and the filter, so they are rebuilt
     * only when one of those changes (not on every prompt keystroke, panel refresh or drawer open/close).
     */
    private val drawerState: Flow<DrawerState> = combine(
        repo.apps, prefs.hidden, prefs.stats, prefs.sort,
        eph.map { it.filter to it.showHidden }.distinctUntilChanged(),
    ) { apps, hidden, stats, sort, fs -> buildDrawer(apps, hidden, stats, sort, fs.first, fs.second) }

    val ui: StateFlow<LauncherUi> = combine(persisted, repo.apps, eph, extras, drawerState) { p, apps, e, x, drawer ->
        val installed = apps.map { it.pkg }.toSet()
        LauncherUi(
            theme = p.theme,
            layout = p.layout.prune(installed),
            apps = apps.associateBy { it.pkg },
            hidden = p.hidden,
            sort = p.sort,
            iconMode = p.icons,
            drawer = drawer,
            drawerOpen = e.drawerOpen,
            showHidden = e.showHidden,
            filter = e.filter,
            prompt = e.prompt.copy(history = p.history.asReversed().take(5)),
            historyAll = p.history,
            matrix = e.matrix,
            wake = e.wake,
            boot = e.boot,
            fx = p.fx,
            wall = p.wall,
            panels = p.panels,
            calendar = if (p.panels.cal) e.calendar else PanelData.Off,
            weather = if (p.panels.wx) e.weather else PanelData.Off,
            weatherLine = if (p.panels.wx) e.weatherLine else null,
            extras = e.extras,
            wx = x.wx,
            quick = (x.quick ?: quickDefaults).filter { it in installed },
            drawerKeyboard = x.drawerKeyboard,
            clockFormat = x.clockFormat,
            clockStyle = x.clockStyle,
            customThemes = x.customThemes,
            homeStyle = x.homeStyle,
            doubleTapLock = x.doubleTapLock,
            density = x.density,
            badges = x.badges,
        )
    }.flowOn(Dispatchers.Default) // the rebuild is pure computation: keep it off the main thread
        .stateIn(viewModelScope, SharingStarted.Eagerly, LauncherUi(homeStyle = HomeStyle.DEFAULT))

    // ---- backup and import ----

    private val backups = BackupRepository(prefs)
    private val _importPreview = MutableStateFlow<ImportPreview?>(null)

    /** The import waiting for the user's `[replace]` / `[cancel]`, or null. */
    val importPreview: StateFlow<ImportPreview?> = _importPreview.asStateFlow()

    suspend fun exportJson(): String = encodeBackup(backups.snapshot())

    /** [json] is the text of the picked file (null when it could not be read). */
    fun startImport(json: String?) {
        if (json == null) { say("import: could not read that file"); return }
        decodeBackup(json).fold(
            onSuccess = { b ->
                _importPreview.value = ImportPreview("import backup", describeBackup(b, ui.value.apps.keys)) { backups.apply(b) }
            },
            onFailure = { say(it.message ?: "import: not a launcher backup") },
        )
    }

    /** A Nova/Lawnchair backup was read ([rows] or the failure message): preview what would be imported. */
    fun startLauncherImport(rows: Result<List<FavRow>>) {
        rows.fold(
            onSuccess = { list ->
                val m = mapFavorites(list, ui.value.apps.keys)
                if (m.apps == 0) {
                    say("import: nothing to import (none of its apps are installed here)")
                } else {
                    val lines = listOf(
                        "pages: ${m.layout.pages.size}",
                        "dock: ${m.layout.dock.size}",
                        "folders: ${m.folders}",
                        "apps found: ${m.apps}",
                        "apps not installed here: ${m.missingApps}",
                        "widgets skipped: ${m.skippedWidgets}",
                        "your current widgets stay: ${ui.value.layout.widgetIds().size}",
                    )
                    _importPreview.value = ImportPreview("import layout", lines) { prefs.editLayout { current -> Result.success(m.layout.withWidgetsFrom(current)) } }
                }
            },
            onFailure = { say(it.message ?: "import: could not read that file") },
        )
    }

    /** Shows [preview] (used by the other-launcher import too). */
    fun showImportPreview(preview: ImportPreview) { _importPreview.value = preview }

    fun confirmImport() {
        val p = _importPreview.value ?: return
        _importPreview.value = null
        viewModelScope.launch { p.onConfirm(); say("import: done") }
    }

    fun cancelImport() { _importPreview.value = null }

    // ---- quick tiles ----

    /** Applies [op] to the stored tile list (or, if never set, to the defaults that are installed here), atomically. */
    private fun quickEdit(op: (List<String>) -> List<String>) {
        viewModelScope.launch { prefs.editQuick { stored -> op(stored ?: quickDefaults.filter { it in ui.value.apps }) } }
    }

    fun addQuick(pkg: String) = quickEdit { quickAdd(it, pkg) }
    fun removeQuick(pkg: String) = quickEdit { quickRemove(it, pkg) }
    fun moveQuickUp(pkg: String) = quickEdit { quickMoveUp(it, pkg) }
    fun resetQuick() { viewModelScope.launch { prefs.setQuick(null) } }

    private fun handleQuick(action: QuickAction, cur: LauncherUi): String? = when (action) {
        QuickAction.Show ->
            if (cur.quick.isEmpty()) "quick: empty (quick add <app>)"
            else cur.quick.joinToString("\n") { "  " + (cur.apps[it]?.label?.lowercase() ?: it) }
        is QuickAction.Add -> withApp(action.name, cur) { app ->
            if (app.pkg in cur.quick) "quick: ${app.label.lowercase()}: already there"
            else if (cur.quick.size >= MAX_QUICK) "quick: full ($MAX_QUICK tiles): quick rm <app> first"
            else { addQuick(app.pkg); null }
        }
        is QuickAction.Remove -> {
            val labels = cur.quick.map { cur.apps[it]?.label ?: it }
            val hit = fuzzyRank(action.name, labels).firstOrNull()
            if (hit == null) "quick: no tile named ${action.name}" else { removeQuick(cur.quick[hit]); null }
        }
        QuickAction.Clear -> { quickEdit { emptyList() }; null }
        QuickAction.Reset -> { resetQuick(); null }
    }

    // ---- settings (used by the settings screen and the `settings` command) ----

    private val _settingsRequests = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    /** Emits when the `settings` command asks the UI to open the settings screen. */
    val settingsRequests: SharedFlow<Unit> = _settingsRequests.asSharedFlow()

    fun setTheme(name: String) {
        ThemeBook(ui.value.customThemes).byName(name)?.let { t -> viewModelScope.launch { prefs.setTheme(t.name) } }
    }

    fun removeCustomTheme(name: String) { handleThemeRm(name, ui.value) }

    private fun handleThemeAdd(args: List<String>, cur: LauncherUi): String {
        val r = addCustomTheme(args, cur.customThemes) // the message; the write redoes it on the stored list
        if (r.list != null) viewModelScope.launch { prefs.editCustomThemes { stored -> addCustomTheme(args, stored).list ?: stored } }
        return r.message
    }

    private fun handleThemeRm(name: String, cur: LauncherUi): String {
        val r = removeCustomTheme(name, cur.customThemes, cur.theme.name)
        if (r.list != null) viewModelScope.launch {
            prefs.editCustomThemes { stored -> removeCustomTheme(name, stored, cur.theme.name).list ?: stored }
            if (r.wasActive) prefs.setTheme(ThemeRegistry.default.name)
        }
        return r.message
    }

    /** Atomic updates: the change is applied to the stored value inside one DataStore transaction. */
    fun updateFx(op: (FxSettings) -> FxSettings) { viewModelScope.launch { prefs.editFx(op) } }

    fun updateWall(op: (WallSettings) -> WallSettings) {
        viewModelScope.launch { prefs.editWall { w -> op(w).let { it.copy(dim = it.dim.coerceIn(0, 100)) } } }
    }

    fun setFx(name: String, on: Boolean) = updateFx { it.with(name, on).getOrDefault(it) }

    fun toggleFx(name: String) = updateFx { fx ->
        val now = when (name.lowercase()) {
            "flicker" -> fx.flicker
            "glitch" -> fx.glitch
            "cursor" -> fx.cursor
            "boot" -> fx.boot
            else -> return@updateFx fx
        }
        fx.with(name, !now).getOrDefault(fx)
    }

    /** `null` = follow the theme again. */
    fun setScanlines(value: Boolean?) = updateFx { it.withScanlines(value) }

    /** auto (theme) -> on -> off -> auto. */
    fun cycleScanlines() = updateFx { it.withScanlines(when (it.scanlines) { null -> true; true -> false; false -> null }) }

    fun setWall(w: WallSettings) = updateWall { w }

    fun setPanel(name: String, on: Boolean) = changePanels { applyPanel(it, name, on).getOrDefault(it) }

    fun togglePanel(name: String) = changePanels { p ->
        val now = when (name.lowercase()) { "cal" -> p.cal; "wx" -> p.wx; "search" -> p.search; "quick" -> p.quick; else -> return@changePanels p }
        applyPanel(p, name, !now).getOrDefault(p)
    }

    private fun changePanels(op: (PanelSettings) -> PanelSettings) {
        viewModelScope.launch {
            prefs.editPanels(op)?.let { (old, new) ->
                if (new.cal && !old.cal && !calendarRepo.hasPermission()) _permissionRequests.tryEmit(Manifest.permission.READ_CALENDAR)
                // Switching a panel on puts it back on the first page if it had been taken off every page.
                val turnedOn = listOfNotNull(
                    PanelKind.CAL.takeIf { new.cal && !old.cal }, PanelKind.WX.takeIf { new.wx && !old.wx }, PanelKind.SEARCH.takeIf { new.search && !old.search },
                )
                if (turnedOn.isNotEmpty()) prefs.editLayout { l ->
                    Result.success(turnedOn.fold(l) { acc, k -> if (k in acc.placedPanels()) acc else acc.addPanel(0, k) })
                }
            }
        }
    }

    fun setWxCity(name: String) { if (name.isNotBlank()) handleWx(WxAction.City(name.trim())) }
    fun setWxUnits(fahrenheit: Boolean) = handleWx(WxAction.Units(fahrenheit))

    private fun savePanels(cur: PanelSettings, next: PanelSettings) {
        viewModelScope.launch { prefs.setPanels(next) }
        if (next.cal && !cur.cal && !calendarRepo.hasPermission()) {
            _permissionRequests.tryEmit(Manifest.permission.READ_CALENDAR)
        }
    }

    // ---- panels ----

    private val calendarRepo = CalendarRepository(application)
    private val _permissionRequests = MutableSharedFlow<String>(extraBufferCapacity = 1)

    /** Runtime permissions the UI should ask for (currently READ_CALENDAR when `panel cal on` needs it). */
    val permissionRequests: SharedFlow<String> = _permissionRequests.asSharedFlow()

    fun onCalendarPermission(granted: Boolean) = refreshPanels()

    // ---- widgets ----

    /** Host for real Android widgets; the Activity starts/stops it and drives bind/configure results. */
    val widgets = WidgetHostController(application)

    /** A widget being added (host id already allocated). Lives here so it survives rotation of the Activity. */
    var pendingWidget: PendingWidget? = null

    /**
     * Adds [widget] to [page]. If the page vanished meanwhile (or the id is already used) the layout is left alone,
     * the freshly allocated host id is deleted so it does not leak, and the user gets a message.
     */
    fun addWidgetOrDelete(page: Int, widget: Item.Widget) {
        viewModelScope.launch {
            val result = prefs.editLayout { l ->
                if (page !in l.pages.indices || widget.id in l.widgetIds()) {
                    Result.failure(IllegalStateException("widget: not added (the page changed)"))
                } else {
                    Result.success(l.addWidget(page, widget))
                }
            }
            result.exceptionOrNull()?.let { widgets.delete(widget.id); say(it.message ?: "widget: not added") }
        }
    }
    fun removeWidget(id: Int) = edit { it.removeWidget(id) }
    fun resizeWidget(id: Int, size: WidgetSize) = edit { it.resizeWidget(id, size) }
    fun showMessage(text: String) = say(text)

    /** Drops widgets whose provider was uninstalled (skipped when the provider query failed or came back empty). */
    private suspend fun pruneMissingWidgets() {
        val providers = widgets.providersOrNull()?.map { it.provider }?.toSet()?.takeIf { it.isNotEmpty() } ?: return
        if (prefs.layout.first().pruneWidgets(providers).second.isEmpty()) return
        applyLayout { Result.success(it.pruneWidgets(providers).first) }
    }

    // ---- weather ----

    private val weatherRepo = WeatherRepository()
    private val weatherMutex = Mutex()
    private var lastWeatherAttemptMs: Long? = null

    /** The city or the units changed: the weather on screen is for the old ones, so drop it until the new forecast arrives. */
    private fun forgetShownWeather() = eph.update { it.copy(weather = PanelData.Message("wx: loading..."), weatherLine = null) }

    private fun say(message: String) = eph.update { it.withPrompt(it.prompt.copy(message = message)) }

    private fun handleWx(action: WxAction) {
        viewModelScope.launch {
            when (action) {
                WxAction.Refresh -> say(refreshWeather(force = true) ?: "wx: ok")
                is WxAction.Units -> {
                    prefs.setWx(prefs.wx.first().copy(fahrenheit = action.fahrenheit))
                    prefs.clearWxCache() // cached values are in the old unit
                    forgetShownWeather()
                    refreshWeather(force = true)?.let(::say)
                }
                is WxAction.City -> weatherRepo.geocode(action.name).fold(
                    onSuccess = { geo ->
                        prefs.setWx(prefs.wx.first().copy(city = geo.name, lat = geo.lat, lon = geo.lon))
                        prefs.setPanels(prefs.panels.first().copy(wx = true))
                        prefs.clearWxCache()
                        forgetShownWeather()
                        refreshWeather(force = true)?.let(::say)
                    },
                    onFailure = { e ->
                        say(if (e.message == "not found") "wx: city ${action.name}: not found" else "wx: offline")
                    },
                )
            }
        }
    }

    /**
     * Updates the weather panel from the cache and fetches when it is older than 30 minutes (or [force]).
     * Returns an error message for the output panel, or null. A failed fetch keeps the cached forecast.
     */
    private suspend fun refreshWeather(force: Boolean): String? = weatherMutex.withLock {
        if (!prefs.panels.first().wx) { eph.update { it.copy(weather = PanelData.Off, weatherLine = null) }; return@withLock null }
        val wx = prefs.wx.first()
        val lat = wx.lat
        val lon = wx.lon
        if (wx.city == null || lat == null || lon == null) {
            eph.update { it.copy(weather = PanelData.Message("wx: set a city: wx city <name>"), weatherLine = null) }
            return@withLock null
        }
        fun show(json: String, at: Long): Boolean = parseForecast(json, at).fold(
            onSuccess = { f ->
                eph.update {
                    it.copy(
                        weather = PanelData.Lines(formatWeather(wx.city, f, wx.fahrenheit, System.currentTimeMillis())),
                        weatherLine = weatherSummary(wx.city, f, wx.fahrenheit),
                    )
                }
                true
            },
            onFailure = { false },
        )
        val cache = prefs.wxCache.first()
        val shown = cache?.let { (json, at) -> show(json, at) } ?: false
        val now = System.currentTimeMillis()
        val fresh = shown && cache != null && now - cache.second < WEATHER_STALE_MS
        if (!shouldFetchWeather(force, fresh, now, lastWeatherAttemptMs)) return@withLock null
        lastWeatherAttemptMs = now
        weatherRepo.fetch(lat, lon, wx.fahrenheit).fold(
            onSuccess = { body ->
                if (show(body, now)) { prefs.setWxCache(body, now); null }
                else { if (!shown) eph.update { it.copy(weather = PanelData.Message("wx: bad data")) }; "wx: bad data" }
            },
            onFailure = { e ->
                if (!shown) eph.update { it.copy(weather = PanelData.Message("wx: offline")) }
                e.message ?: "wx: offline"
            },
        )
    }

    /** Recomputes the calendar panel (cheap; called on the minute tick, on resume and when settings change). */
    fun refreshPanels() {
        viewModelScope.launch {
            val panels = prefs.panels.first()
            val data: PanelData = when {
                !panels.cal -> PanelData.Off
                !calendarRepo.hasPermission() -> PanelData.Message("cal: no permission (grant in Settings)")
                else -> {
                    val now = System.currentTimeMillis()
                    val zone = ZoneId.systemDefault()
                    val (from, to) = calendarQueryWindow(now, zone, CAL_DAYS)
                    PanelData.Lines(formatAgenda(calendarRepo.events(from, to), now, zone, maxLines = CAL_LINES, h24 = h24()))
                }
            }
            eph.update { it.copy(calendar = data) }
            refreshWeather(force = false)
        }
    }

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
            is Command.Theme -> if (cmd.name == null) describeThemes(cur.theme.name, cur.customThemes) else ThemeBook(cur.customThemes).byName(cmd.name)?.let { t -> viewModelScope.launch { prefs.setTheme(t.name) }; null }
                ?: "theme: ${cmd.name}: no such theme (${ThemeBook(cur.customThemes).all.joinToString(" ") { it.name }})"
            is Command.ThemeAdd -> handleThemeAdd(cmd.args, cur)
            is Command.ThemeRm -> handleThemeRm(cmd.name, cur)
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
                cmd.auto ->
                    if (cmd.name.lowercase() == "scanlines") { viewModelScope.launch { prefs.setFx(cur.fx.withScanlines(null)) }; null }
                    else "fx: ${cmd.name}: auto is only for scanlines"
                cmd.on == null -> cur.fx.with(cmd.name, true).fold(
                    onSuccess = { "usage: fx <name> on|off" }, onFailure = { it.message },
                )
                else -> cur.fx.with(cmd.name, cmd.on).fold(
                    onSuccess = { next -> viewModelScope.launch { prefs.setFx(next) }; null },
                    onFailure = { it.message },
                )
            }
            is Command.Panel ->
                if (cmd.name == null) cur.panels.describe()
                else applyPanel(cur.panels, cmd.name, cmd.on).fold(
                    onSuccess = { next -> savePanels(cur.panels, next); null },
                    onFailure = { it.message },
                )
            is Command.Wx -> parseWxAction(cmd.sub, cmd.arg).fold(
                onSuccess = { action -> handleWx(action); null },
                onFailure = { it.message },
            )
            is Command.Layout -> handleLayout(cmd.arg, cur)
            is Command.Density -> if (cmd.arg == null) "density  ${cur.density.name.lowercase()} (${cur.density.columns} columns)"
                else densityPlan(cmd.arg).let { plan -> plan.density?.let { setDensity(it) }; plan.message }
            is Command.Clock -> handleClock(cmd.sub, cmd.arg, cur)
            is Command.Quick -> parseQuickAction(cmd.sub, cmd.arg).fold(
                onSuccess = { action -> handleQuick(action, cur) },
                onFailure = { it.message },
            )
            is Command.Settings -> { _settingsRequests.tryEmit(Unit); null }
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
            is Command.Date -> (if (h24()) DATE_FORMAT else DATE_FORMAT_12).format(LocalDateTime.now()).lowercase()
            is Command.Fortune -> fortune(System.nanoTime().toInt())
            is Command.Clear -> null
            is Command.Matrix -> { startMatrix(); null }
            is Command.Wake -> { startWake(); null }
            is Command.Rabbit -> RABBIT
            is Command.Spoon -> SPOON
            is Command.Pill -> pillPlan(cmd.arg).let { plan ->
                plan.theme?.let { t -> viewModelScope.launch { prefs.setTheme(t) } }
                plan.wallMode?.let { m -> updateWall { it.copy(mode = m) } }
                plan.message
            }
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
    fun startWake() = eph.update { it.copy(wake = true) }
    fun stopWake() = eph.update { it.copy(wake = false) }

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

    fun openDrawer(showHidden: Boolean = false) {
        eph.update { it.copy(drawerOpen = true, showHidden = showHidden, filter = "", extras = emptyList()) }
    }
    fun closeDrawer() = eph.update { it.copy(drawerOpen = false, showHidden = false, filter = "", extras = emptyList()) }

    private val contactsRepo = ContactsRepository(application)
    private var extrasJob: Job? = null

    fun setFilter(filter: String) {
        // The settings and web rows follow the text at once; the contacts arrive a moment later.
        eph.update { it.copy(filter = filter, extras = buildExtras(filter, emptyList(), contactsRepo.hasPermission())) }
        refreshExtras()
    }

    /** Looks up contacts (off the main thread) and builds the settings/web rows for the current search text. */
    fun refreshExtras() {
        extrasJob?.cancel()
        val query = eph.value.filter
        if (query.trim().length < 2) return
        extrasJob = viewModelScope.launch {
            val allowed = contactsRepo.hasPermission()
            val contacts = if (allowed) withContext(Dispatchers.IO) { contactsRepo.search(query) } else emptyList()
            eph.update { if (it.filter == query) it.copy(extras = buildExtras(query, contacts, allowed)) else it }
        }
    }

    fun askForContacts() { _permissionRequests.tryEmit(Manifest.permission.READ_CONTACTS) }
    fun setSort(sort: DrawerSort) { viewModelScope.launch { prefs.setSort(sort) } }
    fun setClockFormat(f: ClockFormat) { viewModelScope.launch { prefs.setClockFormat(f) } }
    fun toggleDoubleTapLock() { viewModelScope.launch { prefs.setDoubleTapLock(!ui.value.doubleTapLock) } }
    fun toggleBadges() { viewModelScope.launch { prefs.setBadges(!ui.value.badges) } }
    fun setDensity(d: Density) { viewModelScope.launch { prefs.setDensity(d) } }
    fun cycleDensity() { setDensity(Density.entries.let { it[(it.indexOf(ui.value.density) + 1) % it.size] }) }
    fun setHomeStyle(s: HomeStyle) { viewModelScope.launch { prefs.setHomeStyle(s) } }
    fun cycleHomeStyle() { setHomeStyle(HomeStyle.entries.let { it[(it.indexOf(ui.value.homeStyle) + 1) % it.size] }) }

    private fun handleLayout(arg: String?, cur: LauncherUi): String {
        if (arg == null) return "layout  ${cur.homeStyle.name.lowercase()}"
        val plan = layoutPlan(arg)
        plan.style?.let { style ->
            setHomeStyle(style)
            when (plan.restructure) {
                Restructure.APPS_FIRST -> edit { it.appsFirst() }
                Restructure.CLASSIC -> edit { it.panelsHome() }
                Restructure.NONE -> {}
            }
        }
        return plan.message
    }

    fun setClockStyle(s: ClockStyle) { viewModelScope.launch { prefs.setClockStyle(s) } }
    fun cycleClockStyle() { setClockStyle(ClockStyle.entries.let { it[(it.indexOf(ui.value.clockStyle) + 1) % it.size] }) }
    fun cycleClockFormat() { setClockFormat(ClockFormat.entries.let { it[(it.indexOf(ui.value.clockFormat) + 1) % it.size] }) }

    /** Whether times are drawn in 24-hour form right now (the setting, or the phone's own when it is `auto`). */
    private fun h24(): Boolean = ui.value.clockFormat.is24(DateFormat.is24HourFormat(getApplication()))

    private fun handleClock(sub: String?, arg: String?, cur: LauncherUi): String? {
        if (sub == null) return clockFormatReply(null, null, cur.clockFormat).text + "\n" + clockStyleReply(null, cur.clockStyle).text
        if (sub == "style") {
            val styleReply = clockStyleCommand(arg, cur.clockStyle)
            styleReply.set?.let { setClockStyle(it) }
            return styleReply.text
        }
        val reply = clockFormatReply(sub, arg, cur.clockFormat)
        reply.set?.let { setClockFormat(it) }
        return reply.text
    }

    suspend fun defaultHomeAsked(): Boolean = prefs.askedDefaultHome.first()
    fun markDefaultHomeAsked() { viewModelScope.launch { prefs.setAskedDefaultHome() } }
    fun toggleDrawerKeyboard() { viewModelScope.launch { prefs.setDrawerKeyboard(!ui.value.drawerKeyboard) } }
    fun setIconMode(mode: IconMode) { viewModelScope.launch { prefs.setIconMode(mode) } }
    fun toggleHidden(pkg: String) {
        val h = ui.value.hidden
        viewModelScope.launch { prefs.setHidden(if (pkg in h) h - pkg else h + pkg) }
    }
    fun dismissMessage() = eph.update { it.withPrompt(it.prompt.copy(message = null)) }

    // ---- layout operations (used by UI directly) ----

    fun pin(pkg: String) { ui.value.apps[pkg]?.let(::pinApp) }
    fun unpin(pkg: String) = edit { it.unpin(pkg) }
    fun addPanelTo(page: Int, kind: PanelKind) = edit { it.addPanel(page, kind) }
    fun movePanel(kind: PanelKind, delta: Int) = edit { it.movePanel(kind, delta) }
    fun movePanelToPage(kind: PanelKind, page: Int) = edit { it.movePanelToPage(kind, page) }
    fun removePanelItem(kind: PanelKind) = edit { it.removePanel(kind) }
    fun moveItemToPage(fromPage: Int, item: Item, toPage: Int, slot: Int) = edit { it.moveItemToPage(fromPage, item, toPage, slot) }
    fun moveItemToDock(page: Int, item: Item) = edit { it.moveToDock(page, item) }
    fun placeItem(page: Int, item: Item, slot: Int) = edit { it.placeItem(page, item, slot) }
    fun addPage() = edit { it.addPage() }
    fun removePage(index: Int) = edit { it.removePage(index) }
    fun movePage(from: Int, to: Int) = edit { it.movePage(from, to) }
    fun addToPage(page: Int, item: Item) = edit { it.addToPage(page, item) }
    fun rmdir(name: String) = edit { it.rmdir(name) }
    fun unfile(pkg: String) = edit { it.unfile(pkg) }
    fun pinFolder(name: String) = edit { it.pinFolder(name) }
    fun unpinFolder(name: String) = edit { it.unpinFolder(name) }
    fun mkdirOnPage(name: String, page: Int) = applyLayout { it.mkdir(name, page) }
    /** An app dropped on another icon: a new folder with both, or the app goes into the folder it landed on. */
    fun mergeOnto(fromPage: Int, item: Item.App, toPage: Int, slot: Int) {
        val layout = ui.value.layout
        val target = layout.pages.getOrNull(toPage)?.items?.getOrNull(slot)
        if (target is Item.App) say("folder '${layout.newFolderName()}' created: tap it to rename")
        edit { it.mergeOnto(fromPage, item, toPage, slot) }
    }
    fun renameFolder(old: String, new: String) = applyLayout { it.renameFolder(old, new) }
    fun addAt(page: Int, pkg: String, slot: Int) = edit { it.addAt(page, pkg, slot) }
    fun addToHome(pkg: String) = edit { it.addToHome(pkg) }
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
        if (next == l) Result.failure(IllegalStateException(l.pinProblem(app.pkg, app.label)))
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

    // Kept last on purpose: the coroutine below may run at once and must find every property above initialized.
    init {
        // Whenever the panel switches change (including the first load), refresh the panels.
        viewModelScope.launch { prefs.panels.distinctUntilChanged().collect { refreshPanels() } }
        // Host ids that left the stored layout (widget or page removed, provider pruned) are deleted exactly once.
        viewModelScope.launch {
            var previous: Set<Int>? = null
            prefs.layout.map { it.widgetIds() }.distinctUntilChanged().collect { current ->
                previous?.let { gone -> (gone - current).forEach(widgets::delete) }
                previous = current
            }
        }
        viewModelScope.launch { repo.apps.collect { pruneMissingWidgets() } }
        // First run: familiar apps instead of an empty home, once, as soon as the app list is known.
        viewModelScope.launch {
            val installed = repo.apps.first { it.isNotEmpty() }.map { it.pkg }.toSet()
            if (prefs.seedLayout(installed)) say(FIRST_RUN_HINT)
        }
    }

    companion object {
        const val LAUNCHER_VERSION = "1.0.6"
        const val FIRST_RUN_HINT = "long-press empty space to edit pages | swipe up for all apps"
        private const val WEATHER_STALE_MS = 30L * 60 * 1000
        private const val CAL_DAYS = 30L
        private const val CAL_LINES = 40
        val DATE_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("EEE d MMM yyyy HH:mm:ss", Locale.ENGLISH)
        val DATE_FORMAT_12: DateTimeFormatter = DateTimeFormatter.ofPattern("EEE d MMM yyyy hh:mm:ss a", Locale.ENGLISH)
        val COMMANDS = listOf(
            "theme", "pin", "hide", "mkdir", "mv", "rmdir", "ls", "help", "fx", "history",
            "neofetch", "whoami", "uptime", "date", "fortune", "clear", "sudo", "matrix", "wall", "panel", "wx", "settings", "quick", "clock", "pill", "layout", "density",
        )
        val HELP = listOf(
            "theme <n>|add <n> bg fg acc dim|rm <n>",
            "clock [auto|24|12|style <name>]",
            "pin|hide <app>  ls [-a]  dock/hidden",
            "mkdir|rmdir <f>  mv <app>|<f> <f|new>",
            "fx [n on|off]  wall <sub> [arg]",
            "panel <p> on|off  cal wx search quick",
            "wx city|units <v>  quick add|rm <app>",
            "history neofetch whoami uptime date",
            "fortune clear help settings sudo",
            "matrix [wake]  pill red|blue",
            "layout classic|bottom|grid|icons|apps",
            "density dense|compact|normal|roomy",
        ).joinToString("\n")
    }
}
