package dev.linkinxp.termlauncher.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class DockLabelTest {
    @Test fun fullFirstWordLowercased() {
        assertEquals("phone", dockName("Phone"))
        assertEquals("messages", dockName("Messages"))
        assertEquals("garmin", dockName("Garmin Connect"))
    }

    @Test fun longNamesAreCutToNineCharacters() {
        assertEquals("authentic", dockName("Authenticator"))
        assertEquals("", dockName("   "))
    }
}
