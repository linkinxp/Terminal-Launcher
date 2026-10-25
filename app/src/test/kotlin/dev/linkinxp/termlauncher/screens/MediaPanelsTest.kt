package dev.linkinxp.termlauncher.screens

import android.app.Application
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import dev.linkinxp.termlauncher.layout.HomeLayout
import dev.linkinxp.termlauncher.layout.Item
import dev.linkinxp.termlauncher.layout.Page
import dev.linkinxp.termlauncher.layout.PanelKind
import dev.linkinxp.termlauncher.LauncherUi
import dev.linkinxp.termlauncher.LauncherViewModel
import dev.linkinxp.termlauncher.media.MediaControls
import dev.linkinxp.termlauncher.media.MediaInfo
import dev.linkinxp.termlauncher.media.MediaStore
import dev.linkinxp.termlauncher.theme.ThemeRegistry
import dev.linkinxp.termlauncher.ui.PagerHome
import dev.linkinxp.termlauncher.ui.TermTheme
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34])
class MediaPanelsTest {
    @get:Rule val rule = createComposeRule()
    private val calls = mutableListOf<String>()
    private val controls = object : MediaControls {
        override fun previous() { calls += "prev" }
        override fun togglePlay() { calls += "toggle" }
        override fun next() { calls += "next" }
    }

    @After fun clear() = MediaStore.publish(null)

    private fun show(kind: PanelKind) {
        val vm = LauncherViewModel(ApplicationProvider.getApplicationContext<Application>())
        val ui = LauncherUi(theme = ThemeRegistry.default, layout = HomeLayout(emptyList(), listOf(Page(listOf(Item.Panel(kind))))))
        rule.setContent { TermTheme(ui.theme) { PagerHome(ui, vm, {}, {}, {}, {}) } }
    }

    private fun grantAccess() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        android.provider.Settings.Secure.putString(app.contentResolver, "enabled_notification_listeners",
            android.content.ComponentName(app, dev.linkinxp.termlauncher.badges.BadgeListenerService::class.java).flattenToString())
    }

    @Test fun withoutNotificationAccessThePanelExplainsWhatIsNeeded() {
        android.provider.Settings.Secure.putString(ApplicationProvider.getApplicationContext<Application>().contentResolver, "enabled_notification_listeners", "")
        show(PanelKind.MEDIA)
        rule.onNodeWithText("allow notification access", substring = true).assertIsDisplayed()
    }

    @Test fun nothingPlayingSaysSo() {
        grantAccess()
        show(PanelKind.MEDIA)
        rule.onNodeWithText("nothing playing", substring = true).assertIsDisplayed()
    }

    @Test fun nowPlayingShowsTheTrackAndTheControlsWork() {
        MediaStore.publish(MediaInfo("pkg.music", "Blue Monday", "New Order", true, controls))
        show(PanelKind.MEDIA)
        rule.onNodeWithText("Blue Monday").assertIsDisplayed()
        rule.onNodeWithText("New Order").assertIsDisplayed()
        rule.onNodeWithText("[<<]").performClick(); rule.onNodeWithText("[pause]").performClick(); rule.onNodeWithText("[>>]").performClick()
        assertEquals(listOf("prev", "toggle", "next"), calls)
    }

    @Test fun theTogglesPanelShowsItsButtonsAndWifiOpensTheSystemPanel() {
        show(PanelKind.TOGGLES)
        listOf("[torch]", "[wifi]", "[bt]", "[sound]", "[location]").forEach { rule.onNodeWithText(it).assertIsDisplayed() }
        rule.onNodeWithText("[wifi]").performClick()
        val started = shadowOf(ApplicationProvider.getApplicationContext<Application>()).nextStartedActivity
        assertEquals(android.provider.Settings.Panel.ACTION_INTERNET_CONNECTIVITY, started?.action)
    }
}
