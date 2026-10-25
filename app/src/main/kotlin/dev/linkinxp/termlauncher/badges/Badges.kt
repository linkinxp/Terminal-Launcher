package dev.linkinxp.termlauncher.badges

import android.content.ComponentName
import android.content.Context
import android.provider.Settings
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

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
    private fun publish() {
        val infos = try {
            activeNotifications.orEmpty().map { it.toInfo() }
        } catch (_: Exception) {
            emptyList()
        }
        BadgeStore.publish(countBadges(infos, ownPackage = packageName))
    }

    private var media: dev.linkinxp.termlauncher.media.MediaWatcher? = null

    override fun onListenerConnected() {
        publish()
        media = dev.linkinxp.termlauncher.media.MediaWatcher(this, ComponentName(this, BadgeListenerService::class.java)).also { it.start() }
    }

    override fun onListenerDisconnected() {
        BadgeStore.publish(emptyMap()); media?.stop(); media = null
    }
    override fun onNotificationPosted(sbn: StatusBarNotification?) = publish()
    override fun onNotificationRemoved(sbn: StatusBarNotification?) = publish()

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
