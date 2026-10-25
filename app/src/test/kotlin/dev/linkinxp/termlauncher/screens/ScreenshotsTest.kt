package dev.linkinxp.termlauncher.screens

import android.app.Application
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.onRoot
import androidx.test.core.app.ApplicationProvider
import com.github.takahirom.roborazzi.captureRoboImage
import dev.linkinxp.termlauncher.LauncherUi
import dev.linkinxp.termlauncher.LauncherViewModel
import dev.linkinxp.termlauncher.PromptState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import dev.linkinxp.termlauncher.calendar.AgendaEvent
import dev.linkinxp.termlauncher.calendar.formatAgenda
import dev.linkinxp.termlauncher.panel.PanelData
import dev.linkinxp.termlauncher.ui.SettingsScreen
import dev.linkinxp.termlauncher.ui.TermPanel
import dev.linkinxp.termlauncher.ui.TermText
import dev.linkinxp.termlauncher.weather.Forecast
import dev.linkinxp.termlauncher.weather.HourPoint
import dev.linkinxp.termlauncher.weather.formatWeather
import java.time.Instant
import java.time.ZoneOffset
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import dev.linkinxp.termlauncher.wall.AsciiStyle
import dev.linkinxp.termlauncher.wall.WallMode
import dev.linkinxp.termlauncher.wall.WallSettings
import dev.linkinxp.termlauncher.apps.AppEntry
import dev.linkinxp.termlauncher.clock.ClockFormat
import dev.linkinxp.termlauncher.clock.ClockStyle
import dev.linkinxp.termlauncher.fx.FxSettings
import dev.linkinxp.termlauncher.prompt.DeviceInfo
import dev.linkinxp.termlauncher.prompt.neofetch
import dev.linkinxp.termlauncher.ui.EffectsOverlay
import dev.linkinxp.termlauncher.apps.DrawerSort
import dev.linkinxp.termlauncher.apps.AppStat
import dev.linkinxp.termlauncher.apps.buildDrawer
import dev.linkinxp.termlauncher.layout.HomeLayout
import dev.linkinxp.termlauncher.layout.Item
import dev.linkinxp.termlauncher.layout.Page
import dev.linkinxp.termlauncher.theme.Theme
import dev.linkinxp.termlauncher.theme.ThemeRegistry
import dev.linkinxp.termlauncher.ui.AppDrawer
import dev.linkinxp.termlauncher.layout.PanelKind
import dev.linkinxp.termlauncher.layout.HomeStyle
import dev.linkinxp.termlauncher.ui.PagerHome
import dev.linkinxp.termlauncher.ui.TermTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Renders the real screens with sample data to PNGs in app/build/shots (run with -Droborazzi.test.record=true). */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w411dp-h914dp-port-xxhdpi")
class ScreenshotsTest {
    @get:Rule val rule = createComposeRule()

    private val names = listOf(
        "Phone", "Messages", "Chrome", "Camera", "Files", "Maps", "Spotify", "Signal", "Notes",
        "Garmin Connect", "Calendar", "Clock", "Photos", "1Password", "Bandcamp", "Strava",
    )
    private val appList = names.mapIndexed { i, n -> AppEntry("pkg.${n.lowercase().replace(' ', '_')}", n, i.toLong()) }
    private fun pkg(name: String) = appList.first { it.label == name }.pkg

    private fun ui(
        theme: Theme,
        drawer: Boolean = false,
        prompt: PromptState = PromptState(),
        fx: FxSettings = FxSettings(),
        wall: WallSettings = WallSettings(),
        filter: String = "",
        calendar: PanelData = PanelData.Off,
        weather: PanelData = PanelData.Off,
        quick: List<String> = emptyList(),
        clockStyle: ClockStyle = ClockStyle.PIXEL,
        clockFormat: ClockFormat = ClockFormat.H24,
    ): LauncherUi {
        val layout = HomeLayout(
            dock = listOf("Phone", "Messages", "Chrome", "Camera").map { Item.App(pkg(it)) },
            pages = listOf(
                Page(listOf(Item.Folder("media", listOf(pkg("Spotify"), pkg("Bandcamp"))), Item.App(pkg("Maps")), Item.App(pkg("Garmin Connect")))),
                Page(listOf(Item.App(pkg("Notes")))),
            ),
        )
        val stats = mapOf(pkg("Chrome") to AppStat(9, 5), pkg("Spotify") to AppStat(4, 4), pkg("Maps") to AppStat(2, 3))
        return LauncherUi(
            theme = theme,
            layout = layout,
            apps = appList.associateBy { it.pkg },
            drawer = buildDrawer(appList, emptySet(), stats, DrawerSort.AZ, filter, false),
            filter = filter,
            calendar = calendar,
            weather = weather,
            quick = quick,
            drawerOpen = drawer,
            prompt = prompt,
            fx = fx,
            wall = wall,
            clockStyle = clockStyle,
            clockFormat = clockFormat,
        )
    }

    private fun vm() = LauncherViewModel(ApplicationProvider.getApplicationContext<Application>())

    private fun home(
        name: String,
        theme: Theme,
        prompt: PromptState = PromptState(),
        fx: FxSettings = FxSettings(),
        wall: WallSettings = WallSettings(),
        calendar: PanelData = PanelData.Off,
        weather: PanelData = PanelData.Off,
        quick: List<String> = emptyList(),
        clockStyle: ClockStyle = ClockStyle.PIXEL,
        clockFormat: ClockFormat = ClockFormat.H24,
    ) {
        val vm = vm()
        rule.setContent {
            TermTheme(theme, wall) {
                val ui = ui(theme, prompt = prompt, fx = fx, wall = wall, calendar = calendar, weather = weather, quick = quick, clockStyle = clockStyle, clockFormat = clockFormat)
                PagerHome(ui, vm, {}, {}, {}, {})
                EffectsOverlay(ui.fx, theme, reduceMotion = true)
            }
        }
        rule.onRoot().captureRoboImage("build/shots/$name.png")
    }

    @Test fun homeClassyBlack() = home("home-classy-black", ThemeRegistry.byName("classy-black")!!)

    @Test fun homeMatrixWithPrompt() = home(
        "home-matrix-prompt", ThemeRegistry.byName("matrix")!!,
        PromptState("ch", listOf(appList.first { it.label == "Chrome" }), null),
    )

    @Test fun homeAmber() = home("home-amber", ThemeRegistry.byName("amber")!!)

    @Test fun homeDracula() = home("home-dracula", ThemeRegistry.byName("dracula")!!)

    @Test fun homeSolarized() = home("home-solarized", ThemeRegistry.byName("solarized-dark")!!)

    @Test fun drawerClassyBlack() {
        val vm = vm()
        val theme = ThemeRegistry.default
        rule.setContent { TermTheme(theme) { AppDrawer(ui(theme, drawer = true), vm, {}, {}) } }
        rule.onRoot().captureRoboImage("build/shots/drawer-classy-black.png")
    }

    @Test fun homeNeonCity() = home("home-neon-city", ThemeRegistry.byName("neon-city")!!)

    @Test fun homeSynthwave() = home("home-synthwave", ThemeRegistry.byName("synthwave")!!)

    @Test fun homeBladeRunner() = home("home-blade-runner", ThemeRegistry.byName("blade-runner")!!)

    @Test fun homeGlitchRed() = home("home-glitch-red", ThemeRegistry.byName("glitch-red")!!)

    @Test fun homeNeonCityScanlinesOff() =
        home("home-neon-city-no-scanlines", ThemeRegistry.byName("neon-city")!!, fx = FxSettings(scanlines = false))

    @Test fun neofetchOutputPanel() {
        val text = neofetch(DeviceInfo("Pixel 10 Pro", "17", "0.2.0", "3d 4h", "neon-city", 142, 41))
        home("home-neofetch", ThemeRegistry.byName("neon-city")!!, PromptState("", emptyList(), text, listOf("theme matrix", "ls -a")))
    }

    @Test fun helpOutputPanel() = home(
        "home-help", ThemeRegistry.byName("blade-runner")!!,
        PromptState("", emptyList(), LauncherViewModel.HELP),
    )

    private fun ascii(style: AsciiStyle) = WallSettings(mode = WallMode.ASCII, style = style, seed = 7)

    @Test fun wallGlyphNeonCity() = home("wall-glyph-neon-city", ThemeRegistry.byName("neon-city")!!, wall = ascii(AsciiStyle.GLYPH))
    @Test fun wallCircuitNeonCity() = home("wall-circuit-neon-city", ThemeRegistry.byName("neon-city")!!, wall = ascii(AsciiStyle.CIRCUIT))
    @Test fun wallContourMatrix() = home("wall-contour-matrix", ThemeRegistry.byName("matrix")!!, wall = ascii(AsciiStyle.CONTOUR))
    @Test fun wallStarsSynthwave() = home("wall-stars-synthwave", ThemeRegistry.byName("synthwave")!!, wall = ascii(AsciiStyle.STARS))
    @Test fun wallHexdumpBladeRunner() = home("wall-hexdump-blade-runner", ThemeRegistry.byName("blade-runner")!!, wall = ascii(AsciiStyle.HEXDUMP))
    @Test fun wallSkylineGlitchRed() = home("wall-skyline-glitch-red", ThemeRegistry.byName("glitch-red")!!, wall = ascii(AsciiStyle.SKYLINE))
    @Test fun wallSkylineMatrix() = home("wall-skyline-matrix", ThemeRegistry.byName("matrix")!!, wall = ascii(AsciiStyle.SKYLINE))

    @Test fun wallSystemOverCheckerboard() {
        val vm = vm()
        val theme = ThemeRegistry.byName("neon-city")!!
        val wall = WallSettings(mode = WallMode.SYSTEM, dim = 55)
        rule.setContent {
            Box(Modifier.fillMaxSize()) {
                // Stand-in for the system wallpaper, which the JVM does not have.
                Canvas(Modifier.fillMaxSize()) {
                    val cell = 60.dp.toPx()
                    for (y in 0 until (size.height / cell).toInt() + 1) for (x in 0 until (size.width / cell).toInt() + 1) {
                        drawRect(if ((x + y) % 2 == 0) Color(0xFFE8E8E8) else Color(0xFF3A6EA5), Offset(x * cell, y * cell), Size(cell, cell))
                    }
                }
                TermTheme(theme, wall) { PagerHome(ui(theme, wall = wall), vm, {}, {}, {}, {}) }
            }
        }
        rule.onRoot().captureRoboImage("build/shots/wall-system-checkerboard.png")
    }

    @Test fun drawerOverAsciiWall() {
        val vm = vm()
        val theme = ThemeRegistry.byName("neon-city")!!
        val wall = ascii(AsciiStyle.CIRCUIT)
        rule.setContent {
            TermTheme(theme, wall) {
                val ui = ui(theme, drawer = true, wall = wall)
                PagerHome(ui, vm, {}, {}, {}, {}, modifier = Modifier.alpha(0f))
                AppDrawer(ui, vm, {}, {})
            }
        }
        rule.onRoot().captureRoboImage("build/shots/wall-drawer-ascii.png")
    }

    @Test fun drawerSearchResults() {
        val vm = vm()
        val theme = ThemeRegistry.byName("neon-city")!!
        rule.setContent { TermTheme(theme) { AppDrawer(ui(theme, drawer = true, filter = "c"), vm, {}, {}) } }
        rule.onRoot().captureRoboImage("build/shots/drawer-search.png")
    }

    // ---- panels ----

    private val nowMs = Instant.parse("2026-10-23T09:00:00Z").toEpochMilli()

    private fun agenda(): PanelData = PanelData.Lines(
        formatAgenda(
            listOf(
                AgendaEvent("Cloud Infra Standup", Instant.parse("2026-10-23T10:05:00Z").toEpochMilli(), Instant.parse("2026-10-23T10:35:00Z").toEpochMilli(), false),
                AgendaEvent("Trivia with #erg_seasons on Slack", Instant.parse("2026-10-23T12:00:00Z").toEpochMilli(), Instant.parse("2026-10-23T13:00:00Z").toEpochMilli(), false),
                AgendaEvent("Holiday", Instant.parse("2026-10-24T00:00:00Z").toEpochMilli(), Instant.parse("2026-10-25T00:00:00Z").toEpochMilli(), true),
                AgendaEvent("Climbing at 8", Instant.parse("2026-10-27T18:30:00Z").toEpochMilli(), Instant.parse("2026-10-27T20:00:00Z").toEpochMilli(), false),
            ),
            nowMs, ZoneOffset.UTC,
        ),
    )

    private fun weather(code: Int, isDay: Boolean, temp: Double = 10.0): PanelData = PanelData.Lines(
        formatWeather(
            "Zagreb",
            Forecast(
                temp, 93, code, 3.2, 135, isDay, 13.2, 6.0,
                listOf(HourPoint(0, 8.6, 1), HourPoint(3, 7.0, 3), HourPoint(6, 6.4, 3), HourPoint(9, 8.0, 0), HourPoint(12, 12.0, 0)),
                nowMs - 12 * 60_000L,
            ),
            false, nowMs,
        ),
    )

    @Test fun panelsCalNeonCity() = home("panels-cal-neon-city", ThemeRegistry.byName("neon-city")!!, calendar = agenda())

    @Test fun panelsCalPermissionMessage() =
        home("panels-cal-no-permission", ThemeRegistry.byName("matrix")!!, calendar = PanelData.Message("cal: no permission (grant in Settings)"))

    @Test fun panelsWxClearDay() = home("panels-wx-clear-day", ThemeRegistry.byName("amber")!!, weather = weather(0, true))
    @Test fun panelsWxPartlyNight() = home("panels-wx-partly-night", ThemeRegistry.byName("neon-city")!!, weather = weather(1, false))
    @Test fun panelsWxRain() = home("panels-wx-rain", ThemeRegistry.byName("blade-runner")!!, weather = weather(63, true, 7.0))
    @Test fun panelsWxThunder() = home("panels-wx-thunder", ThemeRegistry.byName("glitch-red")!!, weather = weather(95, false, 18.0))

    @Test fun panelsBothOverAsciiWall() = home(
        "panels-both-ascii", ThemeRegistry.byName("synthwave")!!, wall = ascii(AsciiStyle.CONTOUR),
        calendar = agenda(), weather = weather(61, true, 9.0),
    )

    @Test fun widgetStandIn() {
        val theme = ThemeRegistry.byName("neon-city")!!
        rule.setContent {
            TermTheme(theme) {
                Column(Modifier.padding(16.dp)) {
                    TermPanel("garmin connect", Modifier.fillMaxWidth()) {
                        // Stand-in for a hosted widget, which the JVM cannot render.
                        Box(Modifier.fillMaxWidth().height(192.dp).background(Color(0xFF2B3A55)))
                    }
                }
            }
        }
        rule.onRoot().captureRoboImage("build/shots/panels-widget-standin.png")
    }

    // ---- settings and import ----

    private fun settings(name: String, theme: Theme, wall: WallSettings = WallSettings()) {
        val vm = vm()
        rule.setContent { TermTheme(theme, wall) { SettingsScreen(ui(theme, wall = wall), vm, {}, {}, {}, {}) } }
        rule.onRoot().captureRoboImage("build/shots/$name.png")
    }

    @Test fun settingsNeonCity() = settings("settings-neon-city", ThemeRegistry.byName("neon-city")!!)

    @Test fun settingsClassyBlackOverAsciiWall() =
        settings("settings-classy-ascii", ThemeRegistry.default, ascii(AsciiStyle.STARS))

    @Test fun importPreviewPanel() {
        val theme = ThemeRegistry.byName("matrix")!!
        rule.setContent {
            TermTheme(theme) {
                Column(Modifier.padding(16.dp)) {
                    TermPanel("import layout", Modifier.fillMaxWidth()) {
                        listOf("pages: 3", "dock: 4", "folders: 2", "apps found: 31", "apps not installed here: 5", "widgets skipped: 2")
                            .forEach { TermText(it) }
                        TermText("this replaces your current layout", Modifier.padding(top = 10.dp), dim = true)
                        TermText("[replace]   [cancel]", Modifier.padding(top = 12.dp), bold = true)
                    }
                }
            }
        }
        rule.onRoot().captureRoboImage("build/shots/import-preview.png")
    }

    @Test fun quickPanelNeonCity() = home(
        "quick-neon-city", ThemeRegistry.byName("neon-city")!!,
        quick = listOf("Phone", "Messages", "Camera", "Maps", "Chrome", "Garmin Connect").map { pkg(it) },
    )

    @Test fun quickPanelMatrixFullEight() = home(
        "quick-matrix-eight", ThemeRegistry.byName("matrix")!!,
        quick = listOf("Phone", "Messages", "Camera", "Maps", "Chrome", "Spotify", "Signal", "Notes").map { pkg(it) },
    )

    // One shot of every clock style in two themes, plus the 12-hour variants (the time shown is the real one).
    @Test fun clockPixelNeonCity() = home("clock-pixel-neon-city", ThemeRegistry.byName("neon-city")!!, clockStyle = ClockStyle.PIXEL)
    @Test fun clockSegmentNeonCity() = home("clock-segment-neon-city", ThemeRegistry.byName("neon-city")!!, clockStyle = ClockStyle.SEGMENT)
    @Test fun clockFigletNeonCity() = home("clock-figlet-neon-city", ThemeRegistry.byName("neon-city")!!, clockStyle = ClockStyle.FIGLET)
    @Test fun clockBinaryNeonCity() = home("clock-binary-neon-city", ThemeRegistry.byName("neon-city")!!, clockStyle = ClockStyle.BINARY)
    @Test fun clockHexNeonCity() = home("clock-hex-neon-city", ThemeRegistry.byName("neon-city")!!, clockStyle = ClockStyle.HEX)
    @Test fun clockEpochNeonCity() = home("clock-epoch-neon-city", ThemeRegistry.byName("neon-city")!!, clockStyle = ClockStyle.EPOCH)
    @Test fun clockPlainNeonCity() = home("clock-plain-neon-city", ThemeRegistry.byName("neon-city")!!, clockStyle = ClockStyle.PLAIN)
    @Test fun clockPixelMatrix() = home("clock-pixel-matrix", ThemeRegistry.byName("matrix")!!, clockStyle = ClockStyle.PIXEL)
    @Test fun clockSegmentMatrix() = home("clock-segment-matrix", ThemeRegistry.byName("matrix")!!, clockStyle = ClockStyle.SEGMENT)
    @Test fun clockFigletMatrix() = home("clock-figlet-matrix", ThemeRegistry.byName("matrix")!!, clockStyle = ClockStyle.FIGLET)
    @Test fun clockBinaryMatrix() = home("clock-binary-matrix", ThemeRegistry.byName("matrix")!!, clockStyle = ClockStyle.BINARY)
    @Test fun clockHexMatrix() = home("clock-hex-matrix", ThemeRegistry.byName("matrix")!!, clockStyle = ClockStyle.HEX)
    @Test fun clockEpochMatrix() = home("clock-epoch-matrix", ThemeRegistry.byName("matrix")!!, clockStyle = ClockStyle.EPOCH)
    @Test fun clockPlainMatrix() = home("clock-plain-matrix", ThemeRegistry.byName("matrix")!!, clockStyle = ClockStyle.PLAIN)
    @Test fun clockPixelTwelve() = home("clock-pixel-12h", ThemeRegistry.byName("matrix")!!, clockStyle = ClockStyle.PIXEL, clockFormat = ClockFormat.H12)
    @Test fun clockFigletTwelve() = home("clock-figlet-12h", ThemeRegistry.byName("matrix")!!, clockStyle = ClockStyle.FIGLET, clockFormat = ClockFormat.H12)
    @Test fun clockPlainTwelve() = home("clock-plain-12h", ThemeRegistry.byName("matrix")!!, clockStyle = ClockStyle.PLAIN, clockFormat = ClockFormat.H12)

    // The six popular palettes on the home screen.
    @Test fun paletteGruvbox() = home("home-gruvbox", ThemeRegistry.byName("gruvbox")!!, quick = listOf(pkg("Phone"), pkg("Messages")))
    @Test fun paletteNord() = home("home-nord", ThemeRegistry.byName("nord")!!, quick = listOf(pkg("Phone"), pkg("Messages")))
    @Test fun paletteTokyoNight() = home("home-tokyo-night", ThemeRegistry.byName("tokyo-night")!!, quick = listOf(pkg("Phone"), pkg("Messages")))
    @Test fun paletteCatppuccin() = home("home-catppuccin", ThemeRegistry.byName("catppuccin")!!, quick = listOf(pkg("Phone"), pkg("Messages")))
    @Test fun paletteMonokai() = home("home-monokai", ThemeRegistry.byName("monokai")!!, quick = listOf(pkg("Phone"), pkg("Messages")))
    @Test fun paletteOneDark() = home("home-one-dark", ThemeRegistry.byName("one-dark")!!, quick = listOf(pkg("Phone"), pkg("Messages")))

    // The rain wallpaper (a still frame: animations are switched off so the test clock does not spin on the ticker).
    @Test fun rainWallpaper() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        android.provider.Settings.Global.putFloat(app.contentResolver, android.provider.Settings.Global.ANIMATOR_DURATION_SCALE, 0f)
        home("home-rain", ThemeRegistry.byName("matrix")!!, wall = WallSettings(mode = WallMode.RAIN, dim = 40), quick = listOf(pkg("Phone")))
    }

    // ---- home layout mockups ----
    private fun mock(name: String, style: HomeStyle, theme: String = "tokyo-night", clockStyle: ClockStyle = ClockStyle.PIXEL, panelsOnly: Boolean = false, appsOnly: Boolean = false) {
        val vm = vm()
        val th = ThemeRegistry.byName(theme)!!
        val items = listOf(
            Item.Panel(PanelKind.SYS), Item.Panel(PanelKind.CAL), Item.Panel(PanelKind.WX),
            Item.Folder("media", listOf(pkg("Spotify"), pkg("Bandcamp"))), Item.App(pkg("Maps")), Item.App(pkg("Garmin Connect")),
            Item.App(pkg("Notes")), Item.App(pkg("Signal")), Item.App(pkg("Files")), Item.App(pkg("Photos")),
        )
        val shown = when { panelsOnly -> items.filterIsInstance<Item.Panel>(); appsOnly -> items.filterNot { it is Item.Panel } + Item.App(pkg("Chrome")) + Item.App(pkg("Spotify")); else -> items }
        rule.setContent {
            TermTheme(th) {
                val base = ui(th, quick = listOf(pkg("Phone"), pkg("Messages"), pkg("Maps"), pkg("Camera")), calendar = agenda(), weather = weather(0, true, 12.0))
                val ui = base.copy(
                    layout = base.layout.copy(pages = listOf(Page(shown), Page(emptyList()))),
                    panels = base.panels.copy(cal = true, wx = true), clockStyle = clockStyle,
                    clockFormat = ClockFormat.H24,
                )
                PagerHome(ui, vm, {}, {}, {}, {}, style = style)
            }
        }
        rule.onRoot().captureRoboImage("build/shots/$name.png")
    }

    @Test fun mockClassic() = mock("mock-home-classic", HomeStyle.CLASSIC)
    @Test fun mockBottom() = mock("mock-home-bottom", HomeStyle.BOTTOM)
    @Test fun mockGrid() = mock("mock-home-grid", HomeStyle.GRID)
    @Test fun mockAppsPageGrid() = mock("mock-apps-grid", HomeStyle.GRID, appsOnly = true)
    @Test fun mockAppsPageList() = mock("mock-apps-list", HomeStyle.BOTTOM, appsOnly = true)
    @Test fun mockGlancePage() = mock("mock-glance", HomeStyle.CLASSIC, panelsOnly = true)

    @Test fun appsFirstGlancePage() {
        val vm = vm()
        val th = ThemeRegistry.byName("tokyo-night")!!
        val items = listOf(Item.Panel(PanelKind.SEARCH), Item.Panel(PanelKind.SYS), Item.Panel(PanelKind.CAL), Item.Panel(PanelKind.WX),
            Item.App(pkg("Maps")), Item.App(pkg("Notes")), Item.App(pkg("Signal")), Item.App(pkg("Files")))
        rule.setContent {
            TermTheme(th) {
                val base = ui(th, quick = listOf(pkg("Phone"), pkg("Messages"), pkg("Maps"), pkg("Camera")), calendar = agenda(), weather = weather(0, true, 12.0))
                val ui = base.copy(layout = base.layout.copy(pages = listOf(Page(items))).appsFirst(), panels = base.panels.copy(cal = true, wx = true), homeStyle = HomeStyle.GRID)
                PagerHome(ui, vm, {}, {}, {}, {})
            }
        }
        rule.onRoot().captureRoboImage("build/shots/apps-first-page1.png")
        rule.onNodeWithText("2:two").performClick()
        rule.waitForIdle()
        rule.onRoot().captureRoboImage("build/shots/apps-first-page2.png")
    }

    // ---- icon style (apps are not installed in the test, so the tiles are letter tiles) ----
    private fun iconHome(name: String, theme: String) {
        val vm = vm()
        val th = ThemeRegistry.byName(theme)!!
        val items = listOf(
            Item.Folder("media", listOf(pkg("Spotify"), pkg("Bandcamp"), pkg("Photos"), pkg("Signal"))), Item.App(pkg("Maps")), Item.App(pkg("Garmin Connect")),
            Item.App(pkg("Notes")), Item.App(pkg("Signal")), Item.App(pkg("Files")), Item.App(pkg("Photos")), Item.App(pkg("Chrome")), Item.App(pkg("Strava")),
        )
        rule.setContent {
            TermTheme(th) {
                val base = ui(th, quick = listOf(pkg("Phone"), pkg("Messages"), pkg("Maps"), pkg("Camera")))
                val ui = base.copy(layout = base.layout.copy(pages = listOf(Page(items))), homeStyle = HomeStyle.ICONS)
                PagerHome(ui, vm, {}, {}, {}, {})
            }
        }
        rule.waitForIdle()
        Thread.sleep(300); rule.waitForIdle()
        rule.onRoot().captureRoboImage("build/shots/$name.png")
    }

    private fun iconDrawer(name: String, theme: String) {
        val vm = vm()
        val th = ThemeRegistry.byName(theme)!!
        rule.setContent {
            TermTheme(th) {
                val base = ui(th, drawer = true)
                AppDrawer(base.copy(homeStyle = HomeStyle.ICONS, drawerKeyboard = false), vm, {}, {})
            }
        }
        rule.waitForIdle()
        Thread.sleep(300); rule.waitForIdle()
        rule.onRoot().captureRoboImage("build/shots/$name.png")
    }

    @Test fun iconHomeTokyo() = iconHome("icons-home-tokyo-night", "tokyo-night")
    @Test fun iconHomeMatrix() = iconHome("icons-home-matrix", "matrix")
    @Test fun iconHomeNeon() = iconHome("icons-home-neon-city", "neon-city")
    @Test fun iconDrawerTokyo() = iconDrawer("icons-drawer-tokyo-night", "tokyo-night")
    @Test fun iconDrawerMatrix() = iconDrawer("icons-drawer-matrix", "matrix")

    // ---- SVG wallpapers (art only; the dim setting is 40 here) ----
    private fun svgShot(name: String, style: dev.linkinxp.termlauncher.wall.SvgStyle, theme: String) {
        val th = ThemeRegistry.byName(theme)!!
        rule.setContent { TermTheme(th, WallSettings(mode = WallMode.SVG, svgStyle = style, dim = 30, seed = 3)) { } }
        rule.waitForIdle(); Thread.sleep(1500); rule.waitForIdle()
        rule.onRoot().captureRoboImage("build/shots/$name.png")
    }
    @Test fun svgCircuit() = svgShot("svg-circuit", dev.linkinxp.termlauncher.wall.SvgStyle.CIRCUIT, "matrix")
    @Test fun svgHexnet() = svgShot("svg-hexnet", dev.linkinxp.termlauncher.wall.SvgStyle.HEXNET, "neon-city")
    @Test fun svgRadar() = svgShot("svg-radar", dev.linkinxp.termlauncher.wall.SvgStyle.RADAR, "matrix")
    @Test fun svgTunnel() = svgShot("svg-tunnel", dev.linkinxp.termlauncher.wall.SvgStyle.TUNNEL, "neon-city")
    @Test fun svgGlitch() = svgShot("svg-glitch", dev.linkinxp.termlauncher.wall.SvgStyle.GLITCH, "matrix")
    @Test fun svgWindows() = svgShot("svg-windows", dev.linkinxp.termlauncher.wall.SvgStyle.WINDOWS, "neon-city")
    @Test fun svgHelix() = svgShot("svg-helix", dev.linkinxp.termlauncher.wall.SvgStyle.HELIX, "matrix")
    @Test fun svgTopo() = svgShot("svg-topo", dev.linkinxp.termlauncher.wall.SvgStyle.TOPO, "neon-city")
}
