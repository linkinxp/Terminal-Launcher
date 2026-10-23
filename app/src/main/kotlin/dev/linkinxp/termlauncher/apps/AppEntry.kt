package dev.linkinxp.termlauncher.apps

import kotlinx.serialization.Serializable

data class AppEntry(val pkg: String, val label: String, val installTime: Long)

@Serializable
data class AppStat(val launches: Int, val lastUsed: Long)

enum class DrawerSort { AZ, RECENTLY_INSTALLED, MOST_USED }
