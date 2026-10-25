package dev.linkinxp.termlauncher.gestures

import android.accessibilityservice.AccessibilityService
import android.content.ComponentName
import android.content.Context
import android.provider.Settings
import android.view.accessibility.AccessibilityEvent

/** What this build includes. The store build leaves out the accessibility service (Google Play restricts that API to accessibility tools). */
object Features {
    @Volatile var lockService: Boolean = dev.linkinxp.termlauncher.BuildConfig.LOCK_SERVICE
}

/** What a swipe down on the home screen opens: the notifications from the left half, the quick settings from the right. */
enum class SwipeDownAction { NOTIFICATIONS, QUICK_SETTINGS }

fun swipeDownAction(downX: Float, width: Float): SwipeDownAction =
    if (width > 0f && downX >= width / 2f) SwipeDownAction.QUICK_SETTINGS else SwipeDownAction.NOTIFICATIONS

/**
 * Locks the screen through an accessibility service (Android gives apps no other way short of device admin): the user
 * switches the "Terminal: lock screen" service on once in the accessibility settings.
 */
class LockAccessibilityService : AccessibilityService() {
    override fun onServiceConnected() { ScreenLock.service = this }
    override fun onUnbind(intent: android.content.Intent?): Boolean { if (ScreenLock.service === this) ScreenLock.service = null; return super.onUnbind(intent) }
    override fun onAccessibilityEvent(event: AccessibilityEvent?) = Unit
    override fun onInterrupt() = Unit
}

object ScreenLock {
    @Volatile var service: LockAccessibilityService? = null

    /** True when the screen was locked; false when the service is not switched on. */
    fun lock(): Boolean = service?.performGlobalAction(AccessibilityService.GLOBAL_ACTION_LOCK_SCREEN) ?: false

    /** Whether the user has switched the service on (checked from the system setting, so it is right after a restart too). */
    fun isEnabled(context: Context): Boolean {
        val me = ComponentName(context, LockAccessibilityService::class.java).flattenToString()
        val enabled = Settings.Secure.getString(context.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES).orEmpty()
        return enabled.split(':').any { it.equals(me, ignoreCase = true) }
    }
}
