/*
 * OmniTune - An open-source music player for Android
 * Licensed under GPL-3.0
 */

package com.omnitune.app.utils

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.mutablePreferencesOf
import androidx.datastore.preferences.core.stringPreferencesKey
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RetiredFeaturePreferenceCleanupTest {
    @Test
    fun cleanupRemovesOnlyPreferencesOwnedByRetiredFeatures() {
        val retiredToken = stringPreferencesKey("discordToken")
        val retiredEncryptedToken = stringPreferencesKey("discordTokenEncrypted")
        val retiredAiToken = stringPreferencesKey("openaiApiKey")
        val retiredAiProvider = stringPreferencesKey("selectedAiProvider")
        val retiredLastFmUsername = stringPreferencesKey("lastFmUsername")
        val retiredFlag = booleanPreferencesKey("together_welcome_shown")
        val retiredCurrentFlag = booleanPreferencesKey("discordRpcEnabled")
        val retiredLastFmFlag = booleanPreferencesKey("lastFmScrobblingEnabled")
        val retainedPreference = stringPreferencesKey("unrelated_preference")
        val preferences = mutablePreferencesOf(
            retiredToken to "legacy-value",
            retiredEncryptedToken to "encrypted-value",
            retiredAiToken to "sk-unused",
            retiredAiProvider to "openai",
            retiredLastFmUsername to "old-user",
            retiredFlag to true,
            retiredCurrentFlag to true,
            retiredLastFmFlag to true,
            retainedPreference to "retain-this",
        )

        RetiredFeaturePreferenceCleanup.removeFrom(preferences)

        assertNull(preferences[retiredToken])
        assertNull(preferences[retiredEncryptedToken])
        assertNull(preferences[retiredAiToken])
        assertNull(preferences[retiredAiProvider])
        assertNull(preferences[retiredLastFmUsername])
        assertNull(preferences[retiredFlag])
        assertNull(preferences[retiredCurrentFlag])
        assertNull(preferences[retiredLastFmFlag])
        assertEquals("retain-this", preferences[retainedPreference])
    }
}
