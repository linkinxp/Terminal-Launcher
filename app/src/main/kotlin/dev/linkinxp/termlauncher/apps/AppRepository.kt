package dev.linkinxp.termlauncher.apps

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.provider.Settings
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.Dispatchers

/** Launchable apps from PackageManager, refreshed on package add/remove/change. Excludes this app. */
class AppRepository(private val context: Context) {

    val apps: Flow<List<AppEntry>> = callbackFlow {
        trySend(query())
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(c: Context, i: Intent) { trySend(query()) }
        }
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_PACKAGE_ADDED)
            addAction(Intent.ACTION_PACKAGE_REMOVED)
            addAction(Intent.ACTION_PACKAGE_CHANGED)
            addAction(Intent.ACTION_PACKAGE_REPLACED)
            addDataScheme("package")
        }
        context.registerReceiver(receiver, filter)
        awaitClose { context.unregisterReceiver(receiver) }
    }.flowOn(Dispatchers.Default)

    @Suppress("DEPRECATION")
    private fun query(): List<AppEntry> {
        val pm = context.packageManager
        val main = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        return pm.queryIntentActivities(main, 0)
            .map { it.activityInfo.packageName to it }
            .filter { (pkg, _) -> pkg != context.packageName }
            .distinctBy { it.first }
            .map { (pkg, ri) ->
                val installed = try { pm.getPackageInfo(pkg, 0).firstInstallTime } catch (e: Exception) { 0L }
                AppEntry(pkg, ri.loadLabel(pm).toString(), installed)
            }
    }

    /** False when the app has no launch intent or can no longer be started. */
    fun launch(pkg: String): Boolean {
        val intent = context.packageManager.getLaunchIntentForPackage(pkg) ?: return false
        return try {
            context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)); true
        } catch (e: Exception) { false }
    }

    fun uninstall(pkg: String) = startSafely(
        Intent(Intent.ACTION_DELETE, Uri.parse("package:$pkg")),
    )

    fun appInfo(pkg: String) = startSafely(
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$pkg")),
    )

    private fun startSafely(intent: Intent) {
        try { context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) } catch (_: Exception) {}
    }
}
