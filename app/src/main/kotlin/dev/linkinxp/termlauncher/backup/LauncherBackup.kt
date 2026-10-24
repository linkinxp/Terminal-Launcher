package dev.linkinxp.termlauncher.backup

import dev.linkinxp.termlauncher.layout.HomeLayout
import dev.linkinxp.termlauncher.layout.Item
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.SetSerializer
import kotlinx.serialization.builtins.nullable
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.intOrNull
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

@Serializable data class PanelBackup(
    val cal: Boolean = false,
    val wx: Boolean = false,
    val search: Boolean = true,
    val quick: Boolean = true,
)

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
    /** The `quick` tiles as package names; `null` = the defaults found on the device. */
    val quick: List<String>? = null,
    /** Whether the drawer raises the keyboard for its search field when it opens (on by default). */
    val drawerKeyboard: Boolean = true,
    val clockFormat: String = "AUTO",
)

private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true; prettyPrint = true }

/** JSON for [b]; widgets are removed first because their host ids mean nothing on another install. */
fun encodeBackup(b: LauncherBackup): String =
    json.encodeToString(LauncherBackup.serializer(), b.copy(layout = b.layout.withoutWidgets()))

/**
 * Parses a backup. Anything that is not a backup of this app fails with `import: not a launcher backup`; a
 * newer format version fails with `import: backup is from a newer version` (checked before anything else is
 * decoded). Each section is decoded on its own: a missing or invalid one falls back to its default and the
 * valid ones are still applied.
 */
fun decodeBackup(text: String?): Result<LauncherBackup> {
    fun fail(msg: String) = Result.failure<LauncherBackup>(IllegalArgumentException(msg))
    if (text.isNullOrBlank()) return fail(NOT_A_BACKUP)
    return try {
        // The format marker must really be present: a bare `{}` must not decode to an "empty backup".
        val root = json.parseToJsonElement(text).jsonObject
        if (root["format"]?.jsonPrimitive?.content != FORMAT) return fail(NOT_A_BACKUP)
        val versionElement = root["version"]
        val version = if (versionElement == null) VERSION else versionElement.jsonPrimitive.intOrNull ?: return fail(NOT_A_BACKUP)
        if (version > VERSION) return fail("import: backup is from a newer version")

        fun <T> section(key: String, serializer: KSerializer<T>, default: T): T {
            val element = root[key] ?: return default
            return try { json.decodeFromJsonElement(serializer, element) } catch (_: Exception) { default }
        }
        val layout = section("layout", HomeLayout.serializer(), HomeLayout.default())
        Result.success(
            LauncherBackup(
                version = version,
                layout = if (layout.pages.isEmpty()) HomeLayout.default() else layout,
                hidden = section("hidden", SetSerializer(String.serializer()), emptySet()),
                theme = section("theme", String.serializer(), "classy-black"),
                fx = section("fx", FxBackup.serializer(), FxBackup()),
                wall = section("wall", WallBackup.serializer(), WallBackup()),
                panels = section("panels", PanelBackup.serializer(), PanelBackup()),
                wx = section("wx", WxBackup.serializer(), WxBackup()),
                drawerSort = section("drawerSort", String.serializer(), "AZ"),
                iconMode = section("iconMode", String.serializer(), "TEXT"),
                quick = section("quick", ListSerializer(String.serializer()).nullable, null),
                drawerKeyboard = section("drawerKeyboard", Boolean.serializer(), true),
                clockFormat = section("clockFormat", String.serializer(), "AUTO"),
            ),
        )
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
