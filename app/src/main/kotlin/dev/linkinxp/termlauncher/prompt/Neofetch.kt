package dev.linkinxp.termlauncher.prompt

data class DeviceInfo(
    val model: String,
    val android: String,
    val launcher: String,
    val uptime: String,
    val theme: String,
    val apps: Int,
    val ramPct: Int?,
)

private const val WIDTH = 40
private val LOGO = listOf("+--------+", "| >_     |", "|        |", "| term   |", "+--------+")

/** ASCII logo on the left, device info on the right; every line is cut to 40 columns. */
fun neofetch(info: DeviceInfo): String {
    val rows = listOf(
        "user@phone",
        "model    ${info.model}",
        "android  ${info.android}",
        "launcher ${info.launcher}",
        "uptime   ${info.uptime}",
        "theme    ${info.theme}",
        "apps     ${info.apps}",
        "ram      ${info.ramPct?.let { "$it%" } ?: "?"}",
    )
    return rows.indices.joinToString("\n") { i ->
        ((LOGO.getOrNull(i) ?: "").padEnd(12) + rows[i]).take(WIDTH)
    }
}
