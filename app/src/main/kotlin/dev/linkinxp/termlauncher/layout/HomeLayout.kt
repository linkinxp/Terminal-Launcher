package dev.linkinxp.termlauncher.layout

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
sealed interface Item {
    @Serializable @SerialName("app") data class App(val pkg: String) : Item
    @Serializable @SerialName("folder") data class Folder(val name: String, val apps: List<String>) : Item
}

@Serializable data class Page(val items: List<Item>)

@Serializable
data class HomeLayout(val dock: List<Item>, val pages: List<Page>) {

    fun folders(): List<Item.Folder> = (dock + pages.flatMap { it.items }).filterIsInstance<Item.Folder>()

    fun pin(pkg: String): HomeLayout = when {
        dock.size >= MAX_DOCK || dock.contains(Item.App(pkg)) -> this
        else -> copy(dock = dock + Item.App(pkg))
    }

    fun unpin(pkg: String): HomeLayout = copy(dock = dock - Item.App(pkg))

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
        name.lowercase() in COMMAND_WORDS -> "$cmd: $name: reserved word"
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
            }
        }
        return copy(dock = clean(dock), pages = pages.map { Page(clean(it.items)) })
    }

    fun encode(): String = json.encodeToString(Envelope.serializer(), Envelope(VERSION, this))

    companion object {
        const val MAX_DOCK = 5
        const val MAX_PAGES = 5
        const val MAX_FOLDER_NAME = 24
        private const val VERSION = 1
        private val COMMAND_WORDS = setOf("ls", "theme", "pin", "hide", "mkdir", "mv", "rmdir", "help")
        private val json = Json { encodeDefaults = true; ignoreUnknownKeys = true }

        fun default(): HomeLayout = HomeLayout(emptyList(), listOf(Page(emptyList())))

        fun decode(raw: String?): HomeLayout {
            if (raw == null) return default()
            return try {
                val env = json.decodeFromString(Envelope.serializer(), raw)
                if (env.v == VERSION && env.layout.pages.isNotEmpty()) env.layout else default()
            } catch (e: Exception) {
                default()
            }
        }
    }
}

@Serializable private data class Envelope(val v: Int, val layout: HomeLayout)
