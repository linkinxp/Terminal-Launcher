package dev.linkinxp.termlauncher.screens

import android.os.Looper
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.lifecycle.ViewModelProvider
import dev.linkinxp.termlauncher.LauncherViewModel
import dev.linkinxp.termlauncher.MainActivity
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Every way into the settings screen, driven through the real activity. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34])
class SettingsEntryTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()

    private fun vm(): LauncherViewModel {
        lateinit var vm: LauncherViewModel
        rule.activityRule.scenario.onActivity { vm = ViewModelProvider(it)[LauncherViewModel::class.java] }
        return vm
    }

    private fun settingsIsOpen(): Boolean =
        rule.onAllNodesWithTagCount("settings-close") > 0

    private fun androidx.compose.ui.test.junit4.AndroidComposeTestRule<*, *>.onAllNodesWithTagCount(tag: String) =
        onAllNodes(androidx.compose.ui.test.hasTestTag(tag)).fetchSemanticsNodes().size

    private fun waitForSettings() {
        val end = System.currentTimeMillis() + 10_000
        while (!settingsIsOpen() && System.currentTimeMillis() < end) {
            shadowOf(Looper.getMainLooper()).idle(); rule.waitForIdle(); Thread.sleep(20)
        }
        assertTrue("settings screen did not open", settingsIsOpen())
    }

    @Test fun theSettingsCommandOpensTheScreen() {
        val vm = vm()
        rule.activityRule.scenario.onActivity { vm.onPromptSubmit("settings") }
        waitForSettings()
    }

    @Test fun theStatusLineCfgButtonOpensTheScreen() {
        rule.onNodeWithText("[cfg]", substring = true).performClick()
        waitForSettings()
    }

    @Test fun theDrawerCfgButtonOpensTheScreen() {
        val vm = vm()
        rule.activityRule.scenario.onActivity { vm.openDrawer() }
        rule.waitForIdle()
        rule.onNodeWithTag("drawer-cfg").performClick()
        waitForSettings()
    }
}
