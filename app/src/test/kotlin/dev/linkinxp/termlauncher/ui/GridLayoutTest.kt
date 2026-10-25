package dev.linkinxp.termlauncher.ui

import dev.linkinxp.termlauncher.apps.AppEntry
import dev.linkinxp.termlauncher.apps.DrawerSection
import dev.linkinxp.termlauncher.apps.DrawerState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GridLayoutTest {
    private fun app(n: String) = AppEntry("pkg.$n", n, 0)
    private val state = DrawerState(
        recent = listOf(app("Chrome"), app("Maps")),
        sections = listOf(DrawerSection('B', listOf(app("Bandcamp"))), DrawerSection('C', listOf(app("Calendar"), app("Camera")))),
    )

    @Test fun recentThenLetteredSectionsWithHeaderPositions() {
        val g = gridLayout(state, emptyList(), filtering = false)
        assertEquals(
            listOf("h:recent", "c:Chrome", "c:Maps", "h:-- B --", "c:Bandcamp", "h:-- C --", "c:Calendar", "c:Camera"),
            g.entries.map { it.debug() },
        )
        assertEquals(mapOf('B' to 3, 'C' to 5), g.headerIndex)
    }

    @Test fun foldersComeFirstAndShiftTheHeaders() {
        val g = gridLayout(state, listOf("media" to 2), filtering = false)
        assertEquals("f:media", g.entries.first().debug())
        assertEquals(mapOf('B' to 4, 'C' to 6), g.headerIndex)
    }

    @Test fun searchingIsAFlatBestFirstGridWithoutHeaders() {
        val s = DrawerState(emptyList(), emptyList(), results = listOf(app("Camera"), app("Calendar")))
        val g = gridLayout(s, listOf("cam" to 1), filtering = true)
        assertEquals(listOf("f:cam", "c:Camera", "c:Calendar"), g.entries.map { it.debug() })
        assertTrue(g.headerIndex.isEmpty())
    }

    @Test fun anEmptyDrawerHasNoEntries() {
        assertTrue(gridLayout(DrawerState(emptyList(), emptyList()), emptyList(), filtering = false).entries.isEmpty())
    }

    private fun GridEntry.debug() = when (this) {
        is GridEntry.Header -> "h:$text"
        is GridEntry.Folder -> "f:$name"
        is GridEntry.Cell -> "c:${app.label}"
        is GridEntry.Extra -> "x:${extra.label}"
    }

    @Test fun extrasFollowTheAppsAndEveryKeyIsUnique() {
        val s = DrawerState(emptyList(), emptyList(), results = listOf(app("Camera")))
        val extras = listOf(
            dev.linkinxp.termlauncher.search.ExtraResult(dev.linkinxp.termlauncher.search.ExtraKind.SETTING, "wifi settings", "x"),
            dev.linkinxp.termlauncher.search.ExtraResult(dev.linkinxp.termlauncher.search.ExtraKind.WEB, "search the web: cam", "cam"),
        )
        val g = gridLayout(s, emptyList(), filtering = true, extras = extras)
        assertEquals(listOf("c:Camera", "x:wifi settings", "x:search the web: cam"), g.entries.map { it.debug() })
        assertEquals(g.entries.size, g.entries.map { it.key }.toSet().size)
    }
}
