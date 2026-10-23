package dev.linkinxp.termlauncher.prompt

sealed interface Command {
    data class Theme(val name: String) : Command
    data class Pin(val app: String) : Command
    data class Hide(val app: String) : Command
    data class Mkdir(val folder: String) : Command
    data class Mv(val app: String, val folder: String) : Command
    data class Rmdir(val folder: String) : Command
    data class Ls(val hidden: Boolean) : Command
    data object Help : Command
    data class Query(val text: String) : Command
}

fun parseCommand(input: String): Command {
    val text = input.trim()
    val word = text.substringBefore(' ').lowercase()
    val args = text.substringAfter(' ', "").trim()
    return when {
        word == "help" && args.isEmpty() -> Command.Help
        word == "ls" && args.isEmpty() -> Command.Ls(false)
        word == "ls" && args == "-a" -> Command.Ls(true)
        word == "theme" && args.isNotEmpty() -> Command.Theme(args)
        word == "pin" && args.isNotEmpty() -> Command.Pin(args)
        word == "hide" && args.isNotEmpty() -> Command.Hide(args)
        word == "mkdir" && args.isNotEmpty() -> Command.Mkdir(args)
        word == "rmdir" && args.isNotEmpty() -> Command.Rmdir(args)
        word == "mv" && args.contains(' ') ->
            Command.Mv(args.substringBeforeLast(' ').trim(), args.substringAfterLast(' '))
        else -> Command.Query(text)
    }
}
