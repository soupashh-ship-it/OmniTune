package com.omnitune.app.update

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GitHubReleaseApi @Inject constructor(
    private val okHttpClient: OkHttpClient,
) {
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
            parseRelease(JSONObject(body))
        }
    }

    private fun parseRelease(json: JSONObject): GitHubRelease {
        val assetsJson = json.optJSONArray("assets")
        val assets = buildList {
            if (assetsJson != null) {
                for (index in 0 until assetsJson.length()) {
                    val item = assetsJson.optJSONObject(index) ?: continue
                    add(
                        GitHubReleaseAsset(
                            name = item.optString("name"),
                            browserDownloadUrl = item.optString("browser_download_url"),
                            size = item.optLong("size", -1L),
                            contentType = item.optString("content_type").ifBlank { null },
                            digest = item.optString("digest").ifBlank { null },
                        )
                    )
                }
            }
        }
        return GitHubRelease(
            tagName = json.optString("tag_name"),
            name = json.optString("name"),
            body = json.optString("body"),
            publishedAt = json.optString("published_at"),
            prerelease = json.optBoolean("prerelease"),
            draft = json.optBoolean("draft"),
            assets = assets,
        )
    }

    private companion object {
        const val LATEST_RELEASE_URL = "https://api.github.com/repos/soupashh-ship-it/OmniTune/releases/latest"
    }
}
