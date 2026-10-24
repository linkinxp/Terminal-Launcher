package dev.linkinxp.termlauncher.screens

import android.app.Application
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import dev.linkinxp.termlauncher.LauncherUi
import dev.linkinxp.termlauncher.LauncherViewModel
import dev.linkinxp.termlauncher.layout.HomeLayout
import dev.linkinxp.termlauncher.layout.Item
import dev.linkinxp.termlauncher.layout.Page
import dev.linkinxp.termlauncher.layout.PanelKind
import dev.linkinxp.termlauncher.theme.ThemeRegistry
import dev.linkinxp.termlauncher.ui.PagerHome
import dev.linkinxp.termlauncher.ui.TermTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Info panels render where the layout puts them: the search bar sits on page two, sys stays on page one. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34])
class PanelPlacementTest {
    @get:Rule val rule = createComposeRule()

    @Test fun aPanelAppearsOnlyOnItsOwnPage() {
        val vm = LauncherViewModel(ApplicationProvider.getApplicationContext<Application>())
        val layout = HomeLayout(emptyList(), listOf(Page(listOf(Item.Panel(PanelKind.SYS))), Page(listOf(Item.Panel(PanelKind.SEARCH)))))
        val ui = LauncherUi(theme = ThemeRegistry.default, layout = layout)
        rule.setContent { TermTheme(ui.theme) { PagerHome(ui, vm, {}, {}, {}, {}) } }
        rule.onNodeWithText("sys").assertIsDisplayed()
        rule.onNodeWithText("search google").assertDoesNotExist()
        rule.onNodeWithText("2:two").performClick()
        rule.waitForIdle()
        rule.onNodeWithText("search google").assertIsDisplayed()
    }

    @Test fun aPanelThatWasTakenOffEveryPageIsGone() {
        val vm = LauncherViewModel(ApplicationProvider.getApplicationContext<Application>())
        val ui = LauncherUi(theme = ThemeRegistry.default, layout = HomeLayout.default())
        rule.setContent { TermTheme(ui.theme) { PagerHome(ui, vm, {}, {}, {}, {}) } }
        rule.onNodeWithText("sys").assertDoesNotExist()
        rule.onNodeWithText("search google").assertDoesNotExist()
    }
}
