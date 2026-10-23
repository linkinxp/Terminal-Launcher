package dev.linkinxp.termlauncher.weather

private const val RETRY_MS = 5 * 60_000L

/**
 * Whether the weather should be fetched now: always when [force]d, never while the cache is fresh, otherwise
 * at most once per 5 minutes so an offline phone does not retry on every minute tick.
 */
fun shouldFetchWeather(force: Boolean, cacheFresh: Boolean, nowMs: Long, lastAttemptMs: Long?): Boolean = when {
    force -> true
    cacheFresh -> false
    lastAttemptMs == null -> true
    else -> nowMs - lastAttemptMs >= RETRY_MS
}
