package dev.linkinxp.termlauncher.prompt

/** Last submitted prompt lines: no blanks, no consecutive duplicates, capped at [max] (oldest dropped). */
class CommandHistory(private val max: Int = 50) {
    private val items = ArrayDeque<String>()

    fun add(line: String) {
        val l = line.trim()
        if (l.isEmpty() || items.lastOrNull() == l) return
        items.addLast(l)
        while (items.size > max) items.removeFirst()
    }

    /** Oldest first. */
    fun all(): List<String> = items.toList()

    /** Newest first. */
    fun last(n: Int): List<String> = items.toList().asReversed().take(n)

    fun encode(): String = items.joinToString("\n")

    companion object {
        fun decode(raw: String?): CommandHistory = CommandHistory().also { h -> raw?.lineSequence()?.forEach(h::add) }
    }
}
