package dev.linkinxp.termlauncher.prompt

/** Keeps only the last [max] lines of [text]; blank text becomes empty. */
fun capLines(text: String, max: Int = 12): String {
    if (text.isBlank()) return ""
    return text.trimEnd('\n').lines().takeLast(max).joinToString("\n")
}
