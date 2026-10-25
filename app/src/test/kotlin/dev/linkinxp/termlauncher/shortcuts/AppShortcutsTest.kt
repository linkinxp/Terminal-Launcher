package dev.linkinxp.termlauncher.shortcuts

import android.app.Application
import android.content.Intent
import android.content.pm.ShortcutInfo
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AppShortcutsTest {
    private fun s(id: String, label: String, rank: Int = 0, enabled: Boolean = true) = AppShortcut("pkg.x", id, label, rank, enabled)

    @Test fun pickKeepsTheBestFiveByRankWithoutBlanksDuplicatesOrDisabled() {
        val all = listOf(
            s("e", "Echo", 5), s("a", "Alpha", 1), s("a", "Alpha again", 2), s("b", " ", 3), s("c", "Charlie", 0),
            s("d", "Disabled", 0, enabled = false), s("f", "Foxtrot", 6), s("g", "Golf", 7), s("h", "Hotel", 8),
        )
        assertEquals(listOf("Charlie", "Alpha", "Echo", "Foxtrot", "Golf"), pickShortcuts(all).map { it.label })
    }

    @Test fun pickOfNothingIsNothing() = assertEquals(emptyList<AppShortcut>(), pickShortcuts(emptyList()))

    @Test fun theRepositoryMapsSystemShortcutInfoAndSurvivesAFailingQuery() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val info = ShortcutInfo.Builder(app, "incognito").setShortLabel("New incognito tab").setIntent(Intent(Intent.ACTION_VIEW)).setRank(2).build()
        val repo = ShortcutRepository(app) { pkg -> if (pkg == "boom") throw SecurityException("not the default launcher") else listOf(info) }
        assertEquals(listOf("New incognito tab"), repo.list("com.chrome").map { it.label })
        assertEquals("incognito", repo.list("com.chrome").first().id)
        assertTrue(repo.list("boom").isEmpty())
    }
}
