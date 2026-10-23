package dev.linkinxp.termlauncher.apps

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DrawerModelTest {
    private fun app(pkg: String, label: String, installed: Long = 0) = AppEntry(pkg, label, installed)
    private val chrome = app("c", "Chrome", 3)
    private val camera = app("cam", "camera", 1)
    private val files = app("f", "Files", 2)
    private val numeric = app("n", "1Password", 4)
    private val all = listOf(files, chrome, numeric, camera)

    private fun build(
        apps: List<AppEntry> = all,
        hidden: Set<String> = emptySet(),
        stats: Map<String, AppStat> = emptyMap(),
        sort: DrawerSort = DrawerSort.AZ,
        filter: String = "",
        showHidden: Boolean = false,
    ) = buildDrawer(apps, hidden, stats, sort, filter, showHidden)

    private fun DrawerState.pkgs() = sections.flatMap { s -> s.apps.map { it.pkg } }

    @Test fun azIsCaseInsensitiveGroupedByLetterWithHashLast() {
        val d = build()
        assertEquals(listOf('C', 'F', '#'), d.sections.map { it.letter })
        assertEquals(listOf("cam", "c", "f", "n"), d.pkgs()) // camera < Chrome ignoring case
    }

    @Test fun hiddenAppsAreExcludedUnlessShowHidden() {
        assertEquals(listOf("cam", "c", "n"), build(hidden = setOf("f")).pkgs())
    }

    @Test fun showHiddenListsOnlyTheHiddenApps() {
        assertEquals(listOf("f"), build(hidden = setOf("f"), showHidden = true).pkgs())
    }

    @Test fun mostUsedOrdersByLaunchesDescendingWithinASection() {
        val stats = mapOf("c" to AppStat(9, 10), "cam" to AppStat(2, 20))
        val d = build(sort = DrawerSort.MOST_USED, stats = stats)
        assertEquals(listOf("c", "cam"), d.sections.first { it.letter == 'C' }.apps.map { it.pkg })
    }

    @Test fun recentlyInstalledOrdersNewestFirstWithinASection() {
        val d = build(sort = DrawerSort.RECENTLY_INSTALLED)
        assertEquals(listOf("c", "cam"), d.sections.first { it.letter == 'C' }.apps.map { it.pkg })
    }

    @Test fun recentHoldsFourMostRecentlyUsedAndIsEmptyWhileFiltering() {
        val many = (1..6).map { app("p$it", "App$it") }
        val stats = many.associate { it.pkg to AppStat(1, it.pkg.drop(1).toLong()) }
        assertEquals(listOf("p6", "p5", "p4", "p3"), build(apps = many, stats = stats).recent.map { it.pkg })
        assertTrue(build(apps = many, stats = stats, filter = "app").recent.isEmpty())
    }

    @Test fun neverLaunchedAppsAreNotRecent() {
        assertTrue(build().recent.isEmpty())
    }

    @Test fun filterAppliesFuzzyRank() {
        assertEquals(listOf("c"), build(filter = "chr").pkgs())
    }

    @Test fun duplicateLabelsAreBothKeptInStableOrder() {
        val a = app("a1", "Same"); val b = app("a2", "Same")
        assertEquals(listOf("a1", "a2"), build(apps = listOf(b, a)).pkgs())
    }

    @Test fun emptyLabelGoesUnderHash() {
        val d = build(apps = listOf(app("e", "")))
        assertEquals(listOf('#'), d.sections.map { it.letter })
    }
}
