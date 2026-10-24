package dev.linkinxp.termlauncher.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import dev.linkinxp.termlauncher.R
import dev.linkinxp.termlauncher.theme.Theme
import dev.linkinxp.termlauncher.theme.ThemeRegistry
import dev.linkinxp.termlauncher.wall.WallSettings

val LocalTheme = staticCompositionLocalOf { ThemeRegistry.default }

val TermFont = FontFamily(
    Font(R.font.jetbrains_mono_regular),
    Font(R.font.jetbrains_mono_bold, FontWeight.Bold),
)

fun Long.color(): Color = Color(this)

@Composable
fun TermTheme(theme: Theme, wall: WallSettings = WallSettings(), content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalTheme provides theme) {
        Box(Modifier.fillMaxSize()) {
            WallBackground(wall, theme, rememberReduceMotion())
            content()
        }
    }
}

/** Dialog windows inherit [LocalTheme]; this only gives their wrapped content the theme background. */
@Composable
fun DialogSurface(theme: Theme, content: @Composable () -> Unit) {
    Box(Modifier.background(theme.background.color())) { content() }
}

/** Monospace text in the current theme; [dim] uses the theme's dim color. */
@Composable
fun TermText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color? = null,
    size: TextUnit = 14.sp,
    bold: Boolean = false,
    dim: Boolean = false,
    maxLines: Int = 1,
) {
    val theme = LocalTheme.current
    val resolved = color ?: (if (dim) theme.dim else theme.foreground).color()
    // BasicText with a remembered style: Material's Text merges several composition locals for every label,
    // which adds up when a screen (like the drawer) builds dozens of them at once.
    val style = remember(resolved, size, bold) {
        TextStyle(
            color = resolved,
            fontFamily = TermFont,
            fontSize = size,
            fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal,
        )
    }
    BasicText(text = text, modifier = modifier, style = style, maxLines = maxLines, overflow = TextOverflow.Ellipsis)
}
