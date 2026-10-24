package dev.linkinxp.termlauncher.ui

import dev.linkinxp.termlauncher.clock.formatClockTime
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.linkinxp.termlauncher.sys.SysSnapshot
import java.time.LocalDateTime

private val PAGE_NAMES = listOf("home", "two", "three", "four", "five")

/** `1:home`, `2:two` ... `5:five`; later pages are just their number. */
fun pageLabel(index: Int, compact: Boolean = false): String =
    if (!compact && index in PAGE_NAMES.indices) "${index + 1}:${PAGE_NAMES[index]}" else "${index + 1}"

/** From three pages on the names would push the battery and clock off a phone-width line. */
fun compactPageLabels(pageCount: Int): Boolean = pageCount > 2

/** Right half of the status line: `bat 66%+ │ 21:08` (`bat ?` when the level is unknown). */
fun statusTail(batteryPct: Int?, charging: Boolean, hour: Int, minute: Int, h24: Boolean = true): String =
    "bat ${batteryPct?.let { "$it%" } ?: "?"}${if (charging && batteryPct != null) "+" else ""}" +
        " │ " + formatClockTime(hour, minute, h24)

/** tmux-style bar: `[0] 1:home* 2:two │ bat 66% │ 21:08`; each page label switches to that page. */
@Composable
fun StatusLine(
    pageCount: Int,
    current: Int,
    snapshot: SysSnapshot,
    now: LocalDateTime,
    h24: Boolean = true,
    onPage: (Int) -> Unit,
    onSettings: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    Row(modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 2.dp), horizontalArrangement = Arrangement.Start) {
        // Tap to open the settings screen.
        TermText("[cfg] ", Modifier.testTag("status-cfg").clickable(onClick = onSettings), bold = true)
        (0 until pageCount).forEach { i ->
            TermText(
                pageLabel(i, compactPageLabels(pageCount)) + if (i == current) "*" else "",
                Modifier.clickable { onPage(i) }.padding(end = 8.dp),
                bold = i == current,
                dim = i != current,
            )
        }
        TermText("│ " + statusTail(snapshot.batteryPct, snapshot.charging, now.hour, now.minute, h24), dim = true)
    }
}
