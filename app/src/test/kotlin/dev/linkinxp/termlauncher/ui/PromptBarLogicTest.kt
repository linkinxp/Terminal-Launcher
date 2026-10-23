package dev.linkinxp.termlauncher.ui

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PromptBarLogicTest {
    @Test fun blockCursorWhenTypingAtTheEndOfShortText() {
        assertTrue(useBlockCursor(true, 5, 5, 5))
        assertTrue(useBlockCursor(true, 0, 0, 0))
    }

    @Test fun nativeCaretWhenTheEffectIsOff() {
        assertFalse(useBlockCursor(false, 5, 5, 5))
    }

    @Test fun nativeCaretWhenTheCaretIsNotAtTheEnd() {
        assertFalse(useBlockCursor(true, 5, 2, 2))
    }

    @Test fun nativeCaretWhenTextIsSelected() {
        assertFalse(useBlockCursor(true, 5, 0, 5))
    }

    @Test fun nativeFieldForLongTextSoNothingIsHiddenByAnEllipsis() {
        assertTrue(useBlockCursor(true, 28, 28, 28))
        assertFalse(useBlockCursor(true, 29, 29, 29))
    }
}
