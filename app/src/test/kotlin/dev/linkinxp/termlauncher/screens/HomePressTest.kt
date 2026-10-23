package dev.linkinxp.termlauncher.screens

import android.content.Intent
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
import android.os.Looper

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34])
class HomePressTest {
    private val homeIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)

    @Test fun homeIntentClosesTheOpenDrawer() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                val vm = ViewModelProvider(activity)[LauncherViewModel::class.java]
                vm.openDrawer()
                shadowOf(Looper.getMainLooper()).idle()
                assertTrue("drawer should be open", vm.ui.value.drawerOpen)
                activity.onNewIntent(homeIntent)
                shadowOf(Looper.getMainLooper()).idle()
                assertFalse("home press should close the drawer", vm.ui.value.drawerOpen)
            }
        }
    }

    @Test fun otherIntentsLeaveTheDrawerAlone() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                val vm = ViewModelProvider(activity)[LauncherViewModel::class.java]
                vm.openDrawer()
                shadowOf(Looper.getMainLooper()).idle()
                activity.onNewIntent(Intent(Intent.ACTION_MAIN))
                shadowOf(Looper.getMainLooper()).idle()
                assertTrue(vm.ui.value.drawerOpen)
            }
        }
    }
}
