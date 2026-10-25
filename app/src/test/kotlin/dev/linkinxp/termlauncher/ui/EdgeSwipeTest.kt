package dev.linkinxp.termlauncher.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class EdgeSwipeTest {
    @Test fun theLeftAndRightEdgesTurnThePage() {
        assertEquals(-1, edgeDirection(10f, 1000f, 60f))
        assertEquals(1, edgeDirection(990f, 1000f, 60f))
        assertEquals(0, edgeDirection(500f, 1000f, 60f))
        assertEquals(0, edgeDirection(60f, 1000f, 60f))
    }

    @Test fun theVerticalSwipeThresholdScalesWithTheScreenDensity() {
        assertEquals(44f, swipeThresholdPx(1f), 0.01f)
        assertEquals(115.5f, swipeThresholdPx(2.625f), 0.01f)
    }
}
