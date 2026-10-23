package dev.linkinxp.termlauncher.weather

data class WxSettings(
    val city: String? = null,
    val lat: Double? = null,
    val lon: Double? = null,
    val fahrenheit: Boolean = false,
)

sealed interface WxAction {
    data object Refresh : WxAction
    data class Units(val fahrenheit: Boolean) : WxAction
    data class City(val name: String) : WxAction
}

/** Parses the `wx` subcommand (`wx`, `wx units c|f`, `wx city <name>`); failures carry the message to show. */
fun parseWxAction(sub: String?, arg: String?): Result<WxAction> {
    fun fail(msg: String) = Result.failure<WxAction>(IllegalArgumentException(msg))
    return when (sub?.lowercase()) {
        null -> Result.success(WxAction.Refresh)
        "units" -> when (arg?.trim()?.lowercase()) {
            "f" -> Result.success(WxAction.Units(true))
            "c" -> Result.success(WxAction.Units(false))
            else -> fail("usage: wx units c|f")
        }
        "city" -> arg?.trim()?.takeIf { it.isNotEmpty() }?.let { Result.success(WxAction.City(it)) } ?: fail("usage: wx city <name>")
        else -> fail("wx: $sub: unknown (city units)")
    }
}
