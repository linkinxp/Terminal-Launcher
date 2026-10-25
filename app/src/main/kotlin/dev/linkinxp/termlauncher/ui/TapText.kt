package dev.linkinxp.termlauncher.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/** Android's minimum comfortable touch target. */
val MIN_TAP = 48.dp

/** A text button: the glyphs stay small, the tappable area is at least 48dp wide and tall. */
@Composable
fun TapText(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    bold: Boolean = false,
    dim: Boolean = false,
    color: Color? = null,
) {
    Box(modifier.clickable(onClick = onClick).sizeIn(minWidth = MIN_TAP, minHeight = MIN_TAP), contentAlignment = Alignment.Center) {
        TermText(text, bold = bold, dim = dim, color = color)
    }
}
