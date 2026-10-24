package dev.linkinxp.termlauncher.importer

import dev.linkinxp.termlauncher.layout.HomeLayout
import dev.linkinxp.termlauncher.layout.Item
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FavoritesMapperTest {
    private var nextId = 1

    private fun intent(pkg: String) =
        "#Intent;action=android.intent.action.MAIN;category=android.intent.category.LAUNCHER;launchFlags=0x10200000;component=$pkg/$pkg.Main;end"

    private fun app(pkg: String, screen: Int = 0, x: Int = 0, y: Int = 0, container: Int = -100, id: Int = nextId++) =
        FavRow(id, 0, container, screen, x, y, pkg, intent(pkg))

    private fun folder(id: Int, title: String?, screen: Int = 0, x: Int = 0, y: Int = 0, container: Int = -100) =
        FavRow(id, 2, container, screen, x, y, title, null)

    private fun widget(screen: Int = 0, x: Int = 0, y: Int = 0, id: Int = nextId++) = FavRow(id, 4, -100, screen, x, y, null, null)

    private fun installedOf(rows: List<FavRow>) = rows.mapNotNull { packageFromIntent(it.intent) }.toSet()
    private fun map(rows: List<FavRow>, installed: Set<String> = installedOf(rows)) = mapFavorites(rows, installed)
    private fun pkgs(items: List<Item>) = items.map { (it as Item.App).pkg }

    @Test fun packageFromIntentReadsTheComponent() {
        assertEquals("com.android.chrome", packageFromIntent(intent("com.android.chrome")))
    }

    @Test fun packageFromIntentFallsBackToThePackageExtra() {
        assertEquals("com.foo", packageFromIntent("#Intent;action=android.intent.action.MAIN;package=com.foo;end"))
    }

    @Test fun packageFromIntentRejectsGarbage() {
        for (bad in listOf(null, "", "garbage", "component=;end", "#Intent;component=/x;end")) assertNull("$bad", packageFromIntent(bad))
    }

    @Test fun twoScreensBecomeTwoPagesOrderedByRowThenColumn() {
        val r = map(listOf(app("a", screen = 1), app("b", screen = 0, x = 1, y = 1), app("c", screen = 0), app("d", screen = 0, x = 2)))
        assertEquals(2, r.layout.pages.size)
        assertEquals(listOf("c", "d", "b"), pkgs(r.layout.pages[0].items))
        assertEquals(listOf("a"), pkgs(r.layout.pages[1].items))
        assertEquals(4, r.apps)
    }

    @Test fun sparseScreenNumbersBecomeConsecutivePages() {
        val r = map(listOf(app("a", screen = 2), app("b", screen = 7), app("c", screen = 9)))
        assertEquals(listOf(listOf("a"), listOf("b"), listOf("c")), r.layout.pages.map { pkgs(it.items) })
    }

    @Test fun moreThanFiveScreensOverflowOntoTheFifthPage() {
        val r = map((0..6).map { app("p$it", screen = it) })
        assertEquals(5, r.layout.pages.size)
        assertEquals(listOf("p4", "p5", "p6"), pkgs(r.layout.pages[4].items))
    }

    @Test fun hotseatFillsTheDockByColumnAndExtrasGoToPageOne() {
        val rows = listOf(5, 3, 0, 4, 1, 2).map { app("h$it", screen = 0, x = it, container = -101) } + app("desk", screen = 0)
        val r = map(rows)
        assertEquals(listOf("h0", "h1", "h2", "h3", "h4"), pkgs(r.layout.dock))
        assertEquals(listOf("desk", "h5"), pkgs(r.layout.pages[0].items))
    }

    @Test fun aFolderHoldsItsAppsInCellOrderAndTheyAreNotAlsoLoose() {
        val rows = listOf(
            folder(10, "media"),
            app("x2", container = 10, x = 2), app("x0", container = 10, x = 0), app("x1", container = 10, x = 1),
        )
        val r = map(rows)
        val f = r.layout.pages[0].items.single() as Item.Folder
        assertEquals("media", f.name)
        assertEquals(listOf("x0", "x1", "x2"), f.apps)
        assertEquals(1, r.folders)
        assertEquals(3, r.apps)
    }

    @Test fun notInstalledAppsAreDroppedAndCounted() {
        val rows = listOf(app("here"), app("gone"), folder(10, "f"), app("in", container = 10), app("gone2", container = 10))
        val r = map(rows, installed = setOf("here", "in"))
        assertEquals(2, r.missingApps)
        assertEquals(listOf("here"), r.layout.pages[0].items.filterIsInstance<Item.App>().map { it.pkg })
        assertEquals(listOf("in"), r.layout.folders().single().apps)
    }

    @Test fun aFolderWithNothingInstalledIsDropped() {
        val rows = listOf(folder(10, "f"), app("gone", container = 10))
        val r = map(rows, installed = emptySet())
        assertEquals(0, r.folders)
        assertTrue(r.layout.folders().isEmpty())
        assertEquals(1, r.missingApps)
    }

    @Test fun widgetsAreSkippedAndCounted() {
        val r = map(listOf(app("a"), widget(), widget(screen = 1)))
        assertEquals(2, r.skippedWidgets)
        assertEquals(1, r.layout.pages.sumOf { it.items.size })
    }

    @Test fun unknownItemTypesAreIgnored() {
        val r = map(listOf(app("a"), FavRow(99, 1, -100, 0, 3, 3, "shortcut", "#Intent;component=x/y;end")))
        assertEquals(1, r.apps)
        assertEquals(0, r.missingApps)
    }

    @Test fun rowsInsideAMissingFolderAreIgnored() {
        val r = map(listOf(app("orphan", container = 77), app("a")))
        assertEquals(listOf("a"), pkgs(r.layout.pages[0].items))
    }

    @Test fun aFolderInsideItselfDoesNotLoop() {
        val r = map(listOf(folder(5, "loop", container = 5), app("kid", container = 5), app("a")))
        assertEquals(listOf("a"), pkgs(r.layout.pages[0].items))
    }

    @Test fun thePackageIsPlacedOnlyOnceFirstInTheDockThenPages() {
        val r = map(listOf(app("dup", screen = 1), app("dup", container = -101), app("dup", screen = 0)))
        assertEquals(listOf("dup"), pkgs(r.layout.dock))
        assertTrue(r.layout.pages.all { it.items.isEmpty() })
        assertEquals(1, r.apps)
    }

    @Test fun folderTitlesAreCleanedToTheLayoutsRules() {
        fun names(vararg titles: String?) =
            map(titles.mapIndexed { i, t -> listOf(folder(100 + i, t, x = i), app("k$i", container = 100 + i)) }.flatten())
                .layout.folders().map { it.name }
        assertEquals(listOf("folder"), names(null))
        assertEquals(listOf("folder"), names("  "))
        assertEquals(listOf("My-Games"), names("My Games"))
        assertEquals(listOf("ab"), names("a/b"))
        assertEquals(listOf("theme_"), names("theme"))
        assertEquals(listOf("x".repeat(24)), names("x".repeat(40)))
        assertEquals(listOf("Games", "games2"), names("Games", "games"))
    }

    @Test fun noRowsGiveTheDefaultLayout() {
        val r = map(emptyList())
        assertEquals(HomeLayout.default(), r.layout)
        assertEquals(0, r.apps)
    }

    @Test fun fiveThousandRowsFinishQuicklyAndCapAtFivePages() {
        val rows = (0 until 5000).map { app("p$it", screen = it % 10, x = it % 7, y = it % 11) }
        val started = System.currentTimeMillis()
        val r = map(rows)
        assertTrue("took too long", System.currentTimeMillis() - started < 5_000)
        assertEquals(5, r.layout.pages.size)
        assertEquals(5000, r.apps)
    }

    @Test fun nonBreakingSpacesAndCutEmojiInFolderTitlesAreHandled() {
        fun name(title: String) =
            map(listOf(folder(10, title), app("k", container = 10))).layout.folders().single().name
        assertEquals("My-Apps", name("My\u00A0Apps"))
        val cut = name("x".repeat(23) + "\uD83C\uDF89") // the emoji would be cut in half at 24 characters
        assertEquals("x".repeat(23), cut)
        assertTrue(cut.none { Character.isSurrogate(it) })
    }
}
