package dev.linkinxp.termlauncher.wall

import org.junit.Assert.assertEquals
import org.junit.Test

class WallFieldsTest {
    @Test fun eachModeShowsOnlyTheFieldsItUses() {
        assertEquals(emptySet<WallField>(), wallFields(WallMode.SOLID))
        assertEquals(setOf(WallField.DIM), wallFields(WallMode.SYSTEM))
        assertEquals(setOf(WallField.STYLE, WallField.SEED, WallField.DIM, WallField.ANIMATE), wallFields(WallMode.ASCII))
        assertEquals(setOf(WallField.SEED, WallField.DIM), wallFields(WallMode.RAIN))
        assertEquals(setOf(WallField.SVG_STYLE, WallField.SEED, WallField.DIM), wallFields(WallMode.SVG))
    }

    @Test fun everyModeIsCovered() {
        WallMode.entries.forEach { wallFields(it) }
    }
}
