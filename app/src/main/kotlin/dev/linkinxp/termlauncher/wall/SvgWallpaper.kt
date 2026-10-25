package dev.linkinxp.termlauncher.wall

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/** The built-in vector wallpapers. */
enum class SvgStyle {
    CIRCUIT, HEXNET, RADAR, TUNNEL, GLITCH, WINDOWS, HELIX, TOPO;

    companion object {
        fun fromName(name: String?): SvgStyle = entries.firstOrNull { it.name.equals(name, ignoreCase = true) } ?: CIRCUIT
    }
}

/** ARGB colors an SVG wallpaper is drawn with: [dim] for the bulk of the art, [accent] and [foreground] for highlights. */
data class SvgColors(val background: Long, val foreground: Long, val accent: Long, val dim: Long)

private fun hex(argb: Long) = "#%06x".format(argb and 0xFFFFFF)
private fun f(v: Double) = "%.1f".format(java.util.Locale.ROOT, v)
private fun f(v: Int) = v.toString()

private const val MAX_ELEMENTS = 1_400

/**
 * A well-formed SVG document (basic shapes, paths, text, groups) for [style] on a [width] x [height] canvas. The same
 * style, seed, colors and size always give the same text; a non-positive size gives an empty 1x1 document.
 */
fun svgText(style: SvgStyle, seed: Int, colors: SvgColors, width: Int, height: Int): String {
    if (width <= 0 || height <= 0) return "<svg xmlns=\"http://www.w3.org/2000/svg\" viewBox=\"0 0 1 1\" width=\"1\" height=\"1\"><rect width=\"1\" height=\"1\" fill=\"${hex(colors.background)}\"/></svg>"
    val s = Svg(width, height, colors, Random(seed * 131 + style.ordinal * 7 + 1))
    when (style) {
        SvgStyle.CIRCUIT -> circuit(s)
        SvgStyle.HEXNET -> hexnet(s)
        SvgStyle.RADAR -> radar(s)
        SvgStyle.TUNNEL -> tunnel(s)
        SvgStyle.GLITCH -> glitch(s)
        SvgStyle.WINDOWS -> windows(s)
        SvgStyle.HELIX -> helix(s)
        SvgStyle.TOPO -> topo(s)
    }
    return s.finish()
}

/** A tiny SVG builder with an element budget so a wallpaper can never balloon. */
private class Svg(val w: Int, val h: Int, val c: SvgColors, val rnd: Random) {
    private val body = StringBuilder()
    private var count = 0
    val bg = hex(c.background); val fg = hex(c.foreground); val ac = hex(c.accent); val dim = hex(c.dim)
    fun raw(text: String) { if (count < MAX_ELEMENTS) { body.append(text).append('\n'); count++ } }
    fun finish(): String =
        "<svg xmlns=\"http://www.w3.org/2000/svg\" xmlns:xlink=\"http://www.w3.org/1999/xlink\" width=\"$w\" height=\"$h\" viewBox=\"0 0 $w $h\">\n" +
            "<rect width=\"$w\" height=\"$h\" fill=\"$bg\"/>\n$body</svg>"
    fun line(x1: Double, y1: Double, x2: Double, y2: Double, color: String, width: Double = 2.0, opacity: Double = 1.0) =
        raw("<line x1=\"${f(x1)}\" y1=\"${f(y1)}\" x2=\"${f(x2)}\" y2=\"${f(y2)}\" stroke=\"$color\" stroke-width=\"${f(width)}\" opacity=\"${f(opacity)}\"/>")
    fun circle(x: Double, y: Double, r: Double, fill: String? = null, stroke: String? = null, sw: Double = 2.0, opacity: Double = 1.0) =
        raw("<circle cx=\"${f(x)}\" cy=\"${f(y)}\" r=\"${f(r)}\" fill=\"${fill ?: "none"}\" stroke=\"${stroke ?: "none"}\" stroke-width=\"${f(sw)}\" opacity=\"${f(opacity)}\"/>")
    fun rect(x: Double, y: Double, rw: Double, rh: Double, fill: String? = null, stroke: String? = null, sw: Double = 2.0, opacity: Double = 1.0, extra: String = "") =
        raw("<rect x=\"${f(x)}\" y=\"${f(y)}\" width=\"${f(rw)}\" height=\"${f(rh)}\" fill=\"${fill ?: "none"}\" stroke=\"${stroke ?: "none"}\" stroke-width=\"${f(sw)}\" opacity=\"${f(opacity)}\" $extra/>")
    fun poly(points: List<Pair<Double, Double>>, color: String, width: Double = 2.0, opacity: Double = 1.0, close: Boolean = false, fill: String? = null) {
        val pts = points.joinToString(" ") { "${f(it.first)},${f(it.second)}" }
        raw("<${if (close) "polygon" else "polyline"} points=\"$pts\" fill=\"${fill ?: "none"}\" stroke=\"$color\" stroke-width=\"${f(width)}\" opacity=\"${f(opacity)}\"/>")
    }
    fun text(x: Double, y: Double, t: String, size: Double, color: String, opacity: Double = 1.0, anchor: String = "start") =
        raw("<text x=\"${f(x)}\" y=\"${f(y)}\" font-family=\"monospace\" font-size=\"${f(size)}\" fill=\"$color\" opacity=\"${f(opacity)}\" text-anchor=\"$anchor\">$t</text>")
}

// ---- CIRCUIT: octilinear traces, vias and a few labelled chips ----
private fun circuit(s: Svg) {
    val pitch = s.w / 18.0
    val cols = (s.w / pitch).toInt()
    val rows = (s.h / pitch).toInt()
    repeat(70) { i ->
        var x = s.rnd.nextInt(cols + 1) * pitch
        var y = s.rnd.nextInt(rows + 1) * pitch
        val pts = mutableListOf(x to y)
        repeat(3 + s.rnd.nextInt(4)) {
            val len = (1 + s.rnd.nextInt(4)) * pitch
            when (s.rnd.nextInt(4)) {
                0 -> x += len
                1 -> y += len
                2 -> { x += len * 0.7; y += len * 0.7 }
                else -> { x -= len * 0.7; y += len * 0.7 }
            }
            pts += x to y
        }
        s.poly(pts, if (i % 9 == 0) s.ac else s.dim, if (i % 9 == 0) 4.0 else 3.0, if (i % 9 == 0) 0.9 else 0.5)
        s.circle(pts.last().first, pts.last().second, 8.0, stroke = if (i % 9 == 0) s.ac else s.dim, sw = 3.0, opacity = 0.8)
        s.circle(pts.last().first, pts.last().second, 3.0, fill = s.fg, opacity = 0.7)
    }
    repeat(4) { i ->
        val cw = pitch * (3 + s.rnd.nextInt(2)); val chh = pitch * 2
        val cx = pitch * (1 + s.rnd.nextInt(cols - 4)); val cy = pitch * (2 + i * (rows / 4))
        s.rect(cx, cy, cw, chh, fill = s.bg, stroke = s.dim, sw = 4.0, opacity = 0.95)
        for (k in 1..5) { val px = cx + cw * k / 6; s.line(px, cy - 14, px, cy, s.dim, 3.0, 0.8); s.line(px, cy + chh, px, cy + chh + 14, s.dim, 3.0, 0.8) }
        s.text(cx + cw / 2, cy + chh / 2 + 8, listOf("CPU", "RAM", "GPU", "TPM", "NIC", "SoC")[s.rnd.nextInt(6)], pitch * 0.5, s.fg, 0.8, "middle")
    }
}

// ---- HEXNET: a hex grid with lit cells and links ----
private fun hexnet(s: Svg) {
    val r = s.w / 10.0
    val dx = r * 1.7320508; val dy = r * 1.5
    val lit = mutableListOf<Pair<Double, Double>>()
    var row = 0
    var y = -r
    while (y < s.h + r) {
        var x = if (row % 2 == 0) 0.0 else dx / 2
        while (x < s.w + r) {
            val pts = (0 until 6).map { k -> val a = PI / 180 * (60 * k - 30); (x + r * 0.94 * cos(a)) to (y + r * 0.94 * sin(a)) }
            val roll = s.rnd.nextInt(100)
            when {
                roll < 6 -> { s.poly(pts, s.ac, 3.0, 0.9, close = true, fill = s.ac); lit += x to y }
                roll < 20 -> { s.poly(pts, s.dim, 2.0, 0.6, close = true, fill = s.dim); lit += x to y }
                else -> s.poly(pts, s.dim, 2.0, 0.25, close = true)
            }
            x += dx
        }
        y += dy; row++
    }
    lit.shuffled(s.rnd).chunked(2).take(14).forEach { p -> if (p.size == 2) s.line(p[0].first, p[0].second, p[1].first, p[1].second, s.ac, 3.0, 0.5) }
    lit.take(24).forEach { s.circle(it.first, it.second, 5.0, fill = s.fg, opacity = 0.85) }
}

// ---- RADAR: rings, ticks, a sweep wedge and blips ----
private fun radar(s: Svg) {
    val cx = s.w / 2.0; val cy = s.h * 0.45; val step = s.w * 0.12
    for (k in 1..7) s.circle(cx, cy, step * k, stroke = s.dim, sw = 2.5, opacity = 0.55)
    s.line(cx - step * 7, cy, cx + step * 7, cy, s.dim, 2.0, 0.5); s.line(cx, cy - step * 7, cx, cy + step * 7, s.dim, 2.0, 0.5)
    for (d in 0 until 360 step 10) {
        val a = PI / 180 * d
        s.line(cx + step * 6.8 * cos(a), cy + step * 6.8 * sin(a), cx + step * 7 * cos(a), cy + step * 7 * sin(a), s.dim, 3.0, 0.8)
    }
    val start = s.rnd.nextDouble() * 2 * PI; val span = PI / 4
    val a1 = start; val a2 = start - span
    s.raw("<path d=\"M ${f(cx)} ${f(cy)} L ${f(cx + step * 7 * cos(a1))} ${f(cy + step * 7 * sin(a1))} A ${f(step * 7)} ${f(step * 7)} 0 0 0 ${f(cx + step * 7 * cos(a2))} ${f(cy + step * 7 * sin(a2))} Z\" fill=\"${s.ac}\" opacity=\"0.22\"/>")
    s.line(cx, cy, cx + step * 7 * cos(a1), cy + step * 7 * sin(a1), s.ac, 4.0, 0.9)
    repeat(8) { i ->
        val a = s.rnd.nextDouble() * 2 * PI; val rr = step * (1 + s.rnd.nextDouble() * 5.8)
        val bx = cx + rr * cos(a); val by = cy + rr * sin(a)
        s.circle(bx, by, 9.0, fill = s.ac, opacity = 0.8); s.circle(bx, by, 22.0, stroke = s.ac, sw = 2.0, opacity = 0.5)
        s.text(bx + 28, by + 6, "T-${100 + i * 7 + s.rnd.nextInt(6)}", s.w * 0.025, s.fg, 0.75)
    }
    s.circle(cx, cy, 8.0, fill = s.fg)
}

// ---- TUNNEL: nested rotated squares with bits along the edges ----
private fun tunnel(s: Svg) {
    val cx = s.w / 2.0; val cy = s.h / 2.0
    for (k in 0 until 26) {
        val side = s.w * (1.05 - k * 0.039)
        if (side <= 8) continue
        val angle = k * 2.6
        val op = 0.2 + 0.03 * k
        s.rect(cx - side / 2, cy - side / 2, side, side, stroke = if (k % 8 == 0) s.ac else s.dim, sw = 2.0 + k * 0.15, opacity = op.coerceAtMost(0.9), extra = "transform=\"rotate(${f(angle)} ${f(cx)} ${f(cy)})\"")
        val bits = (1..(8 + k)).joinToString("") { if (s.rnd.nextBoolean()) "1" else "0" }
        val size = (side * 0.03).coerceIn(10.0, 40.0)
        s.raw("<g transform=\"rotate(${f(angle)} ${f(cx)} ${f(cy)})\"><text x=\"${f(cx - side / 2 + 10)}\" y=\"${f(cy - side / 2 - 6)}\" font-family=\"monospace\" font-size=\"${f(size)}\" fill=\"${if (k % 4 == 0) s.fg else s.dim}\" opacity=\"${f(op)}\">$bits</text></g>")
    }
    s.circle(cx, cy, 6.0, fill = s.fg)
}

// ---- GLITCH: a geometric skull cut into shifted slices ----
private fun glitch(s: Svg) {
    val cx = s.w / 2.0; val cy = s.h * 0.42; val u = s.w / 10.0
    val sb = StringBuilder()
    sb.append("<defs><g id=\"skull\">")
    sb.append("<ellipse cx=\"${f(cx)}\" cy=\"${f(cy)}\" rx=\"${f(u * 3.2)}\" ry=\"${f(u * 3.4)}\" fill=\"${s.dim}\"/>")
    sb.append("<rect x=\"${f(cx - u * 2)}\" y=\"${f(cy + u * 2.2)}\" width=\"${f(u * 4)}\" height=\"${f(u * 2.2)}\" rx=\"${f(u * 0.5)}\" fill=\"${s.dim}\"/>")
    sb.append("<circle cx=\"${f(cx - u * 1.3)}\" cy=\"${f(cy - u * 0.2)}\" r=\"${f(u * 0.95)}\" fill=\"${s.bg}\"/>")
    sb.append("<circle cx=\"${f(cx + u * 1.3)}\" cy=\"${f(cy - u * 0.2)}\" r=\"${f(u * 0.95)}\" fill=\"${s.bg}\"/>")
    sb.append("<polygon points=\"${f(cx)},${f(cy + u * 0.5)} ${f(cx - u * 0.4)},${f(cy + u * 1.5)} ${f(cx + u * 0.4)},${f(cy + u * 1.5)}\" fill=\"${s.bg}\"/>")
    for (k in -2..2) sb.append("<rect x=\"${f(cx + k * u * 0.7 - u * 0.3)}\" y=\"${f(cy + u * 2.5)}\" width=\"${f(u * 0.6)}\" height=\"${f(u * 1.2)}\" fill=\"${s.bg}\"/>")
    sb.append("</g></defs>")
    s.raw(sb.toString())
    val top = cy - u * 4; val bottom = cy + u * 4.8
    val bands = 16; val bh = (bottom - top) / bands
    for (i in 0 until bands) {
        val shift = if (s.rnd.nextInt(2) == 0) (s.rnd.nextDouble() - 0.5) * u * 3.0 else 0.0
        val ghost = if (i % 2 == 0) u * 0.35 else 0.0
        s.raw("<clipPath id=\"b$i\"><rect x=\"0\" y=\"${f(top + i * bh)}\" width=\"${s.w}\" height=\"${f(bh + 1)}\"/></clipPath>")
        s.raw("<use xlink:href=\"#skull\" clip-path=\"url(#b$i)\" transform=\"translate(${f(shift)} 0)\" opacity=\"0.85\"/>")
        if (ghost > 0) s.raw("<use xlink:href=\"#skull\" clip-path=\"url(#b$i)\" transform=\"translate(${f(shift + ghost)} 0)\" fill=\"${s.ac}\" opacity=\"0.45\"/>")
    }
    s.text(cx, bottom + u * 1.2, "SYSTEM FAILURE", u * 0.55, s.fg, 0.9, "middle")
    s.text(cx, bottom + u * 1.9, "0xDEADBEEF", u * 0.4, s.ac, 0.8, "middle")
    repeat(10) { s.line(0.0, s.rnd.nextDouble() * s.h, s.w.toDouble(), s.rnd.nextDouble() * s.h, s.ac, 1.5, 0.12) }
}

// ---- WINDOWS: stacked terminal windows with fake sessions ----
private val SESSION = listOf(
    "\$ nmap -sV 10.0.0.%d", "PORT   STATE SERVICE", "22/tcp open  ssh", "80/tcp open  http", "\$ ssh root@10.0.0.%d",
    "Last login: Tue Oct 21", "# cat /etc/shadow", "\$ sudo apt update", "Hit:1 deb.debian.org", "\$ tail -f auth.log",
    "Accepted publickey for root", "\$ ./exploit --target %d", "[+] shell opened", "[*] escalating...", "uid=0(root) gid=0(root)",
)

private fun windows(s: Svg) {
    val count = 6
    for (i in 0 until count) {
        val ww = s.w * (0.62 + s.rnd.nextDouble() * 0.2); val wh = s.h * 0.2
        val x = s.rnd.nextDouble() * (s.w - ww); val y = s.h * 0.04 + i * s.h * 0.15
        val op = 0.35 + 0.1 * i
        s.rect(x, y, ww, wh, fill = s.bg) // opaque, so a window hides the ones behind it
        s.rect(x, y, ww, wh, stroke = if (i == count - 1) s.ac else s.dim, sw = 3.0, opacity = op.coerceAtMost(0.95))
        s.rect(x, y, ww, wh * 0.14, fill = s.dim, opacity = 0.35)
        listOf(s.ac, s.fg, s.dim).forEachIndexed { k, col -> s.circle(x + 22 + k * 24, y + wh * 0.07, 6.0, fill = col, opacity = 0.9) }
        s.text(x + ww / 2, y + wh * 0.1, "root@host-${s.rnd.nextInt(90) + 10}: ~", s.w * 0.022, s.fg, 0.7, "middle")
        val lineH = wh * 0.1
        for (k in 0 until 7) {
            val raw = SESSION[s.rnd.nextInt(SESSION.size)]
            val t = if (raw.contains("%d")) raw.format(s.rnd.nextInt(250)) else raw
            s.text(x + 18, y + wh * 0.22 + k * lineH, t.replace("&", "&amp;").replace("<", "&lt;"), s.w * 0.024, if (k % 5 == 0) s.ac else s.dim, op + 0.1)
        }
    }
}

// ---- HELIX: a DNA double helix with base-pair rungs ----
private fun helix(s: Svg) {
    val cx = s.w / 2.0; val amp = s.w * 0.28; val period = s.h / 3.2
    val strandA = mutableListOf<Pair<Double, Double>>(); val strandB = mutableListOf<Pair<Double, Double>>()
    var y = -20.0
    while (y <= s.h + 20) {
        val a = 2 * PI * y / period
        strandA += (cx + amp * sin(a)) to y; strandB += (cx - amp * sin(a)) to y
        y += 16.0
    }
    var yy = 10.0
    var i = 0
    while (yy < s.h) {
        val a = 2 * PI * yy / period
        val xa = cx + amp * sin(a); val xb = cx - amp * sin(a)
        val depth = (cos(a) + 1) / 2
        s.line(xa, yy, xb, yy, if (i % 5 == 0) s.ac else s.dim, 3.0, 0.2 + 0.6 * depth)
        s.circle(xa, yy, 6.0 + 4 * depth, fill = s.dim, opacity = 0.5 + 0.4 * depth)
        s.circle(xb, yy, 6.0 + 4 * (1 - depth), fill = s.dim, opacity = 0.5 + 0.4 * (1 - depth))
        if (i % 7 == 0) s.text(cx, yy - 8, "ATGC".let { l -> "${l[s.rnd.nextInt(4)]}-${l[s.rnd.nextInt(4)]}" }, s.w * 0.02, s.fg, 0.75, "middle")
        yy += 36.0; i++
    }
    s.poly(strandA, s.ac, 5.0, 0.85); s.poly(strandB, s.fg, 5.0, 0.7)
}

// ---- TOPO: contour rings with elevation labels ----
private fun topo(s: Svg) {
    val centers = listOf(0.3 to 0.28, 0.72 to 0.58, 0.35 to 0.82).map { (a, b) -> a * s.w to b * s.h }
    centers.forEachIndexed { ci, (cx, cy) ->
        val p1 = s.rnd.nextDouble() * 2 * PI; val p2 = s.rnd.nextDouble() * 2 * PI
        for (k in 1..15) {
            val base = s.w * 0.045 * k
            val pts = (0 until 72).map { j ->
                val t = 2 * PI * j / 72
                val r = base * (1 + 0.16 * sin(3 * t + p1) + 0.09 * sin(5 * t + p2) + 0.03 * k / 15.0 * sin(2 * t))
                (cx + r * cos(t)) to (cy + r * sin(t) * 1.15)
            }
            val major = k % 5 == 0
            s.poly(pts, if (major) s.ac else s.dim, if (major) 4.0 else 2.0, if (major) 0.85 else 0.45, close = true)
            if (major) s.text(pts[0].first + 6, pts[0].second - 6, "${1000 + ci * 120 + k * 40}m", s.w * 0.022, s.fg, 0.8)
        }
        s.circle(cx, cy, 7.0, fill = s.fg, opacity = 0.9)
    }
}
