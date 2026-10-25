package dev.linkinxp.termlauncher.screens

import android.app.Application
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
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

/** With the ICONS style the home page, dock and quick tiles show themed icons with labels; other styles stay text. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34])
class IconHomeTest {
    @get:Rule val rule = createComposeRule()

    private val maps = AppEntry("pkg.maps", "Maps", 0)
    private val notes = AppEntry("pkg.notes", "Notes", 1)
    private val phone = AppEntry("pkg.phone", "Phone", 2)
    private val taps = mutableListOf<Item>()
    private val longs = mutableListOf<Item>()

    private fun show(style: HomeStyle, items: List<Item> = listOf(Item.App(maps.pkg), Item.App(notes.pkg)), dock: List<Item> = listOf(Item.App(phone.pkg)), quick: List<String> = listOf(maps.pkg), extra: List<AppEntry> = emptyList()) {
        val vm = LauncherViewModel(ApplicationProvider.getApplicationContext<Application>())
        val ui = LauncherUi(
            theme = ThemeRegistry.default, layout = HomeLayout(dock, listOf(Page(items))),
            apps = (listOf(maps, notes, phone) + extra).associateBy { it.pkg }, quick = quick, homeStyle = style,
        )
        rule.setContent { TermTheme(ui.theme) { PagerHome(ui, vm, { taps += it }, { longs += it }, {}, {}) } }
    }

    private fun waitForIcon(pkg: String) = rule.waitUntil(8_000) { rule.onAllNodesWithContentDescription("icon-$pkg").fetchSemanticsNodes().isNotEmpty() }

    @Test fun theIconPageShowsACellPerAppWithItsLabel() {
        show(HomeStyle.ICONS, quick = emptyList())
        waitForIcon(maps.pkg); waitForIcon(notes.pkg)
        rule.onNodeWithText("maps").assertIsDisplayed()
        rule.onNodeWithText("notes").assertIsDisplayed()
    }

    @Test fun tappingAndLongPressingACellActsOnItsApp() {
        show(HomeStyle.ICONS, quick = emptyList())
        rule.onNodeWithText("notes").performClick()
        assertEquals(listOf<Item>(Item.App(notes.pkg)), taps)
        rule.onNodeWithText("notes").performTouchInput { longClick() }
        assertEquals(listOf<Item>(Item.App(notes.pkg)), longs)
    }

    @Test fun aFolderIsACellThatOpensOnTap() {
        val f = Item.Folder("media", listOf(maps.pkg, notes.pkg))
        show(HomeStyle.ICONS, items = listOf(f))
        rule.onNodeWithText("~media").performClick()
        assertEquals(listOf<Item>(f), taps)
    }

    @Test fun theDockShowsIconsWithLabelsInTheIconStyleAndTextOtherwise() {
        show(HomeStyle.ICONS)
        waitForIcon(phone.pkg)
        rule.onNodeWithText("phone").assertIsDisplayed()
    }

    @Test fun otherStylesHaveNoIconsAtAll() {
        show(HomeStyle.CLASSIC)
        rule.waitForIdle()
        assertEquals(0, rule.onAllNodes(hasContentDescription("icon-${maps.pkg}")).fetchSemanticsNodes().size)
        assertEquals(0, rule.onAllNodes(hasContentDescription("icon-${phone.pkg}")).fetchSemanticsNodes().size)
    }

    @Test fun quickTilesAreIconCellsInTheIconStyle() {
        show(HomeStyle.ICONS, items = emptyList(), dock = emptyList(), quick = listOf(maps.pkg))
        rule.onNodeWithTag("quick-${maps.pkg}").assertIsDisplayed()
        waitForIcon(maps.pkg)
    }

    @Test fun anOddCountAndALongPageRenderWithoutTrouble() {
        val many = (1..120).map { AppEntry("pkg.m$it", "App $it", it.toLong()) }
        show(HomeStyle.ICONS, items = many.map { Item.App(it.pkg) }, extra = many)
        rule.waitForIdle()
        rule.onNodeWithText("app 1").assertIsDisplayed()
    }

    @Test fun aMissingAppDoesNotWidenTheOtherCellsOfItsRow() {
        val more = listOf("Alpha", "Bravo", "Charlie", "Delta", "Echo", "Foxtrot").mapIndexed { i, n -> AppEntry("pkg.${n.lowercase()}", n, 10L + i) }
        val row1 = listOf(more[0], more[1], more[2], more[3]).map { Item.App(it.pkg) }
        val row2 = listOf(Item.App("pkg.gone"), Item.App(more[4].pkg), Item.App(more[5].pkg), Item.App(maps.pkg))
        show(HomeStyle.ICONS, items = row1 + Item.Panel(dev.linkinxp.termlauncher.layout.PanelKind.SYS) + row2, quick = emptyList(), dock = emptyList(), extra = more)
        rule.waitForIdle()
        // second column of the first row vs the second drawn cell of the row that starts with an uninstalled app
        val second = rule.onNodeWithText("bravo").getUnclippedBoundsInRoot().left
        val shifted = rule.onNodeWithText("foxtrot").getUnclippedBoundsInRoot().left
        assertEquals("both rows keep four equal columns", second.value, shifted.value, 1.5f)
    }
}
