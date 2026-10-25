package dev.linkinxp.termlauncher.screens

import android.app.Application
import android.os.Looper
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import dev.linkinxp.termlauncher.LauncherUi
import dev.linkinxp.termlauncher.LauncherViewModel
import dev.linkinxp.termlauncher.apps.AppEntry
import dev.linkinxp.termlauncher.apps.DrawerSort
import dev.linkinxp.termlauncher.apps.buildDrawer
import dev.linkinxp.termlauncher.layout.HomeStyle
import dev.linkinxp.termlauncher.theme.ThemeRegistry
import dev.linkinxp.termlauncher.ui.AppDrawer
import dev.linkinxp.termlauncher.ui.TermTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34])
class IconDrawerTest {
    @get:Rule val rule = createComposeRule()

    private val names = listOf("Bandcamp", "Calendar", "Camera", "Chrome", "Maps", "Notes")
    private val apps = names.mapIndexed { i, n -> AppEntry("pkg.${n.lowercase()}", n, i.toLong()) }

    private fun show(filter: String = "", style: HomeStyle = HomeStyle.ICONS): LauncherViewModel {
        val vm = LauncherViewModel(ApplicationProvider.getApplicationContext<Application>())
        val drawer = buildDrawer(apps, emptySet(), emptyMap(), DrawerSort.AZ, filter, false)
        val ui = LauncherUi(
            theme = ThemeRegistry.default, apps = apps.associateBy { it.pkg }, drawer = drawer, filter = filter,
            drawerOpen = true, homeStyle = style, drawerKeyboard = false,
        )
        rule.setContent { TermTheme(ui.theme) { AppDrawer(ui, vm, {}, {}) } }
        return vm
    }

    @Test fun theGridShowsLetterHeadersCellsAndTheRail() {
        show()
        rule.onNodeWithText("-- B --").assertIsDisplayed()
        rule.onNodeWithText("bandcamp").assertIsDisplayed()
        rule.onNodeWithText("camera").assertIsDisplayed()
        assertTrue(rule.onAllNodesWithText("B").fetchSemanticsNodes().isNotEmpty()) // the A-Z rail
    }

    @Test fun searchingShowsAFlatGridWithoutHeaders() {
        show(filter = "cam")
        rule.onNodeWithText("camera").assertIsDisplayed()
        assertEquals(0, rule.onAllNodesWithText("-- C --").fetchSemanticsNodes().size)
    }

    @Test fun theControlsLiveInTheBottomBar() {
        show()
        rule.onNodeWithTag("drawer-cfg").assertIsDisplayed()
        rule.onNodeWithText("sort:a-z", substring = true).assertIsDisplayed()
        rule.onNodeWithTag("drawer-search").assertIsDisplayed()
    }

    @Test fun tappingACellLaunchesIt() {
        val vm = show()
        rule.onNodeWithText("maps").performClick()
        val end = System.currentTimeMillis() + 8_000
        while (vm.ui.value.prompt.message == null && System.currentTimeMillis() < end) { shadowOf(Looper.getMainLooper()).idle(); rule.waitForIdle(); Thread.sleep(20) }
        assertEquals("exec: pkg.maps: not found", vm.ui.value.prompt.message) // no such app installed in the test: launch reports its package
    }

    @Test fun otherStylesKeepTheListDrawer() {
        show(style = HomeStyle.CLASSIC)
        rule.onNodeWithText("-- B --").assertIsDisplayed()
        rule.onNodeWithText("| grep ", substring = true).assertIsDisplayed()
    }
}
