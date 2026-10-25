package dev.linkinxp.termlauncher.store

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** The web page Play Console links to must say what PRIVACY.md says (same headings, same promises). */
class PrivacyPageTest {
    private val root = generateSequence(File("").absoluteFile) { it.parentFile }.first { File(it, "PRIVACY.md").exists() }
    private val md = File(root, "PRIVACY.md").readText()
    private val html = File(root, "privacy/index.html").readText()

    @Test fun thePageHasEveryHeadingOfTheMarkdown() {
        val headings = md.lines().filter { it.startsWith("#") }.map { it.trimStart('#', ' ') }
        assertTrue(headings.isNotEmpty())
        headings.forEach { assertTrue("the page misses the heading '$it'", html.contains(it)) }
    }

    @Test fun thePageIsACompleteHtmlDocumentWithTheKeyPromises() {
        assertTrue(html.contains("<!doctype html>", ignoreCase = true) && html.contains("</html>"))
        listOf("no advertising", "Open-Meteo", "never reads the text", "com.linkinxp.terminal").forEach { assertTrue("the page misses '$it'", html.contains(it, ignoreCase = true)) }
    }
}
