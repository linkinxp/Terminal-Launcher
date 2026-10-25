package dev.linkinxp.termlauncher.backup

import dev.linkinxp.termlauncher.fx.FxSettings
import org.junit.Assert.assertEquals
import org.junit.Test

/** An old backup's explicit "scanlines off" must not switch the theme-following scanlines off on a new install. */
class BackupFxTest {
    private val off = FxBackup(scanlines = false, flicker = true, glitch = false, cursor = false, boot = false)

    @Test fun legacyBackupsReadExplicitOffAsFollowTheTheme() {
        val s = off.toSettings(fxSchema = 0)
        assertEquals(null, s.scanlines)
        assertEquals(FxSettings(null, true, false, false, false), s) // everything else is taken as stored
    }

    @Test fun legacyOnStaysOn() = assertEquals(true, FxBackup(scanlines = true).toSettings(0).scanlines)

    @Test fun currentBackupsKeepAnExplicitOff() = assertEquals(false, off.toSettings(FX_SCHEMA).scanlines)

    @Test fun newBackupsRecordTheSchemaAndOldFilesReadAsLegacy() {
        val text = encodeBackup(LauncherBackup(fxSchema = FX_SCHEMA))
        assertEquals(FX_SCHEMA, decodeBackup(text).getOrThrow().fxSchema)
        val legacy = text.replace(Regex("\"fxSchema\":\\s*\\d+,?"), "")
        assertEquals(0, decodeBackup(legacy).getOrThrow().fxSchema)
    }
}
