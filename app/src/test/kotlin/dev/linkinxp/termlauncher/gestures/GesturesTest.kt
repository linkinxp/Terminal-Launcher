package dev.linkinxp.termlauncher.gestures

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class GesturesTest {
    @Test fun theLeftHalfPullsTheNotificationsAndTheRightHalfTheQuickSettings() {
        assertEquals(SwipeDownAction.NOTIFICATIONS, swipeDownAction(10f, 1000f))
        assertEquals(SwipeDownAction.NOTIFICATIONS, swipeDownAction(499f, 1000f))
        assertEquals(SwipeDownAction.QUICK_SETTINGS, swipeDownAction(500f, 1000f))
        assertEquals(SwipeDownAction.QUICK_SETTINGS, swipeDownAction(990f, 1000f))
    }

    @Test fun anUnknownWidthFallsBackToTheNotifications() {
        assertEquals(SwipeDownAction.NOTIFICATIONS, swipeDownAction(500f, 0f))
        assertEquals(SwipeDownAction.NOTIFICATIONS, swipeDownAction(-5f, 1000f))
    }

    @Test fun lockingWithoutTheServiceRunningFails() {
        ScreenLock.service = null
        assertFalse(ScreenLock.lock())
    }
}
