package dev.linkinxp.termlauncher.prompt

val fortunes: List<String> = listOf(
    "There is no place like 127.0.0.1.",
    "It works on my machine.",
    "A watched build never compiles.",
    "To iterate is human, to recurse divine.",
    "Premature optimization is the root of all evil. - Knuth",
    "Talk is cheap. Show me the code. - Linus",
    "Simplicity is prerequisite for reliability. - Dijkstra",
    "First, solve the problem. Then, write the code.",
    "Weeks of coding can save hours of planning.",
    "Real programmers count from zero.",
    "rm -rf: the shortest path to a clean slate.",
    "There are 10 kinds of people: those who read binary and those who don't.",
    "The cloud is just someone else's computer.",
    "Have you tried turning it off and on again?",
    "Make it work, make it right, make it fast. - Beck",
    "Debugging is twice as hard as writing the code. - Kernighan",
    "Programs must be written for people to read. - Abelson",
    "Any sufficiently advanced bug is indistinguishable from a feature.",
)

fun fortune(seed: Int): String = fortunes[Math.floorMod(seed, fortunes.size)]
