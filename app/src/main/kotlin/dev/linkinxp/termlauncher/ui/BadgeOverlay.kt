package dev.linkinxp.termlauncher.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.linkinxp.termlauncher.badges.BadgeStore
import dev.linkinxp.termlauncher.badges.badgeLabel
import dev.linkinxp.termlauncher.badges.folderBadge

/** An icon with a small count badge in its top-right corner when its app(s) have notifications and badges are on. */
@Composable
fun BadgedIcon(pkgs: List<String>, enabled: Boolean, tag: String, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Box(modifier) {
        content()
        if (enabled) BadgeDot(pkgs, tag, Modifier.align(Alignment.TopEnd))
    }
}

@Composable
private fun BadgeDot(pkgs: List<String>, tag: String, modifier: Modifier) {
    val theme = LocalTheme.current
    val counts by BadgeStore.counts.collectAsState()
    val n = remember(pkgs, counts) { folderBadge(pkgs, counts) }
    if (n <= 0) return
    Box(modifier.testTag(tag).background(theme.accent.color(), RoundedCornerShape(8.dp)).padding(horizontal = 4.dp)) {
        TermText(badgeLabel(n), size = 10.sp, bold = true, color = theme.background.color())
    }
}
