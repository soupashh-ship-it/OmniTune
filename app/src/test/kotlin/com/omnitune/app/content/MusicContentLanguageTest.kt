/*
 * OmniTune - An open-source music player for Android
 * Licensed under GPL-3.0
 */

package com.omnitune.app.content

import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.mutablePreferencesOf
import com.omnitune.app.constants.DefaultMusicLanguageKey
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MusicContentLanguageTest {
    @Test
    fun missingPreferenceDefaultsToEnglish() {
        assertEquals(MusicContentLanguage.ENGLISH, emptyPreferences().defaultMusicLanguage())
    }

    @Test
    fun corruptedPreferenceFallsBackToEnglish() {
        val preferences = mutablePreferencesOf(DefaultMusicLanguageKey to "BROKEN")

        assertEquals(MusicContentLanguage.ENGLISH, preferences.defaultMusicLanguage())
    }

    @Test
    fun requestLocaleMappingsMatchLanguageSpec() {
        val mappings = mapOf(
            MusicContentLanguage.ENGLISH to ("en" to "US"),
            MusicContentLanguage.HINDI to ("hi" to "IN"),
            MusicContentLanguage.PUNJABI to ("pa" to "IN"),
            MusicContentLanguage.TELUGU to ("te" to "IN"),
            MusicContentLanguage.TAMIL to ("ta" to "IN"),
            MusicContentLanguage.MALAYALAM to ("ml" to "IN"),
            MusicContentLanguage.KANNADA to ("kn" to "IN"),
            MusicContentLanguage.BENGALI to ("bn" to "IN"),
            MusicContentLanguage.MARATHI to ("mr" to "IN"),
            MusicContentLanguage.GUJARATI to ("gu" to "IN"),
        )

        mappings.forEach { (language, expected) ->
            val locale = language.toYouTubeLocale()

            assertEquals(expected.first, locale.hl)
            assertEquals(expected.second, locale.gl)
        }
    }

    @Test
    fun requestContextExposesEffectiveCodes() {
        val context = MusicContentLanguage.TELUGU.toRequestContext()

        assertEquals(MusicContentLanguage.TELUGU, context.selectedLanguage)
        assertEquals("te", context.effectiveLanguageCode)
        assertEquals("IN", context.effectiveRegion)
        assertEquals(context.requestLocale.hl, context.effectiveLanguageCode)
        assertEquals(context.requestLocale.gl, context.effectiveRegion)
    }

    @Test
    fun automaticUsesNormalDeviceLocaleBehavior() {
        val locale = MusicContentLanguage.AUTOMATIC.toYouTubeLocale(
            automaticLocale = MusicContentLanguage.automaticDeviceLocale(
                Locale.Builder().setLanguage("hi").setRegion("IN").build(),
            ),
        )

        assertEquals("hi", locale.hl)
        assertEquals("IN", locale.gl)
    }

    @Test
    fun supportedSettingsIncludeRequiredLanguagesAndAutomatic() {
        val displayNames = MusicContentLanguage.settingsOptions.map { it.displayName }.toSet()

        listOf(
            "English",
            "Hindi",
            "Punjabi",
            "Telugu",
            "Tamil",
            "Malayalam",
            "Kannada",
            "Bengali",
            "Marathi",
            "Gujarati",
            "Automatic",
        ).forEach { required ->
            assertTrue("$required should be selectable", required in displayNames)
        }
    }

    @Test
    fun requestMappingDoesNotMutateUiLocale() {
        val before = Locale.getDefault()

        MusicContentLanguage.HINDI.toYouTubeLocale()

        assertEquals(before, Locale.getDefault())
    }

    @Test
    fun explicitSearchQueryKeepsUserInputIntact() {
        assertEquals("Arijit Singh", MusicContentDiscoveryPolicy.explicitSearchQuery(" Arijit Singh "))
    }

    @Test
    fun defaultMigrationIsEnglishWhenNoMusicPreferenceExists() {
        val legacyOnlyPreferences = mutablePreferencesOf()

        assertEquals(MusicContentLanguage.ENGLISH, legacyOnlyPreferences.defaultMusicLanguage())
    }
}
