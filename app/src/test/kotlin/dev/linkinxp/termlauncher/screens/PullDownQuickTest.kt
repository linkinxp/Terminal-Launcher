package dev.linkinxp.termlauncher.screens

import android.app.Application
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeDown
import androidx.compose.ui.test.swipeUp
import androidx.compose.runtime.mutableIntStateOf
import androidx.test.core.app.ApplicationProvider
import dev.linkinxp.termlauncher.LauncherUi
import dev.linkinxp.termlauncher.LauncherViewModel
import dev.linkinxp.termlauncher.apps.AppEntry
import dev.linkinxp.termlauncher.layout.HomeLayout
import dev.linkinxp.termlauncher.layout.Item
import dev.linkinxp.termlauncher.layout.Page
import dev.linkinxp.termlauncher.theme.ThemeRegistry
import dev.linkinxp.termlauncher.ui.PagerHome
import dev.linkinxp.termlauncher.ui.TermTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Swiping down on home slides the quick tiles in from the top; tile tap, swipe up or Home closes them. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34])
class PullDownQuickTest {
    @get:Rule val rule = createComposeRule()

    private val phone = AppEntry("pkg.phone", "Phone", 0)
    private val taps = mutableListOf<Item>()
    private var drawerOpens = 0
    private val home = mutableIntStateOf(0)

    private fun show(quick: List<String> = listOf(phone.pkg), longPage: Boolean = false) {
        val vm = LauncherViewModel(ApplicationProvider.getApplicationContext<Application>())
        val many = (1..300).map { AppEntry("pkg.app$it", "App $it", it.toLong()) }
        val apps = (listOf(phone) + if (longPage) many else emptyList()).associateBy { it.pkg }
        val layout = HomeLayout(emptyList(), listOf(Page(if (longPage) many.map { Item.App(it.pkg) } else emptyList())))
        val ui = LauncherUi(theme = ThemeRegistry.default, apps = apps, quick = quick, layout = layout)
        rule.setContent {
            TermTheme(ui.theme) {
                PagerHome(ui, vm, { taps += it }, {}, { drawerOpens++ }, {}, homeSignal = home.intValue)
            }
        }
    }

    private val pulled get() = rule.onNodeWithTag("pull-quick-pkg.phone")

    @Test fun nothingIsPulledDownUntilYouSwipe() {
        show()
        pulled.assertDoesNotExist()
    }

    @Test fun swipingDownShowsTheTilesAtTheTop() {
        show()
        rule.onRoot().performTouchInput { swipeDown() }
        rule.waitForIdle()
        val top = pulled.assertIsDisplayed().getUnclippedBoundsInRoot()
        val bottom = rule.onNodeWithTag("quick-pkg.phone").getUnclippedBoundsInRoot()
        assertTrue("pulled tile ${top.top} should be above the bottom tile ${bottom.top}", top.top < bottom.top)
    }

    @Test fun tappingATileLaunchesItAndClosesThePanel() {
        show()
        rule.onRoot().performTouchInput { swipeDown() }
        rule.waitForIdle()
        pulled.performClick()
        rule.waitForIdle()
        assertEquals(listOf<Item>(Item.App(phone.pkg)), taps)
        pulled.assertDoesNotExist()
    }

    @Test fun swipingUpClosesThePanelInsteadOfOpeningTheDrawer() {
        show()
        rule.onRoot().performTouchInput { swipeDown() }
        rule.waitForIdle()
        rule.onRoot().performTouchInput { swipeUp() }
        rule.waitForIdle()
        pulled.assertDoesNotExist()
        assertEquals(0, drawerOpens)
        rule.mainClock.advanceTimeBy(1_000) // let the injected pointer settle between gestures
        rule.onRoot().performTouchInput { swipeUp() }
        rule.waitForIdle()
        assertEquals(1, drawerOpens)
    }

    @Test fun swipingUpWithNothingPulledStillOpensTheDrawer() {
        show()
        rule.onRoot().performTouchInput { swipeUp() }
        rule.waitForIdle()
        assertEquals(1, drawerOpens)
    }

    @Test fun aPageThatScrollsStillPullsDownFromItsTop() {
        show(longPage = true)
        rule.onRoot().performTouchInput { swipeDown(startY = height * 0.1f, endY = height * 0.6f) }
        rule.waitForIdle()
        pulled.assertIsDisplayed()
    }

    @Test fun aPageThatScrollsScrollsInsteadOfOpeningTheDrawerOnTheFirstSwipeUp() {
        show(longPage = true)
        rule.onRoot().performTouchInput { swipeUp(startY = height * 0.6f, endY = height * 0.1f) }
        rule.waitForIdle()
        rule.onNodeWithText("> App 1", substring = false).assertDoesNotExist()
        assertEquals(0, drawerOpens)
    }

    @Test fun aHomePressClosesThePanel() {
        show()
        rule.onRoot().performTouchInput { swipeDown() }
        rule.waitForIdle()
        home.intValue++
        rule.waitForIdle()
        pulled.assertDoesNotExist()
    }

    @Test fun withNoQuickTilesSwipingDownDoesNothing() {
        show(quick = emptyList())
        rule.onRoot().performTouchInput { swipeDown() }
        rule.waitForIdle()
        pulled.assertDoesNotExist()
    }
}
