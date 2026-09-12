package com.omnitune.app.update

import com.omnitune.app.BuildConfig
import com.omnitune.app.constants.UpdateChannel
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppUpdateChecker @Inject constructor(
    private val releaseApi: GitHubReleaseApi,
) {
    suspend fun checkForUpdate(channel: UpdateChannel = if (BuildConfig.DEBUG) UpdateChannel.NIGHTLY else UpdateChannel.STABLE): AppUpdateInfo? {
        val releases = when (channel) {
            UpdateChannel.STABLE -> listOf(releaseApi.fetchLatestRelease())
            UpdateChannel.BETA,
            UpdateChannel.NIGHTLY -> releaseApi.fetchReleases()
        }
        val update = findBestUpdate(
            releases = releases,
            channel = channel,
            currentVersionName = BuildConfig.VERSION_NAME,
            currentVersionCode = BuildConfig.VERSION_CODE,
        )
        if (update == null && hasNewerEligibleReleaseWithoutCompatibleApk(releases, channel)) {
            throw IllegalStateException("No compatible APK asset found in the selected GitHub release channel.")
        }
        return update
    }

    internal fun findBestUpdate(
        releases: List<GitHubRelease>,
        channel: UpdateChannel,
        currentVersionName: String,
        currentVersionCode: Int,
    ): AppUpdateInfo? {
        val currentVersion = parseVersion(currentVersionName)
            ?: ParsedVersion(listOf(currentVersionCode, 0, 0), prereleaseLabel = null, prereleaseNumber = null, rawSuffix = "")

        return releases
            .asSequence()
            .filter { it.isEligibleFor(channel) }
            .mapNotNull { release ->
                val remoteVersion = parseVersion(release.tagName) ?: return@mapNotNull null
                if (remoteVersion.compareTo(currentVersion) <= 0) return@mapNotNull null
                release.toUpdateInfoOrNull()?.let { CandidateUpdate(remoteVersion, release.publishedAt, it) }
            }
            .maxWithOrNull(
                compareBy<CandidateUpdate> { it.version }
                    .thenBy { it.publishedAt }
            )
            ?.updateInfo
    }

    private fun hasNewerEligibleReleaseWithoutCompatibleApk(
        releases: List<GitHubRelease>,
        channel: UpdateChannel,
    ): Boolean {
        val currentVersion = parseVersion(BuildConfig.VERSION_NAME)
            ?: ParsedVersion(listOf(BuildConfig.VERSION_CODE, 0, 0), prereleaseLabel = null, prereleaseNumber = null, rawSuffix = "")
        return releases.any { release ->
            val remoteVersion = parseVersion(release.tagName) ?: return@any false
            release.isEligibleFor(channel) &&
                remoteVersion.compareTo(currentVersion) > 0 &&
                selectApkAsset(release.assets, release.tagName) == null
        }
    }

    private fun GitHubRelease.toUpdateInfoOrNull(): AppUpdateInfo? {
        val apkAsset = selectApkAsset(assets, tagName) ?: return null
        return AppUpdateInfo(
            versionName = tagName.removePrefix("v"),
            releaseName = name.ifBlank { tagName },
            releaseNotes = body,
            publishedAt = publishedAt,
            apkAsset = apkAsset,
            sha256Asset = selectSha256Asset(assets, apkAsset),
        )
    }

    private fun GitHubRelease.isEligibleFor(channel: UpdateChannel): Boolean =
        !draft && when (channel) {
            UpdateChannel.STABLE -> !prerelease
            UpdateChannel.BETA,
            UpdateChannel.NIGHTLY -> prerelease
        }

    internal fun selectApkAsset(
        assets: List<GitHubReleaseAsset>,
        tagName: String,
    ): GitHubReleaseAsset? {
        val version = tagName.removePrefix("v")
        return assets
            .filter { asset ->
            val name = asset.name
                name.endsWith(".apk", ignoreCase = true) &&
                !name.contains("debug", ignoreCase = true) &&
                !name.contains("unsigned", ignoreCase = true) &&
                !name.contains("unaligned", ignoreCase = true) &&
                !name.endsWith(".idsig", ignoreCase = true) &&
                asset.browserDownloadUrl.isNotBlank()
            }
            .maxWithOrNull(
                compareBy<GitHubReleaseAsset> { apkAssetPriority(it.name, version) }
                    .thenBy { it.size }
            )
    }

    internal fun selectSha256Asset(
        assets: List<GitHubReleaseAsset>,
        apkAsset: GitHubReleaseAsset,
    ): GitHubReleaseAsset? {
        val exactNames = setOf(
            "${apkAsset.name}.sha256",
            "${apkAsset.name}.sha256sum",
            "${apkAsset.name}.sha256.txt",
        )
        return assets.firstOrNull { it.name in exactNames }
            ?: assets.firstOrNull { asset ->
                asset.name.contains("sha256", ignoreCase = true) &&
                    asset.browserDownloadUrl.isNotBlank()
            }
    }

    private fun apkAssetPriority(name: String, version: String): Int {
        val lower = name.lowercase(Locale.ROOT)
        val versionLower = version.lowercase(Locale.ROOT)
        var score = 0
        if ("omnitune" in lower) score += 16
        if ("universal" in lower) score += 8
        if ("release" in lower) score += 4
        if (versionLower in lower || "v$versionLower" in lower) score += 2
        return score
    }

    private fun parseVersion(raw: String): ParsedVersion? {
        val normalized = raw.trim().removePrefix("v")
        val match = Regex("""^(\d+)(?:\.(\d+))?(?:\.(\d+))?""").find(normalized) ?: return null
        val numbers = match.groupValues.drop(1).map { it.toIntOrNull() ?: 0 }
        val suffix = normalized
            .substring(match.value.length)
            .trimStart('-', '.', '_', '+')
        val label = Regex("""[A-Za-z]+""").find(suffix)?.value?.lowercase(Locale.ROOT)
        val prereleaseNumber = Regex("""\d+""").find(suffix)?.value?.toIntOrNull()
        return ParsedVersion(
            numbers = numbers,
            prereleaseLabel = label,
            prereleaseNumber = prereleaseNumber,
            rawSuffix = suffix,
        )
    }

    private data class ParsedVersion(
        val numbers: List<Int>,
        val prereleaseLabel: String?,
        val prereleaseNumber: Int?,
        val rawSuffix: String,
    ) : Comparable<ParsedVersion> {
        override fun compareTo(other: ParsedVersion): Int {
            val versionCompare = compareVersionNumbers(numbers, other.numbers)
            if (versionCompare != 0) return versionCompare

            val isStable = prereleaseLabel == null
            val otherIsStable = other.prereleaseLabel == null
            if (isStable != otherIsStable) return if (isStable) 1 else -1
            if (isStable) return 0

            val labelCompare = prereleaseRank(prereleaseLabel).compareTo(prereleaseRank(other.prereleaseLabel))
            if (labelCompare != 0) return labelCompare

            val numberCompare = (prereleaseNumber ?: -1).compareTo(other.prereleaseNumber ?: -1)
            if (numberCompare != 0) return numberCompare

            return rawSuffix.compareTo(other.rawSuffix)
        }
    }

    private data class CandidateUpdate(
        val version: ParsedVersion,
        val publishedAt: String,
        val updateInfo: AppUpdateInfo,
    )

    private companion object {
        fun compareVersionNumbers(remote: List<Int>, current: List<Int>): Int {
            val max = maxOf(remote.size, current.size)
        for (index in 0 until max) {
            val left = remote.getOrElse(index) { 0 }
            val right = current.getOrElse(index) { 0 }
            if (left != right) return left.compareTo(right)
        }
        return 0
        }

        fun prereleaseRank(label: String?): Int = when (label) {
            "snapshot", "dev", "nightly" -> 0
            "alpha", "a" -> 1
            "beta", "b", "pre", "preview" -> 2
            "rc" -> 3
            null -> 4
            else -> 2
        }
    }
}
