package dev.linkinxp.termlauncher.screens

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import dev.linkinxp.termlauncher.panel.PanelData
import dev.linkinxp.termlauncher.theme.ThemeRegistry
import dev.linkinxp.termlauncher.ui.PanelBlock
import dev.linkinxp.termlauncher.ui.TermTheme
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** A long panel (the calendar) shows a few lines and scrolls inside itself for the rest. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34])
class PanelScrollTest {
    @get:Rule val rule = createComposeRule()
    private val lines = (1..30).map { "event number $it" }

    @Test fun onlyTheFirstLinesShowUntilYouScroll() {
        rule.setContent { TermTheme(ThemeRegistry.default) { PanelBlock("cal", PanelData.Lines(lines), scrollAfter = 6) } }
        rule.onNodeWithText("event number 1").assertIsDisplayed()
        rule.onNodeWithText("event number 30").let { assertFalse(runCatching { it.assertIsDisplayed() }.isSuccess) }
        rule.onNodeWithText("event number 30").performScrollTo().assertIsDisplayed()
    }

    @Test fun aShortPanelIsNotCut() {
        rule.setContent { TermTheme(ThemeRegistry.default) { PanelBlock("cal", PanelData.Lines(lines.take(4)), scrollAfter = 6) } }
        rule.onNodeWithText("event number 4").assertIsDisplayed()
    }
}
