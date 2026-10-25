package dev.linkinxp.termlauncher.ui

import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.unit.dp
import dev.linkinxp.termlauncher.layout.Item

/**
 * Follows a finger that picked an app up in the drawer. It only watches (it never takes part in the gesture): once the
 * finger moved off the spot it asks the drawer to step aside, on release it drops the app on the home screen, and a hold
 * that never moved is the app menu. Lives above both the drawer and the home screen, so it keeps seeing the finger
 * after the drawer has left.
 */
fun Modifier.drawerDragWatcher(drag: DragState, onLeaveDrawer: () -> Unit, onMenu: (String) -> Unit): Modifier =
    pointerInput(drag) {
        awaitEachGesture {
            awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
            do {
                val event = awaitPointerEvent(PointerEventPass.Initial)
                val change = event.changes.firstOrNull() ?: break
                if (drag.fromDrawer && drag.item != null) {
                    drag.finger = change.position
                    if (!drag.moved && (drag.finger - drag.start).getDistance() > viewConfiguration.touchSlop) { drag.moved = true; onLeaveDrawer() }
                    if (drag.moved) drag.edge = edgeDirection(change.position.x, size.width.toFloat(), 48.dp.toPx())
                }
            } while (event.changes.any { it.pressed })
            val item = drag.item
            if (drag.fromDrawer && item != null) {
                if (drag.moved) drag.drawerDrop(drag.finger, item) else (item as? Item.App)?.let { onMenu(it.pkg) }
                drag.item = null; drag.fromDrawer = false; drag.moved = false; drag.edge = 0
            }
        }
    }
