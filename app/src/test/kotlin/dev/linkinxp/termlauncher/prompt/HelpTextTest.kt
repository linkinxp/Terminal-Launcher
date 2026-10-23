package dev.linkinxp.termlauncher.prompt

import dev.linkinxp.termlauncher.LauncherViewModel
import org.junit.Assert.assertTrue
import org.junit.Test

class HelpTextTest {
    @Test fun helpFitsTheOutputPanelAndMentionsEveryCommand() {
        val lines = LauncherViewModel.HELP.lines()
        assertTrue("at most 12 lines, was ${lines.size}", lines.size <= 12)
        assertTrue(lines.all { it.length <= 40 })
        LauncherViewModel.COMMANDS.forEach { word -> assertTrue("help misses $word", LauncherViewModel.HELP.contains(word)) }
    }
}
