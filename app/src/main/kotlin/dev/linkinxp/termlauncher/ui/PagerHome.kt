package dev.linkinxp.termlauncher.ui

import android.text.format.DateFormat
import dev.linkinxp.termlauncher.clock.amPm
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
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.Alignment
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.Velocity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
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

private const val SWIPE_PX = 120f

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
private fun ClockHeader(
    now: LocalDateTime,
    h24: Boolean,
    snapshot: SysSnapshot,
    glitch: Boolean,
    calendar: PanelData,
    weather: PanelData,
    showSearch: Boolean,
    onSearch: (String) -> Unit,
) {
    val date = now.toLocalDate()
    Column(Modifier.fillMaxWidth()) {
        PixelClock(now.toLocalTime(), glitch = glitch, h24 = h24)
        TermText(
            "${date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.ENGLISH).lowercase()} ${date.dayOfMonth} " +
                "${date.month.getDisplayName(TextStyle.SHORT, Locale.ENGLISH).lowercase()} ${date.year}  " +
                "wk ${date.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR)}  day ${date.dayOfYear}" + if (h24) "" else "  ${amPm(now.hour)}",
            Modifier.fillMaxWidth().padding(vertical = 8.dp),
        )
        if (showSearch) SearchBar(onSearch)
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
    onSearch: (String) -> Unit = {},
    onSettings: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val pageCount = ui.layout.pages.size
    val pager = rememberPagerState { pageCount }
    val scope = rememberCoroutineScope()
    // A Home press scrolls back to the first page.
    LaunchedEffect(homeSignal) { if (homeSignal > 0) pager.animateScrollToPage(0) }
    val context = LocalContext.current
    val now = rememberNow()
    val h24 = ui.clockFormat.is24(DateFormat.is24HourFormat(context))
    val snapshot = remember(now) { readSystemStats(context) }
    // Panels that read live data refresh on every minute tick.
    LaunchedEffect(now.minute, ui.clockFormat) { vm.refreshPanels() } // the agenda times follow the clock format
    // Swiping down slides the quick tiles in from the top, where a thumb can reach them.
    var pulled by remember { mutableStateOf(false) }
    val canPull = ui.panels.quick && ui.quick.isNotEmpty()
    LaunchedEffect(homeSignal) { pulled = false }
    BackHandler(enabled = pulled) { pulled = false }
    // A page that scrolls swallows vertical drags, so its leftover at either end counts as the swipe:
    // pulling down at the top shows the quick panel, pushing up at the bottom opens the drawer.
    val overscroll = remember { floatArrayOf(0f) }
    val swipes by rememberUpdatedState({ dy: Float ->
        when {
            dy < -SWIPE_PX -> if (pulled) pulled = false else onSwipeUp()
            dy > SWIPE_PX -> if (canPull) pulled = true
        }
    })
    val edgeSwipes = remember {
        object : NestedScrollConnection {
            override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
                if (source == NestedScrollSource.UserInput) overscroll[0] += available.y
                return Offset.Zero
            }
            override suspend fun onPreFling(available: Velocity): Velocity {
                val dy = overscroll[0]
                overscroll[0] = 0f
                swipes(dy)
                return Velocity.Zero
            }
        }
    }
    Box(modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().imePadding().nestedScroll(edgeSwipes)) {
    Column(Modifier.fillMaxSize()) {
        HorizontalPager(pager, Modifier.weight(1f).fillMaxWidth()) { index ->
            // Always a scroll container: what the page cannot scroll any further reaches edgeSwipes as the swipe.
            val scroll = rememberScrollState()
            Column(
                Modifier.fillMaxSize().padding(horizontal = 16.dp)
                    .pointerInput(Unit) { detectTapGestures(onLongPress = { onEdit() }) }
                    .verticalScroll(scroll),
            ) {
                if (index == 0) ClockHeader(now, h24, snapshot, ui.fx.glitch, ui.calendar, ui.weather, ui.panels.search, onSearch)
                ui.layout.pages[index].items.forEach { item ->
                    if (item is Item.Widget) { key(item.id) { WidgetPanel(item, vm) }; return@forEach }
                    val label = itemLabel(item, ui) ?: return@forEach
                    TermText(
                        "${if (item is Item.Folder) "" else "> "}$label",
                        Modifier.fillMaxWidth().combinedClickable(onClick = { onItemTap(item) }, onLongClick = { onItemLongPress(item) }).padding(vertical = 8.dp),
                    )
                }
            }
        }
        // The bars below the pager are not scrollable, so they read the swipe directly.
        Column(
            Modifier.pointerInput(canPull) {
                var total = 0f
                detectVerticalDragGestures(onDragStart = { total = 0f }, onDragEnd = { swipes(total) }) { _, dy -> total += dy }
            },
        ) {
            StatusLine(pageCount, pager.currentPage, snapshot, now, h24, onPage = { i -> scope.launch { pager.animateScrollToPage(i) } }, onSettings = onSettings)
            // Quick tiles live with the pinned apps at the bottom, so they stay in reach on every page.
            QuickPanel(ui, { onItemTap(Item.App(it)) }, { onItemLongPress(Item.App(it)) }, Modifier.padding(horizontal = 16.dp))
            Dock(ui, onItemTap, onItemLongPress)
            PromptBar(ui, vm)
        }
    }
    if (pulled && canPull) {
        QuickPanel(
            ui, { pulled = false; onItemTap(Item.App(it)) }, { pulled = false; onItemLongPress(Item.App(it)) },
            Modifier.align(Alignment.TopCenter).background(LocalTheme.current.background.color()).padding(horizontal = 16.dp),
            tagPrefix = "pull-quick-",
        )
    }
    }
}
