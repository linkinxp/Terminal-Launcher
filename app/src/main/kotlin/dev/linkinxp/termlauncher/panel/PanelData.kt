package dev.linkinxp.termlauncher.panel

/** What a home-page panel shows: nothing (off), formatted lines, or a one-line message (permission, offline...). */
sealed interface PanelData {
    data object Off : PanelData
    data class Lines(val lines: List<String>) : PanelData
    data class Message(val text: String) : PanelData
}
