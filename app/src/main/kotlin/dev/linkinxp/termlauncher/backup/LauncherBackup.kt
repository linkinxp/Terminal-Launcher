package dev.linkinxp.termlauncher.backup

import dev.linkinxp.termlauncher.layout.HomeLayout
import dev.linkinxp.termlauncher.layout.Item
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

private const val FORMAT = "terminal-launcher-backup"
private const val VERSION = 1
private const val NOT_A_BACKUP = "import: not a launcher backup"

@Serializable data class FxBackup(
    val scanlines: Boolean? = null,
    val flicker: Boolean = false,
    val glitch: Boolean = true,
    val cursor: Boolean = true,
    val boot: Boolean = true,
)

@Serializable data class WallBackup(
    val mode: String = "SOLID",
    val style: String = "GLYPH",
    val seed: Int = 1,
    val dim: Int = 55,
    val animate: Boolean = false,
)

@Serializable data class PanelBackup(val cal: Boolean = false, val wx: Boolean = false, val search: Boolean = true)

@Serializable data class WxBackup(
    val city: String? = null,
    val lat: Double? = null,
    val lon: Double? = null,
    val fahrenheit: Boolean = false,
)

/** Everything worth carrying to a new install: layout, hidden apps and every setting. Widgets are never included. */
@Serializable data class LauncherBackup(
    val format: String = FORMAT,
    val version: Int = VERSION,
    val layout: HomeLayout = HomeLayout.default(),
    val hidden: Set<String> = emptySet(),
    val theme: String = "classy-black",
    val fx: FxBackup = FxBackup(),
    val wall: WallBackup = WallBackup(),
    val panels: PanelBackup = PanelBackup(),
    val wx: WxBackup = WxBackup(),
    val drawerSort: String = "AZ",
    val iconMode: String = "TEXT",
)

private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true; prettyPrint = true }

/** JSON for [b]; widgets are removed first because their host ids mean nothing on another install. */
fun encodeBackup(b: LauncherBackup): String =
    json.encodeToString(LauncherBackup.serializer(), b.copy(layout = b.layout.withoutWidgets()))

/**
 * Parses a backup. Anything that is not a backup of this app fails with `import: not a launcher backup`; a
 * newer format version fails with `import: backup is from a newer version`. Missing sections use defaults.
 */
fun decodeBackup(text: String?): Result<LauncherBackup> {
    fun fail(msg: String) = Result.failure<LauncherBackup>(IllegalArgumentException(msg))
    if (text.isNullOrBlank()) return fail(NOT_A_BACKUP)
    return try {
        // The format marker must really be present: a bare `{}` must not decode to an "empty backup".
        val marker = json.parseToJsonElement(text).jsonObject["format"]?.jsonPrimitive?.content
        if (marker != FORMAT) return fail(NOT_A_BACKUP)
        val backup = json.decodeFromString(LauncherBackup.serializer(), text)
        if (backup.version > VERSION) fail("import: backup is from a newer version") else Result.success(backup)
    } catch (e: Exception) {
        fail(NOT_A_BACKUP)
    }
}

/** Preview lines for the import dialog: what the backup holds and how much of it is installed here. */
fun describeBackup(b: LauncherBackup, installed: Set<String>): List<String> {
    val apps = (b.layout.dock + b.layout.pages.flatMap { it.items }).flatMap {
        when (it) {
            is Item.App -> listOf(it.pkg)
            is Item.Folder -> it.apps
            is Item.Widget -> emptyList()
        }
    }.toSet()
    return listOf(
        "pages: ${b.layout.pages.size}",
        "dock: ${b.layout.dock.size}",
        "folders: ${b.layout.folders().size}",
        "apps not installed here: ${apps.count { it !in installed }}",
        "theme: ${b.theme}",
    )
}
