package dev.linkinxp.termlauncher.media

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MediaTest {
    private object NoControls : MediaControls { override fun previous() {}; override fun togglePlay() {}; override fun next() {} }
    private fun info(title: String = "Song", artist: String = "Band", playing: Boolean = true) = MediaInfo("pkg.music", title, artist, playing, NoControls)

    @Test fun theSessionThatIsPlayingWinsElseTheFirst() {
        assertEquals(1, pickSession(listOf(SessionSummary("a", false), SessionSummary("b", true), SessionSummary("c", true))))
        assertEquals(0, pickSession(listOf(SessionSummary("a", false), SessionSummary("b", false))))
        assertNull(pickSession(emptyList()))
    }

    @Test fun nowPlayingLinesFitTheScreenAndHandleMissingParts() {
        assertEquals(listOf("Song", "Band"), nowPlayingLines(info()))
        assertEquals(listOf("Song"), nowPlayingLines(info(artist = " ")))
        assertEquals(listOf("(untitled)", "Band"), nowPlayingLines(info(title = "")))
        assertEquals(40, nowPlayingLines(info(title = "x".repeat(80))).first().length)
        assertEquals("…", nowPlayingLines(info(title = "x".repeat(80))).first().takeLast(1))
    }

    @Test fun thePlayButtonShowsWhatItWillDo() {
        assertEquals("[pause]", playLabel(true)); assertEquals("[play]", playLabel(false))
    }

    @Test fun theStorePublishesAndClears() {
        MediaStore.publish(info())
        assertEquals("Song", MediaStore.state.value?.title)
        MediaStore.publish(null)
        assertNull(MediaStore.state.value)
    }
}
