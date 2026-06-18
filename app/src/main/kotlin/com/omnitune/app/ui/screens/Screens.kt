/*
 * OmniTune - An open-source music player for Android
 * Licensed under GPL-3.0
 * Licensed Under GPL-3.0
 */

package com.omnitune.app.ui.screens

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes

sealed class Screens(
    @StringRes val titleId: Int,
    @DrawableRes val iconIdInactive: Int,
    @DrawableRes val iconIdActive: Int,
    val route: String,
) {
    object Home : Screens(
        titleId = 0,
        iconIdInactive = 0,
        iconIdActive = 0,
        route = "home"
    )

    object Library : Screens(
        titleId = 0,
        iconIdInactive = 0,
        iconIdActive = 0,
        route = "library"
    )

    object Stats : Screens(
        titleId = 0,
        iconIdInactive = 0,
        iconIdActive = 0,
        route = "stats"
    )

    object History : Screens(
        titleId = 0,
        iconIdInactive = 0,
        iconIdActive = 0,
        route = "history"
    )

    object Search : Screens(
        titleId = 0,
        iconIdInactive = 0,
        iconIdActive = 0,
        route = "search"
    )

    companion object {
        val MainScreens = listOf(Home, Stats, History, Library)
        const val ACTION_SEARCH = "com.omnitune.app.action.SEARCH"
        const val ACTION_LIBRARY = "com.omnitune.app.action.LIBRARY"
    }
}
