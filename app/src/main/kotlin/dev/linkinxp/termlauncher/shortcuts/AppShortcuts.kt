package dev.linkinxp.termlauncher.shortcuts

import android.content.Context
import android.content.pm.LauncherApps
import android.content.pm.ShortcutInfo
import android.os.Process
import android.os.UserHandle

/** An app's own long-press shortcut (Chrome's "New incognito tab", Camera's "Selfie", ...). */
data class AppShortcut(
    val pkg: String,
    val id: String,
    val label: String,
    val rank: Int,
    val enabled: Boolean = true,
    val user: UserHandle? = null,
)

private const val MAX_SHORTCUTS = 5

/** The shortcuts worth showing: enabled, labelled, one per id, best rank first, at most five. */
fun pickShortcuts(all: List<AppShortcut>): List<AppShortcut> =
    all.filter { it.enabled && it.label.isNotBlank() }.distinctBy { it.id }.sortedBy { it.rank }.take(MAX_SHORTCUTS)

/**
 * Reads and starts app shortcuts through LauncherApps. Android only hands them to the default home app, so when
 * Terminal is not the default (or the query is refused) the list is simply empty.
 */
class ShortcutRepository(private val context: Context, private val query: ((String) -> List<ShortcutInfo>)? = null) {
    private val launcherApps get() = context.getSystemService(LauncherApps::class.java)

    fun list(pkg: String): List<AppShortcut> = try {
        val infos = query?.invoke(pkg) ?: defaultQuery(pkg)
        pickShortcuts(infos.map { AppShortcut(it.`package`, it.id, it.shortLabel?.toString().orEmpty(), it.rank, it.isEnabled, it.userHandle) })
    } catch (_: Exception) {
        emptyList()
    }

    private fun defaultQuery(pkg: String): List<ShortcutInfo> {
        val q = LauncherApps.ShortcutQuery().setPackage(pkg).setQueryFlags(
            LauncherApps.ShortcutQuery.FLAG_MATCH_DYNAMIC or LauncherApps.ShortcutQuery.FLAG_MATCH_MANIFEST or LauncherApps.ShortcutQuery.FLAG_MATCH_PINNED,
        )
        return launcherApps?.getShortcuts(q, Process.myUserHandle()).orEmpty()
    }

    /** Starts the shortcut; false when the system refuses. */
    fun start(shortcut: AppShortcut): Boolean = try {
        launcherApps?.startShortcut(shortcut.pkg, shortcut.id, null, null, shortcut.user ?: Process.myUserHandle()); true
    } catch (_: Exception) {
        false
    }
}
