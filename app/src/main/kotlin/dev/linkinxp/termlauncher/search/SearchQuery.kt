package dev.linkinxp.termlauncher.search

import java.net.URLEncoder

private const val MAX_QUERY = 500

/** The query trimmed, with every whitespace run (newlines too) as one space, cut to 500 characters; null if blank. */
fun cleanQuery(raw: String): String? {
    val cut = raw.trim().replace(Regex("\\s+"), " ").take(MAX_QUERY)
    val safe = if (cut.isNotEmpty() && Character.isHighSurrogate(cut.last())) cut.dropLast(1) else cut
    return safe.ifEmpty { null }
}

/** `https://www.google.com/search?q=<query>` with the cleaned query URL-encoded as UTF-8; null for a blank query. */
fun searchUri(raw: String): String? =
    cleanQuery(raw)?.let { "https://www.google.com/search?q=" + URLEncoder.encode(it, "UTF-8") }
