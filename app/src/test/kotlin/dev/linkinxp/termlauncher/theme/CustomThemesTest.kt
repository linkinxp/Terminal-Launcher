package dev.linkinxp.termlauncher.theme

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CustomThemesTest {
    private val builtIn = ThemeRegistry.all.map { it.name }

    @Test fun aGoodNameIsAccepted() {
        assertNull(validateCustomTheme("my-theme_2", emptyList(), builtIn))
        assertNull(validateCustomTheme("a", emptyList(), builtIn))
        assertNull(validateCustomTheme("x".repeat(24), emptyList(), builtIn))
    }

    @Test fun badNamesGetAMessage() {
        listOf("", "  ", "x".repeat(25), "Mine", "my theme", "a/b", "😀", "matrix", "Matrix", "ls", "settings", "clock", "theme")
            .forEach { assertNotNull("'$it'", validateCustomTheme(it, emptyList(), builtIn)) }
    }

    @Test fun anExistingCustomNameMeansReplace() {
        assertNull(validateCustomTheme("mine", listOf("mine"), builtIn))
    }

    @Test fun theNinthCustomThemeIsRefusedButReplacingStillWorks() {
        val eight = (1..8).map { "t$it" }
        assertEquals("theme: too many custom themes (8 max)", validateCustomTheme("new", eight, builtIn))
        assertNull(validateCustomTheme("t3", eight, builtIn))
    }

    private val usage = "usage: theme add <name> <bg> <fg> <accent> <dim> [single|double|rounded|ascii|bracket]"

    @Test fun addArgumentsParse() {
        val a = parseThemeAdd(listOf("mine", "#000", "#0f0", "#0ff", "#066")).getOrThrow()
        assertEquals(CustomThemeSpec("mine", 0xFF000000, 0xFF00FF00, 0xFF00FFFF, 0xFF006666, BorderStyle.SINGLE), a)
        assertEquals(BorderStyle.ROUNDED, parseThemeAdd(listOf("mine", "000", "0f0", "0ff", "066", "Rounded")).getOrThrow().border)
    }

    @Test fun addArgumentErrors() {
        assertEquals(usage, parseThemeAdd(emptyList()).exceptionOrNull()?.message)
        assertEquals(usage, parseThemeAdd(listOf("mine")).exceptionOrNull()?.message)
        assertEquals(usage, parseThemeAdd(listOf("mine", "#000", "#0f0", "#0ff")).exceptionOrNull()?.message)
        assertEquals(usage, parseThemeAdd(listOf("m", "#000", "#0f0", "#0ff", "#066", "single", "extra")).exceptionOrNull()?.message)
        assertEquals("theme: bad color '#GGG' (use #RRGGBB)", parseThemeAdd(listOf("mine", "#000", "#GGG", "#0ff", "#066")).exceptionOrNull()?.message)
        assertEquals("theme: bad border 'wavy' (single|double|rounded|ascii|bracket)", parseThemeAdd(listOf("mine", "#000", "#0f0", "#0ff", "#066", "wavy")).exceptionOrNull()?.message)
    }

    private fun theme(n: String) = buildCustomTheme(n, 0xFF101010, 0xFFEEEEEE, 0xFF3366FF, 0xFF888888, BorderStyle.DOUBLE)

    @Test fun builtThemesHaveNoGlowOrCyber() {
        val t = theme("x")
        assertEquals(false, t.glow); assertEquals(false, t.cyber)
        assertEquals(BorderStyle.DOUBLE, t.border)
    }

    @Test fun listsRoundTrip() {
        val list = listOf(theme("a"), theme("b-2"), theme("c_3"))
        assertEquals(list, CustomThemes.decode(CustomThemes.encode(list)))
        assertEquals(emptyList<Theme>(), CustomThemes.decode(CustomThemes.encode(emptyList())))
    }

    @Test fun badDataGivesAnEmptyList() {
        assertEquals(emptyList<Theme>(), CustomThemes.decode(null))
        assertEquals(emptyList<Theme>(), CustomThemes.decode(""))
        assertEquals(emptyList<Theme>(), CustomThemes.decode("{bad"))
        assertEquals(emptyList<Theme>(), CustomThemes.decode("[1,2,3]"))
    }

    @Test fun entriesWithBadFieldsAreSkippedAndTheListIsCappedAtEight() {
        val raw = """[{"name":"ok","bg":"#000000","fg":"#ffffff","accent":"#ff0000","dim":"#888888","border":"SINGLE"},
            {"name":"","bg":"#000000","fg":"#ffffff","accent":"#ff0000","dim":"#888888","border":"SINGLE"},
            {"name":"badcolor","bg":"nope","fg":"#ffffff","accent":"#ff0000","dim":"#888888","border":"SINGLE"},
            {"name":"weird border","bg":"#000000","fg":"#ffffff","accent":"#ff0000","dim":"#888888","border":"WAVY"}]"""
        assertEquals(listOf("ok"), CustomThemes.decode(raw).map { it.name })
        val many = CustomThemes.encode((1..12).map { theme("t$it") })
        assertEquals(8, CustomThemes.decode(many).size)
    }

    @Test fun theBookListsBuiltInsFirstAndFindsCustomOnesIgnoringCase() {
        val book = ThemeBook(listOf(theme("mine")))
        assertEquals(ThemeRegistry.all + theme("mine"), book.all)
        assertEquals("mine", book.byName("MINE")?.name)
        assertEquals("matrix", book.byName(" Matrix ")?.name)
        assertNull(book.byName("nope"))
    }

    @Test fun aCustomThemeNeverShadowsABuiltInOne() {
        val impostor = theme("matrix")
        val book = ThemeBook(listOf(impostor))
        assertEquals(ThemeRegistry.byName("matrix"), book.byName("matrix"))
        assertTrue(book.all.count { it.name == "matrix" } == 1)
    }

    // ---- the `theme add` / `theme rm` commands as pure functions ----

    private val args = listOf("mine", "#000000", "#00ff00", "#00ffff", "#006666")

    @Test fun addingAppendsAndSays() {
        val r = addCustomTheme(args, emptyList())
        assertEquals(listOf("mine"), r.list?.map { it.name })
        assertEquals("theme: added mine (theme mine to use it)", r.message)
    }

    @Test fun addingTheSameNameReplacesIt() {
        val first = addCustomTheme(args, emptyList()).list!!
        val r = addCustomTheme(listOf("mine", "#111111", "#eeeeee", "#ff0000", "#777777", "double"), first)
        assertEquals(1, r.list!!.size)
        assertEquals(0xFF111111, r.list!![0].background)
        assertEquals(BorderStyle.DOUBLE, r.list!![0].border)
    }

    @Test fun lowContrastStillAddsWithAWarning() {
        val r = addCustomTheme(listOf("dull", "#101010", "#181818", "#ff0000", "#777777"), emptyList())
        assertEquals(listOf("dull"), r.list?.map { it.name })
        assertTrue(r.message, r.message.endsWith("\ntheme: low contrast"))
    }

    @Test fun badInputChangesNothing() {
        assertNull(addCustomTheme(listOf("mine", "#000", "#GGG", "#0ff", "#066"), emptyList()).list)
        assertNull(addCustomTheme(listOf("Matrix", "#000", "#0f0", "#0ff", "#066"), emptyList()).list)
        assertNull(addCustomTheme(emptyList(), emptyList()).list)
        val eight = (1..8).map { theme("t$it") }
        val r = addCustomTheme(args, eight)
        assertNull(r.list)
        assertEquals("theme: too many custom themes (8 max)", r.message)
    }

    @Test fun removingTakesItOutAndFlagsTheActiveOne() {
        val list = listOf(theme("a"), theme("b"))
        val r = removeCustomTheme("A", list, active = "a")
        assertEquals(listOf("b"), r.list?.map { it.name })
        assertTrue(r.wasActive)
        assertEquals("theme: removed a", r.message)
        assertEquals(false, removeCustomTheme("b", list, active = "a").wasActive)
    }

    @Test fun removingBuiltInsOrUnknownNamesIsRefused() {
        assertNull(removeCustomTheme("matrix", emptyList(), "matrix").list)
        assertEquals("theme: matrix is built-in", removeCustomTheme("matrix", emptyList(), "matrix").message)
        assertEquals("theme: nope: no such custom theme", removeCustomTheme("nope", listOf(theme("a")), "a").message)
    }

    @Test fun describingMarksCustomThemes() {
        val lines = describeThemes("mine", listOf(theme("mine"))).lines()
        assertEquals(16, lines.size)
        assertEquals("* mine +", lines.last())
    }
}
