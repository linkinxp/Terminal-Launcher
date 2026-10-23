package dev.linkinxp.termlauncher.ui

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.linkinxp.termlauncher.panel.PanelData

/** A home-page panel (`cal`, `wx`): nothing when off, its lines, or a one-line message. */
@Composable
fun PanelBlock(title: String, data: PanelData, modifier: Modifier = Modifier, trailing: String? = null) {
    if (data is PanelData.Off) return
    TermPanel(title, modifier.fillMaxWidth().padding(top = 8.dp), trailing = trailing) {
        when (data) {
            is PanelData.Lines -> data.lines.forEach { TermText(it) }
            is PanelData.Message -> TermText(data.text, maxLines = 3)
            PanelData.Off -> {}
        }
    }
}
