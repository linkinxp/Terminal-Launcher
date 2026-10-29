package dev.linkinxp.termlauncher.screens

import android.app.Application
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.core.app.ApplicationProvider
import dev.linkinxp.termlauncher.LauncherViewModel
import dev.linkinxp.termlauncher.backup.BackupRepository
import dev.linkinxp.termlauncher.backup.LauncherBackup
import dev.linkinxp.termlauncher.data.Prefs
import dev.linkinxp.termlauncher.theme.BorderStyle
import dev.linkinxp.termlauncher.theme.buildCustomTheme
import kotlinx.coroutines.runBlocking
import dev.linkinxp.termlauncher.ui.SettingsScreen
import dev.linkinxp.termlauncher.ui.TermTheme
import dev.linkinxp.termlauncher.wall.WallMode
import dev.linkinxp.termlauncher.wall.WallSettings
import android.os.Looper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.robolectric.Shadows.shadowOf
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34])
class SettingsScreenTest {
    @get:Rule val rule = createComposeRule()

    private fun show(): LauncherViewModel {
        val app = ApplicationProvider.getApplicationContext<Application>()
        // The DataStore is shared by the tests of this JVM: start every test from the defaults.
        runBlocking { BackupRepository(Prefs(app)).apply(LauncherBackup(customThemes = emptyList())) }
        val vm = LauncherViewModel(app)
        rule.setContent {
            val ui by vm.ui.collectAsState()
            TermTheme(ui.theme, ui.wall) { SettingsScreen(ui, vm, {}, {}, {}, {}) }
        }
        repeat(10) { shadowOf(Looper.getMainLooper()).idle(); rule.waitForIdle(); Thread.sleep(20) } // let the stores load
        return vm
    }

    /** Lets writes that were started by a tap (even a no-op one) finish before the test writes something else. */
    private fun settle() = repeat(15) { shadowOf(Looper.getMainLooper()).idle(); rule.waitForIdle(); Thread.sleep(20) }

    // The screen scrolls: bring the control into view first, as a user would.
    private fun click(tag: String) = rule.onNodeWithTag(tag).performScrollTo().performClick()
    /** DataStore writes run on a background thread and results come back through the main looper: pump both. */
    private fun until(describe: () -> String = { "" }, condition: () -> Boolean) {
        val end = System.currentTimeMillis() + 10_000
        while (!condition() && System.currentTimeMillis() < end) {
            shadowOf(Looper.getMainLooper()).idle()
            rule.waitForIdle()
            Thread.sleep(20)
        }
        assertTrue("condition not met in 10 s: ${describe()}", condition())
    }

    @Test fun showsEverySection() {
        show()
        listOf("theme", "wallpaper", "effects", "panels", "drawer", "backup", "about").forEach { rule.onNodeWithText(it).assertExists() }
    }

    @Test fun tappingAThemeAppliesIt() {
        val vm = show()
        click("theme-matrix")
        until { vm.ui.value.theme.name == "matrix" }
        assertEquals("matrix", vm.ui.value.theme.name)
    }

    @Test fun dimStepsByFiveAndStaysWithinZeroToOneHundred() {
        val vm = show()
        vm.setWall(WallSettings(mode = WallMode.SYSTEM, dim = 55))
        until { vm.ui.value.wall.mode == WallMode.SYSTEM && vm.ui.value.wall.dim == 55 }; settle()
        click("dim-plus"); until { vm.ui.value.wall.dim == 60 }
        click("dim-plus"); until { vm.ui.value.wall.dim == 65 }
        vm.setWall(WallSettings(mode = WallMode.SYSTEM, dim = 98))
        until { vm.ui.value.wall.dim == 98 }; settle()
        click("dim-plus"); until { vm.ui.value.wall.dim == 100 }
        click("dim-plus"); settle()
        assertEquals(100, vm.ui.value.wall.dim)
        vm.setWall(WallSettings(mode = WallMode.SYSTEM, dim = 3))
        until({ "wall=${vm.ui.value.wall}" }) { vm.ui.value.wall.dim == 3 }; settle()
        click("dim-minus"); until { vm.ui.value.wall.dim == 0 }
        click("dim-minus"); settle()
        assertEquals(0, vm.ui.value.wall.dim)
    }

    @Test fun scanlinesCycleAutoOnOffAuto() {
        val vm = show()
        assertEquals(null, vm.ui.value.fx.scanlines)
        click("scanlines-row"); until { vm.ui.value.fx.scanlines == true }
        click("scanlines-row"); until { vm.ui.value.fx.scanlines == false }
        click("scanlines-row"); until { vm.ui.value.fx.scanlines == null }
    }

    @Test fun togglingAnEffectFlipsIt() {
        val vm = show()
        assertEquals(false, vm.ui.value.fx.flicker)
        click("flicker-row"); until { vm.ui.value.fx.flicker }
        assertEquals(true, vm.ui.value.fx.flicker)
    }

    @Test fun rapidDimStepsAreAllApplied() {
        val vm = show()
        vm.setWall(WallSettings(mode = WallMode.SYSTEM, dim = 55))
        until { vm.ui.value.wall.dim == 55 }
        repeat(4) { vm.updateWall { it.copy(dim = (it.dim + 5).coerceIn(0, 100)) } } // no waiting in between
        until({ "wall=${vm.ui.value.wall}" }) { vm.ui.value.wall.dim == 75 }
    }

    @Test fun twoQuickEffectChangesBothStick() {
        val vm = show()
        vm.setFx("flicker", true)
        vm.setFx("glitch", false)
        until({ "fx=${vm.ui.value.fx}" }) { vm.ui.value.fx.flicker && !vm.ui.value.fx.glitch }
    }

    @Test fun quickTogglesOfTheSameEffectEndWhereTheyShould() {
        val vm = show()
        repeat(3) { vm.toggleFx("cursor") } // true -> false -> true -> false
        until({ "fx=${vm.ui.value.fx}" }) { !vm.ui.value.fx.cursor }
        settle()
        assertEquals(false, vm.ui.value.fx.cursor)
    }

    @Test fun theDrawerKeyboardSettingStartsOnAndToggles() {
        val vm = show()
        assertEquals(true, vm.ui.value.drawerKeyboard)
        click("drawer-keyboard"); until({ "kbd=${vm.ui.value.drawerKeyboard}" }) { !vm.ui.value.drawerKeyboard }
        click("drawer-keyboard"); until({ "kbd=${vm.ui.value.drawerKeyboard}" }) { vm.ui.value.drawerKeyboard }
    }

    @Test fun theClockRowCyclesAutoTwentyFourTwelve() {
        val vm = show()
        assertEquals(dev.linkinxp.termlauncher.clock.ClockFormat.AUTO, vm.ui.value.clockFormat)
        click("clock-row"); until { vm.ui.value.clockFormat == dev.linkinxp.termlauncher.clock.ClockFormat.H24 }
        click("clock-row"); until { vm.ui.value.clockFormat == dev.linkinxp.termlauncher.clock.ClockFormat.H12 }
        click("clock-row"); until { vm.ui.value.clockFormat == dev.linkinxp.termlauncher.clock.ClockFormat.AUTO }
    }

    @Test fun theClockStyleRowCyclesThroughAllSevenAndWraps() {
        val vm = show()
        val order = dev.linkinxp.termlauncher.clock.ClockStyle.entries
        assertEquals(order[0], vm.ui.value.clockStyle)
        for (next in order.drop(1) + order[0]) {
            click("clock-style-row"); until({ "style=${vm.ui.value.clockStyle}" }) { vm.ui.value.clockStyle == next }
        }
    }

    @Test fun aCustomThemeIsListedAppliesWhenTappedAndCanBeRemoved() {
        val vm = show()
        runBlocking { Prefs(ApplicationProvider.getApplicationContext<Application>()).editCustomThemes { it + buildCustomTheme("mine", 0xFF101010, 0xFFEEEEEE, 0xFF3366FF, 0xFF888888, BorderStyle.DOUBLE) } }
        until({ "custom=${vm.ui.value.customThemes}" }) { vm.ui.value.customThemes.size == 1 }
        click("theme-mine"); until({ "theme=${vm.ui.value.theme.name}" }) { vm.ui.value.theme.name == "mine" }
        click("theme-rm-mine")
        until({ "custom=${vm.ui.value.customThemes} theme=${vm.ui.value.theme.name}" }) { vm.ui.value.customThemes.isEmpty() && vm.ui.value.theme.name == "classy-black" }
    }

    @Test fun removingTwoCustomThemesInQuickSuccessionRemovesBoth() {
        val vm = show()
        val prefs = Prefs(ApplicationProvider.getApplicationContext<Application>())
        runBlocking { prefs.editCustomThemes { listOf("a", "b", "c").map { buildCustomTheme(it, 0xFF101010, 0xFFEEEEEE, 0xFF3366FF, 0xFF888888, BorderStyle.SINGLE) } } }
        until({ "custom=${vm.ui.value.customThemes.map { it.name }}" }) { vm.ui.value.customThemes.size == 3 }
        vm.removeCustomTheme("a"); vm.removeCustomTheme("b") // before the UI state has caught up with the first
        until({ "custom=${vm.ui.value.customThemes.map { it.name }}" }) { vm.ui.value.customThemes.map { it.name } == listOf("c") }
        settle()
        assertEquals(listOf("c"), vm.ui.value.customThemes.map { it.name })
    }

    @Test fun theHomeLayoutRowCyclesClassicBottomGrid() {
        val vm = show()
        val styles = dev.linkinxp.termlauncher.layout.HomeStyle.entries
        assertEquals(styles[0], vm.ui.value.homeStyle)
        for (next in styles.drop(1) + styles[0]) {
            click("home-style-row"); until({ "style=${vm.ui.value.homeStyle}" }) { vm.ui.value.homeStyle == next }
        }
    }

    @Test fun theDoubleTapLockRowTogglesAndIsOnByDefault() {
        val vm = show()
        assertEquals(true, vm.ui.value.doubleTapLock)
        click("gesture-lock"); until({ "lock=${vm.ui.value.doubleTapLock}" }) { !vm.ui.value.doubleTapLock }
        click("gesture-lock"); until({ "lock=${vm.ui.value.doubleTapLock}" }) { vm.ui.value.doubleTapLock }
    }

    @Test fun theDensityRowCyclesTheFourLevelsAndWraps() {
        val vm = show()
        val levels = dev.linkinxp.termlauncher.layout.Density.entries
        assertEquals(dev.linkinxp.termlauncher.layout.Density.NORMAL, vm.ui.value.density)
        val order = listOf(levels[3], levels[0], levels[1], levels[2]) // from normal: roomy, then wraps to dense, compact, normal
        for (next in order) { click("density-row"); until({ "density=${vm.ui.value.density}" }) { vm.ui.value.density == next } }
    }

    private fun has(tag: String) = rule.onAllNodes(androidx.compose.ui.test.hasTestTag(tag)).fetchSemanticsNodes().isNotEmpty()

    private fun showWithMode(mode: WallMode): LauncherViewModel {
        val vm = show()
        vm.setWall(WallSettings(mode = mode))
        until({ "mode=${vm.ui.value.wall.mode}" }) { vm.ui.value.wall.mode == mode }
        settle()
        return vm
    }

    @Test fun aSolidWallpaperOnlyShowsTheModeRow() {
        showWithMode(WallMode.SOLID)
        assertTrue(has("wall-mode"))
        listOf("wall-style", "wall-svg-style", "wall-random", "dim-plus", "wall-animate").forEach { assertTrue("$it should be hidden", !has(it)) }
    }

    @Test fun theSvgWallpaperShowsItsStyleSeedAndDimOnly() {
        showWithMode(WallMode.SVG)
        listOf("wall-svg-style", "wall-random", "dim-plus").forEach { assertTrue("$it should show", has(it)) }
        listOf("wall-style", "wall-animate").forEach { assertTrue("$it should be hidden", !has(it)) }
    }

    @Test fun theAsciiWallpaperShowsItsOwnFieldsAndNotTheSvgStyle() {
        showWithMode(WallMode.ASCII)
        listOf("wall-style", "wall-random", "dim-plus", "wall-animate").forEach { assertTrue("$it should show", has(it)) }
        assertTrue(!has("wall-svg-style"))
    }

    @Test fun theSystemWallpaperOnlyHasDim() {
        showWithMode(WallMode.SYSTEM)
        assertTrue(has("dim-plus"))
        listOf("wall-style", "wall-svg-style", "wall-random", "wall-animate").forEach { assertTrue("$it should be hidden", !has(it)) }
    }

    @Test fun theBadgesRowTogglesAndIsOnByDefault() {
        val vm = show()
        assertEquals(true, vm.ui.value.badges)
        click("badges-row"); until({ "badges=${vm.ui.value.badges}" }) { !vm.ui.value.badges }
        click("badges-row"); until({ "badges=${vm.ui.value.badges}" }) { vm.ui.value.badges }
    }

    @Test fun onlyTheAccessibilityRowIsHiddenInABuildWithoutTheService() {
        dev.linkinxp.termlauncher.gestures.Features.lockService = false
        try {
            show()
            assertTrue(!has("gesture-service") && has("gesture-lock") && has("settings-lock-admin"))
        } finally {
            dev.linkinxp.termlauncher.gestures.Features.lockService = true
        }
    }

    @Test fun theLockServiceRowsShowWhereTheServiceExists() {
        dev.linkinxp.termlauncher.gestures.Features.lockService = true
        show()
        assertTrue(has("gesture-lock") && has("gesture-service"))
    }
}
