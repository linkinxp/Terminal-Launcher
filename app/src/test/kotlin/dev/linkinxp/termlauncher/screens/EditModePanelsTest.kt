package dev.linkinxp.termlauncher.screens

import android.app.Application
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.core.app.ApplicationProvider
import dev.linkinxp.termlauncher.LauncherUi
import dev.linkinxp.termlauncher.LauncherViewModel
import dev.linkinxp.termlauncher.layout.HomeLayout
import dev.linkinxp.termlauncher.theme.ThemeRegistry
import dev.linkinxp.termlauncher.ui.EditMode
import dev.linkinxp.termlauncher.ui.TermTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** The edit screen names every panel on its own line and keeps all the page buttons reachable. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34])
class EditModePanelsTest {
    @get:Rule val rule = createComposeRule()

    @Test fun everyPanelIsNamedAndThePageButtonsAreAllThere() {
        val vm = LauncherViewModel(ApplicationProvider.getApplicationContext<Application>())
        val ui = LauncherUi(theme = ThemeRegistry.default, layout = HomeLayout.default().withDefaultPanels().addPage())
        rule.setContent { TermTheme(ui.theme) { EditMode(ui, vm, { _, _ -> }, {}, {}) } }
        listOf("search bar panel", "sys stats panel", "calendar panel", "weather panel").forEach { rule.onNodeWithText(it).assertIsDisplayed() }
        // two pages: every button shows up once per page
        listOf("[+app]", "[+dir]", "[+widget]", "[+panel]").forEach { rule.onAllNodesWithText(it).assertCountEquals(2) }
    }
}
