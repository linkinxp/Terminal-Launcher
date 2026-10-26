package dev.linkinxp.termlauncher.screens

import android.app.Application
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
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
import dev.linkinxp.termlauncher.ui.bottomBar
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Pinned apps and the quick tiles are one row at the bottom, not a row of tiles with the dock under it. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34])
class BottomBarTest {
    @get:Rule val rule = createComposeRule()
    private val names = listOf("chrome", "phone", "message", "maps", "camera")
    private val apps = names.associate { "pkg.$it" to AppEntry("pkg.$it", it.replaceFirstChar(Char::uppercase), 0) }

    private fun ui(dock: List<String>, quick: List<String>, style: HomeStyle = HomeStyle.ICONS) = LauncherUi(
        theme = ThemeRegistry.default, layout = HomeLayout(dock.map { Item.App(it) }, listOf(Page(emptyList()))),
        apps = apps, quick = quick, homeStyle = style,
    )

    @Test fun pinnedAppsComeFirstThenTheQuickTilesWithoutRepeats() {
        val items = bottomBar(ui(listOf("pkg.chrome", "pkg.maps"), listOf("pkg.phone", "pkg.maps", "pkg.camera")))
        assertEquals(listOf<Item>(Item.App("pkg.chrome"), Item.App("pkg.maps"), Item.App("pkg.phone"), Item.App("pkg.camera")), items)
    }

    @Test fun withNothingPinnedTheQuickPanelStaysAsItIs() {
        assertNull(bottomBar(ui(emptyList(), listOf("pkg.phone"))))
        assertNull(bottomBar(ui(listOf("pkg.chrome"), listOf("pkg.phone"), HomeStyle.CLASSIC)))
    }

    @Test fun oneLineHoldsTheDockPlusFourQuickTiles() {
        val vm = LauncherViewModel(ApplicationProvider.getApplicationContext<Application>())
        val u = ui(listOf("pkg.chrome"), listOf("pkg.phone", "pkg.message", "pkg.maps", "pkg.camera"))
        rule.setContent { TermTheme(u.theme) { PagerHome(u, vm, {}, {}, {}, {}) } }
        rule.waitForIdle()
        val tops = names.map { rule.onNodeWithText(it).fetchSemanticsNode().boundsInRoot.top }
        assertEquals("all five labels on one line: $tops", 1, tops.distinct().size)
    }
}
