package dev.linkinxp.termlauncher.screens

import android.app.Application
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.unit.dp
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
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

    @Test fun theQuickTilesInTheIconStyleHaveNoFrameAndSmallerIcons() {
        show(HomeStyle.ICONS, items = emptyList(), dock = emptyList(), quick = listOf(maps.pkg))
        waitForIcon(maps.pkg)
        rule.onNodeWithText("quick").assertDoesNotExist() // the bordered `quick` panel title is gone in this style
        rule.onAllNodesWithContentDescription("icon-${maps.pkg}", useUnmergedTree = true)[0].assertHeightIsEqualTo(40.dp)
    }

    @Test fun theDockIconsAreFortyFourDp() {
        show(HomeStyle.ICONS, items = emptyList(), dock = listOf(Item.App(phone.pkg)), quick = emptyList())
        waitForIcon(phone.pkg)
        rule.onAllNodesWithContentDescription("icon-${phone.pkg}", useUnmergedTree = true)[0].assertHeightIsEqualTo(44.dp)
    }

    @Test fun otherStylesKeepTheBorderedQuickPanel() {
        show(HomeStyle.CLASSIC, items = emptyList(), dock = emptyList(), quick = listOf(maps.pkg))
        rule.onNodeWithText("quick").assertIsDisplayed()
    }

    @Test fun theDenseLevelDrawsSmallerIconsAndSixToARow() {
        val more = (1..6).map { AppEntry("pkg.d$it", "Dd$it", 20L + it) }
        val vm = LauncherViewModel(ApplicationProvider.getApplicationContext<Application>())
        val ui = LauncherUi(
            theme = ThemeRegistry.default, layout = HomeLayout(emptyList(), listOf(Page(more.map { Item.App(it.pkg) }))),
            apps = more.associateBy { it.pkg }, homeStyle = HomeStyle.ICONS, density = dev.linkinxp.termlauncher.layout.Density.DENSE,
        )
        rule.setContent { TermTheme(ui.theme) { PagerHome(ui, vm, {}, {}, {}, {}) } }
        rule.waitUntil(8_000) { rule.onAllNodesWithContentDescription("icon-pkg.d1", useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty() }
        rule.onAllNodesWithContentDescription("icon-pkg.d1", useUnmergedTree = true)[0].assertHeightIsEqualTo(44.dp)
        val first = rule.onNodeWithText("dd1").getUnclippedBoundsInRoot().top
        val sixth = rule.onNodeWithText("dd6").getUnclippedBoundsInRoot().top
        assertEquals("all six share one row", first.value, sixth.value, 0.5f)
    }

    private val moves = mutableListOf<Triple<Int, Item, Item>>()

    private fun showForDrag() {
        val vm = LauncherViewModel(ApplicationProvider.getApplicationContext<Application>())
        val ui = LauncherUi(
            theme = ThemeRegistry.default, layout = HomeLayout(emptyList(), listOf(Page(listOf(Item.App(maps.pkg), Item.App(notes.pkg), Item.App(phone.pkg))))),
            apps = listOf(maps, notes, phone).associateBy { it.pkg }, homeStyle = HomeStyle.ICONS,
        )
        rule.setContent { TermTheme(ui.theme) { PagerHome(ui, vm, { taps += it }, { longs += it }, {}, {}, onMoveItem = { p, i, t -> moves += Triple(p, i, t) }) } }
    }

    @Test fun holdingAnIconAndDraggingItOntoAnotherMovesIt() {
        showForDrag()
        val from = rule.onNodeWithText("maps").fetchSemanticsNode().boundsInRoot.center
        val to = rule.onNodeWithText("notes").fetchSemanticsNode().boundsInRoot.center
        rule.onNodeWithText("maps").performTouchInput {
            down(center); advanceEventTime(900); moveBy(androidx.compose.ui.geometry.Offset(to.x - from.x, to.y - from.y), 200); up()
        }
        rule.waitForIdle()
        assertEquals(listOf(Triple(0, Item.App(maps.pkg) as Item, Item.App(notes.pkg) as Item)), moves)
        assertEquals("a drag is not a tap or a menu", emptyList<Item>(), taps + longs)
    }

    @Test fun holdingWithoutMovingStillOpensTheMenu() {
        showForDrag()
        rule.onNodeWithText("notes").performTouchInput { longClick() }
        rule.waitForIdle()
        assertEquals(listOf<Item>(Item.App(notes.pkg)), longs)
        assertEquals(emptyList<Triple<Int, Item, Item>>(), moves)
    }

    @Test fun droppingOnNothingMovesNothing() {
        showForDrag()
        rule.onNodeWithText("maps").performTouchInput {
            down(center); advanceEventTime(900); moveBy(androidx.compose.ui.geometry.Offset(0f, -1500f), 200); up()
        }
        rule.waitForIdle()
        assertEquals(emptyList<Triple<Int, Item, Item>>(), moves)
    }

    @Test fun anAppWithNotificationsShowsABadgeOnItsIconWhenBadgesAreOn() {
        dev.linkinxp.termlauncher.badges.BadgeStore.publish(mapOf(maps.pkg to 3))
        show(HomeStyle.ICONS, quick = emptyList(), dock = emptyList())
        waitForIcon(maps.pkg)
        rule.onNodeWithTag("badge-${maps.pkg}", useUnmergedTree = true).assertIsDisplayed()
        rule.onNodeWithText("3", useUnmergedTree = true).assertIsDisplayed()
        assertEquals(0, rule.onAllNodes(androidx.compose.ui.test.hasTestTag("badge-${notes.pkg}"), useUnmergedTree = true).fetchSemanticsNodes().size)
        dev.linkinxp.termlauncher.badges.BadgeStore.publish(emptyMap())
    }

    @Test fun noBadgesWhenTheSettingIsOff() {
        dev.linkinxp.termlauncher.badges.BadgeStore.publish(mapOf(maps.pkg to 3))
        val vm = LauncherViewModel(ApplicationProvider.getApplicationContext<Application>())
        val ui = LauncherUi(theme = ThemeRegistry.default, layout = HomeLayout(emptyList(), listOf(Page(listOf(Item.App(maps.pkg))))), apps = mapOf(maps.pkg to maps), homeStyle = HomeStyle.ICONS, badges = false)
        rule.setContent { TermTheme(ui.theme) { PagerHome(ui, vm, {}, {}, {}, {}) } }
        waitForIcon(maps.pkg)
        assertEquals(0, rule.onAllNodes(androidx.compose.ui.test.hasTestTag("badge-${maps.pkg}"), useUnmergedTree = true).fetchSemanticsNodes().size)
        dev.linkinxp.termlauncher.badges.BadgeStore.publish(emptyMap())
    }

    @Test fun aHoldWithAJitteringFingerStillOpensTheMenu() {
        showForDrag()
        rule.onNodeWithText("notes").performTouchInput {
            down(center)
            repeat(6) { moveBy(androidx.compose.ui.geometry.Offset(5f, 0f)); moveBy(androidx.compose.ui.geometry.Offset(-5f, 0f)) } // 60 px of wobble, net zero
            advanceEventTime(900); up()
        }
        rule.waitForIdle()
        assertEquals(emptyList<Triple<Int, Item, Item>>(), moves)
    }

    @Test fun anIconOffersItsMenuToAccessibilityServicesToo() {
        showForDrag()
        rule.onNodeWithText("notes").performSemanticsAction(androidx.compose.ui.semantics.SemanticsActions.OnLongClick)
        rule.waitForIdle()
        assertEquals(listOf<Item>(Item.App(notes.pkg)), longs)
    }
}
