package dev.linkinxp.termlauncher.screens

import android.app.Application
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.junit4.createComposeRule
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
import dev.linkinxp.termlauncher.ui.PagerHome
import dev.linkinxp.termlauncher.ui.TermTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Pinned apps can be reordered by holding one and dragging it over another. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34])
class DockMoveTest {
    @get:Rule val rule = createComposeRule()
    private val names = listOf("alpha", "bravo", "charlie")
    private val apps = names.associate { "pkg.$it" to AppEntry("pkg.$it", it.replaceFirstChar(Char::uppercase), 0) }
    private val moves = mutableListOf<Pair<Int, Int>>()
    private val menus = mutableListOf<Item>()

    private fun show() {
        val vm = LauncherViewModel(ApplicationProvider.getApplicationContext<Application>())
        val ui = LauncherUi(
            theme = ThemeRegistry.default, layout = HomeLayout(names.map { Item.App("pkg.$it") }, listOf(Page(emptyList()))),
            apps = apps, homeStyle = HomeStyle.ICONS,
        )
        rule.setContent { TermTheme(ui.theme) { PagerHome(ui, vm, {}, { menus += it }, {}, {}, onMoveDock = { a, b -> moves += a to b }) } }
        rule.waitForIdle()
    }

    private fun center(name: String) = rule.onNodeWithText(name).fetchSemanticsNode().boundsInRoot.center

    @Test fun draggingTheFirstPinnedAppOverTheLastMovesIt() {
        show()
        val from = center("alpha"); val to = center("charlie")
        rule.onNodeWithText("alpha").performTouchInput { down(center) }
        rule.mainClock.advanceTimeBy(800)
        rule.onNodeWithText("alpha").performTouchInput { moveBy(Offset(to.x - from.x, to.y - from.y), 200); up() }
        rule.waitForIdle()
        assertEquals(listOf(0 to 2), moves)
        assertEquals(emptyList<Item>(), menus)
    }

    @Test fun aHoldWithoutMovingIsStillTheMenu() {
        show()
        rule.onNodeWithText("bravo").performTouchInput { down(center) }
        rule.mainClock.advanceTimeBy(800)
        rule.onNodeWithText("bravo").performTouchInput { up() }
        rule.waitForIdle()
        assertEquals(listOf<Item>(Item.App("pkg.bravo")), menus)
        assertEquals(emptyList<Pair<Int, Int>>(), moves)
    }
}
