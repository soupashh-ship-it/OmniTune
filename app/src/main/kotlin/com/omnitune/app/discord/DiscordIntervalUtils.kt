/*
 * OmniTune - An open-source music player for Android
 * Licensed under GPL-3.0
 */

package com.omnitune.app.discord

import com.omnitune.app.constants.DiscordPresenceIntervalValueKey
import com.omnitune.app.constants.DiscordPresenceIntervalUnitKey
import com.omnitune.app.utils.PreferenceStore

fun getPresenceIntervalMillis(): Long {
    val customValue = PreferenceStore.get(DiscordPresenceIntervalValueKey) ?: 30
    val customUnit = PreferenceStore.get(DiscordPresenceIntervalUnitKey) ?: "S"

    val multiplier = when (customUnit) {
        "S" -> 1000L
        "M" -> 60_000L
        "H" -> 3_600_000L
        else -> 1000L
    }

    var interval = customValue * multiplier
    if (customUnit == "S" && interval < 30_000) interval = 30_000

    return interval
}
