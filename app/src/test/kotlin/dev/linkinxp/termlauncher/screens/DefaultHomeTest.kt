package dev.linkinxp.termlauncher.screens

import android.app.Application
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.core.app.ApplicationProvider
import dev.linkinxp.termlauncher.LauncherUi
import dev.linkinxp.termlauncher.LauncherViewModel
import dev.linkinxp.termlauncher.home.DefaultHomeDialog
import dev.linkinxp.termlauncher.home.shouldAskDefaultHome
import dev.linkinxp.termlauncher.layout.HomeLayout
import dev.linkinxp.termlauncher.theme.ThemeRegistry
import dev.linkinxp.termlauncher.ui.SettingsScreen
import dev.linkinxp.termlauncher.ui.TermTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** After install the launcher offers to become the home app: once, and again on request from settings. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34])
class DefaultHomeTest {
    @get:Rule val rule = createComposeRule()

    @Test fun asksOnlyWhenNotHomeYetAndNeverAsked() {
        assertTrue(shouldAskDefaultHome(isHome = false, alreadyAsked = false))
        assertFalse(shouldAskDefaultHome(isHome = true, alreadyAsked = false))
        assertFalse(shouldAskDefaultHome(isHome = false, alreadyAsked = true))
    }

    @Test fun dialogOffersYesAndNotNow() {
        var yes = 0; var later = 0
        rule.setContent { TermTheme(ThemeRegistry.default) { DefaultHomeDialog(ThemeRegistry.default, { yes++ }, { later++ }) } }
        rule.onNodeWithText("make terminal your home app?").assertIsDisplayed()
        rule.onNodeWithTag("default-home-yes").performClick()
        assertEquals(1, yes)
        rule.onNodeWithTag("default-home-later").performClick()
        assertEquals(1, later)
    }

    @Test fun settingsCanAskAgain() {
        var asked = 0
        val ui = LauncherUi(theme = ThemeRegistry.default, layout = HomeLayout.fresh())
        val vm = LauncherViewModel(ApplicationProvider.getApplicationContext<Application>())
        rule.setContent { TermTheme(ui.theme) { SettingsScreen(ui, vm, {}, {}, {}, onClose = {}, onMakeDefault = { asked++ }) } }
        rule.onNodeWithTag("settings-default-home").performScrollTo().performClick()
        assertEquals(1, asked)
    }
}
