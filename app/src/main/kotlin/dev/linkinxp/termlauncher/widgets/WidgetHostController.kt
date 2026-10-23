package dev.linkinxp.termlauncher.widgets

import android.app.Activity
import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetHostView
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import kotlin.math.roundToInt

data class WidgetProviderInfo(val provider: String, val label: String, val minHeightDp: Int)

private const val HOST_ID = 0x7E57

/**
 * Thin wrapper over [AppWidgetHost]/[AppWidgetManager]. Every call tolerates a missing or broken provider and
 * returns false/null/empty instead of throwing.
 */
class WidgetHostController(context: Context) {
    private val app = context.applicationContext
    private val manager = AppWidgetManager.getInstance(app)
    private val host = AppWidgetHost(app, HOST_ID)

    /** Installed providers sorted by label, or null when the system query failed (never treat that as "none"). */
    fun providersOrNull(): List<WidgetProviderInfo>? = try {
        val density = app.resources.displayMetrics.density
        manager.installedProviders.map {
            WidgetProviderInfo(it.provider.flattenToString(), it.loadLabel(app.packageManager), (it.minHeight / density).roundToInt())
        }.sortedBy { it.label.lowercase() }
    } catch (_: Exception) {
        null
    }

    fun providers(): List<WidgetProviderInfo> = providersOrNull() ?: emptyList()

    private fun info(provider: String) = try {
        manager.installedProviders.firstOrNull { it.provider.flattenToString() == provider }
    } catch (_: Exception) {
        null
    }

    fun allocate(): Int = host.allocateAppWidgetId()

    fun delete(id: Int) {
        try { host.deleteAppWidgetId(id) } catch (_: Exception) {}
    }

    /** True when the widget was bound without asking the user; false when the system prompt is needed (or it failed). */
    fun bindIfAllowed(id: Int, provider: String): Boolean {
        val info = info(provider) ?: return false
        return try { manager.bindAppWidgetIdIfAllowed(id, info.provider) } catch (_: Exception) { false }
    }

    /** The system "allow this launcher to use the widget" prompt. */
    fun bindIntent(id: Int, provider: String): Intent =
        Intent(AppWidgetManager.ACTION_APPWIDGET_BIND)
            .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id)
            .putExtra(AppWidgetManager.EXTRA_APPWIDGET_PROVIDER, ComponentName.unflattenFromString(provider))

    fun needsConfigure(provider: String): Boolean = info(provider)?.configure != null

    /** Starts the widget's own setup screen; the result arrives in the activity's onActivityResult. */
    fun configure(activity: Activity, id: Int, requestCode: Int): Boolean = try {
        host.startAppWidgetConfigureActivityForResult(activity, id, 0, requestCode, null)
        true
    } catch (_: Exception) {
        false
    }

    fun createView(id: Int): AppWidgetHostView? = try {
        manager.getAppWidgetInfo(id)?.let { host.createView(app, id, it) }
    } catch (_: Exception) {
        null
    }

    fun start() {
        try { host.startListening() } catch (_: Exception) {}
    }

    fun stop() {
        try { host.stopListening() } catch (_: Exception) {}
    }
}
