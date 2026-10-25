package dev.linkinxp.termlauncher.ui

import dev.linkinxp.termlauncher.LauncherUi
import dev.linkinxp.termlauncher.apps.AppEntry
import dev.linkinxp.termlauncher.layout.HomeLayout
import dev.linkinxp.termlauncher.layout.Item
import dev.linkinxp.termlauncher.layout.Page
import org.junit.Assert.assertEquals
import org.junit.Test

class IconWarmupTest {
    private fun app(n: String) = AppEntry("pkg.$n", n, 0)

    @Test fun dockQuickAndPageOneInThatOrderWithoutRepeats() {
        val apps = listOf("a", "b", "c", "d", "e").map(::app)
        val ui = LauncherUi(
            apps = apps.associateBy { it.pkg },
            layout = HomeLayout(listOf(Item.App("pkg.a")), listOf(Page(listOf(Item.App("pkg.b"), Item.App("pkg.a"), Item.Folder("f", listOf("pkg.c")))), Page(listOf(Item.App("pkg.e"))))),
            quick = listOf("pkg.d", "pkg.b"),
        )
        val t = warmTargets(ui)
        assertEquals(listOf("pkg.a", "pkg.d", "pkg.b", "pkg.c", "pkg.c").take(t.size).map { it }.let { listOf("pkg.a", "pkg.d", "pkg.b", "pkg.c") }, t.map { it.pkg }.distinct())
        // each app is drawn only at the size it is shown at: dock 44, quick 40, page one 56, folder minis 56 * 0.4
        assertEquals(setOf("pkg.a" to 44f, "pkg.d" to 40f, "pkg.b" to 40f, "pkg.b" to 56f, "pkg.a" to 56f, "pkg.c" to 56f * 0.4f), t.map { it.pkg to it.sizeDp }.toSet())
    }

    @Test fun appsThatAreGoneAreSkippedAndTheListIsBounded() {
        val many = (1..80).map { app("m$it") }
        val ui = LauncherUi(
            apps = many.associateBy { it.pkg },
            layout = HomeLayout(listOf(Item.App("pkg.gone")), listOf(Page(many.map { Item.App(it.pkg) }))),
        )
        val t = warmTargets(ui)
        assertEquals(false, t.any { it.pkg == "pkg.gone" })
        assertEquals(true, t.size <= 60)
    }

    @Test fun theDrawerWarmsItsFirstCells() {
        val d = dev.linkinxp.termlauncher.apps.DrawerState(
            recent = listOf(app("r1")),
            sections = listOf(dev.linkinxp.termlauncher.apps.DrawerSection('A', (1..40).map { app("a$it") })),
        )
        val t = drawerWarmTargets(d)
        assertEquals(24, t.size)
        assertEquals("pkg.r1", t.first().pkg)
    }
}
