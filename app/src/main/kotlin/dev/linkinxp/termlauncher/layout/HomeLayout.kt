package dev.linkinxp.termlauncher.layout

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
sealed interface Item {
    @Serializable @SerialName("app") data class App(val pkg: String) : Item
    @Serializable @SerialName("folder") data class Folder(val name: String, val apps: List<String>) : Item

    /** A hosted Android widget: [id] is its AppWidgetHost id, [provider] the flattened ComponentName. */
    @Serializable @SerialName("widget") data class Widget(val id: Int, val provider: String, val size: WidgetSize) : Item

    /** One of the built-in info panels (search, sys, calendar, weather) placed on a page. */
    @Serializable @SerialName("panel") data class Panel(val kind: PanelKind) : Item
}

@Serializable enum class PanelKind { SEARCH, SYS, CAL, WX }

@Serializable data class Page(val items: List<Item>)

@Serializable
data class HomeLayout(val dock: List<Item>, val pages: List<Page>) {

    /** The same layout without any hosted widgets (their host ids are only meaningful on this install). */
    fun withoutWidgets(): HomeLayout =
        copy(dock = dock.filterNot { it is Item.Widget }, pages = pages.map { p -> Page(p.items.filterNot { it is Item.Widget }) })

    /**
     * This layout plus the hosted widgets of [old], each on the page with the same index (or the last page if it
     * no longer exists). Used when a restore/import replaces the layout, so configured widgets are not lost.
     */
    fun withWidgetsFrom(old: HomeLayout): HomeLayout {
        var merged = this
        old.pages.forEachIndexed { i, page ->
            page.items.filterIsInstance<Item.Widget>().forEach { w -> merged = merged.addWidget(i.coerceAtMost(merged.pages.lastIndex), w) }
        }
        return merged
    }

    fun widgetIds(): Set<Int> = pages.flatMap { it.items }.filterIsInstance<Item.Widget>().map { it.id }.toSet()

    /** Adds [widget] to [page]; a missing page or an id that is already on a page leaves the layout unchanged. */
    fun addWidget(page: Int, widget: Item.Widget): HomeLayout =
        if (widget.id in widgetIds()) this else addToPage(page, widget)

    fun removeWidget(id: Int): HomeLayout =
        copy(pages = pages.map { p -> Page(p.items.filterNot { it is Item.Widget && it.id == id }) })

    fun resizeWidget(id: Int, size: WidgetSize): HomeLayout = copy(
        pages = pages.map { p -> Page(p.items.map { if (it is Item.Widget && it.id == id) it.copy(size = size) else it }) },
    )

    /** Drops widgets whose provider is no longer installed; also returns their ids so the host can delete them. */
    fun pruneWidgets(providers: Set<String>): Pair<HomeLayout, List<Int>> {
        val gone = pages.flatMap { it.items }.filterIsInstance<Item.Widget>().filter { it.provider !in providers }
        val pruned = copy(pages = pages.map { p -> Page(p.items.filterNot { it is Item.Widget && it.provider !in providers }) })
        return pruned to gone.map { it.id }
    }

    fun folders(): List<Item.Folder> = (dock + pages.flatMap { it.items }).filterIsInstance<Item.Folder>()

    fun pin(pkg: String): HomeLayout = when {
        dock.size >= MAX_DOCK || dock.contains(Item.App(pkg)) -> this
        else -> copy(dock = dock + Item.App(pkg))
    }

    fun unpin(pkg: String): HomeLayout = copy(dock = dock - Item.App(pkg))

    /** The panel kinds placed on some page. */
    fun placedPanels(): Set<PanelKind> = pages.flatMap { it.items }.filterIsInstance<Item.Panel>().map { it.kind }.toSet()

    /** Puts [kind] on top of [page] (a panel lives on one page only); a missing page changes nothing. */
    fun addPanel(page: Int, kind: PanelKind): HomeLayout {
        if (page !in pages.indices) return this
        val cleared = removePanel(kind)
        return cleared.copy(pages = cleared.pages.mapIndexed { i, p -> if (i == page) Page(listOf<Item>(Item.Panel(kind)) + p.items) else p })
    }

    fun removePanel(kind: PanelKind): HomeLayout =
        copy(pages = pages.map { p -> Page(p.items.filterNot { it == Item.Panel(kind) }) })

    /** Swaps the panel with the item above ([delta] -1) or below (+1) it on its page; at the ends nothing changes. */
    fun movePanel(kind: PanelKind, delta: Int): HomeLayout {
        val page = pages.indexOfFirst { p -> p.items.any { it == Item.Panel(kind) } }
        if (page < 0) return this
        val items = pages[page].items.toMutableList()
        val from = items.indexOf(Item.Panel(kind))
        val to = from + delta.coerceIn(-1, 1)
        if (to !in items.indices) return this
        items[to] = items.set(from, items[to])
        return copy(pages = pages.mapIndexed { i, p -> if (i == page) Page(items) else p })
    }

    fun movePanelToPage(kind: PanelKind, page: Int): HomeLayout =
        if (kind in placedPanels() && page in pages.indices) addPanel(page, kind) else this

    /** The four panels on top of page one, the way they used to be fixed. */
    fun withDefaultPanels(): HomeLayout =
        copy(pages = pages.mapIndexed { i, p -> if (i == 0) Page(DEFAULT_PANELS + p.items) else p })

    /**
     * Moves the panels off page one onto a page of their own right after it (the "glance" page); when all five pages
     * exist they go on top of the current second page. Nothing changes when page one has no panels.
     */
    fun appsFirst(): HomeLayout {
        val moving = pages[0].items.filterIsInstance<Item.Panel>()
        if (moving.isEmpty()) return this
        val rest = pages[0].copy(items = pages[0].items.filterNot { it is Item.Panel })
        val tail = pages.drop(1)
        return if (pages.size < MAX_PAGES) copy(pages = listOf(rest, Page(moving)) + tail)
        else copy(pages = listOf(rest, Page(moving + tail[0].items)) + tail.drop(1))
    }

    /** The reverse: every panel back on top of page one in the original order; pages emptied by that disappear. */
    fun panelsHome(): HomeLayout {
        val order = listOf(PanelKind.SEARCH, PanelKind.SYS, PanelKind.CAL, PanelKind.WX)
        val placed = placedPanels()
        if (placed.isEmpty()) return this
        val cleaned = pages.mapIndexedNotNull { i, p ->
            val items = p.items.filterNot { it is Item.Panel }
            if (items.isEmpty() && i > 0 && p.items.any { it is Item.Panel }) null else Page(items)
        }
        val top = order.filter { it in placed }.map { Item.Panel(it) }
        return copy(pages = cleaned.mapIndexed { i, p -> if (i == 0) Page(top + p.items) else p })
    }

    fun addPage(): HomeLayout = if (pages.size >= MAX_PAGES) this else copy(pages = pages + Page(emptyList()))

    fun removePage(index: Int): HomeLayout =
        if (pages.size <= 1 || index !in pages.indices) this else copy(pages = pages.filterIndexed { i, _ -> i != index })

    fun movePage(from: Int, to: Int): HomeLayout {
        if (from !in pages.indices || to !in pages.indices) return this
        val list = pages.toMutableList()
        list.add(to, list.removeAt(from))
        return copy(pages = list)
    }

    fun addToPage(page: Int, item: Item): HomeLayout {
        if (page !in pages.indices) return this
        return copy(pages = pages.mapIndexed { i, p -> if (i == page) Page(p.items + item) else p })
    }

    fun mkdir(name: String, page: Int = 0): Result<HomeLayout> {
        val error = if (page !in pages.indices) "mkdir: no such page" else nameError(name, "mkdir", except = null)
        return if (error != null) Result.failure(IllegalArgumentException(error))
        else Result.success(addToPage(page, Item.Folder(name, emptyList())))
    }

    /** Renames a folder in place (dock or page); a case-only change of its own name is allowed. */
    fun renameFolder(old: String, new: String): Result<HomeLayout> {
        val folder = folders().firstOrNull { it.name.equals(old, ignoreCase = true) }
            ?: return Result.failure(IllegalArgumentException("mv: $old: no such folder"))
        nameError(new, "mv", except = folder.name)?.let { return Result.failure(IllegalArgumentException(it)) }
        fun rename(items: List<Item>) = items.map { if (it == folder) folder.copy(name = new) else it }
        return Result.success(copy(dock = rename(dock), pages = pages.map { Page(rename(it.items)) }))
    }

    private fun nameError(name: String, cmd: String, except: String?): String? = when {
        name.isBlank() || name.any { it.isWhitespace() || it == '/' } || name.length > MAX_FOLDER_NAME ->
            "$cmd: '$name': invalid name"
        name.lowercase() in RESERVED_WORDS -> "$cmd: $name: reserved word"
        folders().any { it.name.equals(name, ignoreCase = true) && !it.name.equals(except, ignoreCase = true) } ->
            "$cmd: $name: already exists"
        else -> null
    }

    fun mv(pkg: String, folder: String): Result<HomeLayout> {
        val target = folders().firstOrNull { it.name.equals(folder, ignoreCase = true) }
            ?: return Result.failure(IllegalArgumentException("mv: $folder: no such folder"))
        fun clean(items: List<Item>, dropLoose: Boolean) = items.mapNotNull { item ->
            when {
                item is Item.App && dropLoose && item.pkg == pkg -> null
                item is Item.Folder -> {
                    val apps = item.apps - pkg
                    Item.Folder(item.name, if (item.name == target.name) apps + pkg else apps)
                }
                else -> item
            }
        }
        return Result.success(copy(dock = clean(dock, dropLoose = false), pages = pages.map { Page(clean(it.items, true)) }))
    }

    /** Takes [pkg] out of whichever folder holds it and puts it on the first page as a plain shortcut. */
    fun unfile(pkg: String): HomeLayout {
        if (folders().none { pkg in it.apps }) return this
        fun clean(items: List<Item>) = items.map { if (it is Item.Folder) it.copy(apps = it.apps - pkg) else it }
        return copy(dock = clean(dock), pages = pages.map { Page(clean(it.items)) }).addToPage(0, Item.App(pkg))
    }

    /** Removes the plain shortcut for [pkg] from every page (the dock and folders are untouched). */
    fun removeShortcut(pkg: String): HomeLayout =
        copy(pages = pages.map { p -> Page(p.items.filterNot { it == Item.App(pkg) }) })

    fun pinFolder(name: String): HomeLayout {
        val folder = pages.flatMap { it.items }.filterIsInstance<Item.Folder>()
            .firstOrNull { it.name.equals(name, ignoreCase = true) } ?: return this
        if (dock.size >= MAX_DOCK) return this
        return copy(dock = dock + folder, pages = pages.map { p -> Page(p.items.filterNot { it == folder }) })
    }

    fun unpinFolder(name: String): HomeLayout {
        val folder = dock.filterIsInstance<Item.Folder>().firstOrNull { it.name.equals(name, ignoreCase = true) } ?: return this
        return copy(dock = dock.filterNot { it == folder }).addToPage(0, folder)
    }

    fun rmdir(name: String): HomeLayout {
        val folder = folders().firstOrNull { it.name.equals(name, ignoreCase = true) } ?: return this
        val without = copy(
            dock = dock.filterNot { it == folder },
            pages = pages.map { p -> Page(p.items.filterNot { it == folder }) },
        )
        return folder.apps.fold(without) { l, pkg -> l.addToPage(0, Item.App(pkg)) }
    }

    fun prune(installed: Set<String>): HomeLayout {
        fun clean(items: List<Item>) = items.mapNotNull { item ->
            when (item) {
                is Item.App -> item.takeIf { it.pkg in installed }
                is Item.Folder -> item.copy(apps = item.apps.filter { it in installed })
                is Item.Widget -> item
                is Item.Panel -> item
            }
        }
        return copy(dock = clean(dock), pages = pages.map { Page(clean(it.items)) })
    }

    fun encode(): String = json.encodeToString(Envelope.serializer(), Envelope(VERSION, this))

    companion object {
        const val MAX_DOCK = 5
        const val MAX_PAGES = 5
        const val MAX_FOLDER_NAME = 24
        private const val VERSION = 2
        private val DEFAULT_PANELS: List<Item> = listOf(PanelKind.SEARCH, PanelKind.SYS, PanelKind.CAL, PanelKind.WX).map { Item.Panel(it) }
        val RESERVED_WORDS = setOf(
            "ls", "theme", "pin", "hide", "mkdir", "mv", "rmdir", "help",
            "fx", "history", "neofetch", "whoami", "uptime", "date", "fortune", "clear", "sudo", "matrix", "wall", "panel", "wx", "settings", "quick", "clock", "pill", "layout",
        )
        private val json = Json { encodeDefaults = true; ignoreUnknownKeys = true }

        fun default(): HomeLayout = HomeLayout(emptyList(), listOf(Page(emptyList())))

        /** A new home: an empty apps page, then the glance page with the four info panels. */
        fun fresh(): HomeLayout = default().withDefaultPanels().appsFirst()

        fun decode(raw: String?): HomeLayout {
            if (raw == null) return fresh()
            return try {
                val env = json.decodeFromString(Envelope.serializer(), raw)
                when {
                    env.layout.pages.isEmpty() -> fresh()
                    env.v == VERSION -> env.layout
                    env.v == 1 -> env.layout.withDefaultPanels() // panels used to be fixed above everything else
                    else -> fresh()
                }
            } catch (e: Exception) {
                fresh()
            }
        }
    }
}

@Serializable private data class Envelope(val v: Int, val layout: HomeLayout)
