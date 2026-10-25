package dev.linkinxp.termlauncher.badges

import org.junit.Assert.assertEquals
import org.junit.Test

class BadgesTest {
    private fun n(pkg: String, ongoing: Boolean = false, summary: Boolean = false) = NotifInfo(pkg, ongoing, summary)

    @Test fun eachPackageCountsItsOrdinaryNotifications() {
        assertEquals(mapOf("a" to 2, "b" to 1), countBadges(listOf(n("a"), n("a"), n("b"))))
    }

    @Test fun ongoingNotificationsAndGroupSummariesDoNotCount() {
        val list = listOf(n("music", ongoing = true), n("chat", summary = true), n("chat"), n("chat"), n("nav", ongoing = true))
        assertEquals(mapOf("chat" to 2), countBadges(list))
    }

    @Test fun nothingGivesNothingAndOwnNotificationsAreSkipped() {
        assertEquals(emptyMap<String, Int>(), countBadges(emptyList()))
        assertEquals(mapOf("a" to 1), countBadges(listOf(n("a"), n("dev.linkinxp.termlauncher")), ownPackage = "dev.linkinxp.termlauncher"))
    }

    @Test fun theLabelIsTheCountOrNinePlus() {
        assertEquals("", badgeLabel(0)); assertEquals("1", badgeLabel(1)); assertEquals("9", badgeLabel(9)); assertEquals("9+", badgeLabel(10)); assertEquals("9+", badgeLabel(250))
    }

    @Test fun aFolderShowsTheSumOfItsApps() {
        assertEquals(5, folderBadge(listOf("a", "b", "c"), mapOf("a" to 2, "b" to 3, "z" to 9)))
        assertEquals(0, folderBadge(emptyList(), mapOf("a" to 1)))
    }

    @Test fun theStorePublishesAndReplacesTheCounts() {
        BadgeStore.publish(mapOf("a" to 1))
        assertEquals(mapOf("a" to 1), BadgeStore.counts.value)
        BadgeStore.publish(emptyMap())
        assertEquals(emptyMap<String, Int>(), BadgeStore.counts.value)
    }
}
