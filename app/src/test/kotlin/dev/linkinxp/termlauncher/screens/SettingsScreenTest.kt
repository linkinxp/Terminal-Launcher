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
import kotlinx.coroutines.runBlocking
import dev.linkinxp.termlauncher.ui.SettingsScreen
import dev.linkinxp.termlauncher.ui.TermTheme
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
        runBlocking { BackupRepository(Prefs(app)).apply(LauncherBackup()) }
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
        vm.setWall(WallSettings(dim = 55))
        until { vm.ui.value.wall.dim == 55 }
        click("dim-plus"); until { vm.ui.value.wall.dim == 60 }
        click("dim-plus"); until { vm.ui.value.wall.dim == 65 }
        vm.setWall(WallSettings(dim = 98))
        until { vm.ui.value.wall.dim == 98 }
        click("dim-plus"); until { vm.ui.value.wall.dim == 100 }
        click("dim-plus"); settle()
        assertEquals(100, vm.ui.value.wall.dim)
        vm.setWall(WallSettings(dim = 3))
        until({ "wall=${vm.ui.value.wall}" }) { vm.ui.value.wall.dim == 3 }
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
        vm.setWall(WallSettings(dim = 55))
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

    @Test fun theDrawerKeyboardSettingStartsOffAndToggles() {
        val vm = show()
        assertEquals(false, vm.ui.value.drawerKeyboard)
        click("drawer-keyboard"); until({ "kbd=${vm.ui.value.drawerKeyboard}" }) { vm.ui.value.drawerKeyboard }
        click("drawer-keyboard"); until({ "kbd=${vm.ui.value.drawerKeyboard}" }) { !vm.ui.value.drawerKeyboard }
    }
}
