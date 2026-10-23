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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import dev.linkinxp.termlauncher.sys.bootLog
import kotlinx.coroutines.delay

private const val BOOT_MS = 1_000L

/** Fake boot log typed out in about 1 s on unlock; tap to skip. Ends at once under reduce motion. */
@Composable
fun BootLogOverlay(onDone: () -> Unit) {
    val theme = LocalTheme.current
    val reduce = rememberReduceMotion()
    var shown by remember { mutableIntStateOf(0) }
    LaunchedEffect(reduce) {
        if (reduce) { onDone(); return@LaunchedEffect }
        val gap = BOOT_MS / bootLog.size
        repeat(bootLog.size) { shown = it + 1; delay(gap) }
        delay(150)
        onDone()
    }
    Column(
        Modifier.fillMaxSize().background(theme.background.color())
            .pointerInput(Unit) { detectTapGestures { onDone() } }
            .statusBarsPadding().padding(16.dp),
    ) {
        bootLog.take(shown).forEach { TermText(it, color = theme.foreground.color()) }
    }
}
