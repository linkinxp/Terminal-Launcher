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
                // Handlers read the current state when tapped (not the last composed one), so quick taps never reuse a stale value.
                SettingRow("mode", w.mode.name.lowercase(), "wall-mode") {
                    val c = vm.ui.value.wall
                    vm.setWall(c.copy(mode = WallMode.entries.after(c.mode)))
                }
                SettingRow("style", w.style.name.lowercase(), "wall-style") {
                    val c = vm.ui.value.wall
                    vm.setWall(c.copy(style = AsciiStyle.entries.after(c.style)))
                }
                Row(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                    TermText("seed".padEnd(11), dim = true)
                    TermText("${w.seed}  ", bold = true)
                    TermText("[random]", Modifier.testTag("wall-random").clickable {
                        val c = vm.ui.value.wall
                        vm.setWall(applyWall(c, "random", null, Random.Default).getOrDefault(c))
                    })
                }
                Row(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                    TermText("dim".padEnd(11), dim = true)
                    TermText("${w.dim}  ", bold = true)
                    TermText("[-]", Modifier.testTag("dim-minus").clickable { vm.ui.value.wall.let { c -> vm.setWall(c.copy(dim = (c.dim - DIM_STEP).coerceIn(0, 100))) } }.padding(end = 10.dp))
                    TermText("[+]", Modifier.testTag("dim-plus").clickable { vm.ui.value.wall.let { c -> vm.setWall(c.copy(dim = (c.dim + DIM_STEP).coerceIn(0, 100))) } })
                }
                ToggleRow("animate", w.animate, "wall-animate") { vm.ui.value.wall.let { c -> vm.setWall(c.copy(animate = !c.animate)) } }
            }

            val fx = ui.fx
            TermPanel("effects", Modifier.padding(top = 4.dp)) {
                SettingRow(
                    "scanlines",
                    when (fx.scanlines) { null -> "auto (theme)"; true -> "on"; false -> "off" },
                    "scanlines-row",
                ) { vm.setScanlines(when (vm.ui.value.fx.scanlines) { null -> true; true -> false; false -> null }) }
                ToggleRow("flicker", fx.flicker, "flicker-row") { vm.setFx("flicker", !vm.ui.value.fx.flicker) }
                ToggleRow("glitch", fx.glitch, "glitch-row") { vm.setFx("glitch", !vm.ui.value.fx.glitch) }
                ToggleRow("cursor", fx.cursor, "cursor-row") { vm.setFx("cursor", !vm.ui.value.fx.cursor) }
                ToggleRow("boot log", fx.boot, "boot-row") { vm.setFx("boot", !vm.ui.value.fx.boot) }
            }

            TermPanel("panels", Modifier.padding(top = 4.dp)) {
                ToggleRow("search", ui.panels.search, "panel-search") { vm.setPanel("search", !vm.ui.value.panels.search) }
                ToggleRow("calendar", ui.panels.cal, "panel-cal") { vm.setPanel("cal", !vm.ui.value.panels.cal) }
                ToggleRow("weather", ui.panels.wx, "panel-wx") { vm.setPanel("wx", !vm.ui.value.panels.wx) }
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

            TermPanel("drawer", Modifier.padding(top = 4.dp)) {
                SettingRow("sort", ui.sort.name.lowercase().replace('_', ' '), "drawer-sort") { vm.setSort(DrawerSort.entries.after(vm.ui.value.sort)) }
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
