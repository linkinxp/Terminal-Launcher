package dev.linkinxp.termlauncher.screens

import android.app.Application
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeDown
import androidx.compose.ui.test.swipeUp
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
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Swiping down on home pulls the system notification shade; swiping up still opens the drawer. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34])
class SwipeDownTest {
    @get:Rule val rule = createComposeRule()

    private val phone = AppEntry("pkg.phone", "Phone", 0)
    private var shades = 0
    private var drawers = 0

    private fun show(longPage: Boolean = false) {
        val vm = LauncherViewModel(ApplicationProvider.getApplicationContext<Application>())
        val many = (1..300).map { AppEntry("pkg.app$it", "App $it", it.toLong()) }
        val apps = (listOf(phone) + if (longPage) many else emptyList()).associateBy { it.pkg }
        val layout = HomeLayout(emptyList(), listOf(Page(if (longPage) many.map { Item.App(it.pkg) } else emptyList())))
        val ui = LauncherUi(theme = ThemeRegistry.default, apps = apps, quick = listOf(phone.pkg), layout = layout)
        rule.setContent { TermTheme(ui.theme) { PagerHome(ui, vm, {}, {}, { drawers++ }, {}, onSwipeDown = { shades++ }) } }
    }

    @Test fun swipingDownOpensTheShadeAndNotAQuickPanel() {
        show()
        rule.onRoot().performTouchInput { swipeDown() }
        rule.waitForIdle()
        assertEquals(1, shades)
        assertEquals(0, drawers)
        rule.onNodeWithTag("pull-quick-pkg.phone").assertDoesNotExist()
    }

    @Test fun swipingDownFromTheTopOfAScrollingPageOpensTheShade() {
        show(longPage = true)
        rule.onRoot().performTouchInput { swipeDown(startY = height * 0.1f, endY = height * 0.6f) }
        rule.waitForIdle()
        assertEquals(1, shades)
    }

    @Test fun swipingUpStillOpensTheDrawer() {
        show()
        rule.onRoot().performTouchInput { swipeUp() }
        rule.waitForIdle()
        assertEquals(1, drawers)
        assertEquals(0, shades)
    }

    @Test fun aScrollingPageScrollsInsteadOfOpeningTheDrawerOnTheFirstSwipeUp() {
        show(longPage = true)
        rule.onRoot().performTouchInput { swipeUp(startY = height * 0.6f, endY = height * 0.1f) }
        rule.waitForIdle()
        assertEquals(0, drawers)
    }
}
