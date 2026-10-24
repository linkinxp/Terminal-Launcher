package dev.linkinxp.termlauncher.theme

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ColorsTest {
    @Test fun sixAndThreeDigitFormsParseWithOrWithoutHash() {
        assertEquals(0xFF112233L, parseHexColor("#112233"))
        assertEquals(0xFF112233L, parseHexColor("112233"))
        assertEquals(0xFFAABBCCL, parseHexColor("#abc"))
        assertEquals(0xFFAABBCCL, parseHexColor("ABC"))
        assertEquals(0xFFAABBCCL, parseHexColor("  #AbC  "))
        assertEquals(0xFF00FF41L, parseHexColor("#00ff41"))
    }

    @Test fun badInputIsNull() {
        listOf("#12345", "#GGGGGG", "", "#", "#1234567", "12", "#12 34 56", "0x112233", "#ééé").forEach {
            assertNull("'$it'", parseHexColor(it))
        }
    }

    @Test fun contrastRatios() {
        assertEquals(21.0, contrastRatio(0xFF000000, 0xFFFFFFFF), 0.01)
        assertEquals(1.0, contrastRatio(0xFF336699, 0xFF336699), 0.0001)
        assertEquals(contrastRatio(0xFF123456, 0xFFABCDEF), contrastRatio(0xFFABCDEF, 0xFF123456), 0.0001)
        assertEquals(4.0, contrastRatio(0xFF777777, 0xFFFFFFFF), 0.6) // mid grey on white is around 4.5
    }
}
