package com.omnitune.app.ui.screens


sealed class Screens(
    val route: String,
) {
    object Home : Screens("home")
    object Explore : Screens("explore")
    object Library : Screens("library")
    object Search : Screens("search")
    object Stats : Screens("stats")
    object History : Screens("history")
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
        /**
         * The current ten-image reference family uses this persistent dock. Search
         * remains a header destination so it does not displace the content-first tabs.
         */
        val MainScreens = listOf(Home, Stats, History, Library)
        const val ACTION_SEARCH = "com.omnitune.app.action.SEARCH"
        const val ACTION_LIBRARY = "com.omnitune.app.action.LIBRARY"
        const val ROUTE_EQUALIZER = "equalizer"
        const val ROUTE_DOWNLOADS = "downloads"
    }
}
