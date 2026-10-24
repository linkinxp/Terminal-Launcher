package dev.linkinxp.termlauncher.screens

import android.app.Application
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.test.core.app.ApplicationProvider
import dev.linkinxp.termlauncher.LauncherUi
import dev.linkinxp.termlauncher.LauncherViewModel
import dev.linkinxp.termlauncher.theme.ThemeRegistry
import dev.linkinxp.termlauncher.ui.SettingsScreen
import dev.linkinxp.termlauncher.ui.TermTheme
import dev.linkinxp.termlauncher.wall.WallMode
import dev.linkinxp.termlauncher.wall.WallSettings
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** The settings screen is always solid: a bright wallpaper behind it must not wash it out. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34])
class SettingsBackdropTest {
    @get:Rule val rule = createComposeRule()

    private fun darkestBackdrop(mode: WallMode): Int {
        val vm = LauncherViewModel(ApplicationProvider.getApplicationContext<Application>())
        val theme = ThemeRegistry.byName("matrix")!! // black background
        val wall = WallSettings(mode = mode, dim = 55)
        val ui = LauncherUi(theme = theme, wall = wall)
        rule.setContent {
            Box(Modifier.fillMaxSize().background(Color.White)) { // a very bright "wallpaper"
                TermTheme(theme, wall) { SettingsScreen(ui, vm, {}, {}, {}, {}) }
            }
        }
        val bmp = rule.onRoot().captureToImage().asAndroidBitmap()
        // The far right edge below the header is empty space in the screen.
        val px = bmp.getPixel(bmp.width - 4, bmp.height / 2)
        return maxOf(android.graphics.Color.red(px), android.graphics.Color.green(px), android.graphics.Color.blue(px))
    }

    @Test fun overASystemWallpaperTheBackdropIsStillSolid() {
        val brightest = darkestBackdrop(WallMode.SYSTEM)
        assertTrue("backdrop channel value $brightest should be nearly black", brightest <= 10)
    }
}
