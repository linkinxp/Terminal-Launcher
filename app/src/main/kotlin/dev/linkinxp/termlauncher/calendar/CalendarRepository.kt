package dev.linkinxp.termlauncher.calendar

import android.Manifest
import android.content.ContentUris
import android.content.Context
import android.content.pm.PackageManager
import android.provider.CalendarContract
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Reads upcoming calendar instances (needs the READ_CALENDAR permission). */
class CalendarRepository(private val context: Context) {

    fun hasPermission(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALENDAR) == PackageManager.PERMISSION_GRANTED

    /** Events of visible calendars overlapping `[fromMs, toMs]`, soonest first; any failure gives an empty list. */
    suspend fun events(fromMs: Long, toMs: Long): List<AgendaEvent> = withContext(Dispatchers.IO) {
        try {
            val uri = CalendarContract.Instances.CONTENT_URI.buildUpon()
                .also { ContentUris.appendId(it, fromMs); ContentUris.appendId(it, toMs) }
                .build()
            val projection = arrayOf(
                CalendarContract.Instances.TITLE,
                CalendarContract.Instances.BEGIN,
                CalendarContract.Instances.END,
                CalendarContract.Instances.ALL_DAY,
            )
            val out = ArrayList<AgendaEvent>()
            context.contentResolver.query(
                uri, projection, "${CalendarContract.Instances.VISIBLE}=1", null, "${CalendarContract.Instances.BEGIN} ASC",
            )?.use { c ->
                while (c.moveToNext() && out.size < MAX_ROWS) {
                    out += AgendaEvent(c.getString(0) ?: "", c.getLong(1), c.getLong(2), c.getInt(3) == 1)
                }
            }
            out
        } catch (_: Exception) {
            emptyList()
        }
    }

    private companion object {
        const val MAX_ROWS = 50
    }
}
