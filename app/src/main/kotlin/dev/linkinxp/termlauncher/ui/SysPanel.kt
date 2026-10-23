package dev.linkinxp.termlauncher.ui

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.linkinxp.termlauncher.sys.SysSnapshot
import dev.linkinxp.termlauncher.sys.asciiBar
import dev.linkinxp.termlauncher.sys.formatUptime

private fun barRow(label: String, pct: Int?, suffix: String = ""): String =
    if (pct == null) "$label ? " else "$label ${asciiBar(pct / 100f)} $pct%$suffix"

/** The `sys` panel: battery, ram and disk bars, uptime and network; `?` for anything unreadable. */
@Composable
fun SysPanel(snapshot: SysSnapshot, modifier: Modifier = Modifier) {
    TermPanel("sys", modifier.fillMaxWidth().padding(top = 8.dp)) {
        TermText(barRow("bat ", snapshot.batteryPct, if (snapshot.charging) "+" else ""))
        TermText(barRow("ram ", snapshot.ramPct))
        TermText(barRow("disk", snapshot.diskPct))
        TermText("up   ${formatUptime(snapshot.uptimeMs)}   net ${snapshot.net}")
    }
}
