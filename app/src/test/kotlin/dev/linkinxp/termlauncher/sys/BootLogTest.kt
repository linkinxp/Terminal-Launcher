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

class BootWindowTest {
    @Test fun opensOnlyShortlyAfterAnUnlock() {
        assertTrue(bootWindowOpen(nowMs = 10_000, unlockAtMs = 9_000))
        assertTrue(bootWindowOpen(nowMs = 12_000, unlockAtMs = 9_000)) // exactly 3 s: still open
        assertTrue(!bootWindowOpen(nowMs = 12_001, unlockAtMs = 9_000))
    }

    @Test fun neverOpensWithoutAnUnlockOrFromTheFuture() {
        assertTrue(!bootWindowOpen(nowMs = 10_000, unlockAtMs = 0))
        assertTrue(!bootWindowOpen(nowMs = 5_000, unlockAtMs = 9_000))
    }
}
