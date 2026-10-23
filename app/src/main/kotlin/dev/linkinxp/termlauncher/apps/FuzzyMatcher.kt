package dev.linkinxp.termlauncher.apps

/**
 * Indices of [labels] matching [query], best first. Tiers: prefix (0), word-start (1: a word starts
 * with the query, or the query is the start of the labels' initials), subsequence (2). Ties keep
 * original order. A blank query returns every index in order.
 */
fun fuzzyRank(query: String, labels: List<String>): List<Int> {
    val q = query.trim().lowercase()
    if (q.isEmpty()) return labels.indices.toList()
    return labels.indices
        .mapNotNull { i -> tier(q, labels[i].lowercase())?.let { i to it } }
        .sortedBy { it.second } // stable
        .map { it.first }
}

private fun tier(q: String, label: String): Int? {
    if (label.startsWith(q)) return 0
    val words = label.split(' ', '-', '_', '.').filter { it.isNotEmpty() }
    if (words.any { it.startsWith(q) } || words.map { it.first() }.joinToString("").startsWith(q)) return 1
    var from = 0
    for (c in q) {
        from = label.indexOf(c, from) + 1
        if (from == 0) return null
    }
    return 2
}
