package dev.linkinxp.termlauncher.prompt

sealed interface Command {
    data class Theme(val name: String) : Command
    data class Pin(val app: String) : Command
    data class Hide(val app: String) : Command
    data class Mkdir(val folder: String) : Command
    data class Mv(val app: String, val folder: String) : Command
    data class Rmdir(val folder: String) : Command
    data class Ls(val hidden: Boolean) : Command
    data class Fx(val name: String?, val on: Boolean?) : Command
    data class Sudo(val rest: String) : Command
    data class Wall(val sub: String?, val arg: String?) : Command
    data object History : Command
    data object Neofetch : Command
    data object Whoami : Command
    data object Uptime : Command
    data object Date : Command
    data object Fortune : Command
    data object Clear : Command
    data object Matrix : Command
    data object Help : Command
    data class Query(val text: String) : Command
}

fun parseCommand(input: String): Command {
    val text = input.trim()
    val word = text.substringBefore(' ').lowercase()
    val args = text.substringAfter(' ', "").trim()
    return when {
        word == "help" && args.isEmpty() -> Command.Help
        word == "fx" -> parseFx(args)
        word == "wall" -> parseWall(args)
        word == "sudo" && args.isNotEmpty() -> Command.Sudo(args)
        args.isEmpty() && word in SIMPLE -> SIMPLE.getValue(word)
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

private val SIMPLE: Map<String, Command> = mapOf(
    "history" to Command.History,
    "neofetch" to Command.Neofetch,
    "whoami" to Command.Whoami,
    "uptime" to Command.Uptime,
    "date" to Command.Date,
    "fortune" to Command.Fortune,
    "clear" to Command.Clear,
    "matrix" to Command.Matrix,
)

private fun parseFx(args: String): Command.Fx {
    if (args.isEmpty()) return Command.Fx(null, null)
    val tokens = args.split(Regex("\\s+"))
    val on = when (tokens.getOrNull(1)?.lowercase()) { "on" -> true; "off" -> false; else -> null }
    return Command.Fx(tokens[0].lowercase(), on)
}

private fun parseWall(args: String): Command.Wall {
    if (args.isEmpty()) return Command.Wall(null, null)
    val tokens = args.split(Regex("\\s+"))
    return Command.Wall(tokens[0].lowercase(), tokens.getOrNull(1))
}
