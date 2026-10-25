package dev.linkinxp.termlauncher.ui

import android.appwidget.AppWidgetHostView
import android.view.View
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import dev.linkinxp.termlauncher.LauncherViewModel
import dev.linkinxp.termlauncher.layout.Item
import dev.linkinxp.termlauncher.layout.Density
import dev.linkinxp.termlauncher.layout.widgetHeightDp

/** A hosted Android widget inside a terminal-style frame titled with its name; the widget keeps its own look. */
@Composable
fun WidgetPanel(widget: Item.Widget, vm: LauncherViewModel, density: Density = Density.NORMAL, modifier: Modifier = Modifier) {
    val info = remember(widget.provider) { vm.widgets.providers().firstOrNull { it.provider == widget.provider } }
    val heightDp = widgetHeightDp(info?.minHeightDp ?: 0, widget.size, density.widgetScale)
    val widthDp = (LocalConfiguration.current.screenWidthDp - 56).coerceAtLeast(80)
    TermPanel((info?.label ?: "widget").lowercase(), modifier.fillMaxWidth().padding(top = 8.dp)) {
        AndroidView(
            factory = { ctx -> vm.widgets.createView(widget.id) ?: View(ctx) },
            modifier = Modifier.fillMaxWidth().height(heightDp.dp),
            update = { v -> (v as? AppWidgetHostView)?.updateAppWidgetSize(null, widthDp, heightDp, widthDp, heightDp) },
        )
    }
}
