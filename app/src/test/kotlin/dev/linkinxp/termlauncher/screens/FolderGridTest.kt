package dev.linkinxp.termlauncher.screens

import android.app.Application
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTouchInput
import androidx.test.core.app.ApplicationProvider
import dev.linkinxp.termlauncher.LauncherUi
import dev.linkinxp.termlauncher.LauncherViewModel
import dev.linkinxp.termlauncher.apps.AppEntry
import dev.linkinxp.termlauncher.layout.HomeLayout
import dev.linkinxp.termlauncher.layout.HomeStyle
import dev.linkinxp.termlauncher.layout.Item
import dev.linkinxp.termlauncher.layout.Page
import dev.linkinxp.termlauncher.theme.ThemeRegistry
import dev.linkinxp.termlauncher.ui.FolderPanel
import dev.linkinxp.termlauncher.ui.TermTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** In the icon style a folder opens as a grid of icons that can be reordered by dragging. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34])
class FolderGridTest {
    @get:Rule val rule = createComposeRule()
    private val names = listOf("alpha", "bravo", "charlie")
    private val apps = names.associate { "pkg.$it" to AppEntry("pkg.$it", it.replaceFirstChar(Char::uppercase), 0) }
    private val moves = mutableListOf<Triple<String, Int, Int>>()
    private val launched = mutableListOf<String>()

    private fun show(style: HomeStyle) {
        val vm = LauncherViewModel(ApplicationProvider.getApplicationContext<Application>())
        val ui = LauncherUi(
            theme = ThemeRegistry.default, layout = HomeLayout(emptyList(), listOf(Page(listOf(Item.Folder("work", names.map { "pkg.$it" }))))),
            apps = apps, homeStyle = style,
        )
        rule.setContent { TermTheme(ui.theme) { FolderPanel("work", ui, vm, onReorder = { f, a, b -> moves += Triple(f, a, b) }, onDismiss = {}) } }
        rule.waitForIdle()
    }

    private fun center(n: String) = rule.onNodeWithText(n).fetchSemanticsNode().boundsInRoot.center

    @Test fun theIconStyleShowsIconsAndTheTextStylesKeepRows() {
        show(HomeStyle.ICONS)
        rule.waitUntil(8_000) { rule.onAllNodesWithContentDescription("icon-pkg.alpha").fetchSemanticsNodes().isNotEmpty() }
        rule.onNodeWithText("alpha").assertIsDisplayed()
    }

    @Test fun draggingOneIconOverAnotherReordersTheFolder() {
        show(HomeStyle.ICONS)
        val from = center("alpha"); val to = center("charlie")
        rule.onNodeWithText("alpha").performTouchInput { down(center) }
        rule.mainClock.advanceTimeBy(800)
        rule.onNodeWithText("alpha").performTouchInput { moveBy(Offset(to.x - from.x, to.y - from.y), 200); up() }
        rule.waitForIdle()
        assertEquals(listOf(Triple("work", 0, 2)), moves)
    }

    @Test fun otherStylesKeepTheTextList() {
        show(HomeStyle.CLASSIC)
        rule.waitForIdle()
        assertEquals(0, rule.onAllNodesWithContentDescription("icon-pkg.alpha").fetchSemanticsNodes().size)
    }
}
