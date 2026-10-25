package dev.linkinxp.termlauncher.ui

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.constrainHeight
import androidx.compose.ui.unit.constrainWidth
import androidx.compose.ui.unit.offset

/** What the keyboard adds at the bottom once the navigation bar (already padded for) is taken out; nothing while [skip]. */
fun imeBottomPadding(imeBottom: Int, navBottom: Int, skip: Boolean): Int = if (skip) 0 else (imeBottom - navBottom).coerceAtLeast(0)

/**
 * Like `imePadding()`, but while [skip] says so the keyboard is not read at all, so the content is not measured again
 * on every frame of the keyboard animation (the home screen hidden behind the open drawer has no reason to follow it).
 * The check happens in the layout phase: flipping it does not recompose anything.
 */
@Composable
fun Modifier.imePaddingUnless(skip: () -> Boolean): Modifier {
    val ime = WindowInsets.ime
    val nav = WindowInsets.navigationBars
    val density = LocalDensity.current
    return this.layout { measurable, constraints ->
        val bottom = if (skip()) 0 else imeBottomPadding(ime.getBottom(density), nav.getBottom(density), false)
        val placeable = measurable.measure(constraints.offset(vertical = -bottom))
        layout(constraints.constrainWidth(placeable.width), constraints.constrainHeight(placeable.height + bottom)) { placeable.place(0, 0) }
    }
}
