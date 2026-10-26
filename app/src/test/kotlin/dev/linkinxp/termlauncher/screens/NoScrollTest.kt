package dev.linkinxp.termlauncher.screens

import android.app.Application
import androidx.compose.ui.test.junit4.createComposeRule
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

/** The icon home never scrolls: a page that is too full passes its last icon on to the next page. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34])
class NoScrollTest {
    @get:Rule val rule = createComposeRule()
    private val spills = mutableListOf<Int>()

    private fun show(count: Int) {
        val apps = (1..count).map { AppEntry("pkg.a$it", "App$it", it.toLong()) }
        val vm = LauncherViewModel(ApplicationProvider.getApplicationContext<Application>())
        val ui = LauncherUi(
            theme = ThemeRegistry.default, layout = HomeLayout(emptyList(), listOf(Page(apps.map { Item.App(it.pkg) }))),
            apps = apps.associateBy { it.pkg }, homeStyle = HomeStyle.ICONS,
        )
        rule.setContent { TermTheme(ui.theme) { PagerHome(ui, vm, {}, {}, {}, {}, onSpill = { spills += it }) } }
        rule.waitForIdle()
        rule.mainClock.advanceTimeBy(1_500)
        rule.waitForIdle()
    }

    @Test fun aPageThatOverflowsAsksToSpill() {
        show(80)
        assertEquals(true, 0 in spills)
    }

    @Test fun aPageThatFitsStaysPut() {
        show(3)
        assertEquals(emptyList<Int>(), spills)
    }
}
