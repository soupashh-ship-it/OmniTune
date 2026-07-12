package com.omnitune.app.ui.screens

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.omnitune.app.utils.dataStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.omnitune.app.data.StreamRepository
import com.omnitune.app.db.MusicDatabase
import com.omnitune.app.playback.DownloadUtil
import com.omnitune.app.playback.StreamUrlResolver
import coil3.imageLoader

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val database: MusicDatabase,
    private val streamRepository: StreamRepository,
    private val downloadUtil: DownloadUtil,
) : ViewModel() {
    fun <T> updatePreference(context: Context, key: Preferences.Key<T>, value: T) {
        viewModelScope.launch(Dispatchers.IO) {
            context.dataStore.edit { it[key] = value }
        }
    }

    fun clearSearchHistory() {
        viewModelScope.launch(Dispatchers.IO) {
            database.clearSearchHistory()
        }
    }

    fun clearListenHistory() {
        viewModelScope.launch(Dispatchers.IO) {
            database.clearListenHistory()
        }
    }

    fun clearAppCache(context: Context) {
        streamRepository.clearCache()
        StreamUrlResolver.clearMemoryCache("user request")
        downloadUtil.clearPlaybackCache()
        context.imageLoader.memoryCache?.clear()
        context.imageLoader.diskCache?.clear()
    }
}
