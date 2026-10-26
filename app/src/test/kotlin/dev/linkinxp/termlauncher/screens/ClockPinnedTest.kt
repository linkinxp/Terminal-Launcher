package dev.linkinxp.termlauncher.screens

import android.app.Application
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeUp
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

/** The clock stays where it is while a long page scrolls underneath it. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34])
class ClockPinnedTest {
    @get:Rule val rule = createComposeRule()

    @Test fun scrollingALongPageLeavesTheClockInPlace() {
        val apps = (1..40).map { AppEntry("pkg.a$it", "App$it", it.toLong()) }
        val vm = LauncherViewModel(ApplicationProvider.getApplicationContext<Application>())
        val ui = LauncherUi(
            theme = ThemeRegistry.default, layout = HomeLayout(emptyList(), listOf(Page(apps.map { Item.App(it.pkg) }))),
            apps = apps.associateBy { it.pkg }, homeStyle = HomeStyle.CLASSIC,
        )
        rule.setContent { TermTheme(ui.theme) { PagerHome(ui, vm, {}, {}, {}, {}) } }
        rule.waitForIdle()
        val before = rule.onNodeWithTag("clock-header").fetchSemanticsNode().boundsInRoot.top
        rule.onRoot().performTouchInput { swipeUp(startY = height * 0.7f, endY = height * 0.2f) }
        rule.waitForIdle()
        assertEquals(before, rule.onNodeWithTag("clock-header").fetchSemanticsNode().boundsInRoot.top, 0.5f)
    }
}
