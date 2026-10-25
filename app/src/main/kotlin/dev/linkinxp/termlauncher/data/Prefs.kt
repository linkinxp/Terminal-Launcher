package dev.linkinxp.termlauncher.data

import dev.linkinxp.termlauncher.clock.ClockFormat
import dev.linkinxp.termlauncher.clock.ClockStyle
import dev.linkinxp.termlauncher.layout.Density
import dev.linkinxp.termlauncher.layout.HomeStyle
import android.content.Context
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dev.linkinxp.termlauncher.apps.AppStat
import dev.linkinxp.termlauncher.apps.DrawerSort
import dev.linkinxp.termlauncher.fx.FxSettings
import dev.linkinxp.termlauncher.layout.HomeLayout
import dev.linkinxp.termlauncher.panel.PanelSettings
import dev.linkinxp.termlauncher.prompt.CommandHistory
import dev.linkinxp.termlauncher.quick.MAX_QUICK
import dev.linkinxp.termlauncher.quick.decodeQuick
import dev.linkinxp.termlauncher.quick.encodeQuick
import dev.linkinxp.termlauncher.theme.Theme
import dev.linkinxp.termlauncher.weather.WxSettings
import dev.linkinxp.termlauncher.wall.SvgStyle
import dev.linkinxp.termlauncher.wall.WallSettings
import dev.linkinxp.termlauncher.theme.CustomThemes
import dev.linkinxp.termlauncher.theme.ThemeBook
import dev.linkinxp.termlauncher.theme.ThemeRegistry
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json

enum class IconMode { TEXT, GLYPH, ICON }

private val Context.store by preferencesDataStore(
    "terminal_launcher",
    corruptionHandler = ReplaceFileCorruptionHandler { emptyPreferences() },
)

/** All persisted launcher state. Any missing or unparsable value falls back to its default. */
class Prefs(private val context: Context) {
    private val themeKey = stringPreferencesKey("theme")
    private val layoutKey = stringPreferencesKey("layout")
    private val hiddenKey = stringSetPreferencesKey("hidden")
    private val sortKey = stringPreferencesKey("sort")
    private val iconKey = stringPreferencesKey("icons")
    private val statsKey = stringPreferencesKey("stats")
    private val historyKey = stringPreferencesKey("history")
    private val wallModeKey = stringPreferencesKey("wall_mode")
    private val wallStyleKey = stringPreferencesKey("wall_style")
    private val wallSeedKey = intPreferencesKey("wall_seed")
    private val wallDimKey = intPreferencesKey("wall_dim")
    private val wallSvgKey = stringPreferencesKey("wall_svg")
    private val wallAnimateKey = booleanPreferencesKey("wall_animate")
    private val wxCityKey = stringPreferencesKey("wx_city")
    private val wxLatKey = doublePreferencesKey("wx_lat")
    private val wxLonKey = doublePreferencesKey("wx_lon")
    private val wxFahrenheitKey = booleanPreferencesKey("wx_fahrenheit")
    private val wxCacheKey = stringPreferencesKey("wx_cache")
    private val wxCacheAtKey = longPreferencesKey("wx_cache_at")
    private val panelCalKey = booleanPreferencesKey("panel_cal")
    private val panelWxKey = booleanPreferencesKey("panel_wx")
    private val panelSearchKey = booleanPreferencesKey("panel_search")
    private val panelQuickKey = booleanPreferencesKey("panel_quick")
    private val quickKey = stringPreferencesKey("quick")
    private val clockFormatKey = stringPreferencesKey("clock_format")
    private val customThemesKey = stringPreferencesKey("custom_themes")
    private val doubleTapLockKey = booleanPreferencesKey("double_tap_lock")
    private val badgesKey = booleanPreferencesKey("badges")
    private val densityKey = stringPreferencesKey("density")
    private val homeStyleKey = stringPreferencesKey("home_style")
    private val clockStyleKey = stringPreferencesKey("clock_style")
    private val drawerKeyboardKey = booleanPreferencesKey("drawer_keyboard")
    private val fxScanKey = booleanPreferencesKey("fx_scanlines")
    private val fxFlickerKey = booleanPreferencesKey("fx_flicker")
    private val fxGlitchKey = booleanPreferencesKey("fx_glitch")
    private val fxCursorKey = booleanPreferencesKey("fx_cursor")
    private val fxBootKey = booleanPreferencesKey("fx_boot")
    private val statsSerializer = MapSerializer(String.serializer(), AppStat.serializer())

    private val data: Flow<Preferences> get() = context.store.data.catch { e ->
        if (e is IOException) emit(emptyPreferences()) else throw e
    }

    val customThemes: Flow<List<Theme>> = data.map { CustomThemes.decode(it[customThemesKey]) }.distinctUntilChanged()
    val theme: Flow<Theme> = data.map { p ->
        p[themeKey]?.let { ThemeBook(CustomThemes.decode(p[customThemesKey])).byName(it) } ?: ThemeRegistry.default
    }.distinctUntilChanged()
    // Raw strings are compared first, so unrelated writes never trigger a JSON decode or a downstream rebuild.
    val layout: Flow<HomeLayout> = data.map { it[layoutKey] }.distinctUntilChanged().map { HomeLayout.decode(it) }
    val hidden: Flow<Set<String>> = data.map { it[hiddenKey] ?: emptySet() }.distinctUntilChanged()
    val sort: Flow<DrawerSort> = data.map { p -> enumOr(p[sortKey], DrawerSort.AZ) }.distinctUntilChanged()
    val iconMode: Flow<IconMode> = data.map { p -> enumOr(p[iconKey], IconMode.TEXT) }.distinctUntilChanged()
    val fx: Flow<FxSettings> = data.map(::fxFrom).distinctUntilChanged()
    val wall: Flow<WallSettings> = data.map(::wallFrom).distinctUntilChanged()
    val wx: Flow<WxSettings> = data.map { p ->
        WxSettings(p[wxCityKey], p[wxLatKey], p[wxLonKey], p[wxFahrenheitKey] ?: false)
    }.distinctUntilChanged()
    /** Last good forecast body and when it was fetched, or null. */
    val wxCache: Flow<Pair<String, Long>?> = data.map { p ->
        val json = p[wxCacheKey]
        val at = p[wxCacheAtKey]
        if (json != null && at != null) json to at else null
    }.distinctUntilChanged()
    val panels: Flow<PanelSettings> = data.map(::panelsFrom).distinctUntilChanged()
    /** The quick tiles (package names) the user set, or null when never set (then the device defaults apply). */
    val quick: Flow<List<String>?> = data.map { decodeQuick(it[quickKey]) }.distinctUntilChanged()
    val clockFormat: Flow<ClockFormat> = data.map { ClockFormat.fromName(it[clockFormatKey]) }.distinctUntilChanged()
    val doubleTapLock: Flow<Boolean> = data.map { it[doubleTapLockKey] ?: true }.distinctUntilChanged()
    val badges: Flow<Boolean> = data.map { it[badgesKey] ?: true }.distinctUntilChanged()
    val density: Flow<Density> = data.map { Density.fromName(it[densityKey]) }.distinctUntilChanged()
    val homeStyle: Flow<HomeStyle> = data.map { HomeStyle.fromStored(it[homeStyleKey]) }.distinctUntilChanged()
    val clockStyle: Flow<ClockStyle> = data.map { ClockStyle.fromName(it[clockStyleKey]) }.distinctUntilChanged()
    val drawerKeyboard: Flow<Boolean> = data.map { it[drawerKeyboardKey] ?: true }.distinctUntilChanged()
    val history: Flow<CommandHistory> = data.map { it[historyKey] }.distinctUntilChanged().map { CommandHistory.decode(it) }
    val stats: Flow<Map<String, AppStat>> = data.map { it[statsKey] }.distinctUntilChanged().map { decodeStats(it) }

    /** Atomic edit of the user's own themes (kept to at most [CustomThemes.MAX]). */
    suspend fun editCustomThemes(op: (List<Theme>) -> List<Theme>) {
        safeEdit { p -> p[customThemesKey] = CustomThemes.encode(op(CustomThemes.decode(p[customThemesKey])).take(CustomThemes.MAX)) }
    }
    suspend fun setTheme(name: String) { safeEdit { it[themeKey] = name } }
    /**
     * Atomically applies [op] to the stored layout (read-modify-write inside one DataStore transaction,
     * so rapid edits cannot overwrite each other and an unloaded layout is never replaced by the default).
     * A failed [op] leaves the layout untouched and its failure is returned.
     */
    suspend fun editLayout(op: (HomeLayout) -> Result<HomeLayout>): Result<HomeLayout> {
        var result: Result<HomeLayout> = Result.failure(IllegalStateException("settings unavailable"))
        try {
            context.store.edit { p ->
                result = op(HomeLayout.decode(p[layoutKey]))
                result.onSuccess { p[layoutKey] = it.encode() }
            }
        } catch (_: IOException) {
        }
        return result
    }
    suspend fun setHidden(hidden: Set<String>) { safeEdit { it[hiddenKey] = hidden } }
    suspend fun setSort(sort: DrawerSort) { safeEdit { it[sortKey] = sort.name } }
    suspend fun setIconMode(mode: IconMode) { safeEdit { it[iconKey] = mode.name } }

    suspend fun setWall(w: WallSettings) { safeEdit { writeWall(it, w) } }

    /** Atomic read-modify-write of the wallpaper settings: rapid edits never overwrite each other. */
    suspend fun editWall(op: (WallSettings) -> WallSettings) { safeEdit { p -> writeWall(p, op(wallFrom(p))) } }

    suspend fun setWx(wx: WxSettings) {
        safeEdit { p ->
            wx.city?.let { p[wxCityKey] = it } ?: p.remove(wxCityKey)
            wx.lat?.let { p[wxLatKey] = it } ?: p.remove(wxLatKey)
            wx.lon?.let { p[wxLonKey] = it } ?: p.remove(wxLonKey)
            p[wxFahrenheitKey] = wx.fahrenheit
        }
    }

    suspend fun setWxCache(json: String, at: Long) {
        safeEdit { p -> p[wxCacheKey] = json; p[wxCacheAtKey] = at }
    }

    suspend fun clearWxCache() {
        safeEdit { p -> p.remove(wxCacheKey); p.remove(wxCacheAtKey) }
    }

    suspend fun setPanels(panels: PanelSettings) { safeEdit { writePanels(it, panels) } }

    /** Atomic read-modify-write of the panel switches; returns the (old, new) pair that was applied, or null on I/O failure. */
    suspend fun editPanels(op: (PanelSettings) -> PanelSettings): Pair<PanelSettings, PanelSettings>? {
        var result: Pair<PanelSettings, PanelSettings>? = null
        safeEdit { p ->
            val old = panelsFrom(p)
            val new = op(old)
            writePanels(p, new)
            result = old to new
        }
        return result
    }

    /** `null` removes the stored list so the defaults apply again. */
    suspend fun setQuick(list: List<String>?) {
        safeEdit { p -> if (list == null) p.remove(quickKey) else p[quickKey] = encodeQuick(list.take(MAX_QUICK)) }
    }

    /** Atomic read-modify-write of the quick list ([op] gets the stored list or null, returns the new one or null). */
    suspend fun editQuick(op: (List<String>?) -> List<String>?) {
        safeEdit { p ->
            val next = op(decodeQuick(p[quickKey]))
            if (next == null) p.remove(quickKey) else p[quickKey] = encodeQuick(next.take(MAX_QUICK))
        }
    }

    suspend fun setClockFormat(f: ClockFormat) { safeEdit { it[clockFormatKey] = f.name } }
    suspend fun setDoubleTapLock(on: Boolean) { safeEdit { it[doubleTapLockKey] = on } }
    suspend fun setBadges(on: Boolean) { safeEdit { it[badgesKey] = on } }
    suspend fun setDensity(d: Density) { safeEdit { it[densityKey] = d.name } }
    suspend fun setHomeStyle(s: HomeStyle) { safeEdit { it[homeStyleKey] = s.name } }
    suspend fun setClockStyle(s: ClockStyle) { safeEdit { it[clockStyleKey] = s.name } }
    suspend fun setDrawerKeyboard(on: Boolean) { safeEdit { it[drawerKeyboardKey] = on } }

    suspend fun setFx(fx: FxSettings) { safeEdit { writeFx(it, fx) } }

    /** Atomic read-modify-write of the effect switches: rapid edits never overwrite each other. */
    suspend fun editFx(op: (FxSettings) -> FxSettings) { safeEdit { p -> writeFx(p, op(fxFrom(p))) } }

    /** Appends [line] to the persisted command history (atomic; blanks and consecutive duplicates are dropped). */
    suspend fun addHistory(line: String) {
        safeEdit { p ->
            val h = CommandHistory.decode(p[historyKey])
            h.add(line)
            p[historyKey] = h.encode()
        }
    }

    suspend fun recordLaunch(pkg: String, now: Long = System.currentTimeMillis()) {
        safeEdit { p ->
            val stats = decodeStats(p[statsKey]).toMutableMap()
            stats[pkg] = AppStat((stats[pkg]?.launches ?: 0) + 1, now)
            p[statsKey] = Json.encodeToString(statsSerializer, stats)
        }
    }

    private suspend fun safeEdit(block: (MutablePreferences) -> Unit) {
        try { context.store.edit(block) } catch (_: IOException) {}
    }

    private fun fxFrom(p: Preferences): FxSettings {
        val d = FxSettings()
        return FxSettings(
            scanlines = p[fxScanKey], flicker = p[fxFlickerKey] ?: d.flicker, glitch = p[fxGlitchKey] ?: d.glitch,
            cursor = p[fxCursorKey] ?: d.cursor, boot = p[fxBootKey] ?: d.boot,
        )
    }

    private fun writeFx(p: MutablePreferences, fx: FxSettings) {
        fx.scanlines?.let { p[fxScanKey] = it } ?: p.remove(fxScanKey)
        p[fxFlickerKey] = fx.flicker
        p[fxGlitchKey] = fx.glitch
        p[fxCursorKey] = fx.cursor
        p[fxBootKey] = fx.boot
    }

    private fun wallFrom(p: Preferences): WallSettings {
        val d = WallSettings()
        return WallSettings(
            mode = enumOr(p[wallModeKey], d.mode),
            style = enumOr(p[wallStyleKey], d.style),
            seed = p[wallSeedKey] ?: d.seed,
            dim = p[wallDimKey]?.takeIf { it in 0..100 } ?: d.dim,
            animate = p[wallAnimateKey] ?: d.animate,
            svgStyle = SvgStyle.fromName(p[wallSvgKey]),
        )
    }

    private fun writeWall(p: MutablePreferences, w: WallSettings) {
        p[wallModeKey] = w.mode.name
        p[wallStyleKey] = w.style.name
        p[wallSeedKey] = w.seed
        p[wallDimKey] = w.dim
        p[wallAnimateKey] = w.animate
        p[wallSvgKey] = w.svgStyle.name
    }

    private fun panelsFrom(p: Preferences) =
        PanelSettings(
            cal = p[panelCalKey] ?: false, wx = p[panelWxKey] ?: false,
            search = p[panelSearchKey] ?: true, quick = p[panelQuickKey] ?: true,
        )

    private fun writePanels(p: MutablePreferences, panels: PanelSettings) {
        p[panelCalKey] = panels.cal
        p[panelWxKey] = panels.wx
        p[panelSearchKey] = panels.search
        p[panelQuickKey] = panels.quick
    }

    private fun decodeStats(raw: String?): Map<String, AppStat> =
        try { raw?.let { Json.decodeFromString(statsSerializer, it) } ?: emptyMap() } catch (e: Exception) { emptyMap() }

    private inline fun <reified E : Enum<E>> enumOr(name: String?, default: E): E =
        enumValues<E>().firstOrNull { it.name == name } ?: default
}
