package dev.linkinxp.termlauncher.badges

import android.os.Looper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNotSame
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import java.time.Duration
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class BadgeListenerServiceTest {
    private fun waitFor(what: String, ok: () -> Boolean) {
        val end = System.currentTimeMillis() + 5_000
        while (!ok() && System.currentTimeMillis() < end) Thread.sleep(20)
        assertEquals(what, true, ok())
    }

    @Test fun counts_are_refreshed_on_a_background_thread_after_a_removal() {
        val service = Robolectric.buildService(BadgeListenerService::class.java).create().get()
        BadgeStore.publish(mapOf("stale" to 3))
        service.onNotificationRemoved(null)
        Shadows.shadowOf(service.workLooper).idleFor(Duration.ofMillis(500))
        waitFor("the store was refreshed") { BadgeStore.counts.value.isEmpty() }
        assertNotNull(service.publishedOn)
        assertNotSame("not the main thread", Looper.getMainLooper().thread, service.publishedOn)
    }

    @Test fun a_burst_of_removals_is_coalesced_into_one_refresh() {
        val service = Robolectric.buildService(BadgeListenerService::class.java).create().get()
        service.publishCount = 0
        repeat(20) { service.onNotificationRemoved(null) }
        Shadows.shadowOf(service.workLooper).idleFor(Duration.ofMillis(500))
        waitFor("one refresh happened") { service.publishCount >= 1 }
        assertEquals(1, service.publishCount)
    }
}
