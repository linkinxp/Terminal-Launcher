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
        assertEquals(Command.Query("theme"), parseCommand("theme"))
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
}
