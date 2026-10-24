package dev.linkinxp.termlauncher.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import dev.linkinxp.termlauncher.theme.ThemeRegistry
import dev.linkinxp.termlauncher.wall.AsciiStyle
import dev.linkinxp.termlauncher.wall.WallMode
import dev.linkinxp.termlauncher.wall.WallSettings
import dev.linkinxp.termlauncher.wall.applyWall
import kotlin.random.Random

private const val DIM_STEP = 5

@Composable
private fun SettingRow(label: String, value: String, tag: String, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().testTag(tag).clickable(onClick = onClick).padding(vertical = 8.dp)) {
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
) {
    val theme = LocalTheme.current
    BackHandler(onBack = onClose)
    val bg = theme.background.color().let { if (ui.wall.mode == WallMode.SOLID) it else it.copy(alpha = 0.55f) }
    Box(
        Modifier.fillMaxSize().background(bg)
            .pointerInput(Unit) { detectTapGestures(onTap = {}, onLongPress = {}) }
            .statusBarsPadding().navigationBarsPadding(),
    ) {
        Column(Modifier.fillMaxSize().padding(horizontal = 16.dp).verticalScroll(rememberScrollState())) {
            Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                TermText("settings", color = theme.accent.color(), bold = true)
                TermText("[x]", Modifier.testTag("settings-close").clickable(onClick = onClose), dim = true)
            }

            TermPanel("theme", Modifier.padding(top = 4.dp)) {
                ThemeRegistry.all.forEach { t ->
                    Row(
                        Modifier.fillMaxWidth().testTag("theme-${t.name}").clickable { vm.setTheme(t.name) }.padding(vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        listOf(t.background, t.foreground, t.accent, t.dim).forEach {
                            Box(Modifier.padding(end = 3.dp).size(12.dp).background(Color(it)))
                        }
                        TermText(" ${if (t.name == ui.theme.name) "*" else " "} ${t.name}", bold = t.name == ui.theme.name)
                    }
                }
            }

            val w = ui.wall
            TermPanel("wallpaper", Modifier.padding(top = 4.dp)) {
                // Every control updates the stored value atomically, so quick taps never lose each other.
                SettingRow("mode", w.mode.name.lowercase(), "wall-mode") { vm.updateWall { it.copy(mode = WallMode.entries.after(it.mode)) } }
                SettingRow("style", w.style.name.lowercase(), "wall-style") { vm.updateWall { it.copy(style = AsciiStyle.entries.after(it.style)) } }
                Row(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                    TermText("seed".padEnd(11), dim = true)
                    TermText("${w.seed}  ", bold = true)
                    TermText("[random]", Modifier.testTag("wall-random").clickable {
                        vm.updateWall { applyWall(it, "random", null, Random.Default).getOrDefault(it) }
                    })
                }
                Row(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                    TermText("dim".padEnd(11), dim = true)
                    TermText("${w.dim}  ", bold = true)
                    TermText("[-]", Modifier.testTag("dim-minus").clickable { vm.updateWall { it.copy(dim = (it.dim - DIM_STEP).coerceIn(0, 100)) } }.padding(end = 10.dp))
                    TermText("[+]", Modifier.testTag("dim-plus").clickable { vm.updateWall { it.copy(dim = (it.dim + DIM_STEP).coerceIn(0, 100)) } })
                }
                ToggleRow("animate", w.animate, "wall-animate") { vm.updateWall { it.copy(animate = !it.animate) } }
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
                        TermText("[^]", Modifier.testTag("quick-up-$pkg").clickable { vm.moveQuickUp(pkg) }.padding(horizontal = 8.dp))
                        TermText("[rm]", Modifier.testTag("quick-rm-$pkg").clickable { vm.removeQuick(pkg) }.padding(start = 8.dp))
                    }
                }
                Row(Modifier.fillMaxWidth().padding(top = 8.dp)) {
                    TermText("[+ add]", Modifier.testTag("quick-add").clickable { picking = !picking }.padding(end = 16.dp), bold = true)
                    TermText("[reset]", Modifier.testTag("quick-reset").clickable { vm.resetQuick() }, dim = true)
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
