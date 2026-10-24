package dev.linkinxp.termlauncher.quick

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.MediaStore

/** The apps Android itself uses for dialing, texting, taking pictures and maps: the first-run `quick` tiles. */
class QuickDefaults(private val context: Context) {
    fun packages(): List<String> {
        val pm = context.packageManager
        val intents = listOf(
            Intent(Intent.ACTION_DIAL, Uri.parse("tel:")),
            Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:")),
            Intent(MediaStore.ACTION_IMAGE_CAPTURE),
            Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=coffee")),
        )
        return intents.mapNotNull { intent ->
            try { pm.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)?.activityInfo?.packageName } catch (_: Exception) { null }
        }
            // "android" is the system chooser, i.e. there is no single default for that action.
            .filter { it != "android" && it != context.packageName }
            .distinct()
    }
}
