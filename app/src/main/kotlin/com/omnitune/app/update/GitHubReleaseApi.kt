package com.omnitune.app.update

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GitHubReleaseApi @Inject constructor(
    private val okHttpClient: OkHttpClient,
) {
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun fetchLatestRelease(): GitHubRelease = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url(LATEST_RELEASE_URL)
            .header("Accept", "application/vnd.github+json")
            .header("User-Agent", "OmniTune-Android")
            .build()

        okHttpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IllegalStateException("GitHub update check failed: HTTP ${response.code}")
            }
            val body = response.body.string()
            parseRelease(body)
        }
    }

    internal fun parseRelease(body: String): GitHubRelease {
        val release = json.decodeFromString<GitHubReleaseResponse>(body)
        return GitHubRelease(
            tagName = release.tagName,
            name = release.name,
            body = release.body,
            publishedAt = release.publishedAt,
            prerelease = release.prerelease,
            draft = release.draft,
            assets = release.assets.map { asset ->
                GitHubReleaseAsset(
                    name = asset.name,
                    browserDownloadUrl = asset.browserDownloadUrl,
                    size = asset.size,
                    contentType = asset.contentType?.ifBlank { null },
                    digest = asset.digest?.ifBlank { null },
                )
            },
        )
    }

    private companion object {
        const val LATEST_RELEASE_URL = "https://api.github.com/repos/soupashh-ship-it/OmniTune/releases/latest"
    }
}

@Serializable
private data class GitHubReleaseResponse(
    @SerialName("tag_name") val tagName: String = "",
    val name: String = "",
    val body: String = "",
    @SerialName("published_at") val publishedAt: String = "",
    val prerelease: Boolean = false,
    val draft: Boolean = false,
    val assets: List<GitHubReleaseAssetResponse> = emptyList(),
)

@Serializable
private data class GitHubReleaseAssetResponse(
    val name: String = "",
    @SerialName("browser_download_url") val browserDownloadUrl: String = "",
    val size: Long = -1L,
    @SerialName("content_type") val contentType: String? = null,
    val digest: String? = null,
)
