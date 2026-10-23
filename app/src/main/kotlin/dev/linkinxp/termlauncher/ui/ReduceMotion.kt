package dev.linkinxp.termlauncher.ui

import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

/** True when the system animator duration scale is 0 (animations off): motion effects must be skipped. */
@Composable
fun rememberReduceMotion(): Boolean {
    val resolver = LocalContext.current.contentResolver
    return remember(resolver) {
        try {
            Settings.Global.getFloat(resolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
        } catch (_: Exception) {
            false
        }
    }
}
