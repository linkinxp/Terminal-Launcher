package dev.linkinxp.termlauncher.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import dev.linkinxp.termlauncher.LauncherUi
import dev.linkinxp.termlauncher.apps.DrawerState
import dev.linkinxp.termlauncher.icons.IconRepository
import dev.linkinxp.termlauncher.layout.Item

private const val HOME_WARM_MAX = 40
private const val DRAWER_WARM_CELLS = 24

/** The apps whose tiles are needed first: the dock, the quick tiles, then page one (folders contribute their first four). */
fun warmTargets(ui: LauncherUi): List<Pair<String, String>> {
    fun pkgs(item: Item): List<String> = when (item) {
        is Item.App -> listOf(item.pkg)
        is Item.Folder -> item.apps.take(4)
        else -> emptyList()
    }
    val order = ui.layout.dock.flatMap(::pkgs) + ui.quick + ui.layout.pages.firstOrNull()?.items.orEmpty().flatMap(::pkgs)
    return order.distinct().mapNotNull { pkg -> ui.apps[pkg]?.let { pkg to it.label } }.take(HOME_WARM_MAX)
}

/** The first drawer cells: `recent`, then the top of the first sections. */
fun drawerWarmTargets(d: DrawerState): List<Pair<String, String>> =
    (d.recent + d.sections.flatMap { it.apps }).distinctBy { it.pkg }.take(DRAWER_WARM_CELLS).map { it.pkg to it.label }

/** Draws (off the main thread) the tiles in [targets] at each of [sizes] so they are cached before they are needed. */
@Composable
fun WarmIcons(targets: List<Pair<String, String>>, sizes: List<Dp>, delayMs: Long = 0) {
    val context = LocalContext.current
    val theme = LocalTheme.current
    val density = LocalDensity.current
    val sizesPx = sizes.map { with(density) { it.roundToPx() } }
    LaunchedEffect(targets, sizesPx, theme.background, theme.foreground, theme.accent, theme.dim) {
        if (delayMs > 0) kotlinx.coroutines.delay(delayMs)
        val repo = IconRepository.get(context)
        targets.forEach { (pkg, label) -> sizesPx.forEach { px -> repo.tile(pkg, label, theme, px) } }
    }
}
