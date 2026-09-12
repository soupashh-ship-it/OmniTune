/*
 * OmniTune - An open-source music player for Android
 * Licensed under GPL-3.0
 */

package com.omnitune.app.content

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class MusicContentPreferenceRepositoryTest {
    @Test
    fun selectedLanguagePersistsInPreferences() = runTest {
        val dataStore = FakePreferenceDataStore()
        val firstRepository = MusicContentPreferenceRepository(dataStore)

        firstRepository.setLanguage(MusicContentLanguage.TELUGU)

        val secondRepository = MusicContentPreferenceRepository(dataStore)
        assertEquals(MusicContentLanguage.TELUGU, secondRepository.currentLanguage())
    }

    @Test
    fun repositoryDefaultsToEnglishForExistingUserWithoutPreference() = runTest {
        val repository = MusicContentPreferenceRepository(FakePreferenceDataStore())

        assertEquals(MusicContentLanguage.ENGLISH, repository.currentLanguage())
    }

    private class FakePreferenceDataStore(
        initialPreferences: Preferences = emptyPreferences(),
    ) : DataStore<Preferences> {
        private val state = MutableStateFlow(initialPreferences)

        override val data: Flow<Preferences> = state

        override suspend fun updateData(
            transform: suspend (t: Preferences) -> Preferences,
        ): Preferences {
            val updated = transform(state.value)
            state.value = updated
            return updated
        }
    }
}
