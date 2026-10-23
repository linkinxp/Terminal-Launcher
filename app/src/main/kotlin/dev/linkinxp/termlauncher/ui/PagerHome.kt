package dev.linkinxp.termlauncher.ui

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import dev.linkinxp.termlauncher.LauncherUi
import dev.linkinxp.termlauncher.LauncherViewModel
import dev.linkinxp.termlauncher.layout.Item
import kotlinx.coroutines.delay
import java.time.LocalDateTime
import java.time.format.TextStyle
import java.time.temporal.IsoFields
import java.util.Locale

@Composable
private fun rememberNow(): LocalDateTime {
    var now by remember { mutableStateOf(LocalDateTime.now()) }
    LaunchedEffect(Unit) {
        while (true) {
            now = LocalDateTime.now()
            delay(60_000L - now.second * 1000L - now.nano / 1_000_000L + 50)
        }
    }
    return now
}

private fun batteryText(context: Context): String {
    val i = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED)) ?: return "bat ?"
    val level = i.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
    val scale = i.getIntExtra(BatteryManager.EXTRA_SCALE, 100)
    val status = i.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
    val charging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL
    return "bat ${if (level < 0) "?" else "${level * 100 / scale}%"}${if (charging) " +" else ""}"
}

@Composable
private fun ClockHeader() {
    val now = rememberNow()
    val date = now.toLocalDate()
    val context = LocalContext.current
    Column(Modifier.fillMaxWidth()) {
        PixelClock(now.toLocalTime())
        TermText(
            "${date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.ENGLISH).lowercase()} ${date.dayOfMonth} " +
                "${date.month.getDisplayName(TextStyle.SHORT, Locale.ENGLISH).lowercase()} ${date.year}  " +
                "wk ${date.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR)}  day ${date.dayOfYear}",
            Modifier.fillMaxWidth().padding(vertical = 8.dp),
        )
        TermPanel("sys", Modifier.fillMaxWidth().padding(top = 8.dp), trailing = "%02d:%02d".format(now.hour, now.minute)) {
            TermText(batteryText(context))
        }
    }
}

/** Pager of home pages (page 0 carries the clock), then page indicator, dock and prompt. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PagerHome(
    ui: LauncherUi,
    vm: LauncherViewModel,
    onItemTap: (Item) -> Unit,
    onItemLongPress: (Item) -> Unit,
    onSwipeUp: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val pageCount = ui.layout.pages.size
    val pager = rememberPagerState { pageCount }
    Column(
        modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().imePadding()
            .pointerInput(Unit) {
                var total = 0f
                detectVerticalDragGestures(
                    onDragStart = { total = 0f },
                    onDragEnd = { if (total < -120f) onSwipeUp() },
                ) { _, dy -> total += dy }
            },
    ) {
        HorizontalPager(pager, Modifier.weight(1f).fillMaxWidth()) { index ->
            Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
                if (index == 0) ClockHeader()
                ui.layout.pages[index].items.forEach { item ->
                    val label = itemLabel(item, ui) ?: return@forEach
                    TermText(
                        "${if (item is Item.Folder) "" else "> "}$label",
                        Modifier.fillMaxWidth().clickable { onItemTap(item) }.padding(vertical = 8.dp),
                    )
                }
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
            (0 until pageCount).forEach { i ->
                TermText(if (i == pager.currentPage) "[${i + 1}]" else " ${i + 1} ", Modifier.padding(horizontal = 2.dp), dim = i != pager.currentPage)
            }
        }
        Dock(ui, onItemTap, onItemLongPress)
        PromptBar(ui, vm)
    }
}
