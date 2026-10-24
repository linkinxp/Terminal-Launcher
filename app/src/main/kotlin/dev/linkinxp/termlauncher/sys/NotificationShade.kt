package dev.linkinxp.termlauncher.sys

import android.content.Context

/**
 * Pulls down the system notification shade (what a swipe from the top edge does). There is no public API for it:
 * this calls StatusBarManager.expandNotificationsPanel, which the EXPAND_STATUS_BAR permission allows. Returns false
 * when the phone refuses.
 */
fun expandNotificationShade(context: Context): Boolean = try {
    val manager = context.getSystemService("statusbar")
    manager?.javaClass?.getMethod("expandNotificationsPanel")?.invoke(manager)
    manager != null
} catch (_: Exception) {
    false
}
