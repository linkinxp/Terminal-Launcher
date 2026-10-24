package dev.linkinxp.termlauncher.backup

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import dev.linkinxp.termlauncher.data.Prefs
import dev.linkinxp.termlauncher.layout.HomeLayout
import dev.linkinxp.termlauncher.layout.Item
import dev.linkinxp.termlauncher.layout.WidgetSize
import dev.linkinxp.termlauncher.quick.quickAdd
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class BackupRepositoryTest {
    private val prefs = Prefs(ApplicationProvider.getApplicationContext<Application>())
    private val repo = BackupRepository(prefs)

    // The DataStore is shared by the tests of this JVM: start every test from the defaults.
    @Before fun resetStores() = runBlocking {
        repo.apply(LauncherBackup())
        // apply() keeps widgets on purpose, so clear the layout outright to start every test clean.
        prefs.editLayout { Result.success(HomeLayout.default()) }
        Unit
    }

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

    @Test fun applyThenSnapshotRoundTripsEveryStore() = runBlocking {
        repo.apply(sample())
        assertEquals(sample(), repo.snapshot())
    }

    @Test fun snapshotNeverIncludesWidgets() = runBlocking {
        prefs.editLayout { Result.success(it.addWidget(0, Item.Widget(3, "p/.W", WidgetSize.MEDIUM)).pin("x")) }
        val layout = repo.snapshot().layout
        assertEquals(emptySet<Int>(), layout.widgetIds())
        assertEquals(listOf<Item>(Item.App("x")), layout.dock.filterIsInstance<Item.App>())
    }

    @Test fun anUnknownThemeKeepsTheCurrentOne() = runBlocking {
        repo.apply(sample().copy(theme = "matrix"))
        repo.apply(sample().copy(theme = "no-such-theme"))
        assertEquals("matrix", repo.snapshot().theme)
    }

    @Test fun unknownEnumNamesAndOutOfRangeDimFallBackToDefaults() = runBlocking {
        repo.apply(sample().copy(wall = WallBackup("BOGUS", "NOPE", 3, 500, false), drawerSort = "x", iconMode = "y"))
        val s = repo.snapshot()
        assertEquals(WallBackup("SOLID", "GLYPH", 3, 55, false), s.wall)
        assertEquals("AZ", s.drawerSort)
        assertEquals("TEXT", s.iconMode)
    }

    @Test fun applyingTheDefaultBackupDoesNotThrow() = runBlocking {
        repo.apply(LauncherBackup())
        assertEquals(LauncherBackup(), repo.snapshot())
    }

    @Test fun applyKeepsTheWidgetsThatAreOnTheDeviceNow() = runBlocking {
        prefs.editLayout { Result.success(it.addWidget(0, Item.Widget(9, "p/.W", WidgetSize.LARGE))) }
        repo.apply(sample())
        val stored = prefs.layout.first()
        assertEquals(setOf(9), stored.widgetIds())
        assertEquals(sample().layout.dock, stored.dock)
        assertEquals(emptySet<Int>(), repo.snapshot().layout.widgetIds()) // still never exported
    }

    @Test fun theQuickListIsBackedUpAndRestored() = runBlocking {
        repo.apply(sample().copy(quick = listOf("com.a", "com.b")))
        assertEquals(listOf("com.a", "com.b"), repo.snapshot().quick)
        repo.apply(sample().copy(quick = null)) // null = back to the defaults
        assertEquals(null, repo.snapshot().quick)
    }

    @Test fun editQuickStartsFromTheDefaultsWhenNothingIsStored() = runBlocking {
        prefs.setQuick(null)
        prefs.editQuick { stored -> (stored ?: listOf("default.dialer")) + "extra" }
        assertEquals(listOf("default.dialer", "extra"), prefs.quick.first())
        prefs.setQuick(null)
        assertEquals(null, prefs.quick.first())
    }

    @Test fun quickEditsAreAtomicAndCappedAtEight() = runBlocking {
        prefs.setQuick(emptyList())
        (1..12).map { i -> async { prefs.editQuick { l -> quickAdd(l ?: emptyList(), "p$i") } } }.awaitAll()
        assertEquals(8, prefs.quick.first()!!.size)
    }

    @Test fun theDrawerKeyboardSettingIsBackedUpAndRestored() = runBlocking {
        repo.apply(sample().copy(drawerKeyboard = false))
        assertEquals(false, repo.snapshot().drawerKeyboard)
        repo.apply(sample())
        assertEquals(true, repo.snapshot().drawerKeyboard) // on by default
    }

    @Test fun theClockFormatIsBackedUpAndRestored() = runBlocking {
        repo.apply(sample().copy(clockFormat = "H12"))
        assertEquals("H12", repo.snapshot().clockFormat)
        repo.apply(sample())
        assertEquals("AUTO", repo.snapshot().clockFormat)
    }
}
