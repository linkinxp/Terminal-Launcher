package dev.linkinxp.termlauncher.layout

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeLayoutTest {
    private val empty = HomeLayout.default()
    private fun apps(l: HomeLayout) = (l.dock + l.pages.flatMap { it.items }).flatMap {
        when (it) { is Item.App -> listOf(it.pkg); is Item.Folder -> it.apps; is Item.Widget, is Item.Panel -> emptyList() }
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
        val fresh = empty.withDefaultPanels() // a fresh home starts with the four info panels on page one
        assertEquals(fresh, HomeLayout.decode(null))
        assertEquals(fresh, HomeLayout.decode("{bad"))
        assertEquals(fresh, HomeLayout.decode("{\"v\":99}"))
        assertEquals(fresh, HomeLayout.decode("{\"v\":99,\"layout\":{\"dock\":[],\"pages\":[{\"items\":[]}]}}"))
    }

    @Test fun encodeDecodeRoundTrips() {
        val l = empty.pin("a").mkdir("games").getOrThrow().mv("b", "games").getOrThrow()
            .addPage().addToPage(1, Item.App("c"))
        assertEquals(l, HomeLayout.decode(l.encode()))
    }

    @Test fun unfileMovesAppFromFolderToFirstPage() {
        val l = empty.mkdir("a").getOrThrow().mv("x", "a").getOrThrow().unfile("x")
        assertEquals(listOf("x"), apps(l))
        assertTrue(l.folders().single().apps.isEmpty())
        assertEquals(Item.App("x"), l.pages[0].items.last())
    }

    @Test fun unfileOfAppNotInAFolderIsUnchanged() {
        val l = empty.addToPage(0, Item.App("x"))
        assertEquals(l, l.unfile("x"))
    }

    @Test fun mkdirOnASpecificPage() {
        val l = empty.addPage().mkdir("a", page = 1).getOrThrow()
        assertEquals(listOf(Item.Folder("a", emptyList())), l.pages[1].items)
        assertTrue(l.pages[0].items.isEmpty())
    }

    @Test fun mkdirOnMissingPageFails() {
        assertTrue(empty.mkdir("a", page = 3).isFailure)
    }

    @Test fun pinFolderMovesItFromPageToDockAndBack() {
        val pinned = empty.mkdir("a").getOrThrow().pinFolder("A")
        assertEquals(listOf(Item.Folder("a", emptyList())), pinned.dock)
        assertTrue(pinned.pages[0].items.isEmpty())
        val back = pinned.unpinFolder("a")
        assertTrue(back.dock.isEmpty())
        assertEquals(listOf(Item.Folder("a", emptyList())), back.pages[0].items)
    }

    @Test fun pinFolderWithFullDockIsUnchanged() {
        val full = (1..5).fold(empty.mkdir("a").getOrThrow()) { l, i -> l.pin("p$i") }
        assertEquals(full, full.pinFolder("a"))
    }

    @Test fun pinnedFolderStillAcceptsMvAndRmdir() {
        val l = empty.mkdir("a").getOrThrow().pinFolder("a").mv("x", "a").getOrThrow()
        assertEquals(listOf("x"), (l.dock.single() as Item.Folder).apps)
        assertEquals(listOf(Item.App("x")), l.rmdir("a").pages[0].items)
    }

    @Test fun removeShortcutDropsLooseShortcutFromPagesOnly() {
        val l = empty.addToPage(0, Item.App("x")).addToPage(0, Item.App("y"))
            .mkdir("a").getOrThrow().mv("z", "a").getOrThrow().pin("x")
        val r = l.removeShortcut("x")
        assertEquals(listOf("y", "z"), r.pages[0].items.flatMap { if (it is Item.App) listOf(it.pkg) else (it as Item.Folder).apps })
        assertEquals(listOf(Item.App("x")), r.dock)
    }

    @Test fun removeShortcutOfAbsentAppIsUnchanged() {
        assertEquals(empty, empty.removeShortcut("nope"))
    }

    @Test fun renameFolderKeepsAppsAndPosition() {
        val l = empty.addToPage(0, Item.App("first")).mkdir("games").getOrThrow().mv("x", "games").getOrThrow()
            .renameFolder("GAMES", "arcade").getOrThrow()
        assertEquals(listOf(Item.App("first"), Item.Folder("arcade", listOf("x"))), l.pages[0].items)
    }

    @Test fun renameFolderInDock() {
        val l = empty.mkdir("a").getOrThrow().pinFolder("a").renameFolder("a", "b").getOrThrow()
        assertEquals(listOf(Item.Folder("b", emptyList())), l.dock)
    }

    @Test fun renameFolderCaseOnlyChangeIsAllowed() {
        val l = empty.mkdir("games").getOrThrow().renameFolder("games", "Games").getOrThrow()
        assertEquals("Games", l.folders().single().name)
    }

    @Test fun renameFolderRejectsBadOrTakenOrMissing() {
        val l = empty.mkdir("a").getOrThrow().mkdir("b").getOrThrow()
        assertTrue(l.renameFolder("a", "B").isFailure)
        assertTrue(l.renameFolder("a", "ls").isFailure)
        assertTrue(l.renameFolder("a", "x y").isFailure)
        assertTrue(l.renameFolder("nope", "c").isFailure)
        assertEquals(listOf("a", "b"), l.folders().map { it.name })
    }

    @Test fun mkdirRejectsNewReservedWords() {
        for (word in listOf("fx", "history", "neofetch", "whoami", "uptime", "date", "fortune", "clear", "sudo", "matrix")) {
            assertTrue("'$word' should be reserved", empty.mkdir(word).isFailure)
        }
    }

    @Test fun mkdirRejectsWall() {
        assertTrue(empty.mkdir("wall").isFailure)
    }

    @Test fun mkdirRejectsPanelAndWx() {
        assertTrue(empty.mkdir("panel").isFailure)
        assertTrue(empty.mkdir("wx").isFailure)
    }

    private fun w(id: Int, provider: String = "p/.W", size: WidgetSize = WidgetSize.MEDIUM) = Item.Widget(id, provider, size)

    @Test fun addWidgetPutsItOnThePage() {
        assertEquals(listOf<Item>(w(1)), empty.addWidget(0, w(1)).pages[0].items)
    }

    @Test fun addWidgetToAMissingPageLeavesTheLayoutUnchanged() {
        assertEquals(empty, empty.addWidget(5, w(1)))
    }

    @Test fun aDuplicateWidgetIdIsIgnored() {
        assertEquals(1, empty.addWidget(0, w(1)).addWidget(0, w(1)).widgetIds().size)
    }

    @Test fun removeWidgetKeepsOtherItems() {
        val l = empty.addToPage(0, Item.App("a")).addWidget(0, w(1)).addWidget(0, w(2)).removeWidget(1)
        assertEquals(listOf(Item.App("a"), w(2)), l.pages[0].items)
    }

    @Test fun resizeChangesOnlyThatWidget() {
        val l = empty.addWidget(0, w(1)).addWidget(0, w(2)).resizeWidget(2, WidgetSize.LARGE)
        assertEquals(listOf<Item>(w(1), w(2, size = WidgetSize.LARGE)), l.pages[0].items)
    }

    @Test fun pruneWidgetsDropsUninstalledProvidersAndReturnsTheirIds() {
        val l = empty.addToPage(0, Item.App("a")).addWidget(0, w(1, "keep/.W")).addWidget(0, w(2, "gone/.W"))
            .addPage().addWidget(1, w(3, "gone/.W"))
        val (pruned, removed) = l.pruneWidgets(setOf("keep/.W"))
        assertEquals(setOf(1), pruned.widgetIds())
        assertEquals(listOf(Item.App("a"), w(1, "keep/.W")), pruned.pages[0].items)
        assertEquals(setOf(2, 3), removed.toSet())
    }

    @Test fun widgetIdsListEveryPage() {
        val l = empty.addWidget(0, w(1)).addPage().addWidget(1, w(2))
        assertEquals(setOf(1, 2), l.widgetIds())
    }

    @Test fun removingAPageMakesItsWidgetIdsDisappear() {
        val l = empty.addWidget(0, w(1)).addPage().addWidget(1, w(2)).removePage(1)
        assertEquals(setOf(1), l.widgetIds())
    }

    @Test fun layoutsWithWidgetsRoundTrip() {
        val l = empty.pin("a").addWidget(0, w(7, "p/.W", WidgetSize.SMALL)).mkdir("g").getOrThrow()
        assertEquals(l, HomeLayout.decode(l.encode()))
    }

    @Test fun layoutsSavedBeforeWidgetsStillDecode() {
        val old = """{"v":1,"layout":{"dock":[],"pages":[{"items":[{"type":"app","pkg":"x"}]}]}}"""
        assertEquals(listOf<Item>(Item.App("x")), HomeLayout.decode(old).pages[0].items.filterNot { it is Item.Panel })
    }

    @Test fun appPruningLeavesWidgetsAlone() {
        val l = empty.addWidget(0, w(1)).addToPage(0, Item.App("gone")).prune(emptySet())
        assertEquals(listOf<Item>(w(1)), l.pages[0].items)
    }

    @Test fun folderOperationsStillWorkNextToAWidget() {
        val l = empty.addWidget(0, w(1)).mkdir("a").getOrThrow().mv("x", "a").getOrThrow()
        assertEquals(setOf(1), l.widgetIds())
        assertEquals(listOf("x"), l.folders().single().apps)
        val back = l.unfile("x").rmdir("a").pinFolder("nope")
        assertEquals(setOf(1), back.widgetIds())
    }

    @Test fun mkdirRejectsSettings() {
        assertTrue(empty.mkdir("settings").isFailure)
    }

    @Test fun withWidgetsFromKeepsWidgetsOnTheSamePageIndex() {
        val old = empty.addPage().addWidget(0, w(1)).addWidget(1, w(2, "q/.W", WidgetSize.SMALL))
        val fresh = HomeLayout.default().addPage().pin("a").addToPage(0, Item.App("x"))
        val merged = fresh.withWidgetsFrom(old)
        assertEquals(listOf<Item>(Item.App("x"), w(1)), merged.pages[0].items)
        assertEquals(listOf<Item>(w(2, "q/.W", WidgetSize.SMALL)), merged.pages[1].items)
        assertEquals(fresh.dock, merged.dock)
    }

    @Test fun widgetsOfAPageThatNoLongerExistsGoToTheLastPage() {
        val old = empty.addPage().addPage().addWidget(2, w(5))
        val merged = HomeLayout.default().withWidgetsFrom(old)
        assertEquals(listOf<Item>(w(5)), merged.pages.single().items)
    }

    @Test fun withWidgetsFromIsANoOpWithoutWidgetsAndSkipsIdsAlreadyThere() {
        val fresh = HomeLayout.default().addToPage(0, Item.App("x"))
        assertEquals(fresh, fresh.withWidgetsFrom(empty))
        val both = fresh.addWidget(0, w(1))
        assertEquals(both, both.withWidgetsFrom(empty.addWidget(0, w(1))))
    }

    @Test fun mkdirRejectsQuick() {
        assertTrue(empty.mkdir("quick").isFailure)
    }

    @Test fun mkdirRejectsClock() {
        assertTrue(empty.mkdir("clock").isFailure)
    }
}
