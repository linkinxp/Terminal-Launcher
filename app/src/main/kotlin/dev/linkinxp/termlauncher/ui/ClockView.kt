package dev.linkinxp.termlauncher.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import dev.linkinxp.termlauncher.clock.ClockStyle
import dev.linkinxp.termlauncher.clock.amPm
import dev.linkinxp.termlauncher.clock.binaryColumns
import dev.linkinxp.termlauncher.clock.clockDigits
import dev.linkinxp.termlauncher.clock.epochText
import dev.linkinxp.termlauncher.clock.figletLines
import dev.linkinxp.termlauncher.clock.formatClockTime
import dev.linkinxp.termlauncher.clock.hexTime
import kotlinx.coroutines.delay
import java.time.LocalDateTime

private val CLOCK_HEIGHT = 96.dp // every style fills the same height so the page below never jumps

/** The home clock in the chosen [style]; BINARY, HEX and EPOCH always use 24-hour values. */
@Composable
fun ClockView(style: ClockStyle, time: LocalDateTime, h24: Boolean, glitch: Boolean, modifier: Modifier = Modifier) {
    when (style) {
        ClockStyle.PIXEL -> PixelClock(time.toLocalTime(), modifier, glitch, h24)
        ClockStyle.SEGMENT -> PixelClock(time.toLocalTime(), modifier, glitch, h24, segments = true)
        ClockStyle.FIGLET -> BigText(figletLines(clockDigits(time.hour, time.minute, h24)), 27.sp, modifier)
        ClockStyle.BINARY -> BinaryClock(time, modifier)
        ClockStyle.HEX -> BigText(listOf(hexTime(time.hour, time.minute)), 44.sp, modifier)
        ClockStyle.EPOCH -> EpochClock(modifier)
        ClockStyle.PLAIN -> BigText(listOf(formatClockTime(time.hour, time.minute, h24).removeSuffix(amPm(time.hour))), 72.sp, modifier)
    }
}

/** Monospace [lines] centred in the clock's fixed box; soft glow when the theme has one. */
@Composable
private fun BigText(lines: List<String>, size: TextUnit, modifier: Modifier = Modifier) {
    val theme = LocalTheme.current
    val color = theme.foreground.color()
    val style = remember(color, size, theme.glow) {
        TextStyle(
            color = color, fontFamily = TermFont, fontSize = size, fontWeight = FontWeight.Bold,
            lineHeight = size * 1.05f, textAlign = TextAlign.Center,
            shadow = if (theme.glow) Shadow(color.copy(alpha = 0.7f), Offset.Zero, 18f) else null,
        )
    }
    Box(modifier.fillMaxWidth().height(CLOCK_HEIGHT).graphicsLayer(), contentAlignment = Alignment.Center) {
        BasicText(lines.joinToString("\n"), style = style, maxLines = lines.size, softWrap = false)
    }
}

/** Four columns of lit/unlit dots (hour tens and units, minute tens and units), least significant at the bottom. */
@Composable
private fun BinaryClock(time: LocalDateTime, modifier: Modifier = Modifier) {
    val theme = LocalTheme.current
    val columns = binaryColumns(time.hour, time.minute)
    val digits = clockDigits(time.hour, time.minute, h24 = true)
    Row(
        modifier.fillMaxWidth().height(CLOCK_HEIGHT).graphicsLayer(),
        horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically,
    ) {
        columns.forEachIndexed { i, bits ->
            Column(
                Modifier.padding(start = if (i == 2) 28.dp else if (i == 0) 0.dp else 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                // Pad the short columns at the top so every column's last bit lines up.
                repeat(4 - bits.size) { TermText(" ", size = 14.sp) }
                bits.forEach { on -> TermText(if (on) "●" else "○", size = 14.sp, color = if (on) theme.accent.color() else theme.dim.color(), bold = on) }
                TermText("${digits[i]}", size = 10.sp, dim = true)
            }
        }
    }
}

/** Unix seconds, ticking every second only while the launcher is resumed (once a minute with reduced motion). */
@Composable
private fun EpochClock(modifier: Modifier = Modifier) {
    val reduce = rememberReduceMotion()
    var seconds by remember { mutableLongStateOf(System.currentTimeMillis() / 1000) }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(lifecycle, reduce) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            while (true) {
                val now = System.currentTimeMillis()
                seconds = now / 1000
                delay(if (reduce) 60_000 - now % 60_000 else 1_000 - now % 1_000)
            }
        }
    }
    BigText(listOf(epochText(seconds)), 36.sp, modifier)
}
