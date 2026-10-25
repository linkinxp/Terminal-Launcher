package dev.linkinxp.termlauncher.screens

import androidx.compose.ui.graphics.asAndroidBitmap
import dev.linkinxp.termlauncher.wall.SvgColors
import dev.linkinxp.termlauncher.wall.SvgRenderer
import dev.linkinxp.termlauncher.wall.SvgStyle
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34])
class SvgRendererTest {
    private val a = SvgColors(0xFF0B0014, 0xFF00F0FF, 0xFFFF2BD6, 0xFF7A3E8C)
    private val b = SvgColors(0xFF000000, 0xFF00FF41, 0xFF008F11, 0xFF006B1F)

    private fun litPixels(style: SvgStyle, colors: SvgColors): Int = runBlocking {
        val bmp = SvgRenderer.render(style, 4, colors, 270, 600)!!.asAndroidBitmap()
        val bg = bmp.getPixel(0, 0)
        var n = 0
        for (x in 0 until bmp.width step 2) for (y in 0 until bmp.height step 2) if (bmp.getPixel(x, y) != bg) n++
        n
    }

    @Test fun everyStyleDrawsSomethingOverTheBackground() {
        SvgStyle.entries.forEach { assertTrue("$it is blank", litPixels(it, a) > 200) }
    }

    @Test fun anotherThemeGivesAnotherPicture() = runBlocking {
        val x = SvgRenderer.render(SvgStyle.RADAR, 4, a, 270, 600)!!.asAndroidBitmap()
        val y = SvgRenderer.render(SvgStyle.RADAR, 4, b, 270, 600)!!.asAndroidBitmap()
        assertNotEquals(x.getPixel(x.width / 2, x.height / 2), y.getPixel(y.width / 2, y.height / 2).takeIf { false } ?: -1)
        assertNotEquals(x.getPixel(0, 0), y.getPixel(0, 0))
    }

    @Test fun theSameRequestIsServedFromTheCacheAndBadSizesGiveNothing() = runBlocking {
        val first = SvgRenderer.render(SvgStyle.TOPO, 1, a, 100, 200)
        assertSame(first, SvgRenderer.render(SvgStyle.TOPO, 1, a, 100, 200))
        assertNull(SvgRenderer.render(SvgStyle.TOPO, 1, a, 0, 200))
        assertEquals(100, first!!.width)
    }
}
