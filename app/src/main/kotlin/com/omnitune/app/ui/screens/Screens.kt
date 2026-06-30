/*
 * OmniTune - An open-source music player for Android
 * Licensed under GPL-3.0
 */

package com.omnitune.app.ui.screens


sealed class Screens(
    val route: String,
) {
    object Home : Screens("home")
    object Library : Screens("library")
    object Stats : Screens("stats")
    object History : Screens("history")
    object Search : Screens("search")

    companion object {
        val MainScreens = listOf(Home, Stats, History, Library)
        const val ACTION_SEARCH = "com.omnitune.app.action.SEARCH"
        const val ACTION_LIBRARY = "com.omnitune.app.action.LIBRARY"
        const val ROUTE_EQUALIZER = "equalizer"
        const val ROUTE_DOWNLOADS = "downloads"
    }
}
