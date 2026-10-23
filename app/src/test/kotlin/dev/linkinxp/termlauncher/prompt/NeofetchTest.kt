package dev.linkinxp.termlauncher.prompt

import org.junit.Assert.assertTrue
import org.junit.Test

class NeofetchTest {
    private val info = DeviceInfo("Pixel 10 Pro", "17", "0.2.0", "3d 4h", "neon-city", 142, 41)

    @Test fun containsEveryField() {
        val out = neofetch(info)
        listOf("Pixel 10 Pro", "17", "0.2.0", "3d 4h", "neon-city", "142", "41%").forEach {
            assertTrue("missing $it", out.contains(it))
        }
    }

    @Test fun linesFitFortyColumns() {
        assertTrue(neofetch(info).lines().all { it.length <= 40 })
        val long = neofetch(info.copy(model = "M".repeat(60)))
        assertTrue(long.lines().all { it.length <= 40 })
    }

    @Test fun unknownRamPrintsQuestionMark() {
        assertTrue(neofetch(info.copy(ramPct = null)).contains("?"))
    }
}
