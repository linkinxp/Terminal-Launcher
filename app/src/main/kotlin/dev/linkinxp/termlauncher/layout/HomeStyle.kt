package dev.linkinxp.termlauncher.layout

/** How a home page is arranged: the original top-down list, content anchored to the bottom, a two-column bottom text grid, or a four-column grid of icons. */
enum class HomeStyle {
    CLASSIC, BOTTOM, GRID, ICONS;

    companion object {
        /** What a phone gets until the user picks a style: the icon grid. */
        val DEFAULT = ICONS

        fun fromName(name: String?): HomeStyle = entries.firstOrNull { it.name == name } ?: CLASSIC

        /** Reads a stored style: nothing stored (or an unknown name) means the default. */
        fun fromStored(name: String?): HomeStyle = entries.firstOrNull { it.name == name } ?: DEFAULT
    }
}

/** What `layout <name>` does to the page contents besides the style. */
enum class Restructure { NONE, APPS_FIRST, CLASSIC }

class LayoutPlan(val style: HomeStyle?, val restructure: Restructure, val message: String) {
    override fun equals(other: Any?) = other is LayoutPlan && style == other.style && restructure == other.restructure && message == other.message
    override fun hashCode() = (style.hashCode() * 31 + restructure.hashCode()) * 31 + message.hashCode()
}

private const val LAYOUT_USAGE = "usage: layout classic|bottom|grid|icons|apps-first"

/** `layout classic|bottom|grid|apps-first`; anything else is a usage error that changes nothing. */
fun layoutPlan(arg: String?): LayoutPlan = when (arg?.trim()?.lowercase()) {
    "classic" -> LayoutPlan(HomeStyle.CLASSIC, Restructure.CLASSIC, "layout: classic")
    "bottom" -> LayoutPlan(HomeStyle.BOTTOM, Restructure.NONE, "layout: bottom")
    "grid" -> LayoutPlan(HomeStyle.GRID, Restructure.NONE, "layout: grid")
    "icons" -> LayoutPlan(HomeStyle.ICONS, Restructure.APPS_FIRST, "layout: icons (icon grid, glance page)")
    "apps-first" -> LayoutPlan(HomeStyle.GRID, Restructure.APPS_FIRST, "layout: apps-first (apps page, glance page)")
    else -> LayoutPlan(null, Restructure.NONE, LAYOUT_USAGE)
}
