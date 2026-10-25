package dev.linkinxp.termlauncher.screens

import android.app.Application
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import dev.linkinxp.termlauncher.LauncherUi
import dev.linkinxp.termlauncher.LauncherViewModel
import dev.linkinxp.termlauncher.apps.AppEntry
import dev.linkinxp.termlauncher.layout.HomeStyle
import dev.linkinxp.termlauncher.theme.ThemeRegistry
import dev.linkinxp.termlauncher.ui.AppMenu
import dev.linkinxp.termlauncher.ui.EditMode
import dev.linkinxp.termlauncher.ui.TermTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34])
class IconMenusTest {
    @get:Rule val rule = createComposeRule()
    private val maps = AppEntry("pkg.maps", "Maps", 0)

    private fun ui(style: HomeStyle) = LauncherUi(theme = ThemeRegistry.default, apps = mapOf(maps.pkg to maps), homeStyle = style)
    private fun vm() = LauncherViewModel(ApplicationProvider.getApplicationContext<Application>())
    private fun icons() = rule.onAllNodes(hasContentDescription("icon-pkg.maps")).fetchSemanticsNodes().size

    @Test fun theAppMenuShowsTheIconInTheIconStyleOnly() {
        val u = ui(HomeStyle.ICONS); val v = vm()
        rule.setContent { TermTheme(u.theme) { AppMenu("pkg.maps", u, v) {} } }
        rule.waitUntil(8_000) { icons() > 0 }
        assertEquals(1, icons())
    }

    @Test fun theAppMenuIsTextInOtherStyles() {
        val u = ui(HomeStyle.CLASSIC); val v = vm()
        rule.setContent { TermTheme(u.theme) { AppMenu("pkg.maps", u, v) {} } }
        rule.waitForIdle()
        assertEquals(0, icons())
    }

    @Test fun editModeShowsIconsInItsAddAppListWhenAsked() {
        val u = ui(HomeStyle.ICONS); val v = vm()
        rule.setContent { TermTheme(u.theme) { EditMode(u, v, { _, _ -> }, {}, {}) } }
        rule.onAllNodesWithContentDescription("none").fetchSemanticsNodes() // dialog composed
        rule.onNodeWithTextClick("[+app]")
        rule.waitUntil(8_000) { icons() > 0 }
    }

    private fun androidx.compose.ui.test.junit4.ComposeContentTestRule.onNodeWithTextClick(t: String) =
        onNode(androidx.compose.ui.test.hasText(t)).performClick()
}
