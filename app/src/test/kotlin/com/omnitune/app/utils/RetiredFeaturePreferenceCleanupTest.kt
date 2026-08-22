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
        val retiredFlag = booleanPreferencesKey("together_welcome_shown")
        val retainedPreference = stringPreferencesKey("unrelated_preference")
        val preferences = mutablePreferencesOf(
            retiredToken to "legacy-value",
            retiredFlag to true,
            retainedPreference to "retain-this",
        )

        RetiredFeaturePreferenceCleanup.removeFrom(preferences)

        assertNull(preferences[retiredToken])
        assertNull(preferences[retiredFlag])
        assertEquals("retain-this", preferences[retainedPreference])
    }
}
