package dev.linkinxp.termlauncher.layout

import org.junit.Assert.assertEquals
import org.junit.Test

class FolderOrderTest {
    private val l = HomeLayout.default().mkdir("work").getOrThrow().mv("a", "work").getOrThrow().mv("b", "work").getOrThrow().mv("c", "work").getOrThrow()
    private fun apps(l: HomeLayout) = l.folders().first { it.name == "work" }.apps

    @Test fun reordersInsideTheFolder() {
        assertEquals(listOf("b", "c", "a"), apps(l.moveInFolder("work", 0, 2)))
        assertEquals(listOf("c", "a", "b"), apps(l.moveInFolder("work", 2, 0)))
    }

    @Test fun worksForAPinnedFolderToo() {
        val pinned = l.pinFolder("work")
        assertEquals(listOf("b", "a", "c"), apps(pinned.moveInFolder("work", 0, 1)))
    }

    @Test fun badInputChangesNothing() {
        assertEquals(l, l.moveInFolder("work", 1, 1))
        assertEquals(l, l.moveInFolder("work", 0, 9))
        assertEquals(l, l.moveInFolder("nope", 0, 1))
    }
}
