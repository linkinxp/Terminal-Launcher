package dev.linkinxp.termlauncher.search

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.ContactsContract
import android.provider.Settings
import androidx.core.content.ContextCompat

enum class ExtraKind { CONTACT, SETTING, WEB, ASK_CONTACTS }

/** One non-app answer under the drawer's app results; [target] is a contact URI, a settings action or the web query. */
data class ExtraResult(val kind: ExtraKind, val label: String, val target: String) {
    /** A list key that stays unique even for two contacts with the same name. */
    fun listKey(): String = "x-$kind-$label-$target"
}

private data class SettingsPage(val label: String, val action: String, val keywords: List<String>)

private val PAGES = listOf(
    SettingsPage("wifi settings", Settings.ACTION_WIFI_SETTINGS, listOf("wifi", "wi-fi", "internet", "network")),
    SettingsPage("bluetooth settings", Settings.ACTION_BLUETOOTH_SETTINGS, listOf("bluetooth", "pair")),
    SettingsPage("display settings", Settings.ACTION_DISPLAY_SETTINGS, listOf("display", "dark", "brightness", "screen", "font")),
    SettingsPage("sound settings", Settings.ACTION_SOUND_SETTINGS, listOf("sound", "volume", "ringtone", "vibrate")),
    SettingsPage("battery saver", Settings.ACTION_BATTERY_SAVER_SETTINGS, listOf("battery", "power")),
    SettingsPage("apps settings", Settings.ACTION_MANAGE_APPLICATIONS_SETTINGS, listOf("apps", "applications", "permissions")),
    SettingsPage("default apps", Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS, listOf("default", "home app", "browser")),
    SettingsPage("location settings", Settings.ACTION_LOCATION_SOURCE_SETTINGS, listOf("location", "gps")),
    SettingsPage("accessibility settings", Settings.ACTION_ACCESSIBILITY_SETTINGS, listOf("accessibility")),
    SettingsPage("storage settings", Settings.ACTION_INTERNAL_STORAGE_SETTINGS, listOf("storage", "space", "files")),
    SettingsPage("date and time", Settings.ACTION_DATE_SETTINGS, listOf("date", "time", "timezone", "clock")),
    SettingsPage("language settings", Settings.ACTION_LOCALE_SETTINGS, listOf("language", "locale", "keyboard")),
    SettingsPage("airplane mode", Settings.ACTION_AIRPLANE_MODE_SETTINGS, listOf("airplane", "flight")),
    SettingsPage("nfc settings", Settings.ACTION_NFC_SETTINGS, listOf("nfc", "tap")),
    SettingsPage("security settings", Settings.ACTION_SECURITY_SETTINGS, listOf("security", "lock", "password", "fingerprint")),
    SettingsPage("data usage", Settings.ACTION_DATA_USAGE_SETTINGS, listOf("data", "usage", "mobile")),
    SettingsPage("about phone", Settings.ACTION_DEVICE_INFO_SETTINGS, listOf("about", "version", "device")),
)

private const val MAX_SETTINGS = 3
private const val MAX_CONTACTS = 4
private const val MIN_QUERY = 2

/** Settings screens whose name or keywords start with / contain the query (at most three). */
fun settingsMatches(query: String): List<ExtraResult> {
    val q = query.trim().lowercase()
    if (q.length < MIN_QUERY) return emptyList()
    return PAGES.filter { p -> p.label.contains(q) || p.keywords.any { it.startsWith(q) || (q.length >= 3 && it.contains(q)) } }
        .take(MAX_SETTINGS).map { ExtraResult(ExtraKind.SETTING, it.label, it.action) }
}

/** The "search the web" row, for any query of two or more characters. */
fun webResult(query: String): ExtraResult? {
    val q = query.trim()
    return if (q.length < MIN_QUERY) null else ExtraResult(ExtraKind.WEB, "search the web: $q", q)
}

/** Everything shown under the app results, in order: contacts (or the row asking for permission), settings, web. */
fun buildExtras(query: String, contacts: List<ExtraResult>, contactsAllowed: Boolean): List<ExtraResult> {
    if (query.trim().length < MIN_QUERY) return emptyList()
    val first = if (contactsAllowed) contacts.take(MAX_CONTACTS) else listOf(ExtraResult(ExtraKind.ASK_CONTACTS, "allow contacts to search them", ""))
    return first + settingsMatches(query) + listOfNotNull(webResult(query))
}

data class ContactRow(val id: Long, val lookupKey: String, val name: String)

/** Contact rows to results; a row without a lookup key cannot be opened and is dropped. */
fun contactResults(rows: List<ContactRow>): List<ExtraResult> = rows.filter { it.lookupKey.isNotBlank() && it.name.isNotBlank() }.map {
    // the same URI ContactsContract.Contacts.getLookupUri builds
    ExtraResult(ExtraKind.CONTACT, it.name, "content://com.android.contacts/contacts/lookup/${java.net.URLEncoder.encode(it.lookupKey, "UTF-8").replace("+", "%20")}/${it.id}")
}

class ContactsRepository(private val context: Context) {
    fun hasPermission() = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED

    fun search(query: String): List<ExtraResult> {
        if (!hasPermission() || query.trim().length < MIN_QUERY) return emptyList()
        return try {
            val uri = Uri.withAppendedPath(ContactsContract.Contacts.CONTENT_FILTER_URI, Uri.encode(query.trim()))
            context.contentResolver.query(
                uri, arrayOf(ContactsContract.Contacts._ID, ContactsContract.Contacts.LOOKUP_KEY, ContactsContract.Contacts.DISPLAY_NAME_PRIMARY),
                null, null, "${ContactsContract.Contacts.DISPLAY_NAME_PRIMARY} COLLATE NOCASE ASC",
            )?.use { c ->
                val rows = ArrayList<ContactRow>()
                while (c.moveToNext() && rows.size < MAX_CONTACTS) rows += ContactRow(c.getLong(0), c.getString(1).orEmpty(), c.getString(2).orEmpty())
                contactResults(rows)
            }.orEmpty()
        } catch (_: Exception) {
            emptyList()
        }
    }
}
