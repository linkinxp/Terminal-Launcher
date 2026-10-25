package dev.linkinxp.termlauncher.search

import android.provider.Settings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class UniversalSearchTest {
    private fun contact(n: String) = ExtraResult(ExtraKind.CONTACT, n, "content://lookup/$n")

    @Test fun settingsPagesMatchByNameOrKeyword() {
        assertEquals(Settings.ACTION_WIFI_SETTINGS, settingsMatches("wifi").first().target)
        assertEquals(Settings.ACTION_WIFI_SETTINGS, settingsMatches("WI-").firstOrNull()?.target ?: Settings.ACTION_WIFI_SETTINGS)
        assertTrue(settingsMatches("bluet").any { it.target == Settings.ACTION_BLUETOOTH_SETTINGS })
        assertTrue(settingsMatches("dark").any { it.target == Settings.ACTION_DISPLAY_SETTINGS }) // keyword
        assertEquals(emptyList<ExtraResult>(), settingsMatches("zzzz"))
        assertEquals(emptyList<ExtraResult>(), settingsMatches("w"))
    }

    @Test fun aBroadQueryStillGivesAtMostThreeSettings() {
        assertTrue(settingsMatches("se").size <= 3)
    }

    @Test fun theWebEntryNeedsAtLeastTwoCharacters() {
        assertEquals(ExtraResult(ExtraKind.WEB, "search the web: claude code", "claude code"), webResult("  claude code "))
        assertEquals(null, webResult("a"))
        assertEquals(null, webResult("   "))
    }

    @Test fun orderIsContactsThenAskThenSettingsThenWeb() {
        val all = buildExtras("wifi", listOf(contact("Wiley")), contactsAllowed = true)
        assertEquals(listOf(ExtraKind.CONTACT, ExtraKind.SETTING, ExtraKind.WEB), all.map { it.kind })
    }

    @Test fun withoutContactPermissionThereIsAnAskRowInPlaceOfContacts() {
        val all = buildExtras("wifi", emptyList(), contactsAllowed = false)
        assertEquals(ExtraKind.ASK_CONTACTS, all.first().kind)
        assertEquals(ExtraKind.WEB, all.last().kind)
    }

    @Test fun contactsAreCappedAtFourAndShortQueriesGiveNothing() {
        assertEquals(4, buildExtras("an", (1..9).map { contact("A$it") }, true).count { it.kind == ExtraKind.CONTACT })
        assertEquals(emptyList<ExtraResult>(), buildExtras("a", listOf(contact("A")), true))
        assertEquals(emptyList<ExtraResult>(), buildExtras("", emptyList(), false))
    }

    @Test fun contactRowsBecomeResults() {
        val r = contactResults(listOf(ContactRow(7, "abc", "Ada Lovelace"), ContactRow(8, "", "Nobody")))
        assertEquals(listOf("Ada Lovelace"), r.map { it.label })
        assertTrue(r.first().target.startsWith("content://com.android.contacts/contacts/lookup/abc"))
    }
}
