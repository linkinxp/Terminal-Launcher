package dev.linkinxp.termlauncher.ui

import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import dev.linkinxp.termlauncher.badges.isBadgeAccessGranted
import dev.linkinxp.termlauncher.media.MediaStore
import dev.linkinxp.termlauncher.media.Torch
import dev.linkinxp.termlauncher.media.nowPlayingLines
import dev.linkinxp.termlauncher.media.playLabel
import dev.linkinxp.termlauncher.media.toggleSpecs
import dev.linkinxp.termlauncher.media.torchLabel

@Composable
private fun Button(text: String, onClick: () -> Unit) {
    TermText(text, Modifier.clickable(onClick = onClick).padding(end = 14.dp, top = 6.dp, bottom = 6.dp), bold = true)
}

/** The `media` panel: the track that is playing with previous / play-pause / next. Needs notification access. */
@Composable
fun MediaPanel(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val info by MediaStore.state.collectAsState()
    TermPanel("now playing", modifier.padding(top = 8.dp)) {
        val current = info
        when {
            current != null -> {
                nowPlayingLines(current).forEachIndexed { i, line -> TermText(line, bold = i == 0, dim = i > 0) }
                FlowRow {
                    Button("[<<]") { current.controls.previous() }
                    Button(playLabel(current.playing)) { current.controls.togglePlay() }
                    Button("[>>]") { current.controls.next() }
                }
            }
            !isBadgeAccessGranted(context) -> TermText("allow notification access in settings to see what plays", dim = true, maxLines = 2)
            else -> TermText("nothing playing", dim = true)
        }
    }
}

/** The `toggles` panel: the flashlight, and shortcuts to the system's wifi, bluetooth, sound and location screens. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TogglesPanel(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val torchOn by Torch.on.collectAsState()
    DisposableEffect(Unit) { Torch.register(context); onDispose { } }
    TermPanel("toggles", modifier.padding(top = 8.dp)) {
        FlowRow {
            toggleSpecs().forEach { spec ->
                if (spec.id == "torch") Button(torchLabel(torchOn)) { Torch.toggle(context) }
                else Button("[${spec.label}]") {
                    try { context.startActivity(Intent(spec.action).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) } catch (_: Exception) {}
                }
            }
        }
    }
}
