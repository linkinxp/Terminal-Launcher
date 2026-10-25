package dev.linkinxp.termlauncher.media

import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.provider.Settings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** One button of the toggles panel; [action] is the system screen it opens (null: the torch, handled in-app). */
data class ToggleSpec(val id: String, val label: String, val action: String?)

fun toggleSpecs(): List<ToggleSpec> = listOf(
    ToggleSpec("torch", "torch", null),
    ToggleSpec("wifi", "wifi", Settings.Panel.ACTION_INTERNET_CONNECTIVITY),
    ToggleSpec("bt", "bt", Settings.ACTION_BLUETOOTH_SETTINGS),
    ToggleSpec("sound", "sound", Settings.Panel.ACTION_VOLUME),
    ToggleSpec("location", "location", Settings.ACTION_LOCATION_SOURCE_SETTINGS),
)

fun torchLabel(on: Boolean) = if (on) "[torch on]" else "[torch]"

/** The flashlight: no permission needed; its state is followed through the camera service so other apps' changes show too. */
object Torch {
    private val _on = MutableStateFlow(false)
    val on: StateFlow<Boolean> = _on.asStateFlow()
    private var registered = false

    private fun cameraId(manager: CameraManager): String? = manager.cameraIdList.firstOrNull {
        val c = manager.getCameraCharacteristics(it)
        c.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true && c.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_BACK
    }

    fun register(context: Context) {
        if (registered) return
        registered = true
        try {
            context.getSystemService(CameraManager::class.java).registerTorchCallback(object : CameraManager.TorchCallback() {
                override fun onTorchModeChanged(id: String, enabled: Boolean) { _on.value = enabled }
            }, null)
        } catch (_: Exception) { registered = false }
    }

    fun toggle(context: Context): Boolean = try {
        val manager = context.getSystemService(CameraManager::class.java)
        val id = cameraId(manager)
        if (id != null) { manager.setTorchMode(id, !_on.value); true } else false
    } catch (_: Exception) { false }
}
