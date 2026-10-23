package dev.linkinxp.termlauncher.wall

import kotlin.math.sin
import kotlin.random.Random

enum class AsciiStyle { GLYPH, CIRCUIT, CONTOUR, STARS, HEXDUMP, SKYLINE }

private const val GLYPHS = "01{}<>/\\|=+*#\$%&:;"
private const val BANDS = " .:-=+*#%"

/**
 * Seeded ASCII art: exactly [rows] lines of exactly [cols] printable ASCII characters (nothing for
 * non-positive sizes). The same style, size and seed always give the same picture.
 */
fun generateAscii(style: AsciiStyle, cols: Int, rows: Int, seed: Int): List<String> {
    val r = rows.coerceAtLeast(0)
    val c = cols.coerceAtLeast(0)
    val grid = Array(r) { CharArray(c) { ' ' } }
    val rnd = Random(seed * 31 + style.ordinal)
    if (r > 0 && c > 0) when (style) {
        AsciiStyle.GLYPH -> glyph(grid, rnd)
        AsciiStyle.CIRCUIT -> circuit(grid, rnd)
        AsciiStyle.CONTOUR -> contour(grid, rnd)
        AsciiStyle.STARS -> stars(grid, rnd)
        AsciiStyle.HEXDUMP -> hexdump(grid, rnd)
        AsciiStyle.SKYLINE -> skyline(grid, rnd)
    }
    return grid.map { String(it) }
}

/** Rotates the rows up by [step] (wrapping; negative steps rotate down). */
fun driftLines(lines: List<String>, step: Int): List<String> {
    if (lines.isEmpty()) return lines
    val s = Math.floorMod(step, lines.size)
    return lines.drop(s) + lines.take(s)
}

private fun glyph(g: Array<CharArray>, rnd: Random) {
    for (row in g) for (x in row.indices) if (rnd.nextFloat() >= 0.15f) row[x] = GLYPHS[rnd.nextInt(GLYPHS.length)]
}

private fun stars(g: Array<CharArray>, rnd: Random) {
    val kinds = ".+*'"
    for (row in g) for (x in row.indices) if (rnd.nextFloat() < 0.04f) row[x] = kinds[rnd.nextInt(kinds.length)]
}

private fun circuit(g: Array<CharArray>, rnd: Random) {
    val h = g.size
    val w = g[0].size
    fun put(y: Int, x: Int, ch: Char) {
        if (y !in 0 until h || x !in 0 until w) return
        val cur = g[y][x]
        g[y][x] = if (cur == ' ' || cur == ch) ch else if (cur == 'o' || cur == '#') cur else '+'
    }
    repeat(h * w / 60 + 1) {
        val horizontal = rnd.nextBoolean()
        var y = rnd.nextInt(h)
        var x = rnd.nextInt(w)
        put(y, x, 'o')
        repeat(4 + rnd.nextInt(17)) {
            if (horizontal) x++ else y++
            put(y, x, if (horizontal) '-' else '|')
        }
        put(y, x, if (rnd.nextBoolean()) 'o' else '#')
    }
}

private fun contour(g: Array<CharArray>, rnd: Random) {
    val p1 = rnd.nextDouble() * 6.28
    val p2 = rnd.nextDouble() * 6.28
    val p3 = rnd.nextDouble() * 6.28
    val fx = 0.08 + rnd.nextDouble() * 0.08
    val fy = 0.12 + rnd.nextDouble() * 0.10
    for (y in g.indices) for (x in g[y].indices) {
        val height = sin(x * fx + p1) + sin(y * fy + p2) + sin((x + y) * 0.07 + p3)
        val band = ((height + 3.0) / 6.0 * (BANDS.length - 1)).toInt().coerceIn(0, BANDS.length - 1)
        g[y][x] = BANDS[band]
    }
}

private fun hexdump(g: Array<CharArray>, rnd: Random) {
    for (row in g) {
        val sb = StringBuilder("%04x: ".format(rnd.nextInt(0x10000)))
        repeat(8) { sb.append("%02x ".format(rnd.nextInt(256))) }
        sb.append(' ')
        repeat(8) { sb.append((0x21 + rnd.nextInt(0x7E - 0x21 + 1)).toChar()) }
        val line = sb.toString().padEnd(row.size).take(row.size)
        line.forEachIndexed { i, ch -> row[i] = ch }
    }
}

private fun skyline(g: Array<CharArray>, rnd: Random) {
    val h = g.size
    val w = g[0].size
    var x = 0
    while (x < w) {
        val bw = 3 + rnd.nextInt(6)
        val bh = minOf(h, 1 + rnd.nextInt((h * 7 / 10).coerceAtLeast(1)))
        for (col in x until minOf(x + bw, w)) {
            for (row in h - bh until h) {
                val window = (col - x) % 2 == 1 && (h - row) % 2 == 0 && row < h - 1
                g[row][col] = if (window) '.' else '#'
            }
        }
        x += bw
    }
}
