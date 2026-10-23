package dev.linkinxp.termlauncher.layout

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeLayoutTest {
    private val empty = HomeLayout.default()
    private fun apps(l: HomeLayout) = (l.dock + l.pages.flatMap { it.items }).flatMap {
        when (it) { is Item.App -> listOf(it.pkg); is Item.Folder -> it.apps }
    }

    @Test fun defaultHasEmptyDockAndOneEmptyPage() {
        assertTrue(empty.dock.isEmpty())
        assertEquals(1, empty.pages.size)
        assertTrue(empty.pages[0].items.isEmpty())
    }

    @Test fun pinSixthAppLeavesDockUnchanged() {
        val five = (1..5).fold(empty) { l, i -> l.pin("p$i") }
        assertEquals(5, five.dock.size)
        assertEquals(five, five.pin("p6"))
    }

    @Test fun pinTwiceDoesNotDuplicate() {
        assertEquals(1, empty.pin("a").pin("a").dock.size)
    }

    @Test fun unpinRemovesFromDock() {
        assertTrue(empty.pin("a").unpin("a").dock.isEmpty())
    }

    @Test fun mkdirRejectsBadNames() {
        for (bad in listOf("ls", "theme", "MKDIR", "my games", "a/b", "", "  ", "x".repeat(25))) {
            assertTrue("'$bad' should fail", empty.mkdir(bad).isFailure)
        }
    }

    @Test fun mkdirRejectsCaseInsensitiveDuplicate() {
        val l = empty.mkdir("Games").getOrThrow()
        assertTrue(l.mkdir("games").isFailure)
    }

    @Test fun mkdirCreatesFolderOnFirstPage() {
        val l = empty.mkdir("games").getOrThrow()
        assertEquals(listOf(Item.Folder("games", emptyList())), l.pages[0].items)
        assertEquals(listOf(Item.Folder("games", emptyList())), l.folders())
    }

    @Test fun mvLeavesAppInExactlyOneFolder() {
        val l = empty.mkdir("a").getOrThrow().mkdir("b").getOrThrow()
            .mv("pkg", "a").getOrThrow().mv("pkg", "B").getOrThrow()
        assertEquals(listOf("pkg"), apps(l))
        assertEquals(listOf("pkg"), l.folders().first { it.name == "b" }.apps)
    }

    @Test fun mvIntoMissingFolderFails() {
        assertTrue(empty.mv("pkg", "nope").isFailure)
    }

    @Test fun mvRemovesLooseShortcutOfSameApp() {
        val l = empty.addToPage(0, Item.App("pkg")).mkdir("a").getOrThrow().mv("pkg", "a").getOrThrow()
        assertEquals(listOf("pkg"), apps(l))
    }

    @Test fun rmdirReturnsAppsToFirstPage() {
        val l = empty.mkdir("a").getOrThrow().mv("x", "a").getOrThrow().mv("y", "a").getOrThrow().rmdir("a")
        assertEquals(listOf(Item.App("x"), Item.App("y")), l.pages[0].items)
        assertTrue(l.folders().isEmpty())
    }

    @Test fun pruneRemovesUninstalledButKeepsEmptyFolders() {
        val l = empty.pin("gone").pin("keep").addToPage(0, Item.App("gone"))
            .mkdir("a").getOrThrow().mv("gone", "a").getOrThrow().mv("keep", "a").getOrThrow()
            .prune(setOf("keep"))
        assertEquals(listOf("keep", "keep"), apps(l))
        val emptied = empty.mkdir("a").getOrThrow().mv("gone", "a").getOrThrow().prune(emptySet())
        assertEquals(listOf(Item.Folder("a", emptyList())), emptied.folders())
    }

    @Test fun addPageStopsAtFive() {
        val l = (1..10).fold(empty) { acc, _ -> acc.addPage() }
        assertEquals(5, l.pages.size)
    }

    @Test fun removePageKeepsAtLeastOne() {
        assertEquals(empty, empty.removePage(0))
        assertEquals(1, empty.addPage().removePage(0).pages.size)
    }

    @Test fun movePageReorders() {
        val l = empty.addToPage(0, Item.App("first")).addPage().addPage().movePage(0, 2)
        assertEquals(listOf(Item.App("first")), l.pages[2].items)
    }

    @Test fun outOfRangePageOperationsAreNoOps() {
        assertEquals(empty, empty.addToPage(7, Item.App("a")))
        assertEquals(empty, empty.movePage(0, 9))
        assertEquals(empty, empty.removePage(-1))
    }

    @Test fun decodeOfNullCorruptOrNewerVersionIsDefault() {
        assertEquals(empty, HomeLayout.decode(null))
        assertEquals(empty, HomeLayout.decode("{bad"))
        assertEquals(empty, HomeLayout.decode("{\"v\":99}"))
        assertEquals(empty, HomeLayout.decode("{\"v\":99,\"layout\":{\"dock\":[],\"pages\":[{\"items\":[]}]}}"))
    }

    @Test fun encodeDecodeRoundTrips() {
        val l = empty.pin("a").mkdir("games").getOrThrow().mv("b", "games").getOrThrow()
            .addPage().addToPage(1, Item.App("c"))
        assertEquals(l, HomeLayout.decode(l.encode()))
    }
}
