package dev.linkinxp.termlauncher.screens

import android.app.Application
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.ColorDrawable
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.test.core.app.ApplicationProvider
import dev.linkinxp.termlauncher.icons.IconRepository
import dev.linkinxp.termlauncher.icons.drawTile
import dev.linkinxp.termlauncher.theme.BorderStyle
import dev.linkinxp.termlauncher.theme.Theme
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertSame
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
class IconTileTest {
    private val green = Theme("g", 0xFF000000, 0xFF00FF41, 0xFFFF00FF, 0xFF008F11, BorderStyle.SINGLE, false)
    private val white = Theme("w", 0xFF101010, 0xFFFFFFFF, 0xFF3366FF, 0xFF888888, BorderStyle.SINGLE, false)

    private fun center(b: Bitmap) = b.getPixel(b.width / 2, b.height / 2)

    @Test fun aMonochromeLayerIsTintedWithTheForegroundOnARoundedTile() {
        val mono = AdaptiveIconDrawable(ColorDrawable(Color.RED), ColorDrawable(Color.RED), ColorDrawable(Color.WHITE))
        val b = drawTile(mono, "m", green, 96)
        val c = center(b)
        assertEquals(Color.rgb(0, 255, 65), Color.rgb(Color.red(c), Color.green(c), Color.blue(c)))
        assertEquals("the corner outside the rounded tile is transparent", 0, Color.alpha(b.getPixel(0, 0)))
    }

    @Test fun anIconWithoutAMonochromeLayerIsGrayscaledAndTinted() {
        val b = drawTile(ColorDrawable(Color.RED), "r", white, 96)
        val c = center(b)
        assertEquals("white tint of a gray image stays neutral", Color.red(c), Color.green(c))
        assertEquals(Color.green(c), Color.blue(c))
        assertNotEquals("no longer the original red", 255, Color.red(c))
        assertTrue(Color.alpha(c) > 0)
    }

    @Test fun noIconDrawsTheLetterTile() {
        val b = drawTile(null, "Maps", white, 96)
        var lit = 0
        for (x in 24 until 72) for (y in 24 until 72) if (Color.alpha(b.getPixel(x, y)) > 200 && Color.red(b.getPixel(x, y)) > 200) lit++
        assertTrue("the letter has bright pixels in the middle ($lit)", lit > 20)
    }

    @Test fun selectedTilesUseTheAccent() {
        val mono = AdaptiveIconDrawable(ColorDrawable(Color.RED), ColorDrawable(Color.RED), ColorDrawable(Color.WHITE))
        val c = center(drawTile(mono, "m", green, 96, selected = true))
        assertEquals(Color.rgb(255, 0, 255), Color.rgb(Color.red(c), Color.green(c), Color.blue(c)))
    }

    private fun repo(): IconRepository {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val pm = shadowOf(app.packageManager)
        pm.installPackage(PackageInfo().apply { packageName = "p.mono"; applicationInfo = ApplicationInfo().apply { packageName = "p.mono" } })
        pm.setApplicationIcon("p.mono", AdaptiveIconDrawable(ColorDrawable(Color.RED), ColorDrawable(Color.RED), ColorDrawable(Color.WHITE)))
        return IconRepository(app)
    }

    @Test fun twoThemesGiveTwoDifferentTilesAndTheSameKeyIsCached() = runBlocking {
        val r = repo()
        val a = r.tile("p.mono", "mono", green, 96)
        val b = r.tile("p.mono", "mono", white, 96)
        assertNotEquals(center(a.asAndroidBitmap()), center(b.asAndroidBitmap()))
        assertSame(a, r.tile("p.mono", "mono", green, 96))
    }

    @Test fun anUnknownPackageNeverThrows() = runBlocking {
        val t = repo().tile("no.such.app", "Nothing", green, 64)
        assertEquals(64, t.width)
    }
}
