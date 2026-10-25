package dev.linkinxp.termlauncher.screens

import android.app.Application
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.test.core.app.ApplicationProvider
import dev.linkinxp.termlauncher.LauncherUi
import dev.linkinxp.termlauncher.LauncherViewModel
import dev.linkinxp.termlauncher.apps.AppEntry
import dev.linkinxp.termlauncher.layout.HomeLayout
import dev.linkinxp.termlauncher.layout.Item
import dev.linkinxp.termlauncher.theme.ThemeRegistry
import dev.linkinxp.termlauncher.ui.AppMenu
import dev.linkinxp.termlauncher.ui.TermTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** An app from the drawer can be put on the home screen from its long-press menu. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34])
class AddToHomeTest {
    @get:Rule val rule = createComposeRule()
    private val maps = AppEntry("pkg.maps", "Maps", 0)
    private fun vm() = LauncherViewModel(ApplicationProvider.getApplicationContext<Application>())

    @Test fun layoutPutsTheAppOnTheFirstPageWithoutPanels() {
        val l = HomeLayout.fresh().addToHome("a") // fresh = apps page, then the glance page
        assertEquals(listOf<Item>(Item.App("a")), l.pages[0].items)
        val panelsFirst = HomeLayout.fresh().movePage(0, 1).addToHome("a")
        assertEquals(listOf<Item>(Item.App("a")), panelsFirst.pages[1].items)
    }

    @Test fun addingTwiceOrAlreadyPlacedChangesNothing() {
        val l = HomeLayout.fresh().addToHome("a")
        assertEquals(l, l.addToHome("a"))
        assertEquals(HomeLayout.fresh().pin("a"), HomeLayout.fresh().pin("a").addToHome("a")) // docked counts as placed
    }

    @Test fun theMenuOffersAddToHomeOnlyForAppsNotOnHome() {
        val off = LauncherUi(theme = ThemeRegistry.default, layout = HomeLayout.fresh(), apps = mapOf(maps.pkg to maps))
        rule.setContent { TermTheme(off.theme) { AppMenu("pkg.maps", off, vm()) {} } }
        rule.onNodeWithText("add to home").assertIsDisplayed()
    }

    @Test fun theMenuHidesAddToHomeWhenItIsAlreadyThere() {
        val on = LauncherUi(theme = ThemeRegistry.default, layout = HomeLayout.fresh().addToHome("pkg.maps"), apps = mapOf(maps.pkg to maps))
        rule.setContent { TermTheme(on.theme) { AppMenu("pkg.maps", on, vm()) {} } }
        assertEquals(0, rule.onAllNodesWithText("add to home").fetchSemanticsNodes().size)
        rule.onNodeWithText("remove from home").assertIsDisplayed()
    }
}
