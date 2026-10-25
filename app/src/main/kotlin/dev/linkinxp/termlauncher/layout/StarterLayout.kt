package dev.linkinxp.termlauncher.layout

private val STARTER_DOCK = listOf("com.android.chrome", "com.android.vending", "com.android.settings")
private val STARTER_HOME = listOf(
    "com.google.android.apps.photos", "com.google.android.gm", "com.google.android.youtube",
    "com.google.android.apps.maps", "com.google.android.calendar", "com.google.android.apps.messaging",
)

/**
 * The home a new install starts with: a few familiar apps in the dock and on the first page (only those installed),
 * next to the usual glance page. Null when a layout was already stored or none of the starter apps is installed.
 */
fun seededLayout(storedRaw: String?, installed: Set<String>): HomeLayout? {
    if (storedRaw != null) return null
    val dock = STARTER_DOCK.filter { it in installed }
    val home = STARTER_HOME.filter { it in installed }
    if (dock.isEmpty() && home.isEmpty()) return null
    val base = HomeLayout.fresh()
    return base.copy(
        dock = dock.map { Item.App(it) },
        pages = base.pages.mapIndexed { i, p -> if (i == 0) Page(home.map { Item.App(it) }) else p },
    )
}
