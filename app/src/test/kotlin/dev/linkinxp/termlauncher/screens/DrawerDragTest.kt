package dev.linkinxp.termlauncher.screens

import android.app.Application
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import dev.linkinxp.termlauncher.LauncherUi
import dev.linkinxp.termlauncher.LauncherViewModel
import dev.linkinxp.termlauncher.apps.AppEntry
import dev.linkinxp.termlauncher.layout.HomeLayout
import dev.linkinxp.termlauncher.layout.HomeStyle
import dev.linkinxp.termlauncher.layout.Item
import dev.linkinxp.termlauncher.layout.Page
import dev.linkinxp.termlauncher.theme.ThemeRegistry
import dev.linkinxp.termlauncher.ui.DragState
import dev.linkinxp.termlauncher.ui.GridEntry
import dev.linkinxp.termlauncher.ui.GridLayout
import dev.linkinxp.termlauncher.ui.IconDrawerGrid
import dev.linkinxp.termlauncher.ui.PagerHome
import dev.linkinxp.termlauncher.ui.TermTheme
import dev.linkinxp.termlauncher.ui.drawerDragWatcher
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Hold an app in the drawer and drag it out: the drawer steps aside and the app drops on a home slot. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34])
class DrawerDragTest {
    @get:Rule val rule = createComposeRule()
    private val maps = AppEntry("pkg.maps", "Maps", 0)
    private val notes = AppEntry("pkg.notes", "Notes", 1)
    private val added = mutableListOf<Triple<Int, String, Int>>()
    private val docked = mutableListOf<String>()
    private val menus = mutableListOf<String>()
    private var left = 0

    private fun show() {
        val vm = LauncherViewModel(ApplicationProvider.getApplicationContext<Application>())
        val ui = LauncherUi(
            theme = ThemeRegistry.default, layout = HomeLayout(emptyList(), listOf(Page(listOf(Item.App(maps.pkg))))),
            apps = listOf(maps, notes).associateBy { it.pkg }, homeStyle = HomeStyle.ICONS,
        )
        val grid = GridLayout(listOf(GridEntry.Cell(notes, "n")), emptyMap())
        rule.setContent {
            val drag = remember { DragState() }
            TermTheme(ui.theme) {
                Box(Modifier.fillMaxSize().drawerDragWatcher(drag, onLeaveDrawer = { left++ }, onMenu = { menus += it })) {
                    PagerHome(ui, vm, {}, {}, {}, {}, drag = drag, onAddAt = { p, pkg, s -> added += Triple(p, pkg, s) }, onPinNew = { docked += it }, modifier = Modifier.height(250.dp))
                    Box(Modifier.offset(y = 300.dp)) {
                        IconDrawerGrid(grid, ui, rememberLazyGridState(), remember { object : NestedScrollConnection {} }, {}, { menus += "plain-$it" }, {}, drag = drag)
                    }
                }
            }
        }
        rule.waitForIdle()
    }

    @Test fun draggingADrawerAppOntoAHomeSlotAddsItThere() {
        show()
        val target = rule.onNodeWithTag("slot-1").fetchSemanticsNode().boundsInRoot.center
        val from = rule.onNodeWithText("notes").fetchSemanticsNode().boundsInRoot
        rule.onNodeWithText("notes").performTouchInput { down(center) }
        rule.mainClock.advanceTimeBy(700) // the hold that picks it up
        rule.onNodeWithText("notes").performTouchInput {
            moveTo(center + Offset(0f, -60f))
            moveTo(target - from.topLeft)
            up()
        }
        rule.waitForIdle()
        assertEquals(listOf(Triple(0, "pkg.notes", 1)), added)
        assertEquals(1, left)
        assertEquals(emptyList<String>(), menus)
    }

    @Test fun aHoldWithoutMovingStillOpensTheMenu() {
        show()
        rule.onNodeWithText("notes").performTouchInput { down(center) }
        rule.mainClock.advanceTimeBy(700)
        rule.onNodeWithText("notes").performTouchInput { up() }
        rule.waitForIdle()
        assertEquals(listOf("pkg.notes"), menus)
        assertEquals(emptyList<Triple<Int, String, Int>>(), added)
        assertEquals(0, left)
    }

    @Test fun aTapStillLaunchesNotDrags() {
        show()
        rule.onNodeWithText("notes").performClick()
        rule.waitForIdle()
        assertEquals(emptyList<Triple<Int, String, Int>>(), added)
        assertEquals(emptyList<String>(), menus)
    }
}
