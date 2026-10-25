package dev.linkinxp.termlauncher.wall

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import javax.xml.parsers.DocumentBuilderFactory

class SvgWallpaperTest {
    private val colors = SvgColors(background = 0xFF0B0014, foreground = 0xFF00F0FF, accent = 0xFFFF2BD6, dim = 0xFF7A3E8C)

    private fun parse(text: String) = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(ByteArrayInputStream(text.toByteArray()))

    @Test fun everyStyleIsAWellFormedSvgWithTheRightViewBox() {
        SvgStyle.entries.forEach { style ->
            val doc = parse(svgText(style, 7, colors, 1080, 2400))
            assertEquals("$style", "svg", doc.documentElement.nodeName)
            assertEquals("$style", "0 0 1080 2400", doc.documentElement.getAttribute("viewBox"))
        }
    }

    @Test fun everyStyleUsesAllFourThemeColorsAndStaysUnderTheElementCap() {
        SvgStyle.entries.forEach { style ->
            val text = svgText(style, 3, colors, 1080, 2400)
            listOf("#0b0014", "#00f0ff", "#ff2bd6", "#7a3e8c").forEach { assertTrue("$style misses $it", text.contains(it)) }
            val count = parse(text).getElementsByTagName("*").length
            assertTrue("$style has $count elements", count in 10..1500)
        }
    }

    @Test fun theSameInputsGiveTheSameDocumentAndSeedsAndStylesDiffer() {
        SvgStyle.entries.forEach { style ->
            assertEquals(svgText(style, 5, colors, 1080, 2400), svgText(style, 5, colors, 1080, 2400))
            assertNotEquals("$style ignores the seed", svgText(style, 5, colors, 1080, 2400), svgText(style, 6, colors, 1080, 2400))
        }
        assertEquals(SvgStyle.entries.size, SvgStyle.entries.map { svgText(it, 5, colors, 1080, 2400) }.toSet().size)
    }

    @Test fun otherScreenSizesScaleTheArt() {
        SvgStyle.entries.forEach { style ->
            val doc = parse(svgText(style, 2, colors, 720, 1560))
            assertEquals("0 0 720 1560", doc.documentElement.getAttribute("viewBox"))
        }
    }

    @Test fun degenerateSizesStillGiveAValidDocument() {
        listOf(0 to 0, -5 to 100, 100 to -5).forEach { (w, h) ->
            SvgStyle.entries.forEach { style -> assertEquals("svg", parse(svgText(style, 1, colors, w, h)).documentElement.nodeName) }
        }
    }

    @Test fun styleNamesParse() {
        assertEquals(SvgStyle.HEXNET, SvgStyle.fromName("HEXNET"))
        assertEquals(SvgStyle.CIRCUIT, SvgStyle.fromName("nope"))
        assertEquals(SvgStyle.CIRCUIT, SvgStyle.fromName(null))
        assertEquals(listOf("CIRCUIT", "HEXNET", "RADAR", "TUNNEL", "GLITCH", "WINDOWS", "HELIX", "TOPO"), SvgStyle.entries.map { it.name })
    }
}
