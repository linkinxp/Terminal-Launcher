package dev.linkinxp.termlauncher.layout

import kotlinx.serialization.Serializable

@Serializable
enum class WidgetSize(val baseDp: Int) { SMALL(96), MEDIUM(192), LARGE(288) }

private const val MAX_WIDGET_DP = 400

/** Panel height for a widget: the size's base height (times the density [scale]), or the provider's own minimum if that is larger (cap 400). */
fun widgetHeightDp(providerMinHeightDp: Int, size: WidgetSize, scale: Float = 1f): Int =
    maxOf(providerMinHeightDp, (size.baseDp * scale).toInt()).coerceAtMost(MAX_WIDGET_DP)
