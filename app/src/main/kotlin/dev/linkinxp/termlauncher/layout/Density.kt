package dev.linkinxp.termlauncher.layout

/**
 * How much fits on the screen: the icon grid's columns, the icon, dock and quick-icon sizes, the label size and how
 * tall widgets are drawn. [NORMAL] is the size everything had before this setting existed.
 */
enum class Density(val columns: Int, val iconDp: Float, val labelSp: Float, val dockDp: Float, val quickDp: Float, val widgetScale: Float) {
    DENSE(6, 44f, 10f, 40f, 36f, 0.7f),
    COMPACT(5, 50f, 10f, 42f, 38f, 0.85f),
    NORMAL(4, 56f, 11f, 44f, 40f, 1f),
    ROOMY(3, 64f, 12f, 48f, 44f, 1.15f);

    companion object {
        fun fromName(name: String?): Density = entries.firstOrNull { it.name == name } ?: NORMAL
    }
}

class DensityPlan(val density: Density?, val message: String) {
    override fun equals(other: Any?) = other is DensityPlan && density == other.density && message == other.message
    override fun hashCode() = density.hashCode() * 31 + message.hashCode()
}

private const val DENSITY_USAGE = "usage: density dense|compact|normal|roomy"

/** `density dense|compact|normal|roomy`; anything else is a usage error that changes nothing. */
fun densityPlan(arg: String?): DensityPlan {
    val d = Density.entries.firstOrNull { it.name.equals(arg?.trim(), ignoreCase = true) } ?: return DensityPlan(null, DENSITY_USAGE)
    return DensityPlan(d, "density: ${d.name.lowercase()} (${d.columns} columns)")
}
