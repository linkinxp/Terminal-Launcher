package dev.linkinxp.termlauncher.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.linkinxp.termlauncher.theme.BorderStyle
import dev.linkinxp.termlauncher.theme.ThemeRegistry

/** Bordered box with its title cut into the top edge (and optional [trailing] on the right). */
@Composable
fun TermPanel(
    title: String,
    modifier: Modifier = Modifier,
    trailing: String? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val theme = LocalTheme.current
    val stroke = theme.dim.color()
    Box(modifier.padding(top = 9.dp)) {
        Column(
            Modifier
                .fillMaxWidth()
                .drawBehind {
                    if (theme.border == BorderStyle.BRACKET) {
                        // Corner ticks only: four L-shaped strokes.
                        val t = 12.dp.toPx()
                        val sw = 2.dp.toPx()
                        val x1 = size.width
                        val y1 = size.height
                        fun line(a: Offset, b: Offset) = drawLine(stroke, a, b, strokeWidth = sw)
                        line(Offset(0f, 0f), Offset(t, 0f)); line(Offset(0f, 0f), Offset(0f, t))
                        line(Offset(x1, 0f), Offset(x1 - t, 0f)); line(Offset(x1, 0f), Offset(x1, t))
                        line(Offset(0f, y1), Offset(t, y1)); line(Offset(0f, y1), Offset(0f, y1 - t))
                        line(Offset(x1, y1), Offset(x1 - t, y1)); line(Offset(x1, y1), Offset(x1, y1 - t))
                        return@drawBehind
                    }
                    val w = 1.dp.toPx()
                    val radius = if (theme.border == BorderStyle.ROUNDED) 10.dp.toPx() else 0f
                    val dash = if (theme.border == BorderStyle.ASCII) PathEffect.dashPathEffect(floatArrayOf(6f, 5f)) else null
                    fun rect(inset: Float) = drawRoundRect(
                        color = stroke,
                        topLeft = Offset(inset, inset),
                        size = Size(size.width - 2 * inset, size.height - 2 * inset),
                        cornerRadius = CornerRadius(radius),
                        style = Stroke(w, pathEffect = dash),
                    )
                    rect(w / 2)
                    if (theme.border == BorderStyle.DOUBLE) rect(w / 2 + 3.dp.toPx())
                }
                .padding(horizontal = 12.dp, vertical = 12.dp),
            content = content,
        )
        TermText(
            title,
            Modifier.align(Alignment.TopStart).offset(x = 12.dp, y = (-9).dp)
                .background(theme.background.color()).padding(horizontal = 4.dp),
            color = theme.accent.color(), bold = true,
        )
        if (trailing != null) {
            TermText(
                trailing,
                Modifier.align(Alignment.TopEnd).offset(x = (-12).dp, y = (-9).dp)
                    .background(theme.background.color()).padding(horizontal = 4.dp),
                dim = true,
            )
        }
    }
}

@Composable
private fun PanelPreview(index: Int) {
    TermTheme(ThemeRegistry.all[index]) {
        TermPanel("wx", Modifier.padding(16.dp), trailing = "zagreb") {
            TermText("10° mostly clear")
            TermText("uv 0 low", dim = true)
        }
    }
}

@Preview(showBackground = true) @Composable private fun PreviewClassyBlack() = PanelPreview(0)
@Preview(showBackground = true) @Composable private fun PreviewMatrix() = PanelPreview(1)
@Preview(showBackground = true) @Composable private fun PreviewAmber() = PanelPreview(2)
@Preview(showBackground = true) @Composable private fun PreviewSolarized() = PanelPreview(3)
@Preview(showBackground = true) @Composable private fun PreviewDracula() = PanelPreview(4)
