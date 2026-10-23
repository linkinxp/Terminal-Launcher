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
}
