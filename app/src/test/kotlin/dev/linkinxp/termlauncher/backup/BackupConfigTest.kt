package dev.linkinxp.termlauncher.backup

import android.app.Application
import android.content.pm.ApplicationInfo
import androidx.test.core.app.ApplicationProvider
import dev.linkinxp.termlauncher.R
import dev.linkinxp.termlauncher.layout.HomeLayout
import dev.linkinxp.termlauncher.layout.Item
import dev.linkinxp.termlauncher.layout.WidgetSize
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.xmlpull.v1.XmlPullParser

/** Android's own backup restores the layout and settings on a new phone or after a reinstall. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class BackupConfigTest {
    private val context = ApplicationProvider.getApplicationContext<Application>()

    @Test fun theAppAllowsBackup() = assertTrue(context.applicationInfo.flags and ApplicationInfo.FLAG_ALLOW_BACKUP != 0)

    @Test fun cloudBackupAndDeviceTransferBothCarryTheSettingsFiles() {
        val xml = context.resources.getXml(R.xml.data_extraction_rules)
        val sections = mutableListOf<String>()
        val included = mutableMapOf<String, MutableList<String>>()
        var section = ""
        while (xml.next() != XmlPullParser.END_DOCUMENT) {
            if (xml.eventType == XmlPullParser.START_TAG) {
                when (xml.name) {
                    "cloud-backup", "device-transfer" -> { section = xml.name; sections += section }
                    "include" -> included.getOrPut(section) { mutableListOf() } += "${xml.getAttributeValue(null, "domain")}/${xml.getAttributeValue(null, "path")}"
                }
            }
        }
        assertEquals(listOf("cloud-backup", "device-transfer"), sections)
        assertEquals(listOf("file/datastore"), included["cloud-backup"])
        assertEquals(listOf("file/datastore"), included["device-transfer"])
    }

    @Test fun widgetsThatDoNotExistOnThisPhoneAreDropped() {
        val l = HomeLayout.default().addWidget(0, Item.Widget(3, "p/.W", WidgetSize.SMALL)).addWidget(0, Item.Widget(4, "p/.W", WidgetSize.SMALL)).pin("a")
        val cleaned = l.dropWidgets(setOf(3))
        assertEquals(setOf(4), cleaned.widgetIds())
        assertEquals(l.dock, cleaned.dock)
        assertEquals(l, l.dropWidgets(emptySet()))
    }
}
