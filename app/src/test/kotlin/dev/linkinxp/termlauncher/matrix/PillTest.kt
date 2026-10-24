package dev.linkinxp.termlauncher.matrix

import dev.linkinxp.termlauncher.wall.WallMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PillTest {
    @Test fun theRedPillGoesDownTheRabbitHole() {
        val p = pillPlan("red")
        assertEquals("matrix", p.theme)
        assertEquals(WallMode.RAIN, p.wallMode)
        assertEquals("you take the red pill. welcome to the real world.", p.message)
    }

    @Test fun theBluePillEndsTheStory() {
        val p = pillPlan(" BLUE ")
        assertEquals("classy-black", p.theme)
        assertEquals(WallMode.SOLID, p.wallMode)
        assertEquals("you take the blue pill. the story ends.", p.message)
    }

    @Test fun anythingElseOnlyShowsTheChoice() {
        listOf(null, "", "green", "red blue").forEach {
            val p = pillPlan(it)
            assertNull(p.theme); assertNull(p.wallMode)
            assertEquals("usage: pill red|blue", p.message)
        }
    }

    @Test fun easterEggs() {
        assertEquals("there is no spoon.", SPOON)
        assertEquals("(\\_/)\n(o.o)  knock, knock, neo.", RABBIT)
    }
}
