package dev.linkinxp.termlauncher.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class ImePaddingTest {
    @Test fun theKeyboardPushesTheContentUpByWhatTheNavigationBarDoesNotAlreadyCover() {
        assertEquals(800, imeBottomPadding(imeBottom = 920, navBottom = 120, skip = false))
        assertEquals(0, imeBottomPadding(imeBottom = 0, navBottom = 120, skip = false))
        assertEquals(0, imeBottomPadding(imeBottom = 100, navBottom = 120, skip = false)) // the bar is taller than the keyboard
    }

    @Test fun whileSkippedTheKeyboardIsIgnoredAltogether() {
        assertEquals(0, imeBottomPadding(imeBottom = 920, navBottom = 120, skip = true))
    }
}
