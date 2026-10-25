package dev.linkinxp.termlauncher.screens

import android.app.Application
import android.content.ComponentName
import android.os.Looper
import android.provider.Settings
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.test.core.app.ApplicationProvider
import dev.linkinxp.termlauncher.LauncherUi
import dev.linkinxp.termlauncher.LauncherViewModel
import dev.linkinxp.termlauncher.badges.BadgeListenerService
import dev.linkinxp.termlauncher.theme.ThemeRegistry
import dev.linkinxp.termlauncher.ui.SettingsScreen
import dev.linkinxp.termlauncher.ui.TermTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** The "access" status line follows what the user did in the system settings as soon as they come back. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34])
class SettingsStatusTest {
    @get:Rule val rule = createComposeRule()

    private class Owner : LifecycleOwner {
        val registry = LifecycleRegistry(this)
        override val lifecycle: Lifecycle get() = registry
    }

    @Test fun grantingNotificationAccessShowsAsOnWhenTheScreenResumes() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        Settings.Secure.putString(app.contentResolver, "enabled_notification_listeners", "")
        val owner = Owner().also { it.registry.currentState = Lifecycle.State.RESUMED }
        val vm = LauncherViewModel(app)
        val ui = LauncherUi(theme = ThemeRegistry.default)
        rule.setContent { CompositionLocalProvider(LocalLifecycleOwner provides owner) { TermTheme(ui.theme) { SettingsScreen(ui, vm, {}, {}, {}, {}) } } }
        rule.onNodeWithTag("badges-access").assertTextContains("off", substring = true)

        // The user goes to the system screen, grants access, and comes back.
        owner.registry.currentState = Lifecycle.State.STARTED
        Settings.Secure.putString(app.contentResolver, "enabled_notification_listeners", ComponentName(app, BadgeListenerService::class.java).flattenToString())
        owner.registry.currentState = Lifecycle.State.RESUMED
        repeat(5) { shadowOf(Looper.getMainLooper()).idle(); rule.waitForIdle() }
        rule.onNodeWithTag("badges-access").assertTextContains("on", substring = true)
    }
}
