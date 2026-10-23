package dev.linkinxp.termlauncher.prompt

/**
 * Tab-completion: the longest common prefix (candidate casing) of every candidate that starts with
 * [input], or [input] unchanged when nothing matches or the prefix would not be any longer.
 */
fun complete(input: String, candidates: List<String>): String {
    if (input.isBlank()) return input
    val typed = input.trimStart()
    val matches = candidates.filter { it.startsWith(typed, ignoreCase = true) }
    if (matches.isEmpty()) return input
    val common = matches.drop(1).fold(matches.first()) { acc, m -> acc.commonPrefixWith(m, ignoreCase = true) }
    return if (common.length > typed.length) common else input
}
