package dev.linkinxp.termlauncher.layout

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DensityTest {
    @Test fun denserLevelsFitMoreColumnsAndSmallerThings() {
        val d = Density.entries
        assertEquals(listOf("DENSE", "COMPACT", "NORMAL", "ROOMY"), d.map { it.name })
        assertEquals(listOf(6, 5, 4, 3), d.map { it.columns })
        assertTrue(d.zipWithNext().all { (a, b) -> a.iconDp < b.iconDp && a.dockDp < b.dockDp && a.quickDp < b.quickDp && a.widgetScale < b.widgetScale })
        assertEquals(56f, Density.NORMAL.iconDp, 0f) // the size icons had before density existed
        assertEquals(44f, Density.NORMAL.dockDp, 0f); assertEquals(40f, Density.NORMAL.quickDp, 0f)
        assertEquals(1f, Density.NORMAL.widgetScale, 0f)
    }

    @Test fun namesDecodeAndUnknownOnesAreNormal() {
        assertEquals(Density.DENSE, Density.fromName("DENSE"))
        assertEquals(Density.NORMAL, Density.fromName("junk"))
        assertEquals(Density.NORMAL, Density.fromName(null))
    }

    @Test fun theDensityCommand() {
        assertEquals(DensityPlan(Density.DENSE, "density: dense (6 columns)"), densityPlan(" Dense "))
        assertEquals(DensityPlan(Density.ROOMY, "density: roomy (3 columns)"), densityPlan("roomy"))
        listOf(null, "", "tiny", "dense compact").forEach {
            assertEquals(DensityPlan(null, "usage: density dense|compact|normal|roomy"), densityPlan(it))
        }
    }

    @Test fun smallerWidgetsShrinkTheBaseButNeverBelowWhatTheWidgetNeeds() {
        assertEquals(96, widgetHeightDp(0, WidgetSize.SMALL))
        assertEquals(67, widgetHeightDp(0, WidgetSize.SMALL, 0.7f))      // 96 * 0.7
        assertEquals(134, widgetHeightDp(0, WidgetSize.MEDIUM, 0.7f))    // 192 * 0.7
        assertEquals(110, widgetHeightDp(110, WidgetSize.SMALL, 0.7f))   // the provider's own minimum wins
        assertEquals(400, widgetHeightDp(0, WidgetSize.LARGE, 1.5f))     // still capped
    }
}
