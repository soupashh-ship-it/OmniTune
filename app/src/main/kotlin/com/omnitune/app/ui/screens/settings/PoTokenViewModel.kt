/*
 * OmniTune - An open-source music player for Android
 * Licensed under GPL-3.0
 */

package com.omnitune.app.ui.screens.settings

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.omnitune.app.constants.PoTokenGvsKey
import com.omnitune.app.constants.PoTokenKey
import com.omnitune.app.constants.PoTokenPlayerKey
import com.omnitune.app.constants.VisitorDataKey
import com.omnitune.app.constants.WebClientPoTokenEnabledKey
import com.omnitune.app.utils.SecurePreferenceCipher
import com.omnitune.app.utils.dataStore
import com.omnitune.innertube.YouTube
import com.omnitune.innertube.utils.PoTokenGenerator
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class PoTokenUiState(
    val visitorData: String = "",
    val gvsToken: String = "",
    val playerToken: String = "",
    val webClientPoTokensEnabled: Boolean = false,
    val isRefreshingVisitorData: Boolean = false,
    val message: String? = null,
    val errorMessage: String? = null,
)

@HiltViewModel
class PoTokenViewModel
@Inject
constructor(
    @ApplicationContext private val context: Context,
) : ViewModel() {

    private val _state = MutableStateFlow(PoTokenUiState())
    val state: StateFlow<PoTokenUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch(Dispatchers.IO) {
            context.dataStore.data
                .map { prefs ->
                    PoTokenUiState(
                        visitorData = prefs[VisitorDataKey].orEmpty().takeUnless { it == "null" }.orEmpty(),
                        gvsToken = SecurePreferenceCipher.decryptOrPlain(prefs[PoTokenGvsKey]),
                        playerToken = SecurePreferenceCipher.decryptOrPlain(prefs[PoTokenPlayerKey]),
                        webClientPoTokensEnabled = prefs[WebClientPoTokenEnabledKey] ?: false,
                    )
                }
                .collect { storedState ->
                    _state.update { current ->
                        storedState.copy(
                            isRefreshingVisitorData = current.isRefreshingVisitorData,
                            message = current.message,
                            errorMessage = current.errorMessage,
                        )
                    }
                }
        }
    }

    fun setWebClientPoTokensEnabled(enabled: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            context.dataStore.edit { prefs ->
                prefs[WebClientPoTokenEnabledKey] = enabled
            }
        }
    }

    fun refreshVisitorData() {
        viewModelScope.launch(Dispatchers.IO) {
            _state.update { it.copy(isRefreshingVisitorData = true, message = null, errorMessage = null) }
            YouTube.visitorData()
                .onSuccess { visitorData ->
                    context.dataStore.edit { prefs ->
                        prefs[VisitorDataKey] = visitorData
                    }
                    _state.update {
                        it.copy(
                            isRefreshingVisitorData = false,
                            message = "Visitor data refreshed",
                        )
                    }
                }
                .onFailure { error ->
                    _state.update {
                        it.copy(
                            isRefreshingVisitorData = false,
                            errorMessage = error.message ?: "Could not refresh visitor data",
                        )
                    }
                }
        }
    }

    fun generateSessionToken() {
        viewModelScope.launch(Dispatchers.IO) {
            _state.update { it.copy(isRefreshingVisitorData = true, message = null, errorMessage = null) }

            val visitorData = _state.value.visitorData.takeIf { it.isNotBlank() }
                ?: YouTube.visitorData().getOrElse { error ->
                    _state.update {
                        it.copy(
                            isRefreshingVisitorData = false,
                            errorMessage = error.message ?: "Could not create a visitor session",
                        )
                    }
                    return@launch
                }

            val sessionToken = PoTokenGenerator.generateSessionToken(visitorData)
            val encryptedSessionToken = SecurePreferenceCipher.encrypt(sessionToken)

            context.dataStore.edit { prefs ->
                prefs[VisitorDataKey] = visitorData
                prefs[PoTokenKey] = encryptedSessionToken
                prefs[PoTokenGvsKey] = encryptedSessionToken
                prefs[WebClientPoTokenEnabledKey] = true
            }

            _state.update {
                it.copy(
                    isRefreshingVisitorData = false,
                    message = "Session PO token generated",
                )
            }
        }
    }

    fun saveTokens(
        gvsToken: String,
        playerToken: String,
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val trimmedGvsToken = gvsToken.trim()
            val trimmedPlayerToken = playerToken.trim()

            context.dataStore.edit { prefs ->
                if (trimmedGvsToken.isBlank()) {
                    prefs.remove(PoTokenKey)
                    prefs.remove(PoTokenGvsKey)
                } else {
                    val encryptedGvsToken = SecurePreferenceCipher.encrypt(trimmedGvsToken)
                    prefs[PoTokenKey] = encryptedGvsToken
                    prefs[PoTokenGvsKey] = encryptedGvsToken
                }

                if (trimmedPlayerToken.isBlank()) {
                    prefs.remove(PoTokenPlayerKey)
                } else {
                    prefs[PoTokenPlayerKey] = SecurePreferenceCipher.encrypt(trimmedPlayerToken)
                }

                prefs[WebClientPoTokenEnabledKey] =
                    trimmedGvsToken.isNotBlank() || trimmedPlayerToken.isNotBlank()
            }

            _state.update { it.copy(message = "PO token settings saved", errorMessage = null) }
        }
    }

    fun clearTokens() {
        viewModelScope.launch(Dispatchers.IO) {
            context.dataStore.edit { prefs ->
                prefs.remove(PoTokenKey)
                prefs.remove(PoTokenGvsKey)
                prefs.remove(PoTokenPlayerKey)
                prefs[WebClientPoTokenEnabledKey] = false
            }
            _state.update { it.copy(message = "PO tokens cleared", errorMessage = null) }
        }
    }

    fun clearMessages() {
        _state.update { it.copy(message = null, errorMessage = null) }
    }
}
