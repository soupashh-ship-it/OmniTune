package com.omnitune.app.update

import android.content.Context
import android.net.ConnectivityManager
import androidx.datastore.preferences.core.edit
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.omnitune.app.BuildConfig
import com.omnitune.app.constants.LastUpdateCheckKey
import com.omnitune.app.constants.UpdateChannel
import com.omnitune.app.utils.dataStore
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class UpdateViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val appUpdateChecker: AppUpdateChecker,
    private val apkDownloadManager: ApkDownloadManager,
) : ViewModel() {
    private val _state = MutableStateFlow<UpdateState>(UpdateState.Idle)
    val state: StateFlow<UpdateState> = _state.asStateFlow()

    fun checkForUpdates(channel: UpdateChannel = UpdateChannel.STABLE) {
        viewModelScope.launch {
            _state.value = UpdateState.Checking
            runCatching {
                appUpdateChecker.checkForUpdate(
                    allowPrereleases = channel == UpdateChannel.NIGHTLY,
                )
            }
                .onSuccess { update ->
                    _state.value = if (update == null) {
                        UpdateState.NoUpdate
                    } else {
                        UpdateState.UpdateAvailable(update)
                    }
                }
                .onFailure { error ->
                    _state.value = UpdateState.Error(
                        error.message ?: "Could not check for updates. Check your connection."
                    )
                }
            context.dataStore.edit { preferences ->
                preferences[LastUpdateCheckKey] = System.currentTimeMillis()
            }
        }
    }

    fun downloadUpdate(confirmMetered: Boolean = false) {
        val update = when (val current = _state.value) {
            is UpdateState.UpdateAvailable -> current.update
            is UpdateState.Downloaded -> current.update.updateInfo
            else -> return
        }
        if (isActiveNetworkMetered() && !confirmMetered) {
            _state.value = UpdateState.UpdateAvailable(update, requireMeteredConfirmation = true)
            return
        }

        viewModelScope.launch {
            _state.value = UpdateState.Downloading(0f)
            runCatching {
                apkDownloadManager.downloadUpdate(update) { progress ->
                    _state.value = UpdateState.Downloading(progress)
                }
            }.onSuccess { downloaded ->
                _state.value = UpdateState.Downloaded(downloaded)
            }.onFailure { error ->
                _state.value = UpdateState.Error(error.message ?: "Downloaded update is invalid.")
            }
        }
    }

    fun reset() {
        _state.value = UpdateState.Idle
    }

    fun showError(message: String) {
        _state.value = UpdateState.Error(message)
    }

    private fun isActiveNetworkMetered(): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        return connectivityManager?.isActiveNetworkMetered ?: false
    }

    val currentVersionLabel: String
        get() = "${BuildConfig.VERSION_NAME} (code ${BuildConfig.VERSION_CODE})"
}
