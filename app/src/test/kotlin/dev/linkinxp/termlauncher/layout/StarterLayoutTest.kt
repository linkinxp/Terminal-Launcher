package dev.linkinxp.termlauncher.layout

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** A new install is not an empty home: familiar apps are placed once, and only when no layout was ever stored. */
class StarterLayoutTest {
    private val all = setOf("com.android.chrome", "com.android.vending", "com.android.settings", "com.google.android.apps.photos", "com.google.android.gm", "x.other")

    @Test fun commonAppsFillTheDockAndTheFirstPage() {
        val l = seededLayout(null, all)!!
        assertEquals(listOf<Item>(Item.App("com.android.chrome"), Item.App("com.android.vending"), Item.App("com.android.settings")), l.dock)
        assertEquals(listOf<Item>(Item.App("com.google.android.apps.photos"), Item.App("com.google.android.gm")), l.pages[0].items)
        assertEquals(setOf(PanelKind.SEARCH, PanelKind.SYS, PanelKind.CAL, PanelKind.WX), l.placedPanels()) // the glance page stays
    }

    @Test fun appsThatAreNotInstalledAreSkippedAndUnknownOnesNeverAdded() {
        val l = seededLayout(null, setOf("com.android.vending", "x.other"))!!
        assertEquals(listOf<Item>(Item.App("com.android.vending")), l.dock)
        assertTrue(l.pages[0].items.isEmpty())
    }

    @Test fun nothingIsSeededOnceALayoutExists() {
        assertNull(seededLayout(HomeLayout.default().encode(), all))
    }

    @Test fun nothingIsSeededWhenNoStarterAppIsInstalled() {
        assertNull(seededLayout(null, setOf("x.other")))
    }
}
