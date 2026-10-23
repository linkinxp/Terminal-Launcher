package dev.linkinxp.termlauncher.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dev.linkinxp.termlauncher.apps.AppStat
import dev.linkinxp.termlauncher.apps.DrawerSort
import dev.linkinxp.termlauncher.layout.HomeLayout
import dev.linkinxp.termlauncher.theme.Theme
import dev.linkinxp.termlauncher.theme.ThemeRegistry
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json

enum class IconMode { TEXT, GLYPH, ICON }

private val Context.store by preferencesDataStore("terminal_launcher")

/** All persisted launcher state. Any missing or unparsable value falls back to its default. */
class Prefs(private val context: Context) {
    private val themeKey = stringPreferencesKey("theme")
    private val layoutKey = stringPreferencesKey("layout")
    private val hiddenKey = stringSetPreferencesKey("hidden")
    private val sortKey = stringPreferencesKey("sort")
    private val iconKey = stringPreferencesKey("icons")
    private val statsKey = stringPreferencesKey("stats")
    private val statsSerializer = MapSerializer(String.serializer(), AppStat.serializer())

    private val data: Flow<Preferences> get() = context.store.data

    val theme: Flow<Theme> = data.map { p -> p[themeKey]?.let(ThemeRegistry::byName) ?: ThemeRegistry.default }
    val layout: Flow<HomeLayout> = data.map { HomeLayout.decode(it[layoutKey]) }
    val hidden: Flow<Set<String>> = data.map { it[hiddenKey] ?: emptySet() }
    val sort: Flow<DrawerSort> = data.map { p -> enumOr(p[sortKey], DrawerSort.AZ) }
    val iconMode: Flow<IconMode> = data.map { p -> enumOr(p[iconKey], IconMode.TEXT) }
    val stats: Flow<Map<String, AppStat>> = data.map { decodeStats(it[statsKey]) }

    suspend fun setTheme(name: String) { context.store.edit { it[themeKey] = name } }
    suspend fun setLayout(layout: HomeLayout) { context.store.edit { it[layoutKey] = layout.encode() } }
    suspend fun setHidden(hidden: Set<String>) { context.store.edit { it[hiddenKey] = hidden } }
    suspend fun setSort(sort: DrawerSort) { context.store.edit { it[sortKey] = sort.name } }
    suspend fun setIconMode(mode: IconMode) { context.store.edit { it[iconKey] = mode.name } }

    suspend fun recordLaunch(pkg: String, now: Long = System.currentTimeMillis()) {
        context.store.edit { p ->
            val stats = decodeStats(p[statsKey]).toMutableMap()
            stats[pkg] = AppStat((stats[pkg]?.launches ?: 0) + 1, now)
            p[statsKey] = Json.encodeToString(statsSerializer, stats)
        }
    }

    private fun decodeStats(raw: String?): Map<String, AppStat> =
        try { raw?.let { Json.decodeFromString(statsSerializer, it) } ?: emptyMap() } catch (e: Exception) { emptyMap() }

    private inline fun <reified E : Enum<E>> enumOr(name: String?, default: E): E =
        enumValues<E>().firstOrNull { it.name == name } ?: default
}
