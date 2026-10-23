package dev.linkinxp.termlauncher.ui

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import dev.linkinxp.termlauncher.LauncherUi
import dev.linkinxp.termlauncher.LauncherViewModel
import dev.linkinxp.termlauncher.layout.Item
import dev.linkinxp.termlauncher.panel.PanelData
import dev.linkinxp.termlauncher.sys.SysSnapshot
import dev.linkinxp.termlauncher.sys.readSystemStats
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.format.TextStyle
import java.time.temporal.IsoFields
import java.util.Locale

@Composable
private fun rememberNow(): LocalDateTime {
    val context = LocalContext.current
    var now by remember { mutableStateOf(LocalDateTime.now()) }
    // Ticks are broadcasts (not delay()), so the clock is right again as soon as the screen wakes.
    DisposableEffect(context) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(c: Context, i: Intent) { now = LocalDateTime.now() }
        }
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_TIME_TICK)
            addAction(Intent.ACTION_TIME_CHANGED)
            addAction(Intent.ACTION_TIMEZONE_CHANGED)
            addAction(Intent.ACTION_SCREEN_ON)
        }
        ContextCompat.registerReceiver(context, receiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
        now = LocalDateTime.now()
        onDispose { context.unregisterReceiver(receiver) }
    }
    return now
}

@Composable
private fun ClockHeader(now: LocalDateTime, snapshot: SysSnapshot, glitch: Boolean, calendar: PanelData, weather: PanelData) {
    val date = now.toLocalDate()
    Column(Modifier.fillMaxWidth()) {
        PixelClock(now.toLocalTime(), glitch = glitch)
        TermText(
            "${date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.ENGLISH).lowercase()} ${date.dayOfMonth} " +
                "${date.month.getDisplayName(TextStyle.SHORT, Locale.ENGLISH).lowercase()} ${date.year}  " +
                "wk ${date.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR)}  day ${date.dayOfYear}",
            Modifier.fillMaxWidth().padding(vertical = 8.dp),
        )
        SysPanel(snapshot)
        PanelBlock("cal", calendar)
        PanelBlock("wx", weather)
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
    onEdit: () -> Unit,
    homeSignal: Int = 0,
    modifier: Modifier = Modifier,
) {
    val pageCount = ui.layout.pages.size
    val pager = rememberPagerState { pageCount }
    val scope = rememberCoroutineScope()
    // A Home press scrolls back to the first page.
    LaunchedEffect(homeSignal) { if (homeSignal > 0) pager.animateScrollToPage(0) }
    val context = LocalContext.current
    val now = rememberNow()
    val snapshot = remember(now) { readSystemStats(context) }
    // Panels that read live data refresh on every minute tick.
    LaunchedEffect(now.minute) { vm.refreshPanels() }
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
            Column(
                Modifier.fillMaxSize().padding(horizontal = 16.dp)
                    .pointerInput(Unit) { detectTapGestures(onLongPress = { onEdit() }) },
            ) {
                if (index == 0) ClockHeader(now, snapshot, ui.fx.glitch, ui.calendar, ui.weather)
                ui.layout.pages[index].items.forEach { item ->
                    val label = itemLabel(item, ui) ?: return@forEach
                    TermText(
                        "${if (item is Item.Folder) "" else "> "}$label",
                        Modifier.fillMaxWidth().combinedClickable(onClick = { onItemTap(item) }, onLongClick = { onItemLongPress(item) }).padding(vertical = 8.dp),
                    )
                }
            }
        }
        StatusLine(pageCount, pager.currentPage, snapshot, now, onPage = { i -> scope.launch { pager.animateScrollToPage(i) } })
        Dock(ui, onItemTap, onItemLongPress)
        PromptBar(ui, vm)
    }
}
