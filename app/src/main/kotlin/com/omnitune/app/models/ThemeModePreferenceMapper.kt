/*
 * This file was adapted from SuvMusic.
 * Original copyright follows:
 *
 * Copyright (C) Suvojeet
 * Licensed under the GNU General Public License v3.0 (GPLv3)
 */

package com.omnitune.app.models

import java.util.Locale

object ThemeModePreferenceMapper {
    val DefaultMode: ThemeMode = ThemeMode.SYSTEM

    fun resolveMode(
        themeModeValue: String?,
        legacyDarkModeValue: String? = null,
    ): ThemeMode =
        parse(themeModeValue)
            ?: parse(legacyDarkModeValue)
            ?: DefaultMode

    fun isDarkTheme(mode: ThemeMode, systemDark: Boolean): Boolean =
        when (mode) {
            ThemeMode.DARK -> true
            ThemeMode.LIGHT -> false
            ThemeMode.SYSTEM -> systemDark
        }

    private fun parse(value: String?): ThemeMode? =
        when (value?.uppercase(Locale.ROOT)) {
            ThemeMode.DARK.name,
            "ON" -> ThemeMode.DARK

            ThemeMode.LIGHT.name,
            "OFF" -> ThemeMode.LIGHT

            ThemeMode.SYSTEM.name,
            "AUTO" -> ThemeMode.SYSTEM

            else -> null
        }
}
