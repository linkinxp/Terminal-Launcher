package dev.linkinxp.termlauncher.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.linkinxp.termlauncher.matrix.wakeDurationMs
import dev.linkinxp.termlauncher.matrix.wakeText
import kotlinx.coroutines.delay

/** "Wake up, Neo...": the lines typed out on black in the theme's foreground color; tap to leave. Static under reduce motion. */
@Composable
fun WakeUpOverlay(onDone: () -> Unit) {
    val theme = LocalTheme.current
    val reduce = rememberReduceMotion()
    var elapsed by remember { mutableLongStateOf(0L) }
    LaunchedEffect(reduce) {
        if (reduce) { elapsed = wakeDurationMs(); delay(2_500); onDone(); return@LaunchedEffect }
        val start = System.nanoTime()
        while (elapsed < wakeDurationMs()) {
            elapsed = (System.nanoTime() - start) / 1_000_000L
            delay(30)
        }
        onDone()
    }
    val lines = wakeText(elapsed)
    val cursorOn = reduce || (elapsed / 500) % 2 == 0L
    Column(
        Modifier.fillMaxSize().background(Color.Black)
            .pointerInput(Unit) { detectTapGestures { onDone() } }
            .statusBarsPadding().padding(24.dp),
    ) {
        lines.forEachIndexed { i, line ->
            TermText(line + if (i == lines.lastIndex && cursorOn) "█" else "", color = theme.foreground.color(), size = 20.sp, bold = true, maxLines = 2)
        }
    }
}
