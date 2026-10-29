package dev.linkinxp.termlauncher.ui

import dev.linkinxp.termlauncher.gestures.AdminLock
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.linkinxp.termlauncher.LauncherUi
import dev.linkinxp.termlauncher.LauncherViewModel
import dev.linkinxp.termlauncher.apps.DrawerSort
import dev.linkinxp.termlauncher.data.IconMode
import dev.linkinxp.termlauncher.theme.ThemeBook
import dev.linkinxp.termlauncher.theme.ThemeRegistry
import dev.linkinxp.termlauncher.wall.AsciiStyle
import dev.linkinxp.termlauncher.badges.isBadgeAccessGranted
import dev.linkinxp.termlauncher.gestures.Features
import dev.linkinxp.termlauncher.gestures.ScreenLock
import dev.linkinxp.termlauncher.wall.SvgStyle
import dev.linkinxp.termlauncher.wall.WallField
import dev.linkinxp.termlauncher.wall.wallFields
import dev.linkinxp.termlauncher.wall.WallMode
import dev.linkinxp.termlauncher.wall.WallSettings
import dev.linkinxp.termlauncher.wall.applyWall
import kotlin.random.Random

private const val DIM_STEP = 5

@Composable
private fun SettingRow(label: String, value: String, tag: String, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().testTag(tag).clickable(onClick = onClick).heightIn(min = MIN_TAP).padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        TermText(label.padEnd(11), dim = true)
        TermText(value, bold = true)
    }
}

@Composable
private fun ToggleRow(label: String, on: Boolean, tag: String, onToggle: () -> Unit) {
    SettingRow(label, if (on) "[x] on" else "[ ] off", tag, onToggle)
}

private fun <T> List<T>.after(current: T): T = this[(indexOf(current) + 1) % size]

/** Every launcher setting in one terminal-style screen; each control applies at once through the ViewModel. */
@Composable
fun SettingsScreen(
    ui: LauncherUi,
    vm: LauncherViewModel,
    onExport: () -> Unit,
    onImport: () -> Unit,
    onImportLauncher: () -> Unit,
    onClose: () -> Unit,
    onEditPages: () -> Unit = {},
    onMakeDefault: () -> Unit = {},
) {
    val theme = LocalTheme.current
    BackHandler(onBack = onClose)
    val bg = theme.background.color() // always solid: settings must stay readable over any wallpaper
    Box(
        Modifier.fillMaxSize().background(bg)
            .pointerInput(Unit) { detectTapGestures(onTap = {}, onLongPress = {}) }
            .statusBarsPadding().navigationBarsPadding(),
    ) {
        Column(Modifier.fillMaxSize().padding(horizontal = 16.dp).verticalScroll(rememberScrollState())) {
            Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                TermText("settings", color = theme.accent.color(), bold = true)
                TapText("[x]", onClose, Modifier.testTag("settings-close"), dim = true)
            }

            TermPanel("theme", Modifier.padding(top = 4.dp)) {
                ThemeBook(ui.customThemes).all.forEach { t ->
                    Row(
                        Modifier.fillMaxWidth().testTag("theme-${t.name}").clickable { vm.setTheme(t.name) }.padding(vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        listOf(t.background, t.foreground, t.accent, t.dim).forEach {
                            Box(Modifier.padding(end = 3.dp).size(12.dp).background(Color(it)))
                        }
                        val custom = ThemeRegistry.byName(t.name) == null
                        TermText(" ${if (t.name == ui.theme.name) "*" else " "} ${t.name}${if (custom) " +" else ""}", Modifier.weight(1f), bold = t.name == ui.theme.name)
                        if (custom) TapText("[rm]", { vm.removeCustomTheme(t.name) }, Modifier.testTag("theme-rm-${t.name}"), dim = true)
                    }
                }
            }

            val w = ui.wall
            val shown = wallFields(w.mode) // only what the chosen wallpaper uses
            TermPanel("wallpaper", Modifier.padding(top = 4.dp)) {
                // Every control updates the stored value atomically, so quick taps never lose each other.
                SettingRow("mode", w.mode.name.lowercase(), "wall-mode") { vm.updateWall { it.copy(mode = WallMode.entries.after(it.mode)) } }
                if (WallField.SVG_STYLE in shown) SettingRow("svg style", w.svgStyle.name.lowercase(), "wall-svg-style") { vm.updateWall { it.copy(svgStyle = SvgStyle.entries.after(it.svgStyle)) } }
                if (WallField.STYLE in shown) SettingRow("style", w.style.name.lowercase(), "wall-style") { vm.updateWall { it.copy(style = AsciiStyle.entries.after(it.style)) } }
                if (WallField.SEED in shown) Row(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                    TermText("seed".padEnd(11), dim = true)
                    TermText("${w.seed}  ", bold = true)
                    TermText("[random]", Modifier.testTag("wall-random").clickable {
                        vm.updateWall { applyWall(it, "random", null, Random.Default).getOrDefault(it) }
                    })
                }
                if (WallField.DIM in shown) Row(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                    TermText("dim".padEnd(11), dim = true)
                    TermText("${w.dim}  ", bold = true)
                    TermText("[-]", Modifier.testTag("dim-minus").clickable { vm.updateWall { it.copy(dim = (it.dim - DIM_STEP).coerceIn(0, 100)) } }.padding(end = 10.dp))
                    TermText("[+]", Modifier.testTag("dim-plus").clickable { vm.updateWall { it.copy(dim = (it.dim + DIM_STEP).coerceIn(0, 100)) } })
                }
                if (WallField.ANIMATE in shown) ToggleRow("animate", w.animate, "wall-animate") { vm.updateWall { it.copy(animate = !it.animate) } }
            }

            val fx = ui.fx
            TermPanel("effects", Modifier.padding(top = 4.dp)) {
                SettingRow(
                    "scanlines",
                    when (fx.scanlines) { null -> "auto (theme)"; true -> "on"; false -> "off" },
                    "scanlines-row",
                ) { vm.cycleScanlines() }
                ToggleRow("flicker", fx.flicker, "flicker-row") { vm.toggleFx("flicker") }
                ToggleRow("glitch", fx.glitch, "glitch-row") { vm.toggleFx("glitch") }
                ToggleRow("cursor", fx.cursor, "cursor-row") { vm.toggleFx("cursor") }
                ToggleRow("boot log", fx.boot, "boot-row") { vm.toggleFx("boot") }
            }

            TermPanel("panels", Modifier.padding(top = 4.dp)) {
                ToggleRow("search", ui.panels.search, "panel-search") { vm.togglePanel("search") }
                ToggleRow("quick", ui.panels.quick, "panel-quick") { vm.togglePanel("quick") }
                ToggleRow("calendar", ui.panels.cal, "panel-cal") { vm.togglePanel("cal") }
                ToggleRow("weather", ui.panels.wx, "panel-wx") { vm.togglePanel("wx") }
                SettingRow("units", if (ui.wx.fahrenheit) "fahrenheit" else "celsius", "wx-units") { vm.setWxUnits(!vm.ui.value.wx.fahrenheit) }
                var city by remember { mutableStateOf("") }
                Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    TermText("city".padEnd(11), dim = true)
                    BasicTextField(
                        value = city,
                        onValueChange = { city = it },
                        singleLine = true,
                        textStyle = TextStyle(color = theme.foreground.color(), fontFamily = TermFont, fontSize = 14.sp),
                        cursorBrush = SolidColor(theme.accent.color()),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { if (city.isNotBlank()) { vm.setWxCity(city.trim()); city = "" } }),
                        decorationBox = { inner -> Box { if (city.isEmpty()) TermText(ui.wx.city ?: "set a city", dim = true); inner() } },
                        modifier = Modifier.weight(1f).testTag("wx-city"),
                    )
                    TermText("[set]", Modifier.clickable { if (city.isNotBlank()) { vm.setWxCity(city.trim()); city = "" } }.padding(start = 8.dp))
                }
            }

            TermPanel("quick tiles", Modifier.padding(top = 4.dp)) {
                var picking by remember { mutableStateOf(false) }
                if (ui.quick.isEmpty()) TermText("no tiles yet", dim = true)
                ui.quick.forEach { pkg ->
                    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                        TermText(ui.apps[pkg]?.label?.lowercase() ?: pkg, Modifier.weight(1f))
                        TapText("[^]", { vm.moveQuickUp(pkg) }, Modifier.testTag("quick-up-$pkg"))
                        TapText("[rm]", { vm.removeQuick(pkg) }, Modifier.testTag("quick-rm-$pkg"))
                    }
                }
                Row(Modifier.fillMaxWidth().padding(top = 8.dp)) {
                    TapText("[+ add]", { picking = !picking }, Modifier.testTag("quick-add"), bold = true)
                    TapText("[reset]", { vm.resetQuick() }, Modifier.testTag("quick-reset"), dim = true)
                }
                if (picking) {
                    ui.apps.values.filter { it.pkg !in ui.quick && it.pkg !in ui.hidden }.sortedBy { it.label.lowercase() }.forEach { app ->
                        TermText(
                            "  + ${app.label.lowercase()}",
                            Modifier.fillMaxWidth().clickable { vm.addQuick(app.pkg); picking = false }.padding(vertical = 5.dp),
                        )
                    }
                }
            }

            TermPanel("home", Modifier.padding(top = 4.dp)) {
                SettingRow("layout", ui.homeStyle.name.lowercase(), "home-style-row") { vm.cycleHomeStyle() }
                SettingRow("density", "${ui.density.name.lowercase()} (${ui.density.columns})", "density-row") { vm.cycleDensity() }
                SettingRow("pages", "[edit pages, widgets, panels]", "settings-edit-pages", onEditPages)
                val homeContext = androidx.compose.ui.platform.LocalContext.current
                val homeTick = rememberResumeTick()
                val isHome = remember(homeTick) { dev.linkinxp.termlauncher.home.isDefaultHome(homeContext) }
                SettingRow("home app", if (isHome) "terminal" else "other [make terminal the default]", "settings-default-home", onMakeDefault)
            }

            TermPanel("hidden apps", Modifier.padding(top = 4.dp)) {
                val hiddenApps = ui.hidden.mapNotNull { ui.apps[it] }.sortedBy { it.label.lowercase() }
                if (hiddenApps.isEmpty()) TermText("none (long-press an app > hide)", dim = true, size = 12.sp, maxLines = 2)
                hiddenApps.forEach { app ->
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        TermText(app.label.lowercase(), Modifier.weight(1f))
                        TapText("[unhide]", { vm.toggleHidden(app.pkg) }, Modifier.testTag("unhide-${app.pkg}"))
                    }
                }
            }

            TermPanel("notifications", Modifier.padding(top = 4.dp)) {
                val context = androidx.compose.ui.platform.LocalContext.current
                ToggleRow("badges", ui.badges, "badges-row") { vm.toggleBadges() }
                val resumed = rememberResumeTick() // coming back from the system screen re-reads the status
                SettingRow("access", if (remember(resumed) { isBadgeAccessGranted(context) }) "on" else "off [grant]", "badges-access") {
                    context.startActivity(android.content.Intent(android.provider.Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK))
                }
                TermText("counts only: Terminal never reads the notification text", dim = true, size = 12.sp, maxLines = 2)
            }

            TermPanel("gestures", Modifier.padding(top = 4.dp)) {
                val context = androidx.compose.ui.platform.LocalContext.current
                ToggleRow("double-tap lock", ui.doubleTapLock, "gesture-lock") { vm.toggleDoubleTapLock() }
                val resumedAdmin = rememberResumeTick()
                val adminOn = remember(resumedAdmin) { AdminLock.isActive(context) }
                SettingRow("lock access", if (adminOn) "on [turn off]" else "off [enable]", "settings-lock-admin") {
                    if (adminOn) AdminLock.remove(context)
                    else context.startActivity(AdminLock.requestIntent(context).addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK))
                }
                if (Features.lockService) {
                    val resumedLock = rememberResumeTick()
                    SettingRow("lock service", if (remember(resumedLock) { ScreenLock.isEnabled(context) }) "on" else "off [enable]", "gesture-service") {
                        context.startActivity(android.content.Intent(android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS).addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK))
                    }
                }
                TermText("swipe down: left = notifications, right = quick settings", dim = true, size = 12.sp, maxLines = 2)
            }

            TermPanel("clock", Modifier.padding(top = 4.dp)) {
                SettingRow("format", ui.clockFormat.name.lowercase().removePrefix("h").let { if (it == "auto") it else "${it}h" }, "clock-row") { vm.cycleClockFormat() }
                SettingRow("style", ui.clockStyle.name.lowercase(), "clock-style-row") { vm.cycleClockStyle() }
            }

            TermPanel("drawer", Modifier.padding(top = 4.dp)) {
                SettingRow("sort", ui.sort.name.lowercase().replace('_', ' '), "drawer-sort") { vm.setSort(DrawerSort.entries.after(vm.ui.value.sort)) }
                ToggleRow("keyboard", ui.drawerKeyboard, "drawer-keyboard") { vm.toggleDrawerKeyboard() }
                SettingRow("icons", ui.iconMode.name.lowercase(), "drawer-icons") { vm.setIconMode(IconMode.entries.after(vm.ui.value.iconMode)) }
            }

            TermPanel("backup", Modifier.padding(top = 4.dp)) {
                SettingRow("export", "[save layout + settings]", "backup-export", onExport)
                SettingRow("import", "[restore from a backup]", "backup-import", onImport)
                SettingRow("import", "[from nova / lawnchair]", "backup-import-launcher", onImportLauncher)
            }

            TermPanel("about", Modifier.padding(top = 4.dp, bottom = 16.dp)) {
                TermText("terminal-launcher ${LauncherViewModel.LAUNCHER_VERSION}")
                TermText("back to your old launcher:", dim = true, modifier = Modifier.padding(top = 8.dp))
                TermText("Settings > Apps > Default apps > Home app", dim = true, maxLines = 2)
            }
        }
    }
}
