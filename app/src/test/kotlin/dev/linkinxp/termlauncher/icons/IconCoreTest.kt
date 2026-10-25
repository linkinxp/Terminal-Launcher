package dev.linkinxp.termlauncher.icons

import dev.linkinxp.termlauncher.theme.BorderStyle
import dev.linkinxp.termlauncher.theme.Theme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class IconCoreTest {
    private fun theme(bg: Long = 0xFF000000, fg: Long = 0xFFFFFFFF, accent: Long = 0xFF3366FF, dim: Long = 0xFF888888, name: String = "t", glow: Boolean = false) =
        Theme(name, bg, fg, accent, dim, BorderStyle.SINGLE, glow)

    @Test fun themeKeyChangesOnlyWithTheFourColors() {
        assertEquals(themeKey(theme()), themeKey(theme(name = "other", glow = true)))
        assertNotEquals(themeKey(theme()), themeKey(theme(bg = 0xFF111111)))
        assertNotEquals(themeKey(theme()), themeKey(theme(fg = 0xFFEEEEEE)))
        assertNotEquals(themeKey(theme()), themeKey(theme(accent = 0xFF3366FE)))
        assertNotEquals(themeKey(theme()), themeKey(theme(dim = 0xFF888889)))
    }

    @Test fun iconKeysCompareByAllParts() {
        assertEquals(IconKey("a", "k", 96), IconKey("a", "k", 96))
        assertNotEquals(IconKey("a", "k", 96), IconKey("a", "k", 48))
        assertNotEquals(IconKey("a", "k", 96), IconKey("b", "k", 96))
    }

    private fun apply(m: FloatArray, r: Float, g: Float, b: Float, a: Float = 1f): FloatArray = floatArrayOf(
        m[0] * r + m[1] * g + m[2] * b + m[3] * a + m[4] / 255f,
        m[5] * r + m[6] * g + m[7] * b + m[8] * a + m[9] / 255f,
        m[10] * r + m[11] * g + m[12] * b + m[13] * a + m[14] / 255f,
        m[15] * r + m[16] * g + m[17] * b + m[18] * a + m[19] / 255f,
    )

    @Test fun tintMapsWhiteToTheColorAndBlackToBlackAndKeepsAlpha() {
        val m = tintMatrix(0xFFFF0000)
        assertEquals(20, m.size)
        assertEquals(listOf(1f, 0f, 0f, 1f), apply(m, 1f, 1f, 1f).map { Math.round(it * 100) / 100f })
        assertEquals(listOf(0f, 0f, 0f, 1f), apply(m, 0f, 0f, 0f).map { Math.round(it * 100) / 100f })
        assertEquals(listOf(0f, 0f, 0f, 0.5f), apply(m, 0f, 0f, 0f, 0.5f).map { Math.round(it * 100) / 100f })
        val gray = apply(m, 0.5f, 0.5f, 0.5f)
        assertEquals(0.5f, gray[0], 0.01f); assertEquals(0f, gray[1], 0.01f)
    }

    @Test fun cacheEvictsTheLeastRecentlyUsed() {
        val c = IconCache<String>(2)
        val a = IconKey("a", "k", 1); val b = IconKey("b", "k", 1); val d = IconKey("d", "k", 1)
        c.put(a, "A"); c.put(b, "B")
        assertEquals("A", c.get(a)) // a is now the freshest
        c.put(d, "D")
        assertNull(c.get(b))
        assertEquals("A", c.get(a)); assertEquals("D", c.get(d))
        assertEquals(2, c.size)
    }

    @Test fun cacheStaysBoundedUnderConcurrentUse() {
        val c = IconCache<Int>(16)
        val pool = Executors.newFixedThreadPool(8)
        repeat(8) { t -> pool.execute { repeat(500) { i -> c.put(IconKey("p${(t * 31 + i) % 60}", "k", 1), i); c.get(IconKey("p$i", "k", 1)) } } }
        pool.shutdown(); assertTrue(pool.awaitTermination(10, TimeUnit.SECONDS))
        assertTrue(c.size <= 16)
    }
}
