package dev.linkinxp.termlauncher.screens

import android.app.Application
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import dev.linkinxp.termlauncher.LauncherUi
import dev.linkinxp.termlauncher.LauncherViewModel
import dev.linkinxp.termlauncher.apps.AppEntry
import dev.linkinxp.termlauncher.layout.HomeLayout
import dev.linkinxp.termlauncher.layout.Item
import dev.linkinxp.termlauncher.sys.SysSnapshot
import dev.linkinxp.termlauncher.theme.ThemeRegistry
import dev.linkinxp.termlauncher.ui.EditMode
import dev.linkinxp.termlauncher.ui.SettingsScreen
import dev.linkinxp.termlauncher.ui.StatusLine
import dev.linkinxp.termlauncher.ui.TermTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.time.LocalDateTime

/** Usability fixes found in the UX review: reachable tap targets, safe page removal, settings that cover everything. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34])
class UxFixesTest {
    @get:Rule val rule = createComposeRule()
    private val vm get() = LauncherViewModel(ApplicationProvider.getApplicationContext<Application>())
    private val alpha = AppEntry("a", "Alpha", 0)
    private val hiddenApp = AppEntry("h", "Secret", 1)

    private fun assertBig(label: String, w: Float, h: Float) {
        assertTrue("$label is ${w}x${h}dp, under 48dp", w >= 48f && h >= 48f)
    }

    private fun bounds(node: androidx.compose.ui.test.SemanticsNodeInteraction): Pair<Float, Float> {
        val b = node.getUnclippedBoundsInRoot()
        return (b.right - b.left).value to (b.bottom - b.top).value
    }

    @Test fun editModeButtonsAreAtLeast48dp() {
        val ui = LauncherUi(theme = ThemeRegistry.default, layout = HomeLayout.fresh().addPage(), apps = mapOf("a" to alpha))
        rule.setContent { TermTheme(ui.theme) { EditMode(ui, vm, { _, _ -> }, {}, {}) } }
        for (label in listOf("[up]", "[down]", "[rm]", "[+app]", "[+dir]", "[settings]", "[done]")) {
            rule.onAllNodesWithText(label).fetchSemanticsNodes().forEachIndexed { i, n ->
                assertBig("$label #$i", n.size.width.toFloat(), n.size.height.toFloat()) // test density is 1px = 1dp
            }
        }
    }

    @Test fun settingsCloseStatusCfgAndRowsAreAtLeast48dp() {
        val ui = LauncherUi(theme = ThemeRegistry.default, layout = HomeLayout.fresh(), apps = emptyMap())
        rule.setContent { TermTheme(ui.theme) {
            StatusLine(2, 0, SysSnapshot(50, false, 40, 40, 1000L, "wifi"), LocalDateTime.now(), true, {}, {})
            SettingsScreen(ui, vm, {}, {}, {}, {})
        } }
        for (tag in listOf("settings-close", "status-cfg", "wall-mode", "clock-row", "badges-row")) {
            val (w, h) = bounds(rule.onNodeWithTag(tag))
            assertBig(tag, w, h)
        }
    }

    @Test fun removingAPageWithItemsAsksFirstAndAnEmptyOneDoesNot() {
        val layout = HomeLayout.default().addPage().addToPage(1, Item.App("a")).addPage()
        val ui = LauncherUi(theme = ThemeRegistry.default, layout = layout, apps = mapOf("a" to alpha))
        rule.setContent { TermTheme(ui.theme) { EditMode(ui, vm, { _, _ -> }, {}, {}) } }
        rule.onNodeWithTag("page-rm-1").performClick()
        rule.onNodeWithText("move 1 item to page 1?").assertIsDisplayed()
        rule.onNodeWithTag("page-rm-confirm").assertIsDisplayed()
        rule.onNodeWithTag("page-rm-cancel").performClick()
        assertEquals(0, rule.onAllNodesWithText("move 1 item to page 1?").fetchSemanticsNodes().size)
        rule.onNodeWithTag("page-rm-2").performClick() // page 3 is empty: no question
        assertEquals(0, rule.onAllNodesWithText("move", substring = true).fetchSemanticsNodes().size)
    }

    @Test fun settingsListsHiddenAppsWithUnhideAndOffersPageEditing() {
        var edited = 0
        val ui = LauncherUi(theme = ThemeRegistry.default, layout = HomeLayout.fresh(), apps = mapOf("h" to hiddenApp), hidden = setOf("h"))
        rule.setContent { TermTheme(ui.theme) { SettingsScreen(ui, vm, {}, {}, {}, onEditPages = { edited++ }, onClose = {}) } }
        rule.onNodeWithText("secret", ignoreCase = true).performScrollTo().assertIsDisplayed()
        rule.onNodeWithTag("unhide-h").performScrollTo().assertIsDisplayed()
        rule.onNodeWithTag("settings-edit-pages").performScrollTo().performClick()
        assertEquals(1, edited)
    }
}
