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
import com.omnitune.app.utils.SensitivePreferenceCodec
import com.omnitune.app.utils.SecurePreferenceCipher
import com.omnitune.app.utils.dataStore
import com.omnitune.innertube.YouTube
import com.omnitune.innertube.utils.PoTokenGenerator
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber

data class PoTokenUiState(
    val visitorDataPreview: String = "",
    val gvsTokenPreview: String = "",
    val playerTokenPreview: String = "",
    val hasGvsToken: Boolean = false,
    val hasPlayerToken: Boolean = false,
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
    private companion object {
        private const val TAG = "PoTokenSettings"
    }

    private val _state = MutableStateFlow(PoTokenUiState())
    val state: StateFlow<PoTokenUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch(Dispatchers.IO) {
            context.dataStore.data
                .map { prefs ->
                    val visitorData = prefs[VisitorDataKey].orEmpty().takeUnless { it == "null" }.orEmpty()
                    val gvsToken = SecurePreferenceCipher.decryptOrPlain(prefs[PoTokenGvsKey] ?: prefs[PoTokenKey])
                    val playerToken = SecurePreferenceCipher.decryptOrPlain(prefs[PoTokenPlayerKey])
                    PoTokenUiState(
                        visitorDataPreview = SensitivePreferenceCodec.maskedPreview(visitorData),
                        gvsTokenPreview = SensitivePreferenceCodec.maskedPreview(gvsToken),
                        playerTokenPreview = SensitivePreferenceCodec.maskedPreview(playerToken),
                        hasGvsToken = gvsToken.isNotBlank(),
                        hasPlayerToken = playerToken.isNotBlank(),
                        webClientPoTokensEnabled = prefs[WebClientPoTokenEnabledKey] ?: false,
                    )
                }
                .catch { error ->
                    reportFailure(error, "Could not read PO token settings")
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
            try {
                if (enabled && !hasStoredWebPoToken()) {
                    _state.update {
                        it.copy(errorMessage = "Add a PO token before enabling Web PO tokens")
                    }
                    return@launch
                }

                context.dataStore.edit { prefs ->
                    prefs[WebClientPoTokenEnabledKey] = enabled
                }
            } catch (error: Exception) {
                reportFailure(error, "Could not update Web PO token setting")
            }
        }
    }

    fun refreshVisitorData() {
        viewModelScope.launch(Dispatchers.IO) {
            _state.update { it.copy(isRefreshingVisitorData = true, message = null, errorMessage = null) }
            try {
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
                        reportFailure(error, error.message ?: "Could not refresh visitor data")
                    }
            } catch (error: Exception) {
                reportFailure(error, "Could not refresh visitor data")
            }
        }
    }

    fun generateSessionToken() {
        viewModelScope.launch(Dispatchers.IO) {
            _state.update { it.copy(isRefreshingVisitorData = true, message = null, errorMessage = null) }

            try {
                val visitorData = storedVisitorData().takeIf { it.isNotBlank() }
                    ?: YouTube.visitorData().getOrElse { error ->
                        reportFailure(error, error.message ?: "Could not create a visitor session")
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
            } catch (error: Exception) {
                reportFailure(error, "Could not generate session PO token")
            }
        }
    }

    fun saveTokens(
        gvsToken: String,
        playerToken: String,
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val encryptedGvsToken = SensitivePreferenceCodec.encodeForStorage(
                    gvsToken,
                    SecurePreferenceCipher::encrypt,
                )
                val encryptedPlayerToken = SensitivePreferenceCodec.encodeForStorage(
                    playerToken,
                    SecurePreferenceCipher::encrypt,
                )

                if (encryptedGvsToken == null && encryptedPlayerToken == null) {
                    _state.update { it.copy(errorMessage = "Enter a new PO token before saving") }
                    return@launch
                }

                context.dataStore.edit { prefs ->
                    encryptedGvsToken?.let {
                        prefs[PoTokenKey] = it
                        prefs[PoTokenGvsKey] = it
                    }
                    encryptedPlayerToken?.let {
                        prefs[PoTokenPlayerKey] = it
                    }

                    prefs[WebClientPoTokenEnabledKey] =
                        !prefs[PoTokenGvsKey].isNullOrBlank() || !prefs[PoTokenPlayerKey].isNullOrBlank()
                }

                _state.update { it.copy(message = "PO token settings saved", errorMessage = null) }
            } catch (error: Exception) {
                reportFailure(error, "Could not save PO token settings")
            }
        }
    }

    fun clearTokens() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                context.dataStore.edit { prefs ->
                    prefs.remove(PoTokenKey)
                    prefs.remove(PoTokenGvsKey)
                    prefs.remove(PoTokenPlayerKey)
                    prefs[WebClientPoTokenEnabledKey] = false
                }
                _state.update { it.copy(message = "PO tokens cleared", errorMessage = null) }
            } catch (error: Exception) {
                reportFailure(error, "Could not clear PO tokens")
            }
        }
    }

    fun clearMessages() {
        _state.update { it.copy(message = null, errorMessage = null) }
    }

    private suspend fun storedVisitorData(): String =
        context.dataStore.data.first()[VisitorDataKey]
            .orEmpty()
            .takeUnless { it == "null" }
            .orEmpty()

    private suspend fun hasStoredWebPoToken(): Boolean {
        val prefs = context.dataStore.data.first()
        return SecurePreferenceCipher.decryptOrPlain(prefs[PoTokenGvsKey] ?: prefs[PoTokenKey]).isNotBlank() ||
            SecurePreferenceCipher.decryptOrPlain(prefs[PoTokenPlayerKey]).isNotBlank()
    }

    private fun reportFailure(error: Throwable, message: String) {
        if (error is CancellationException) throw error
        Timber.tag(TAG).w(error, message)
        _state.update {
            it.copy(
                isRefreshingVisitorData = false,
                errorMessage = message,
            )
        }
    }
}
