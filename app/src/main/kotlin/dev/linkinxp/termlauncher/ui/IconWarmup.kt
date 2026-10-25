package dev.linkinxp.termlauncher.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import dev.linkinxp.termlauncher.LauncherUi
import dev.linkinxp.termlauncher.apps.DrawerState
import dev.linkinxp.termlauncher.icons.IconRepository
import dev.linkinxp.termlauncher.layout.Item

private const val HOME_WARM_MAX = 60
private const val DRAWER_WARM_CELLS = 24

/** An app tile to draw ahead of time at the size ([sizeDp]) it is shown at. */
data class WarmTarget(val pkg: String, val label: String, val sizeDp: Float)

private const val DOCK_DP = 48f
private const val PAGE_DP = 56f
private const val MINI = 0.4f // a folder's mini tiles are this fraction of the folder tile

/** The tiles needed first, each at its real size: the dock, the quick tiles, then page one (folders: their first four apps, small). */
fun warmTargets(ui: LauncherUi): List<WarmTarget> {
    val out = ArrayList<WarmTarget>()
    fun add(pkg: String, size: Float) { ui.apps[pkg]?.let { out += WarmTarget(pkg, it.label, size) } }
    fun item(i: Item, size: Float) = when (i) {
        is Item.App -> add(i.pkg, size)
        is Item.Folder -> i.apps.take(4).forEach { add(it, size * MINI) }
        else -> {}
    }
    ui.layout.dock.forEach { item(it, DOCK_DP) }
    ui.quick.forEach { add(it, DOCK_DP) }
    ui.layout.pages.firstOrNull()?.items.orEmpty().forEach { item(it, PAGE_DP) }
    return out.distinctBy { it.pkg to it.sizeDp }.take(HOME_WARM_MAX)
}

/** The first drawer cells: `recent`, then the top of the first sections. */
fun drawerWarmTargets(d: DrawerState): List<WarmTarget> =
    (d.recent + d.sections.flatMap { it.apps }).distinctBy { it.pkg }.take(DRAWER_WARM_CELLS).map { WarmTarget(it.pkg, it.label, PAGE_DP) }

/** Draws (off the main thread) the tiles in [targets] at each of [sizes] so they are cached before they are needed. */
@Composable
fun WarmIcons(targets: List<WarmTarget>, delayMs: Long = 0) {
    val context = LocalContext.current
    val theme = LocalTheme.current
    val density = LocalDensity.current
    LaunchedEffect(targets, density.density, theme.background, theme.foreground, theme.accent, theme.dim) {
        if (delayMs > 0) kotlinx.coroutines.delay(delayMs)
        val repo = IconRepository.get(context)
        targets.forEach { t -> repo.tile(t.pkg, t.label, theme, with(density) { t.sizeDp.dp.roundToPx() }) }
    }
}
