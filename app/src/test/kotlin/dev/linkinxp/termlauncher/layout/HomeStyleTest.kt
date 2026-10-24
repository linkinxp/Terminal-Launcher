package dev.linkinxp.termlauncher.layout

import org.junit.Assert.assertEquals
import org.junit.Test

class HomeStyleTest {
    @Test fun layoutCommandPlans() {
        assertEquals(LayoutPlan(HomeStyle.GRID, Restructure.APPS_FIRST, "layout: apps-first (apps page, glance page)"), layoutPlan("apps-first"))
        assertEquals(LayoutPlan(HomeStyle.CLASSIC, Restructure.CLASSIC, "layout: classic"), layoutPlan(" Classic "))
        assertEquals(LayoutPlan(HomeStyle.BOTTOM, Restructure.NONE, "layout: bottom"), layoutPlan("bottom"))
        assertEquals(LayoutPlan(HomeStyle.GRID, Restructure.NONE, "layout: grid"), layoutPlan("grid"))
    }

    @Test fun badOrMissingNamesGiveUsage() {
        listOf("nope", "", null, "grid classic").forEach {
            val p = layoutPlan(it)
            assertEquals(null, p.style)
            assertEquals("usage: layout classic|bottom|grid|apps-first", p.message)
        }
    }

    @Test fun storedNamesDecode() {
        assertEquals(HomeStyle.GRID, HomeStyle.fromName("GRID"))
        assertEquals(HomeStyle.CLASSIC, HomeStyle.fromName("junk"))
        assertEquals(HomeStyle.CLASSIC, HomeStyle.fromName(null))
    }
}
