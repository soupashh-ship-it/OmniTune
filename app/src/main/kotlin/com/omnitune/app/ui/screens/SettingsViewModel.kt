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

@HiltViewModel
class SettingsViewModel @Inject constructor() : ViewModel() {
    fun <T> updatePreference(context: Context, key: Preferences.Key<T>, value: T) {
        viewModelScope.launch(Dispatchers.IO) {
            context.dataStore.edit { it[key] = value }
        }
    }
}
