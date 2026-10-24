package dev.linkinxp.termlauncher.screens

import android.app.Application
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.core.app.ApplicationProvider
import dev.linkinxp.termlauncher.LauncherUi
import dev.linkinxp.termlauncher.LauncherViewModel
import dev.linkinxp.termlauncher.PromptState
import dev.linkinxp.termlauncher.theme.ThemeRegistry
import dev.linkinxp.termlauncher.ui.PromptBar
import dev.linkinxp.termlauncher.ui.TermTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** The home screen does not list your previous commands above the prompt (the `history` command still does). */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34])
class PromptHistoryRowsTest {
    @get:Rule val rule = createComposeRule()

    @Test fun anEmptyPromptShowsNoHistoryRows() {
        val vm = LauncherViewModel(ApplicationProvider.getApplicationContext<Application>())
        val ui = LauncherUi(prompt = PromptState(history = listOf("maps", "theme matrix")))
        rule.setContent { TermTheme(ThemeRegistry.default) { PromptBar(ui, vm) } }
        rule.onNodeWithText("^ maps").assertDoesNotExist()
        rule.onNodeWithText("^ theme matrix").assertDoesNotExist()
    }
}
