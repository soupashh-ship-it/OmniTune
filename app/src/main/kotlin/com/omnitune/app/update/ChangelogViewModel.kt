package com.omnitune.app.update

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ChangelogViewModel @Inject constructor(
    private val releaseApi: GitHubReleaseApi,
) : ViewModel() {
    private val _state = MutableStateFlow(ChangelogState(release = AppChangelog.bundled))
    val state: StateFlow<ChangelogState> = _state.asStateFlow()

    fun refreshLatestRelease() {
        viewModelScope.launch {
            _state.value = _state.value.copy(loading = true, errorMessage = null)
            runCatching { releaseApi.fetchLatestRelease() }
                .onSuccess { release ->
                    _state.value = ChangelogState(
                        release = ChangelogRelease(
                            versionName = release.tagName.removePrefix("v"),
                            releaseName = release.name.ifBlank { release.tagName },
                            source = ChangelogSource.GitHub,
                            body = release.body.ifBlank { "No release notes were provided for this version." },
                            publishedAt = release.publishedAt.takeIf { it.isNotBlank() },
                        ),
                    )
                }
                .onFailure { error ->
                    _state.value = _state.value.copy(
                        loading = false,
                        errorMessage = error.message ?: "Could not load latest release notes.",
                    )
                }
        }
    }
}

data class ChangelogState(
    val release: ChangelogRelease,
    val loading: Boolean = false,
    val errorMessage: String? = null,
)
