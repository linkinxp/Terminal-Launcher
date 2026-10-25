package dev.linkinxp.termlauncher.media

import android.provider.Settings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TogglesTest {
    @Test fun theToggleRowHasTheFiveButtonsInOrder() {
        assertEquals(listOf("torch", "wifi", "bt", "sound", "location"), toggleSpecs().map { it.id })
    }

    @Test fun everyNonTorchToggleOpensASystemScreen() {
        val actions = toggleSpecs().filter { it.id != "torch" }.associate { it.id to it.action }
        assertEquals(Settings.Panel.ACTION_INTERNET_CONNECTIVITY, actions["wifi"])
        assertEquals(Settings.ACTION_BLUETOOTH_SETTINGS, actions["bt"])
        assertEquals(Settings.Panel.ACTION_VOLUME, actions["sound"])
        assertEquals(Settings.ACTION_LOCATION_SOURCE_SETTINGS, actions["location"])
        assertTrue(toggleSpecs().first { it.id == "torch" }.action == null)
    }

    @Test fun theTorchButtonShowsItsState() {
        assertEquals("[torch on]", torchLabel(true)); assertEquals("[torch]", torchLabel(false))
    }
}
