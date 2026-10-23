package dev.linkinxp.termlauncher.sys

import org.junit.Assert.assertTrue
import org.junit.Test

class BootLogTest {
    @Test fun linesAreShortAndEndAtThePrompt() {
        assertTrue(bootLog.isNotEmpty())
        assertTrue(bootLog.all { it.length <= 40 })
        assertTrue(bootLog.last().contains("user@phone"))
    }
}
