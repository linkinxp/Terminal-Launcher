package dev.linkinxp.termlauncher.apps

private const val MAX_SHORT = 5
private const val VOWELS = "aeiou"

/** Dock-style abbreviation: first word, vowels dropped after the first letter, repeats collapsed, max 5 chars. */
fun shortName(label: String): String {
    val word = label.trim().substringBefore(' ').lowercase()
    if (word.length <= 3) return word
    val body = StringBuilder().append(word.first())
    for (c in word.drop(1)) {
        if (c in VOWELS || c == body.last()) continue
        body.append(c)
    }
    return body.take(MAX_SHORT).toString()
}
