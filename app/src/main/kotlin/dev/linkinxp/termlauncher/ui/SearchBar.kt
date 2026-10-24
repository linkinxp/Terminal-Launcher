package dev.linkinxp.termlauncher.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** `g> search google [go]`: Enter or the Search key hands the text to [onSearch] and clears the field. */
@Composable
fun SearchBar(onSearch: (String) -> Unit, modifier: Modifier = Modifier) {
    val theme = LocalTheme.current
    var text by remember { mutableStateOf("") }
    fun submit() {
        if (text.isBlank()) return
        onSearch(text)
        text = ""
    }
    Row(modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        TermText("g> ", color = theme.accent.color(), bold = true)
        BasicTextField(
            value = text,
            onValueChange = { text = it },
            singleLine = true,
            textStyle = TextStyle(color = theme.foreground.color(), fontFamily = TermFont, fontSize = 14.sp),
            cursorBrush = SolidColor(theme.accent.color()),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { submit() }),
            decorationBox = { inner ->
                Box { if (text.isEmpty()) TermText("search google", dim = true); inner() }
            },
            modifier = Modifier.weight(1f),
        )
        TermText("[go]", Modifier.clickable { submit() }.padding(start = 8.dp), dim = true)
    }
}
