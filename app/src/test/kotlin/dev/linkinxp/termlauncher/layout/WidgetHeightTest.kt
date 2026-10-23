package dev.linkinxp.termlauncher.layout

import org.junit.Assert.assertEquals
import org.junit.Test

class WidgetHeightTest {
    @Test fun baseHeightsPerSize() {
        assertEquals(96, widgetHeightDp(0, WidgetSize.SMALL))
        assertEquals(192, widgetHeightDp(0, WidgetSize.MEDIUM))
        assertEquals(288, widgetHeightDp(0, WidgetSize.LARGE))
    }

    @Test fun theProvidersMinimumWinsWhenLarger() {
        assertEquals(250, widgetHeightDp(250, WidgetSize.SMALL))
        assertEquals(288, widgetHeightDp(100, WidgetSize.LARGE))
    }

    @Test fun cappedAtFourHundred() {
        assertEquals(400, widgetHeightDp(500, WidgetSize.LARGE))
    }

    @Test fun negativeMinimumFallsBackToTheBase() {
        assertEquals(96, widgetHeightDp(-5, WidgetSize.SMALL))
    }
}
