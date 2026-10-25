package dev.linkinxp.termlauncher.prompt

import org.junit.Assert.assertEquals
import org.junit.Test

class CommandParserTest {
    @Test fun themeCommand() {
        assertEquals(Command.Theme("matrix"), parseCommand("theme matrix"))
    }

    @Test fun commandWordIsCaseInsensitiveAndTrimmedArgsKeepCase() {
        assertEquals(Command.Theme("Matrix"), parseCommand("  THEME  Matrix "))
    }

    @Test fun mvTakesLastTokenAsFolder() {
        assertEquals(Command.Mv("my app", "games"), parseCommand("mv my app games"))
    }

    @Test fun lsVariants() {
        assertEquals(Command.Ls(true), parseCommand("ls -a"))
        assertEquals(Command.Ls(false), parseCommand("ls"))
    }

    @Test fun helpCommand() {
        assertEquals(Command.Help, parseCommand("help"))
    }

    @Test fun pinHideMkdirRmdir() {
        assertEquals(Command.Pin("Chrome"), parseCommand("pin Chrome"))
        assertEquals(Command.Hide("Chrome"), parseCommand("hide Chrome"))
        assertEquals(Command.Mkdir("games"), parseCommand("mkdir games"))
        assertEquals(Command.Rmdir("games"), parseCommand("rmdir games"))
    }

    @Test fun commandWordWithoutArgsIsAQuery() {
        assertEquals(Command.Theme(null), parseCommand("theme")) // a bare theme lists the themes
        assertEquals(Command.Query("mv"), parseCommand("mv"))
    }

    @Test fun mvWithSingleArgIsAQuery() {
        assertEquals(Command.Query("mv games"), parseCommand("mv games"))
    }

    @Test fun plainTextAndEmptyAreQueries() {
        assertEquals(Command.Query("chrome"), parseCommand("chrome"))
        assertEquals(Command.Query(""), parseCommand(""))
        assertEquals(Command.Query(""), parseCommand("   "))
    }

    @Test fun fxVariants() {
        assertEquals(Command.Fx(null, null), parseCommand("fx"))
        assertEquals(Command.Fx("scanlines", true), parseCommand("fx scanlines on"))
        assertEquals(Command.Fx("scanlines", false), parseCommand("FX Scanlines OFF"))
        assertEquals(Command.Fx("scanlines", null), parseCommand("fx scanlines"))
        assertEquals(Command.Fx("scanlines", null), parseCommand("fx scanlines maybe"))
    }

    @Test fun argumentlessCommands() {
        assertEquals(Command.History, parseCommand("history"))
        assertEquals(Command.Neofetch, parseCommand("neofetch"))
        assertEquals(Command.Whoami, parseCommand("whoami"))
        assertEquals(Command.Uptime, parseCommand("uptime"))
        assertEquals(Command.Date, parseCommand("date"))
        assertEquals(Command.Fortune, parseCommand("fortune"))
        assertEquals(Command.Clear, parseCommand("clear"))
        assertEquals(Command.Matrix, parseCommand("matrix"))
    }

    @Test fun extraArgumentsMakeThemQueries() {
        assertEquals(Command.Query("date now"), parseCommand("date now"))
        assertEquals(Command.Query("clear all"), parseCommand("clear all"))
    }

    @Test fun sudoKeepsTheRestOfTheLine() {
        assertEquals(Command.Sudo("rm -rf /"), parseCommand("sudo rm -rf /"))
        assertEquals(Command.Query("sudo"), parseCommand("sudo"))
    }

    @Test fun wallVariants() {
        assertEquals(Command.Wall(null, null), parseCommand("wall"))
        assertEquals(Command.Wall("mode", "ascii"), parseCommand("wall mode ascii"))
        assertEquals(Command.Wall("dim", "40"), parseCommand("WALL Dim 40"))
        assertEquals(Command.Wall("style", "stars"), parseCommand("wall style stars extra"))
        assertEquals(Command.Wall("random", null), parseCommand("wall random"))
    }

    @Test fun panelVariants() {
        assertEquals(Command.Panel(null, null), parseCommand("panel"))
        assertEquals(Command.Panel("cal", true), parseCommand("panel cal on"))
        assertEquals(Command.Panel("wx", false), parseCommand("PANEL WX Off"))
        assertEquals(Command.Panel("cal", null), parseCommand("panel cal maybe"))
        assertEquals(Command.Panel("x", true), parseCommand("panel x on"))
    }

    @Test fun wxVariantsKeepTheRestOfTheLineAsTheArgument() {
        assertEquals(Command.Wx(null, null), parseCommand("wx"))
        assertEquals(Command.Wx("units", "f"), parseCommand("wx units f"))
        assertEquals(Command.Wx("city", "São Paulo"), parseCommand("wx city São Paulo"))
        assertEquals(Command.Wx("city", "new york"), parseCommand("WX City  new york "))
    }

    @Test fun fxAutoAndSettingsCommands() {
        assertEquals(Command.Fx("scanlines", null, auto = true), parseCommand("fx scanlines auto"))
        assertEquals(Command.Fx("glitch", null, auto = true), parseCommand("FX Glitch AUTO"))
        assertEquals(Command.Fx("scanlines", null), parseCommand("fx scanlines"))
        assertEquals(Command.Settings, parseCommand("settings"))
        assertEquals(Command.Query("settings x"), parseCommand("settings x"))
    }

    @Test fun quickCommands() {
        assertEquals(Command.Quick(null, null), parseCommand("quick"))
        assertEquals(Command.Quick("add", "google maps"), parseCommand("quick add Google Maps".lowercase()))
        assertEquals(Command.Quick("rm", "Chrome"), parseCommand("QUICK RM Chrome"))
        assertEquals(Command.Quick("clear", null), parseCommand("quick clear"))
    }

    @Test fun clockCommand() {
        assertEquals(Command.Clock(null, null), parseCommand("clock"))
        assertEquals(Command.Clock("12", null), parseCommand("clock 12"))
        assertEquals(Command.Clock("auto", null), parseCommand("CLOCK AUTO"))
        assertEquals(Command.Clock("style", "figlet"), parseCommand("clock style figlet"))
        assertEquals(Command.Clock("style", "nope now"), parseCommand("clock style nope now"))
    }

    @Test fun themeAddAndRemove() {
        assertEquals(Command.ThemeAdd(listOf("mine", "#000", "#0f0", "#0ff", "#066")), parseCommand("theme add mine #000 #0f0 #0ff #066"))
        assertEquals(Command.ThemeAdd(emptyList()), parseCommand("theme add"))
        assertEquals(Command.ThemeAdd(listOf("a", "b")), parseCommand("THEME  ADD   a   b"))
        assertEquals(Command.ThemeRm("mine"), parseCommand("theme rm mine"))
        assertEquals(Command.ThemeRm(""), parseCommand("theme rm"))
        assertEquals(Command.Theme("matrix"), parseCommand("theme matrix"))
    }

    @Test fun matrixEasterEggs() {
        assertEquals(Command.Matrix, parseCommand("matrix"))
        assertEquals(Command.Wake, parseCommand("matrix wake"))
        assertEquals(Command.Wake, parseCommand("MATRIX  Wake"))
        assertEquals(Command.Query("matrix foo"), parseCommand("matrix foo"))
        assertEquals(Command.Pill(null), parseCommand("pill"))
        assertEquals(Command.Pill("red"), parseCommand("PILL red"))
        assertEquals(Command.Pill("red blue"), parseCommand("pill red blue"))
        assertEquals(Command.Rabbit, parseCommand("follow the white rabbit"))
        assertEquals(Command.Rabbit, parseCommand("Follow White Rabbit."))
        assertEquals(Command.Spoon, parseCommand("there is no spoon"))
        assertEquals(Command.Query("follow me"), parseCommand("follow me"))
    }

    @Test fun layoutCommand() {
        assertEquals(Command.Layout(null), parseCommand("layout"))
        assertEquals(Command.Layout("Apps-First"), parseCommand("LAYOUT Apps-First"))
        assertEquals(Command.Layout("grid now"), parseCommand("layout grid now"))
    }

    @Test fun densityCommand() {
        assertEquals(Command.Density(null), parseCommand("density"))
        assertEquals(Command.Density("Dense"), parseCommand("DENSITY Dense"))
        assertEquals(Command.Density("a b"), parseCommand("density a b"))
    }
}
