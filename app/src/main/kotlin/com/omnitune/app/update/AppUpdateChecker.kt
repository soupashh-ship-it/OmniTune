package com.omnitune.app.update

import com.omnitune.app.BuildConfig
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppUpdateChecker @Inject constructor(
    private val releaseApi: GitHubReleaseApi,
) {
    suspend fun checkForUpdate(allowPrereleases: Boolean = BuildConfig.DEBUG): AppUpdateInfo? {
        val release = releaseApi.fetchLatestRelease()
        if (release.draft) return null
        if (release.prerelease && !allowPrereleases) return null

        val remoteVersion = parseVersion(release.tagName) ?: return null
        val currentVersion = parseVersion(BuildConfig.VERSION_NAME) ?: listOf(BuildConfig.VERSION_CODE)
        if (compareVersions(remoteVersion, currentVersion) <= 0) return null

        val apkAsset = selectApkAsset(release.assets, release.tagName)
            ?: throw IllegalStateException("No APK asset found in latest GitHub release.")

        return AppUpdateInfo(
            versionName = release.tagName.removePrefix("v"),
            releaseName = release.name.ifBlank { release.tagName },
            releaseNotes = release.body,
            publishedAt = release.publishedAt,
            apkAsset = apkAsset,
            sha256Asset = release.assets.firstOrNull { it.name == "${apkAsset.name}.sha256" },
        )
    }

    private fun selectApkAsset(
        assets: List<GitHubReleaseAsset>,
        tagName: String,
    ): GitHubReleaseAsset? {
        val version = tagName.removePrefix("v")
        val allowed = setOf(
            "OmniTune-v$version-release.apk",
            "OmniTune-v$version-universal-release.apk",
        )
        return assets.firstOrNull { asset ->
            val name = asset.name
            name in allowed &&
                !name.contains("debug", ignoreCase = true) &&
                !name.contains("unsigned", ignoreCase = true) &&
                !name.endsWith(".idsig", ignoreCase = true) &&
                asset.browserDownloadUrl.isNotBlank()
        }
    }

    private fun parseVersion(raw: String): List<Int>? {
        val normalized = raw.trim().removePrefix("v")
        val match = Regex("""^(\d+)(?:\.(\d+))?(?:\.(\d+))?.*""").matchEntire(normalized) ?: return null
        return match.groupValues.drop(1).map { it.toIntOrNull() ?: 0 }
    }

    private fun compareVersions(remote: List<Int>, current: List<Int>): Int {
        val max = maxOf(remote.size, current.size)
        for (index in 0 until max) {
            val left = remote.getOrElse(index) { 0 }
            val right = current.getOrElse(index) { 0 }
            if (left != right) return left.compareTo(right)
        }
        return 0
    }
}
