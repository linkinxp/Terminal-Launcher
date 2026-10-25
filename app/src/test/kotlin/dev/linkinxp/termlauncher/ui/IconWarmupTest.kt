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
        assertEquals(listOf("pkg.a", "pkg.d", "pkg.b", "pkg.c"), warmTargets(ui).map { it.first })
    }

    @Test fun appsThatAreGoneAreSkippedAndTheListIsBounded() {
        val many = (1..80).map { app("m$it") }
        val ui = LauncherUi(
            apps = many.associateBy { it.pkg },
            layout = HomeLayout(listOf(Item.App("pkg.gone")), listOf(Page(many.map { Item.App(it.pkg) }))),
        )
        val t = warmTargets(ui)
        assertEquals(false, t.any { it.first == "pkg.gone" })
        assertEquals(true, t.size <= 40)
    }

    @Test fun theDrawerWarmsItsFirstCells() {
        val d = dev.linkinxp.termlauncher.apps.DrawerState(
            recent = listOf(app("r1")),
            sections = listOf(dev.linkinxp.termlauncher.apps.DrawerSection('A', (1..40).map { app("a$it") })),
        )
        val t = drawerWarmTargets(d)
        assertEquals(24, t.size)
        assertEquals("pkg.r1", t.first().first)
    }
}
