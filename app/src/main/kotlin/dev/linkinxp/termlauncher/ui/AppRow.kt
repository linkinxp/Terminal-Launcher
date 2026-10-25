package dev.linkinxp.termlauncher.ui

import android.content.Context
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import dev.linkinxp.termlauncher.apps.AppEntry
import dev.linkinxp.termlauncher.data.IconMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** One app line in the drawer or a folder: text only, mono glyph `[c]`, or the real icon. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AppRow(
    app: AppEntry,
    mode: IconMode,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier.fillMaxWidth().combinedClickable(onClick = onClick, onLongClick = onLongClick).padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        when (mode) {
            IconMode.TEXT -> {}
            IconMode.GLYPH -> TermText("[${app.label.firstOrNull()?.lowercaseChar() ?: '?'}] ", dim = true)
            IconMode.ICON -> { IconTile(app.pkg, app.label, 24.dp); TermText(" ") }
        }
        TermText(app.label.lowercase())
    }
}
