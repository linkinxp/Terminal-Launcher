package dev.linkinxp.termlauncher.backup

import dev.linkinxp.termlauncher.apps.DrawerSort
import dev.linkinxp.termlauncher.data.IconMode
import dev.linkinxp.termlauncher.data.Prefs
import dev.linkinxp.termlauncher.fx.FxSettings
import dev.linkinxp.termlauncher.panel.PanelSettings
import dev.linkinxp.termlauncher.theme.ThemeRegistry
import dev.linkinxp.termlauncher.wall.AsciiStyle
import dev.linkinxp.termlauncher.wall.WallMode
import dev.linkinxp.termlauncher.wall.WallSettings
import dev.linkinxp.termlauncher.weather.WxSettings
import kotlinx.coroutines.flow.first

private inline fun <reified E : Enum<E>> enumOr(name: String, default: E): E =
    enumValues<E>().firstOrNull { it.name == name } ?: default

/** Reads every store into a [LauncherBackup] and writes one back; never throws on odd values. */
class BackupRepository(private val prefs: Prefs) {

    /** The current state, without widgets. */
    suspend fun snapshot(): LauncherBackup {
        val fx = prefs.fx.first()
        val wall = prefs.wall.first()
        val panels = prefs.panels.first()
        val wx = prefs.wx.first()
        return LauncherBackup(
            layout = prefs.layout.first().withoutWidgets(),
            hidden = prefs.hidden.first(),
            theme = prefs.theme.first().name,
            fx = FxBackup(fx.scanlines, fx.flicker, fx.glitch, fx.cursor, fx.boot),
            wall = WallBackup(wall.mode.name, wall.style.name, wall.seed, wall.dim, wall.animate),
            panels = PanelBackup(panels.cal, panels.wx, panels.search, panels.quick),
            wx = WxBackup(wx.city, wx.lat, wx.lon, wx.fahrenheit),
            drawerSort = prefs.sort.first().name,
            iconMode = prefs.iconMode.first().name,
            quick = prefs.quick.first(),
            drawerKeyboard = prefs.drawerKeyboard.first(),
        )
    }

    /**
     * Replaces the layout and every setting with [b]. Unknown theme or enum names keep the current/default
     * value field by field; the stored layout's widgets disappear with it (their host ids are then cleaned up).
     */
    suspend fun apply(b: LauncherBackup) {
        ThemeRegistry.byName(b.theme)?.let { prefs.setTheme(it.name) }
        // The imported layout never has widgets; the ones configured on this device stay where they were.
        prefs.editLayout { current -> Result.success(b.layout.withoutWidgets().withWidgetsFrom(current)) }
        prefs.setHidden(b.hidden)
        prefs.setFx(FxSettings(b.fx.scanlines, b.fx.flicker, b.fx.glitch, b.fx.cursor, b.fx.boot))
        val d = WallSettings()
        prefs.setWall(
            WallSettings(
                mode = enumOr(b.wall.mode, d.mode),
                style = enumOr(b.wall.style, d.style),
                seed = b.wall.seed,
                dim = b.wall.dim.takeIf { it in 0..100 } ?: d.dim,
                animate = b.wall.animate,
            ),
        )
        prefs.setPanels(PanelSettings(b.panels.cal, b.panels.wx, b.panels.search, b.panels.quick))
        prefs.setQuick(b.quick)
        prefs.setDrawerKeyboard(b.drawerKeyboard)
        prefs.setWx(WxSettings(b.wx.city, b.wx.lat, b.wx.lon, b.wx.fahrenheit))
        prefs.clearWxCache()
        prefs.setSort(enumOr(b.drawerSort, DrawerSort.AZ))
        prefs.setIconMode(enumOr(b.iconMode, IconMode.TEXT))
    }
}
