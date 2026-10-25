package dev.linkinxp.termlauncher.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import dev.linkinxp.termlauncher.icons.IconRepository

/** The themed tile of an app, or null until it has been drawn (off the main thread; cached across screens). */
@Composable
fun rememberThemedIcon(pkg: String, label: String, size: Dp, selected: Boolean = false): ImageBitmap? {
    val context = LocalContext.current
    val theme = LocalTheme.current
    val px = with(LocalDensity.current) { size.roundToPx() }
    val tile by produceState<ImageBitmap?>(null, pkg, label, theme.background, theme.foreground, theme.accent, theme.dim, px, selected) {
        value = IconRepository.get(context).tile(pkg, label, theme, px, selected)
    }
    return tile
}

/** The tile at its final size; an empty box of the same size stands in until it is ready, so nothing jumps. */
@Composable
fun IconTile(pkg: String, label: String, size: Dp, modifier: Modifier = Modifier, selected: Boolean = false) {
    val tile = rememberThemedIcon(pkg, label, size, selected)
    Box(modifier.size(size)) {
        tile?.let { Image(it, contentDescription = "icon-$pkg", modifier = Modifier.size(size)) }
    }
}
