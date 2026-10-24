package dev.linkinxp.termlauncher.screens

import android.content.Intent
import android.os.Looper
import androidx.lifecycle.ViewModelProvider
import androidx.test.core.app.ActivityScenario
import dev.linkinxp.termlauncher.LauncherViewModel
import dev.linkinxp.termlauncher.MainActivity
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34])
class HomePressTest {
    private val homeIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)

    /** The UI state is computed on a background thread now, so wait for it to arrive like the UI does. */
    private fun waitFor(what: String, condition: () -> Boolean) {
        val end = System.currentTimeMillis() + 10_000
        while (!condition() && System.currentTimeMillis() < end) {
            shadowOf(Looper.getMainLooper()).idle()
            Thread.sleep(10)
        }
        assertTrue(what, condition())
    }

    @Test fun homeIntentClosesTheOpenDrawer() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                val vm = ViewModelProvider(activity)[LauncherViewModel::class.java]
                vm.openDrawer()
                waitFor("drawer should be open") { vm.ui.value.drawerOpen }
                activity.onNewIntent(homeIntent)
                waitFor("home press should close the drawer") { !vm.ui.value.drawerOpen }
                assertFalse(vm.ui.value.drawerOpen)
            }
        }
    }

    @Test fun otherIntentsLeaveTheDrawerAlone() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                val vm = ViewModelProvider(activity)[LauncherViewModel::class.java]
                vm.openDrawer()
                waitFor("drawer should be open") { vm.ui.value.drawerOpen }
                activity.onNewIntent(Intent(Intent.ACTION_MAIN))
                repeat(20) { shadowOf(Looper.getMainLooper()).idle(); Thread.sleep(10) } // give a wrong close time to happen
                assertTrue(vm.ui.value.drawerOpen)
            }
        }
    }
}
