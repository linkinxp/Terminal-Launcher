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

    /** An empty slot in the icon grid: lets an icon be placed anywhere, with holes around it. */
    @Serializable @SerialName("gap") data object Gap : Item
}

@Serializable enum class PanelKind { SEARCH, SYS, CAL, WX, MEDIA, TOGGLES }

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

    /** Removes the widgets with these host ids (ones a restored backup brought along that this phone never created). */
    fun dropWidgets(ids: Set<Int>): HomeLayout =
        if (ids.isEmpty()) this else copy(pages = pages.map { p -> Page(p.items.filterNot { it is Item.Widget && it.id in ids }) })

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

    /**
     * Drops app [pkg] on slot [slot] of [page] (from the drawer): an occupied slot inserts it, a hole is taken, past the end
     * pads with holes. An app that is already on a page or in the dock is moved, never duplicated.
     */
    fun addAt(page: Int, pkg: String, slot: Int): HomeLayout {
        val app = Item.App(pkg)
        if (page !in pages.indices || slot < 0) return this
        val cleared = copy(
            dock = dock - app,
            pages = pages.map { p -> if (app in p.items) Page(trimmed(p.items.map { if (it == app) Item.Gap else it }.toMutableList())) else p },
        )
        val items = cleared.pages[page].items.toMutableList()
        if (slot > items.size + MAX_PAD) return this
        when {
            slot >= items.size -> { while (items.size < slot) items.add(Item.Gap); items.add(app) }
            items[slot] == Item.Gap -> items[slot] = app
            else -> items.add(slot, app)
        }
        return cleared.copy(pages = cleared.pages.mapIndexed { i, p -> if (i == page) Page(items) else p })
    }

    /** `folder`, or `folder2`, `folder3`... the first name no folder has yet. */
    fun newFolderName(): String =
        generateSequence(1) { it + 1 }.map { if (it == 1) "folder" else "folder$it" }.first { nameError(it, "mkdir", except = null) == null }

    /**
     * An app dropped right on another icon: two apps become a new folder where the target was, a folder takes the app in.
     * The dragged app leaves its old slot (a hole, so nothing shifts). Anything else (self, empty slot, bad page) changes nothing.
     */
    fun mergeOnto(fromPage: Int, dragged: Item.App, toPage: Int, slot: Int): HomeLayout {
        if (fromPage !in pages.indices || toPage !in pages.indices) return this
        val target = pages[toPage].items.getOrNull(slot) ?: return this
        if (target == dragged || (target !is Item.App && target !is Item.Folder) || dragged !in pages[fromPage].items) return this
        val merged: Item = when (target) {
            is Item.App -> Item.Folder(newFolderName(), listOf(target.pkg, dragged.pkg))
            is Item.Folder -> if (dragged.pkg in target.apps) target else target.copy(apps = target.apps + dragged.pkg)
            else -> return this
        }
        return copy(pages = pages.mapIndexed { i, p ->
            val items = p.items.toMutableList()
            if (i == fromPage) items[items.indexOf(dragged)] = Item.Gap
            if (i == toPage) items[slot] = merged
            Page(trimmed(items))
        })
    }

    /** Puts an app on the first page without panels (the apps page); nothing changes when it is already docked or on a page. */
    fun addToHome(pkg: String): HomeLayout {
        val app = Item.App(pkg)
        if (app in dock || pages.any { app in it.items }) return this
        val target = pages.indexOfFirst { p -> p.items.none { it is Item.Panel } }.coerceAtLeast(0)
        return addToPage(target, app)
    }

    /** Why [pin] would do nothing, in plain words. */
    fun pinProblem(pkg: String, label: String): String =
        if (dock.contains(Item.App(pkg))) "${label.lowercase()} is already in the dock" else "dock is full ($MAX_DOCK): unpin one first"

    /**
     * A full page hands its last icon (app or folder) to the front of the next page without panels or widgets, adding a page
     * when there is none and room for one. Unchanged when the page has no icon to give or nowhere to put it.
     */
    fun spillPage(page: Int): HomeLayout {
        if (page !in pages.indices) return this
        val items = pages[page].items
        val at = items.indexOfLast { it is Item.App || it is Item.Folder }
        if (at < 0 || items.drop(at + 1).any { it !is Item.Gap }) return this
        val icon = items[at]
        val rest = trimmed(items.take(at).toMutableList())
        fun plain(i: Int) = pages[i].items.none { it is Item.Panel || it is Item.Widget }
        val target = (page + 1 until pages.size).firstOrNull(::plain)
        val moved = when {
            target != null -> pages.mapIndexed { i, p -> if (i == target) Page(listOf(icon) + p.items) else if (i == page) Page(rest) else p }
            pages.size < MAX_PAGES -> pages.mapIndexed { i, p -> if (i == page) Page(rest) else p } + Page(listOf(icon))
            else -> return this
        }
        return copy(pages = moved)
    }

    /** Reorders the apps of folder [name] (on a page or in the dock): the app at [from] goes to position [to]. */
    fun moveInFolder(name: String, from: Int, to: Int): HomeLayout {
        val folder = folders().firstOrNull { it.name == name } ?: return this
        if (from !in folder.apps.indices || to !in folder.apps.indices || from == to) return this
        val apps = folder.apps.toMutableList().also { it.add(to, it.removeAt(from)) }
        fun fix(items: List<Item>) = items.map { if (it == folder) folder.copy(apps = apps) else it }
        return copy(dock = fix(dock), pages = pages.map { Page(fix(it.items)) })
    }

    /** Moves the pinned item at [from] to position [to]; anything out of range or unchanged leaves the dock alone. */
    fun moveInDock(from: Int, to: Int): HomeLayout {
        if (from !in dock.indices || to !in dock.indices || from == to) return this
        val list = dock.toMutableList()
        list.add(to, list.removeAt(from))
        return copy(dock = list)
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
        val order = PanelKind.entries
        val placed = placedPanels()
        if (placed.isEmpty()) return this
        val cleaned = pages.mapIndexedNotNull { i, p ->
            val items = p.items.filterNot { it is Item.Panel }
            if (items.isEmpty() && i > 0 && p.items.any { it is Item.Panel }) null else Page(items)
        }
        val top = order.filter { it in placed }.map { Item.Panel(it) }
        return copy(pages = cleaned.mapIndexed { i, p -> if (i == 0) Page(top + p.items) else p })
    }

    /**
     * Puts [item] in grid slot [slot] of [page] (a drag and drop). An empty slot is simply taken (the old slot becomes a
     * hole, so nothing else moves); an occupied one gets the item inserted there; past the end the gap is padded with holes.
     * Trailing holes are dropped. An item that is not on the page, its own slot or an absurd slot change nothing.
     */
    fun placeItem(page: Int, item: Item, slot: Int): HomeLayout {
        if (page !in pages.indices || item == Item.Gap) return this
        val items = pages[page].items.toMutableList()
        val from = items.indexOf(item)
        if (from < 0 || slot < 0 || slot == from || slot > items.size + MAX_PAD) return this
        when {
            slot >= items.size -> { items[from] = Item.Gap; while (items.size < slot) items.add(Item.Gap); items.add(item) } // past the end: holes in between
            items[slot] == Item.Gap -> { items[slot] = item; items[from] = Item.Gap }                                         // an empty slot: take it, leave a hole
            else -> items.add(slot, items.removeAt(from))                                                                      // an occupied slot: insert, the rest shift
        }
        while (items.lastOrNull() == Item.Gap) items.removeAt(items.lastIndex)
        return copy(pages = pages.mapIndexed { i, p -> if (i == page) Page(items) else p })
    }

    private fun trimmed(items: MutableList<Item>): List<Item> {
        while (items.lastOrNull() == Item.Gap) items.removeAt(items.lastIndex)
        return items
    }

    /** Moves [item] from one page to slot [slot] of another (a drag across pages); the old slot becomes a hole. */
    fun moveItemToPage(fromPage: Int, item: Item, toPage: Int, slot: Int): HomeLayout {
        if (fromPage == toPage || fromPage !in pages.indices || toPage !in pages.indices || item == Item.Gap || slot < 0) return this
        val src = pages[fromPage].items.toMutableList()
        val at = src.indexOf(item)
        val dst = pages[toPage].items.toMutableList()
        if (at < 0 || slot > dst.size + MAX_PAD) return this
        src[at] = Item.Gap
        when {
            slot >= dst.size -> { while (dst.size < slot) dst.add(Item.Gap); dst.add(item) }
            dst[slot] == Item.Gap -> dst[slot] = item
            else -> dst.add(slot, item)
        }
        return copy(pages = pages.mapIndexed { i, p -> when (i) { fromPage -> Page(trimmed(src)); toPage -> Page(trimmed(dst)); else -> p } })
    }

    /** Pins [item] (an app or a folder) to the dock by moving it off [page]; a full dock or a duplicate changes nothing. */
    fun moveToDock(page: Int, item: Item): HomeLayout {
        if (page !in pages.indices || (item !is Item.App && item !is Item.Folder) || dock.size >= MAX_DOCK || item in dock) return this
        val src = pages[page].items.toMutableList()
        val at = src.indexOf(item)
        if (at < 0) return this
        src[at] = Item.Gap
        return copy(dock = dock + item, pages = pages.mapIndexed { i, p -> if (i == page) Page(trimmed(src)) else p })
    }

    fun addPage(): HomeLayout = if (pages.size >= MAX_PAGES) this else copy(pages = pages + Page(emptyList()))

    /** Removes a page; whatever was on it (apps, folders, widgets, panels) moves to the page before it, or the next one for page one. */
    fun removePage(index: Int): HomeLayout {
        if (pages.size <= 1 || index !in pages.indices) return this
        val moved = pages[index].items.filter { it != Item.Gap }
        val target = if (index > 0) index - 1 else 1
        return copy(pages = pages.mapIndexedNotNull { i, p ->
            when (i) {
                index -> null
                target -> Page(if (index > 0) p.items + moved else moved + p.items)
                else -> p
            }
        })
    }

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
                is Item.Gap -> item
            }
        }
        return copy(dock = clean(dock), pages = pages.map { Page(clean(it.items)) })
    }

    fun encode(): String = json.encodeToString(Envelope.serializer(), Envelope(VERSION, this))

    companion object {
        const val MAX_DOCK = 5
        const val MAX_PAGES = 5
        const val MAX_FOLDER_NAME = 24
        private const val MAX_PAD = 40
        private const val VERSION = 2
        private val DEFAULT_PANELS: List<Item> = listOf(PanelKind.SEARCH, PanelKind.SYS, PanelKind.CAL, PanelKind.WX).map { Item.Panel(it) }
        val RESERVED_WORDS = setOf(
            "ls", "theme", "pin", "hide", "mkdir", "mv", "rmdir", "help",
            "fx", "history", "neofetch", "whoami", "uptime", "date", "fortune", "clear", "sudo", "matrix", "wall", "panel", "wx", "settings", "quick", "clock", "pill", "layout", "density",
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
