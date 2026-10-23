package dev.linkinxp.termlauncher.weather

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FetchPolicyTest {
    private val now = 10_000_000L
    private val fiveMin = 5 * 60_000L

    @Test fun forcedRefreshAlwaysFetches() {
        assertTrue(shouldFetchWeather(force = true, cacheFresh = true, nowMs = now, lastAttemptMs = now))
    }

    @Test fun aFreshCacheIsNotRefetched() {
        assertFalse(shouldFetchWeather(force = false, cacheFresh = true, nowMs = now, lastAttemptMs = null))
    }

    @Test fun neverAttemptedFetchesWhenTheCacheIsNotFresh() {
        assertTrue(shouldFetchWeather(force = false, cacheFresh = false, nowMs = now, lastAttemptMs = null))
    }

    @Test fun aRecentFailedAttemptIsNotRetriedEveryMinute() {
        assertFalse(shouldFetchWeather(force = false, cacheFresh = false, nowMs = now, lastAttemptMs = now - 60_000))
        assertFalse(shouldFetchWeather(force = false, cacheFresh = false, nowMs = now, lastAttemptMs = now - fiveMin + 1))
    }

    @Test fun retriesAfterFiveMinutes() {
        assertTrue(shouldFetchWeather(force = false, cacheFresh = false, nowMs = now, lastAttemptMs = now - fiveMin))
    }
}
