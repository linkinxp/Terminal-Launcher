package dev.linkinxp.termlauncher.backup

import dev.linkinxp.termlauncher.layout.HomeLayout
import dev.linkinxp.termlauncher.layout.Item
import dev.linkinxp.termlauncher.layout.WidgetSize
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LauncherBackupTest {
    private fun sample() = LauncherBackup(
        layout = HomeLayout.default().pin("a").mkdir("g").getOrThrow().mv("b", "g").getOrThrow(),
        hidden = setOf("h"),
        theme = "neon-city",
        fx = FxBackup(scanlines = true, flicker = true, glitch = false, cursor = false, boot = false),
        wall = WallBackup("ASCII", "CIRCUIT", 7, 40, true),
        panels = PanelBackup(cal = true, wx = true, search = false),
        wx = WxBackup("Zagreb", 45.8, 15.97, true),
        drawerSort = "MOST_USED",
        iconMode = "GLYPH",
    )

    @Test fun aPopulatedBackupRoundTrips() {
        assertEquals(sample(), decodeBackup(encodeBackup(sample())).getOrThrow())
    }

    @Test fun scanlinesFollowingTheThemeRoundTripsAsNull() {
        val b = sample().copy(fx = FxBackup(scanlines = null))
        assertEquals(null, decodeBackup(encodeBackup(b)).getOrThrow().fx.scanlines)
    }

    @Test fun widgetsAreNeverExported() {
        val withWidget = sample().copy(layout = sample().layout.addWidget(0, Item.Widget(5, "p/.W", WidgetSize.LARGE)))
        val json = encodeBackup(withWidget)
        assertFalse(json.contains("widget"))
        assertEquals(setOf<Int>(), decodeBackup(json).getOrThrow().layout.widgetIds())
    }

    @Test fun garbageInputsFailWithTheNotABackupMessage() {
        for (bad in listOf(null, "", "   ", "{bad", "[]", "null", "{}", """{"format":"other"}""", """{"theme":"matrix"}""")) {
            assertEquals("import: not a launcher backup", decodeBackup(bad).exceptionOrNull()?.message)
        }
    }

    @Test fun aNewerVersionIsRefused() {
        val json = """{"format":"terminal-launcher-backup","version":2}"""
        assertEquals("import: backup is from a newer version", decodeBackup(json).exceptionOrNull()?.message)
    }

    @Test fun aWrongTypedSectionFallsBackToItsDefaultAndTheRestStays() {
        val json = """{"format":"terminal-launcher-backup","version":1,"theme":"matrix","wall":{"dim":"x"},"fx":null,"hidden":["h"]}"""
        val b = decodeBackup(json).getOrThrow()
        assertEquals("matrix", b.theme)
        assertEquals(setOf("h"), b.hidden)
        assertEquals(WallBackup(), b.wall)
        assertEquals(FxBackup(), b.fx)
    }

    @Test fun aWrongTypedThemeIsOnlyThatSectionBeingInvalid() {
        val json = """{"format":"terminal-launcher-backup","version":1,"theme":5,"drawerSort":"MOST_USED"}"""
        val b = decodeBackup(json).getOrThrow()
        assertEquals("classy-black", b.theme)
        assertEquals("MOST_USED", b.drawerSort)
    }

    @Test fun anUnknownLayoutItemTypeResetsOnlyTheLayout() {
        val json = """{"format":"terminal-launcher-backup","version":1,"theme":"matrix","layout":{"dock":[],"pages":[{"items":[{"type":"alien"}]}]}}"""
        val b = decodeBackup(json).getOrThrow()
        assertEquals(HomeLayout.default(), b.layout)
        assertEquals("matrix", b.theme)
    }

    @Test fun aLayoutWithoutPagesResetsToTheDefault() {
        val json = """{"format":"terminal-launcher-backup","version":1,"layout":{"dock":[],"pages":[]}}"""
        assertEquals(HomeLayout.default(), decodeBackup(json).getOrThrow().layout)
    }

    @Test fun aNewerVersionIsReportedEvenWhenTheRestHasChangedTypes() {
        val json = """{"format":"terminal-launcher-backup","version":2,"theme":5,"layout":"new shape"}"""
        assertEquals("import: backup is from a newer version", decodeBackup(json).exceptionOrNull()?.message)
    }

    @Test fun aNonNumericVersionIsNotABackup() {
        val json = """{"format":"terminal-launcher-backup","version":"one"}"""
        assertEquals("import: not a launcher backup", decodeBackup(json).exceptionOrNull()?.message)
    }

    @Test fun missingSectionsFallBackToDefaultsAndUnknownFieldsAreIgnored() {
        val json = """{"format":"terminal-launcher-backup","version":1,"theme":"matrix","somethingNew":{"x":1}}"""
        val b = decodeBackup(json).getOrThrow()
        assertEquals("matrix", b.theme)
        assertEquals(FxBackup(), b.fx)
        assertEquals(HomeLayout.default(), b.layout)
        assertEquals(PanelBackup(), b.panels)
    }

    @Test fun describeBackupSummarisesWhatWouldBeRestored() {
        val lines = describeBackup(sample(), installed = setOf("a"))
        assertTrue(lines.any { it == "pages: 1" })
        assertTrue(lines.any { it == "dock: 1" })
        assertTrue(lines.any { it == "folders: 1" })
        assertTrue(lines.any { it == "apps not installed here: 1" })
        assertTrue(lines.any { it == "theme: neon-city" })
    }

    @Test fun withoutWidgetsRemovesOnlyWidgets() {
        val l = HomeLayout.default().addToPage(0, Item.App("x")).addWidget(0, Item.Widget(1, "p/.W", WidgetSize.SMALL))
        assertEquals(listOf<Item>(Item.App("x")), l.withoutWidgets().pages[0].items)
        assertEquals(l.dock, l.withoutWidgets().dock)
    }

    @Test fun theQuickListRoundTripsAndNullMeansDefaults() {
        val custom = sample().copy(quick = listOf("com.a", "com.b"))
        assertEquals(listOf("com.a", "com.b"), decodeBackup(encodeBackup(custom)).getOrThrow().quick)
        assertEquals(null, decodeBackup(encodeBackup(sample())).getOrThrow().quick)
        val json = """{"format":"terminal-launcher-backup","version":1,"quick":"not a list"}"""
        assertEquals(null, decodeBackup(json).getOrThrow().quick)
    }

    @Test fun theDrawerKeyboardSettingRoundTripsAndDefaultsOff() {
        assertEquals(true, decodeBackup(encodeBackup(sample().copy(drawerKeyboard = true))).getOrThrow().drawerKeyboard)
        val json = """{"format":"terminal-launcher-backup","version":1}"""
        assertEquals(false, decodeBackup(json).getOrThrow().drawerKeyboard)
    }
}
