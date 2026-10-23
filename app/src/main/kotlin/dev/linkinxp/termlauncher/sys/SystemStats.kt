package dev.linkinxp.termlauncher.sys

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.BatteryManager
import android.os.Environment
import android.os.StatFs
import android.os.SystemClock

data class SysSnapshot(
    val batteryPct: Int?,
    val charging: Boolean,
    val ramPct: Int?,
    val diskPct: Int?,
    val uptimeMs: Long,
    val net: String,
)

private inline fun <T> orNull(block: () -> T?): T? = try { block() } catch (_: Exception) { null }

/** Reads every source independently; one that fails gives null (or `?` for the network), never throws. */
fun readSystemStats(context: Context): SysSnapshot {
    val battery = orNull { context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED)) }
    val level = battery?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
    val scale = battery?.getIntExtra(BatteryManager.EXTRA_SCALE, 100) ?: 100
    val status = battery?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
    val ram = orNull {
        val info = ActivityManager.MemoryInfo()
        context.getSystemService(ActivityManager::class.java).getMemoryInfo(info)
        if (info.totalMem <= 0) null else ((info.totalMem - info.availMem) * 100 / info.totalMem).toInt()
    }
    val disk = orNull {
        val fs = StatFs(Environment.getDataDirectory().path)
        if (fs.blockCountLong <= 0) null else ((fs.blockCountLong - fs.availableBlocksLong) * 100 / fs.blockCountLong).toInt()
    }
    val net = orNull {
        val cm = context.getSystemService(ConnectivityManager::class.java)
        val caps = cm.activeNetwork?.let { cm.getNetworkCapabilities(it) }
        when {
            caps == null -> "offline"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "wifi"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "cell"
            else -> "other"
        }
    } ?: "?"
    return SysSnapshot(
        batteryPct = if (level < 0 || scale <= 0) null else level * 100 / scale,
        charging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL,
        ramPct = ram,
        diskPct = disk,
        uptimeMs = SystemClock.elapsedRealtime(),
        net = net,
    )
}
