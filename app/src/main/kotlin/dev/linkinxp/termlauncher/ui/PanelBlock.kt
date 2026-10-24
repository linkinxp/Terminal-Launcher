package dev.linkinxp.termlauncher.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.linkinxp.termlauncher.panel.PanelData

private const val LINE_DP = 21

/** A home-page panel (`cal`, `wx`): nothing when off, its lines, or a one-line message. */
@Composable
fun PanelBlock(title: String, data: PanelData, modifier: Modifier = Modifier, trailing: String? = null, scrollAfter: Int = Int.MAX_VALUE) {
    if (data is PanelData.Off) return
    val scrolls = data is PanelData.Lines && data.lines.size > scrollAfter
    TermPanel(title, modifier.fillMaxWidth().padding(top = 8.dp), trailing = trailing ?: if (scrolls) "↕ ${(data as PanelData.Lines).lines.size}" else null) {
        when (data) {
            is PanelData.Lines ->
                // A long list shows a few lines (the next one peeking out as a hint) and scrolls inside itself.
                Column(if (scrolls) Modifier.heightIn(max = (scrollAfter * LINE_DP + LINE_DP / 2).dp).verticalScroll(rememberScrollState()) else Modifier) {
                    data.lines.forEach { TermText(it) }
                }
            is PanelData.Message -> TermText(data.text, maxLines = 3)
            PanelData.Off -> {}
        }
    }
}
