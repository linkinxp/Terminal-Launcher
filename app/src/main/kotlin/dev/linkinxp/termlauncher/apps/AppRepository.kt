package dev.linkinxp.termlauncher.apps

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.LauncherApps
import android.net.Uri
import android.os.Process
import android.os.UserHandle
import android.os.UserManager
import android.provider.Settings
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/**
 * Launchable apps of every profile of this user (work profile, suspended apps included) via
 * [LauncherApps], refreshed on package changes and on [refresh]. Excludes this app.
 */
class AppRepository(private val context: Context) {
    private val launcherApps = context.getSystemService(LauncherApps::class.java)
    private val userManager = context.getSystemService(UserManager::class.java)
    private val refreshSignal = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    @Volatile private var owners: Map<String, UserHandle> = emptyMap()

    // Broadcasts only signal; the (slow) scan runs on Dispatchers.Default, conflated so bursts collapse.
    val apps: Flow<List<AppEntry>> = callbackFlow<Unit> {
        trySend(Unit)
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(c: Context, i: Intent) { trySend(Unit) }
        }
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_PACKAGE_ADDED)
            addAction(Intent.ACTION_PACKAGE_REMOVED)
            addAction(Intent.ACTION_PACKAGE_CHANGED)
            addAction(Intent.ACTION_PACKAGE_REPLACED)
            addDataScheme("package")
        }
        ContextCompat.registerReceiver(context, receiver, filter, ContextCompat.RECEIVER_EXPORTED)
        // The profile broadcasts carry no data scheme, so they need a second filter.
        val profileFilter = IntentFilter().apply {
            addAction(Intent.ACTION_MANAGED_PROFILE_ADDED)
            addAction(Intent.ACTION_MANAGED_PROFILE_REMOVED)
            addAction(Intent.ACTION_MANAGED_PROFILE_AVAILABLE)
            addAction(Intent.ACTION_MANAGED_PROFILE_UNAVAILABLE)
        }
        ContextCompat.registerReceiver(context, receiver, profileFilter, ContextCompat.RECEIVER_EXPORTED)
        val job = launch { refreshSignal.collect { trySend(Unit) } }
        awaitClose { job.cancel(); context.unregisterReceiver(receiver) }
    }.conflate().map { query() }.flowOn(Dispatchers.Default)

    /** Rescan now, e.g. after a launch failed because an app disappeared. */
    fun refresh() { refreshSignal.tryEmit(Unit) }

    private fun query(): List<AppEntry> {
        val found = LinkedHashMap<String, AppEntry>()
        val handles = HashMap<String, UserHandle>()
        // userProfiles lists the current user first, so a package present in both profiles keeps the personal one.
        for (user in userManager.userProfiles) {
            for (info in launcherApps.getActivityList(null, user)) {
                val pkg = info.applicationInfo.packageName
                if (pkg == context.packageName || pkg in found) continue
                found[pkg] = AppEntry(pkg, info.label.toString(), info.firstInstallTime)
                handles[pkg] = user
            }
        }
        owners = handles
        return found.values.toList()
    }

    /** False when the app has no launch activity or can no longer be started. */
    fun launch(pkg: String): Boolean {
        val user = owners[pkg] ?: Process.myUserHandle()
        try {
            val info = launcherApps.getActivityList(pkg, user).firstOrNull() ?: return false
            launcherApps.startMainActivity(info.componentName, user, null, null)
            return true
        } catch (e: Exception) {
            // Not the default home (or profile unavailable): fall back to the plain launch intent.
            val intent = context.packageManager.getLaunchIntentForPackage(pkg) ?: return false
            return try {
                context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)); true
            } catch (e2: Exception) { false }
        }
    }

    fun uninstall(pkg: String) = startSafely(Intent(Intent.ACTION_DELETE, Uri.parse("package:$pkg")))

    fun appInfo(pkg: String) = startSafely(
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$pkg")),
    )

    private fun startSafely(intent: Intent) {
        try { context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) } catch (_: Exception) {}
    }
}
