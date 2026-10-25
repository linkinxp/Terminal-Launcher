package dev.linkinxp.termlauncher.home

import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import dev.linkinxp.termlauncher.theme.Theme
import dev.linkinxp.termlauncher.ui.DialogSurface
import dev.linkinxp.termlauncher.ui.TapText
import dev.linkinxp.termlauncher.ui.TermPanel
import dev.linkinxp.termlauncher.ui.TermText

/** Ask once, and only while another launcher is still the home app. */
fun shouldAskDefaultHome(isHome: Boolean, alreadyAsked: Boolean): Boolean = !isHome && !alreadyAsked

fun isDefaultHome(context: Context): Boolean = try {
    context.getSystemService(RoleManager::class.java)?.isRoleHeld(RoleManager.ROLE_HOME) == true
} catch (_: Exception) {
    false
}

/** Android's own "set as home app" prompt; the system home settings when that prompt is not available. */
fun homeRoleIntent(context: Context): Intent {
    val rm = context.getSystemService(RoleManager::class.java)
    return if (rm != null && rm.isRoleAvailable(RoleManager.ROLE_HOME)) rm.createRequestRoleIntent(RoleManager.ROLE_HOME)
    else Intent(Settings.ACTION_HOME_SETTINGS)
}

@Composable
fun DefaultHomeDialog(theme: Theme, onYes: () -> Unit, onLater: () -> Unit) {
    Dialog(onDismissRequest = onLater) {
        Box(Modifier.padding(8.dp)) {
            DialogSurface(theme) {
                TermPanel("home app", Modifier.fillMaxWidth().padding(8.dp)) {
                    TermText("make terminal your home app?", maxLines = 2)
                    TermText("it replaces your current launcher; you can switch back any time in android settings", Modifier.padding(top = 6.dp), dim = true, size = 12.sp, maxLines = 4)
                    Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        TapText("[yes]", onYes, Modifier.testTag("default-home-yes"), bold = true)
                        TapText("[not now]", onLater, Modifier.testTag("default-home-later"), dim = true)
                    }
                }
            }
        }
    }
}
