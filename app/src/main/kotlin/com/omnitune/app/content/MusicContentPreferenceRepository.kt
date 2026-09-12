/*
 * OmniTune - An open-source music player for Android
 * Licensed under GPL-3.0
 */

package com.omnitune.app.content

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import com.omnitune.app.constants.DefaultMusicLanguageKey
import com.omnitune.innertube.YouTube
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MusicContentPreferenceRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) {
    val selectedLanguage: Flow<MusicContentLanguage> =
        dataStore.data
            .map { preferences -> preferences.defaultMusicLanguage() }
            .distinctUntilChanged()

    val requestContext: Flow<MusicContentRequestContext> =
        selectedLanguage
            .map { language -> language.toRequestContext() }
            .distinctUntilChanged()

    suspend fun currentLanguage(): MusicContentLanguage = selectedLanguage.first()

    suspend fun currentRequestContext(): MusicContentRequestContext =
        currentLanguage().toRequestContext()

    suspend fun setLanguage(language: MusicContentLanguage) {
        dataStore.edit { preferences ->
            preferences[DefaultMusicLanguageKey] = language.preferenceValue
        }
        applyLanguage(language)
    }

    suspend fun applyCurrentPreferenceToYouTube(): MusicContentLanguage =
        currentLanguage().also(::applyLanguage)

    fun applyLanguage(language: MusicContentLanguage) {
        YouTube.locale = language.toYouTubeLocale()
    }
}
