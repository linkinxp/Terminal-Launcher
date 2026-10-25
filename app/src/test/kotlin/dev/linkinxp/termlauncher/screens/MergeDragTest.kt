package dev.linkinxp.termlauncher.screens

import android.app.Application
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
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
import dev.linkinxp.termlauncher.ui.PagerHome
import dev.linkinxp.termlauncher.ui.TermTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Dropping an icon on top of another makes a folder; dropping it at the edge still just moves it. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34])
class MergeDragTest {
    @get:Rule val rule = createComposeRule()
    private val maps = AppEntry("pkg.maps", "Maps", 0)
    private val notes = AppEntry("pkg.notes", "Notes", 1)
    private val merges = mutableListOf<Triple<Int, Item.App, Int>>()
    private val moves = mutableListOf<Triple<Int, Item, Int>>()

    private fun show() {
        val vm = LauncherViewModel(ApplicationProvider.getApplicationContext<Application>())
        val ui = LauncherUi(
            theme = ThemeRegistry.default, layout = HomeLayout(emptyList(), listOf(Page(listOf(Item.App(maps.pkg), Item.App(notes.pkg))))),
            apps = listOf(maps, notes).associateBy { it.pkg }, homeStyle = HomeStyle.ICONS,
        )
        rule.setContent {
            TermTheme(ui.theme) {
                PagerHome(ui, vm, {}, {}, {}, {}, onMoveItem = { p, i, s -> moves += Triple(p, i, s) }, onMerge = { _, item, p, s -> merges += Triple(p, item, s) })
            }
        }
        rule.waitForIdle()
    }

    private fun dragMapsTo(x: (androidx.compose.ui.geometry.Rect) -> Float) {
        val from = rule.onNodeWithText("maps").fetchSemanticsNode().boundsInRoot.center
        val cell = rule.onNodeWithText("notes").fetchSemanticsNode().boundsInRoot
        val label = cell.center
        rule.onNodeWithText("maps").performTouchInput { down(center) }
        rule.mainClock.advanceTimeBy(800)
        rule.onNodeWithText("maps").performTouchInput { moveBy(Offset(x(cell) - from.x, label.y - from.y), 200); up() }
        rule.waitForIdle()
    }

    @Test fun droppingOnTheMiddleOfAnotherIconAsksForAFolder() {
        show()
        dragMapsTo { it.center.x }
        assertEquals(listOf(Triple(0, Item.App(maps.pkg), 1)), merges)
        assertEquals(emptyList<Triple<Int, Item, Int>>(), moves)
    }

    @Test fun droppingNearTheEdgeOfAnotherIconStillMovesIt() {
        show()
        dragMapsTo { it.left + 3f }
        assertEquals(emptyList<Triple<Int, Item.App, Int>>(), merges)
        assertEquals(listOf(Triple(0, Item.App(maps.pkg) as Item, 1)), moves)
    }

    @Test fun aFolderCanBeRenamedFromItsPanel() {
        val vm = LauncherViewModel(ApplicationProvider.getApplicationContext<Application>())
        val ui = LauncherUi(
            theme = ThemeRegistry.default, layout = HomeLayout(emptyList(), listOf(Page(listOf(Item.Folder("folder", listOf(maps.pkg, notes.pkg)))))),
            apps = listOf(maps, notes).associateBy { it.pkg },
        )
        rule.setContent { TermTheme(ui.theme) { FolderPanel("folder", ui, vm) {} } }
        rule.onNodeWithTag("folder-rename").assertIsDisplayed()
        rule.onNodeWithTag("folder-rename-ok").assertIsDisplayed()
    }
}
