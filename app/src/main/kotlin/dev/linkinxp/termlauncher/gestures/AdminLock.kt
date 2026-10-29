package dev.linkinxp.termlauncher.gestures

import android.app.admin.DeviceAdminReceiver
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent

/** Receives the device-admin callbacks; the only policy it asks for is "lock the screen" (res/xml/device_admin.xml). */
class LockAdminReceiver : DeviceAdminReceiver()

/**
 * Locks the screen by making Terminal a device administrator with the single "force lock" policy: the user switches it on
 * once (Android shows what it allows). While it is on the app cannot be uninstalled until it is switched off here or in
 * Settings > Security > Device admin apps.
 */
object AdminLock {
    private fun component(context: Context) = ComponentName(context, LockAdminReceiver::class.java)
    private fun policy(context: Context) = context.getSystemService(DevicePolicyManager::class.java)

    fun isActive(context: Context): Boolean = try { policy(context)?.isAdminActive(component(context)) == true } catch (_: Exception) { false }

    /** True when the screen was locked; false when the admin is not switched on. */
    fun lock(context: Context): Boolean {
        if (!isActive(context)) return false
        return try { policy(context)?.lockNow(); true } catch (_: Exception) { false }
    }

    fun requestIntent(context: Context): Intent =
        Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN)
            .putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, component(context))
            .putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, "Lets Terminal lock the screen when you double-tap the home screen. It cannot read or change anything else.")

    fun remove(context: Context) {
        try { policy(context)?.removeActiveAdmin(component(context)) } catch (_: Exception) {}
    }
}
