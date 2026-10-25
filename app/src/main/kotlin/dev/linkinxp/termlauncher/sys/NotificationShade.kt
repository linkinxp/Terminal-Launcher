package dev.linkinxp.termlauncher.sys

import android.content.Context

/**
 * Pulls down the system notification shade (what a swipe from the top edge does). There is no public API for it:
 * this calls StatusBarManager.expandNotificationsPanel, which the EXPAND_STATUS_BAR permission allows. Returns false
 * when the phone refuses.
 */
fun expandNotificationShade(context: Context): Boolean = callStatusBar(context, "expandNotificationsPanel")

/** Pulls down the quick settings panel (the right-hand swipe), the same hidden call on the same permission. */
fun expandQuickSettings(context: Context): Boolean = callStatusBar(context, "expandSettingsPanel")

private fun callStatusBar(context: Context, method: String): Boolean = try {
    val manager = context.getSystemService("statusbar")
    manager?.javaClass?.getMethod(method)?.invoke(manager)
    manager != null
} catch (_: Exception) {
    false
}
