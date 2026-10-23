package dev.linkinxp.termlauncher.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.linkinxp.termlauncher.LauncherUi
import dev.linkinxp.termlauncher.LauncherViewModel

const val PROMPT = "user@phone:~$"

/** Message line, suggestion rows and the `user@phone:~$` input. */
@Composable
fun PromptBar(ui: LauncherUi, vm: LauncherViewModel, modifier: Modifier = Modifier) {
    val theme = LocalTheme.current
    val p = ui.prompt
    Column(modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
        p.message?.let {
            TermText(it, Modifier.clickable { vm.dismissMessage() }, color = theme.accent.color(), maxLines = 3)
        }
        p.suggestions.forEach { app ->
            TermText(
                "> ${app.label.lowercase()}",
                Modifier.fillMaxWidth().clickable { vm.launch(app.pkg); vm.onPromptChange("") }.padding(vertical = 6.dp),
            )
        }
        Row {
            TermText("$PROMPT ", color = theme.accent.color(), bold = true)
            BasicTextField(
                value = p.text,
                onValueChange = vm::onPromptChange,
                singleLine = true,
                textStyle = TextStyle(color = theme.foreground.color(), fontFamily = TermFont, fontSize = 14.sp),
                cursorBrush = SolidColor(theme.accent.color()),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                keyboardActions = KeyboardActions(onGo = { vm.onPromptSubmit(p.text) }),
                modifier = Modifier.weight(1f),
            )
        }
    }
}
