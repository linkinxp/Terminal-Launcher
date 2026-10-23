package dev.linkinxp.termlauncher.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import kotlinx.coroutines.delay
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.linkinxp.termlauncher.LauncherUi
import dev.linkinxp.termlauncher.LauncherViewModel

const val PROMPT = "user@phone:~$"

private const val MAX_BLOCK_CHARS = 28

/**
 * The drawn block cursor is only accurate for short text with the caret at the end; otherwise the real
 * (visible) field is used so long input and mid-text edits are never hidden.
 */
fun useBlockCursor(enabled: Boolean, textLength: Int, selStart: Int, selEnd: Int): Boolean =
    enabled && selStart == selEnd && selEnd == textLength && textLength <= MAX_BLOCK_CHARS

/** Message line, suggestion rows and the `user@phone:~$` input. */
@Composable
fun PromptBar(ui: LauncherUi, vm: LauncherViewModel, modifier: Modifier = Modifier) {
    val theme = LocalTheme.current
    val p = ui.prompt
    // Text lives locally so typing never waits on the async view-model round trip; the VM only clears it.
    var value by remember { mutableStateOf(TextFieldValue(p.text)) }
    LaunchedEffect(p.text) { if (p.text.isEmpty() && value.text.isNotEmpty()) value = TextFieldValue("") }
    Column(modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
        p.message?.let { OutputPanel(it, onDismiss = vm::dismissMessage, modifier = Modifier.padding(bottom = 6.dp)) }
        p.suggestions.forEach { app ->
            TermText(
                "> ${app.label.lowercase()}",
                Modifier.fillMaxWidth().clickable { vm.launch(app.pkg); value = TextFieldValue(""); vm.onPromptChange("") }.padding(vertical = 6.dp),
            )
        }
        if (value.text.isEmpty() && p.suggestions.isEmpty()) {
            p.history.forEach { line ->
                TermText(
                    "^ $line",
                    Modifier.fillMaxWidth().clickable {
                        value = TextFieldValue(line, TextRange(line.length)); vm.onPromptChange(line)
                    }.padding(vertical = 4.dp),
                    dim = true,
                )
            }
        }
        Row {
            TermText("$PROMPT ", color = theme.accent.color(), bold = true)
            Box(Modifier.weight(1f)) {
                val blockCursor = useBlockCursor(ui.fx.cursor, value.text.length, value.selection.start, value.selection.end)
                BasicTextField(
                    value = value,
                    onValueChange = { value = it; vm.onPromptChange(it.text) },
                    singleLine = true,
                    // With the block cursor the field is only the (invisible) input: the text is drawn below.
                    textStyle = TextStyle(
                        color = if (blockCursor) Color.Transparent else theme.foreground.color(),
                        fontFamily = TermFont, fontSize = 14.sp,
                    ),
                    cursorBrush = SolidColor(if (blockCursor) Color.Transparent else theme.accent.color()),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                    keyboardActions = KeyboardActions(onGo = { vm.onPromptSubmit(value.text) }),
                    modifier = Modifier.fillMaxWidth(),
                )
                if (blockCursor) {
                    var lit by remember { mutableStateOf(true) }
                    LaunchedEffect(Unit) { while (true) { delay(530); lit = !lit } }
                    // Touches fall through to the field underneath; the block follows the text.
                    Row {
                        TermText(value.text, Modifier.weight(1f, fill = false))
                        TermText("█", color = theme.accent.color().copy(alpha = if (lit) 1f else 0f))
                    }
                }
            }
            TermText(
                "⇥",
                Modifier.clickable {
                    val done = vm.complete(value.text)
                    value = TextFieldValue(done, TextRange(done.length)); vm.onPromptChange(done)
                }.padding(start = 10.dp),
                bold = true,
            )
        }
    }
}
