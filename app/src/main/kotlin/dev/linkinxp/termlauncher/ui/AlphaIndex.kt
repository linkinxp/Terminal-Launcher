package dev.linkinxp.termlauncher.ui

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.Arrangement

/** Fast-scroll rail: tap or drag along the letters to jump to that section. */
@Composable
fun AlphaIndex(letters: List<Char>, onSelect: (Char) -> Unit, modifier: Modifier = Modifier) {
    var height by remember { mutableIntStateOf(1) }
    // The gesture handlers below live across recompositions, so they must read the latest values.
    val currentLetters by rememberUpdatedState(letters)
    val currentOnSelect by rememberUpdatedState(onSelect)
    fun pick(y: Float) {
        val l = currentLetters
        if (l.isEmpty()) return
        val i = (y / height * l.size).toInt().coerceIn(0, l.lastIndex)
        currentOnSelect(l[i])
    }
    Column(
        modifier.fillMaxHeight().width(24.dp)
            .pointerInput(Unit) { detectTapGestures { pick(it.y) } }
            .pointerInput(Unit) { detectDragGestures(onDragStart = { pick(it.y) }) { change, _ -> pick(change.position.y) } }
            .onSizeChanged { height = it.height },
        verticalArrangement = Arrangement.SpaceEvenly,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        letters.forEach { TermText(it.toString(), Modifier.padding(vertical = 1.dp), dim = true) }
    }
}
