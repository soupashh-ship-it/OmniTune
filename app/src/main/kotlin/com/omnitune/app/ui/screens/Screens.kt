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
    object YearInMusic : Screens("year_in_music")
    object ThemeCreator : Screens("theme_creator")
    object PalettePicker : Screens("palette_picker")
    object CustomizeBackground : Screens("customize_background")
    object PoToken : Screens("po_token")


    object MoodAndGenres : Screens("mood_and_genres")
    object YouTubeBrowse : Screens("youtube_browse")


    object Login : Screens("login")
    object AccountSettings : Screens("account_settings")

    object Changelog : Screens("changelog")
    companion object {
        val MainScreens = listOf(Home, Stats, History, Library)
        const val ACTION_SEARCH = "com.omnitune.app.action.SEARCH"
        const val ACTION_LIBRARY = "com.omnitune.app.action.LIBRARY"
        const val ROUTE_EQUALIZER = "equalizer"
        const val ROUTE_DOWNLOADS = "downloads"
    }
}
