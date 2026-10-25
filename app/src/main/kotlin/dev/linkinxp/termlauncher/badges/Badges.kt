package dev.linkinxp.termlauncher.badges

import android.content.ComponentName
import android.content.Context
import android.provider.Settings
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

private const val PUBLISH_DELAY_MS = 300L

/** What a badge needs to know about one notification. */
data class NotifInfo(val pkg: String, val ongoing: Boolean, val groupSummary: Boolean)

/** Notifications per package, leaving out ongoing ones (music, navigation) and group summaries (they repeat their children). */
fun countBadges(list: List<NotifInfo>, ownPackage: String? = null): Map<String, Int> =
    list.filter { !it.ongoing && !it.groupSummary && it.pkg != ownPackage }.groupingBy { it.pkg }.eachCount()

/** The badge text: the count, or `9+`. */
fun badgeLabel(count: Int): String = when {
    count <= 0 -> ""
    count > 9 -> "9+"
    else -> count.toString()
}

/** A folder's badge is the sum over the apps inside it. */
fun folderBadge(pkgs: List<String>, counts: Map<String, Int>): Int = pkgs.sumOf { counts[it] ?: 0 }

/** The latest per-package counts, shared by the listener service and the icons. */
object BadgeStore {
    private val _counts = MutableStateFlow<Map<String, Int>>(emptyMap())
    val counts: StateFlow<Map<String, Int>> = _counts.asStateFlow()
    fun publish(counts: Map<String, Int>) { _counts.value = counts }
}

/**
 * Feeds [BadgeStore] from the notification list. Android only runs it after the user grants notification access to
 * Terminal; it never reads notification text, only which app each notification belongs to.
 */
class BadgeListenerService : NotificationListenerService() {
    /** The thread of the last refresh and how many there were (read by the tests: they must be few and off the main thread). */
    @Volatile var publishedOn: Thread? = null
    @Volatile var publishCount = 0

    private fun publish() {
        publishedOn = Thread.currentThread(); publishCount++
        val infos = try {
            activeNotifications.orEmpty().map { it.toInfo() }
        } catch (_: Exception) {
            emptyList()
        }
        BadgeStore.publish(countBadges(infos, ownPackage = packageName))
    }

    private var media: dev.linkinxp.termlauncher.media.MediaWatcher? = null

    // Reading the active notifications copies all of them: do it on a background thread, at most a few times a second.
    private var thread: android.os.HandlerThread? = null

    /** The background looper the refreshes run on (the tests advance its clock). */
    val workLooper: android.os.Looper? get() = thread?.looper
    private var handler: android.os.Handler? = null
    private val publishTask = Runnable { publish() }

    override fun onCreate() {
        super.onCreate()
        thread = android.os.HandlerThread("badges").also { it.start(); handler = android.os.Handler(it.looper) }
    }

    override fun onDestroy() {
        thread?.quitSafely(); thread = null; handler = null
        super.onDestroy()
    }

    private fun schedulePublish() {
        handler?.let { it.removeCallbacks(publishTask); it.postDelayed(publishTask, PUBLISH_DELAY_MS) }
    }

    override fun onListenerConnected() {
        media?.stop()
        handler?.post(publishTask)
        media = dev.linkinxp.termlauncher.media.MediaWatcher(this, ComponentName(this, BadgeListenerService::class.java)).also { it.start() }
    }

    override fun onListenerDisconnected() {
        BadgeStore.publish(emptyMap()); media?.stop(); media = null
    }
    // An ongoing notification (a download's progress, music) never changes a count: skip its updates.
    override fun onNotificationPosted(sbn: StatusBarNotification?) { if (sbn?.isOngoing != true) schedulePublish() }
    override fun onNotificationRemoved(sbn: StatusBarNotification?) = schedulePublish()

    private fun StatusBarNotification.toInfo() = NotifInfo(
        packageName,
        ongoing = isOngoing,
        groupSummary = notification.flags and android.app.Notification.FLAG_GROUP_SUMMARY != 0,
    )
}

/** Whether the user has granted Terminal notification access (read from the system setting). */
fun isBadgeAccessGranted(context: Context): Boolean {
    val me = ComponentName(context, BadgeListenerService::class.java).flattenToString()
    val enabled = Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners").orEmpty()
    return enabled.split(':').any { it.equals(me, ignoreCase = true) }
}
