package dev.linkinxp.termlauncher.screens

import android.app.Application
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.test.core.app.ApplicationProvider
import com.github.takahirom.roborazzi.captureRoboImage
import dev.linkinxp.termlauncher.LauncherUi
import dev.linkinxp.termlauncher.LauncherViewModel
import dev.linkinxp.termlauncher.PromptState
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
            drawer = buildDrawer(appList, emptySet(), stats, DrawerSort.AZ, "", false),
            drawerOpen = drawer,
            prompt = prompt,
            fx = fx,
            wall = wall,
        )
    }

    private fun vm() = LauncherViewModel(ApplicationProvider.getApplicationContext<Application>())

    private fun home(
        name: String,
        theme: Theme,
        prompt: PromptState = PromptState(),
        fx: FxSettings = FxSettings(),
        wall: WallSettings = WallSettings(),
    ) {
        val vm = vm()
        rule.setContent {
            TermTheme(theme, wall) {
                val ui = ui(theme, prompt = prompt, fx = fx, wall = wall)
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
}
