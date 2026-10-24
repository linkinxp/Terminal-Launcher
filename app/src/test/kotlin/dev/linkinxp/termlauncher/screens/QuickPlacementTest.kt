package dev.linkinxp.termlauncher.screens

import android.app.Application
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import dev.linkinxp.termlauncher.LauncherUi
import dev.linkinxp.termlauncher.LauncherViewModel
import dev.linkinxp.termlauncher.apps.AppEntry
import dev.linkinxp.termlauncher.layout.HomeLayout
import dev.linkinxp.termlauncher.layout.Item
import dev.linkinxp.termlauncher.theme.ThemeRegistry
import dev.linkinxp.termlauncher.ui.PagerHome
import dev.linkinxp.termlauncher.ui.TermTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** The quick tiles live in the bottom area, directly above the pinned apps, and stay there on every page. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34])
class QuickPlacementTest {
    @get:Rule val rule = createComposeRule()

    private val phone = AppEntry("pkg.phone", "Phone", 0)
    private val maps = AppEntry("pkg.maps", "Maps", 1)

    private fun show() {
        val vm = LauncherViewModel(ApplicationProvider.getApplicationContext<Application>())
        val layout = HomeLayout(dock = listOf(Item.App(maps.pkg)), pages = HomeLayout.default().withDefaultPanels().addPage().pages)
        val ui = LauncherUi(
            theme = ThemeRegistry.default,
            layout = layout,
            apps = listOf(phone, maps).associateBy { it.pkg },
            quick = listOf(phone.pkg),
        )
        rule.setContent { TermTheme(ui.theme) { PagerHome(ui, vm, {}, {}, {}, {}) } }
    }

    @Test fun theTileSitsAboveThePinnedAppsAndBelowTheHomeContent() {
        show()
        val tile = rule.onNodeWithTag("quick-pkg.phone").assertIsDisplayed().getUnclippedBoundsInRoot()
        val dock = rule.onNodeWithText("mps").getUnclippedBoundsInRoot() // the dock tab for Maps
        val sys = rule.onNodeWithText("sys").getUnclippedBoundsInRoot()
        assertTrue("tile ${tile.bottom} must end above the dock ${dock.top}", tile.bottom <= dock.top)
        assertTrue("tile ${tile.top} must be below the sys panel title ${sys.top}", tile.top > sys.top)
    }

    @Test fun theTileIsStillThereOnTheSecondPage() {
        show()
        rule.onNodeWithText("2:two").performClick()
        rule.waitForIdle()
        rule.onNodeWithTag("quick-pkg.phone").assertIsDisplayed()
    }
}
