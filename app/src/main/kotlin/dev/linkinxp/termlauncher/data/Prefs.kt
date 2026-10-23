package dev.linkinxp.termlauncher.data

import android.content.Context
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dev.linkinxp.termlauncher.apps.AppStat
import dev.linkinxp.termlauncher.apps.DrawerSort
import dev.linkinxp.termlauncher.fx.FxSettings
import dev.linkinxp.termlauncher.layout.HomeLayout
import dev.linkinxp.termlauncher.prompt.CommandHistory
import dev.linkinxp.termlauncher.theme.Theme
import dev.linkinxp.termlauncher.wall.WallSettings
import dev.linkinxp.termlauncher.theme.ThemeRegistry
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
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
    private val wallAnimateKey = booleanPreferencesKey("wall_animate")
    private val fxScanKey = booleanPreferencesKey("fx_scanlines")
    private val fxFlickerKey = booleanPreferencesKey("fx_flicker")
    private val fxGlitchKey = booleanPreferencesKey("fx_glitch")
    private val fxCursorKey = booleanPreferencesKey("fx_cursor")
    private val fxBootKey = booleanPreferencesKey("fx_boot")
    private val statsSerializer = MapSerializer(String.serializer(), AppStat.serializer())

    private val data: Flow<Preferences> get() = context.store.data.catch { e ->
        if (e is IOException) emit(emptyPreferences()) else throw e
    }

    val theme: Flow<Theme> = data.map { p -> p[themeKey]?.let(ThemeRegistry::byName) ?: ThemeRegistry.default }
    val layout: Flow<HomeLayout> = data.map { HomeLayout.decode(it[layoutKey]) }
    val hidden: Flow<Set<String>> = data.map { it[hiddenKey] ?: emptySet() }
    val sort: Flow<DrawerSort> = data.map { p -> enumOr(p[sortKey], DrawerSort.AZ) }
    val iconMode: Flow<IconMode> = data.map { p -> enumOr(p[iconKey], IconMode.TEXT) }
    val fx: Flow<FxSettings> = data.map { p ->
        val d = FxSettings()
        FxSettings(
            scanlines = p[fxScanKey], flicker = p[fxFlickerKey] ?: d.flicker, glitch = p[fxGlitchKey] ?: d.glitch,
            cursor = p[fxCursorKey] ?: d.cursor, boot = p[fxBootKey] ?: d.boot,
        )
    }
    val wall: Flow<WallSettings> = data.map { p ->
        val d = WallSettings()
        WallSettings(
            mode = enumOr(p[wallModeKey], d.mode),
            style = enumOr(p[wallStyleKey], d.style),
            seed = p[wallSeedKey] ?: d.seed,
            dim = p[wallDimKey]?.takeIf { it in 0..100 } ?: d.dim,
            animate = p[wallAnimateKey] ?: d.animate,
        )
    }
    val history: Flow<CommandHistory> = data.map { CommandHistory.decode(it[historyKey]) }
    val stats: Flow<Map<String, AppStat>> = data.map { decodeStats(it[statsKey]) }

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

    suspend fun setWall(w: WallSettings) {
        safeEdit { p ->
            p[wallModeKey] = w.mode.name
            p[wallStyleKey] = w.style.name
            p[wallSeedKey] = w.seed
            p[wallDimKey] = w.dim
            p[wallAnimateKey] = w.animate
        }
    }

    suspend fun setFx(fx: FxSettings) {
        safeEdit { p ->
            fx.scanlines?.let { p[fxScanKey] = it } ?: p.remove(fxScanKey)
            p[fxFlickerKey] = fx.flicker
            p[fxGlitchKey] = fx.glitch
            p[fxCursorKey] = fx.cursor
            p[fxBootKey] = fx.boot
        }
    }

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

    private suspend fun safeEdit(block: (androidx.datastore.preferences.core.MutablePreferences) -> Unit) {
        try { context.store.edit(block) } catch (_: IOException) {}
    }

    private fun decodeStats(raw: String?): Map<String, AppStat> =
        try { raw?.let { Json.decodeFromString(statsSerializer, it) } ?: emptyMap() } catch (e: Exception) { emptyMap() }

    private inline fun <reified E : Enum<E>> enumOr(name: String?, default: E): E =
        enumValues<E>().firstOrNull { it.name == name } ?: default
}
