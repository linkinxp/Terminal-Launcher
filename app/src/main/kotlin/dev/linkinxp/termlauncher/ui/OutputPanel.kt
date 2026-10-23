package dev.linkinxp.termlauncher.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/** Bordered `out` panel for command output (already capped to 12 lines); tap anywhere on it to dismiss. */
@Composable
fun OutputPanel(text: String, onDismiss: () -> Unit, modifier: Modifier = Modifier) {
    TermPanel("out", modifier.fillMaxWidth().clickable(onClick = onDismiss), trailing = "[x]") {
        TermText(text, maxLines = 12)
    }
}
