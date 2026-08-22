/*
 * OmniTune - An open-source music player for Android
 * Licensed under GPL-3.0
 */

package com.omnitune.app.utils

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.first

/**
 * Removes data belonging exclusively to features that are no longer shipped.
 *
 * The migration is idempotent and deliberately enumerates the old keys rather than touching
 * unknown preferences, so existing user settings and account credentials remain intact.
 */
object RetiredFeaturePreferenceCleanup {
    private const val migrationVersion = 1
    private val migrationVersionKey = intPreferencesKey("retired_feature_cleanup_version")

    private val stringKeys = listOf(
        "together_display_name",
        "together_client_id",
        "together_last_join_link",
        "together_online_endpoint_cache",
        "discordToken",
        "discordUsername",
        "discordName",
        "discordActivityName",
        "discordActivityDetails",
        "discordActivityState",
        "discordActivityButton1Label",
        "discordActivityButton1UrlSource",
        "discordActivityButton1CustomUrl",
        "discordActivityButton2Label",
        "discordActivityButton2UrlSource",
        "discordActivityButton2CustomUrl",
        "discordActivityType",
        "discordPresenceIntervalUnit",
        "discordPresenceStatus",
        "discordLargeImageType",
        "discordLargeTextSource",
        "discordLargeTextCustom",
        "discordLargeImageCustomUrl",
        "discordSmallImageType",
        "discordSmallImageCustomUrl",
        "discordActivityPlatform",
        "lastfmSession",
        "lastfmUsername",
    ).map(::stringPreferencesKey)

    private val booleanKeys = listOf(
        "together_allow_guests_add_tracks",
        "together_allow_guests_control_playback",
        "together_require_host_approval_to_join",
        "together_welcome_shown",
        "discordInfoDismissed",
        "discordRPCEnable",
        "discordActivityButton1Enabled",
        "discordActivityButton2Enabled",
        "discordShowWhenPaused",
        "lastfmScrobblingEnable",
        "lastfmUseNowPlaying",
    ).map(::booleanPreferencesKey)

    private val intKeys = listOf(
        "together_default_port",
        "discordPresenceIntervalValue",
    ).map(::intPreferencesKey)

    private val longKeys = listOf("together_online_endpoint_last_checked_at")
        .map(::longPreferencesKey)

    suspend fun apply(dataStore: DataStore<Preferences>): Boolean {
        if (dataStore.data.first()[migrationVersionKey] == migrationVersion) return false

        dataStore.edit { preferences ->
            removeFrom(preferences)
            preferences[migrationVersionKey] = migrationVersion
        }
        return true
    }

    internal fun removeFrom(preferences: MutablePreferences) {
        stringKeys.forEach(preferences::remove)
        booleanKeys.forEach(preferences::remove)
        intKeys.forEach(preferences::remove)
        longKeys.forEach(preferences::remove)
    }
}
