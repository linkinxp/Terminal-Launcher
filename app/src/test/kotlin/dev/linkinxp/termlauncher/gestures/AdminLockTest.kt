package dev.linkinxp.termlauncher.gestures

import android.app.Application
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.pm.PackageManager
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.core.app.ApplicationProvider
import dev.linkinxp.termlauncher.LauncherUi
import dev.linkinxp.termlauncher.LauncherViewModel
import dev.linkinxp.termlauncher.layout.HomeLayout
import dev.linkinxp.termlauncher.theme.ThemeRegistry
import dev.linkinxp.termlauncher.ui.SettingsScreen
import dev.linkinxp.termlauncher.ui.TermTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Double-tap lock works through a device administrator with only the "lock the screen" policy (the store build has no accessibility service). */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34])
class AdminLockTest {
    @get:Rule val rule = createComposeRule()
    private val context = ApplicationProvider.getApplicationContext<Application>()

    @Test fun lockingDoesNothingUntilTheAdminIsOn() = assertFalse(AdminLock.lock(context))

    @Test fun theRequestIntentAsksForThisReceiverWithAnExplanation() {
        val intent = AdminLock.requestIntent(context)
        assertEquals(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN, intent.action)
        assertEquals(ComponentName(context, LockAdminReceiver::class.java), intent.getParcelableExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, ComponentName::class.java))
        assertNotNull(intent.getStringExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION))
    }

    @Test fun theReceiverIsDeclaredAndProtected() {
        val info = context.packageManager.getReceiverInfo(ComponentName(context, LockAdminReceiver::class.java), PackageManager.GET_META_DATA)
        assertEquals("android.permission.BIND_DEVICE_ADMIN", info.permission)
        assertNotNull(info.metaData)
    }

    @Test fun settingsOffersToEnableTheLockAccess() {
        val ui = LauncherUi(theme = ThemeRegistry.default, layout = HomeLayout.fresh())
        val vm = LauncherViewModel(context)
        rule.setContent { TermTheme(ui.theme) { SettingsScreen(ui, vm, {}, {}, {}, onClose = {}) } }
        rule.onNodeWithTag("settings-lock-admin").performScrollTo().performClick() // starts the system prompt: must not crash
    }
}
