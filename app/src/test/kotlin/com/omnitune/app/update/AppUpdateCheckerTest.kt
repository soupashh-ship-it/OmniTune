package com.omnitune.app.update

import com.omnitune.app.constants.UpdateChannel
import okhttp3.OkHttpClient
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AppUpdateCheckerTest {
    private val checker = AppUpdateChecker(GitHubReleaseApi(OkHttpClient()))

    @Test
    fun stableChannelIgnoresPrereleases() {
        val update = checker.findBestUpdate(
            releases = listOf(
                release(tag = "v1.2.0-pre5", prerelease = true),
                release(tag = "v1.1.9", prerelease = false),
            ),
            channel = UpdateChannel.STABLE,
            currentVersionName = "1.2.0-pre4",
            currentVersionCode = 122,
        )

        assertNull(update)
    }

    @Test
    fun prereleaseChannelFindsNewerPrereleaseWithSameBaseVersion() {
        val update = checker.findBestUpdate(
            releases = listOf(
                release(tag = "v1.2.0-pre3", prerelease = true),
                release(tag = "v1.2.0-pre5", prerelease = true),
                release(tag = "v1.2.0", prerelease = false),
            ),
            channel = UpdateChannel.NIGHTLY,
            currentVersionName = "1.2.0-pre4",
            currentVersionCode = 122,
        )

        assertEquals("1.2.0-pre5", update?.versionName)
    }

    @Test
    fun stableReleaseBeatsInstalledPrereleaseWithSameBaseVersion() {
        val update = checker.findBestUpdate(
            releases = listOf(release(tag = "v1.2.0", prerelease = false)),
            channel = UpdateChannel.STABLE,
            currentVersionName = "1.2.0-pre4",
            currentVersionCode = 122,
        )

        assertEquals("1.2.0", update?.versionName)
    }

    @Test
    fun selectApkAssetPrefersUniversalReleaseButRejectsDebugAndUnsigned() {
        val selected = checker.selectApkAsset(
            assets = listOf(
                asset("OmniTune-v1.2.0-pre5-debug.apk"),
                asset("OmniTune-v1.2.0-pre5-unsigned.apk"),
                asset("app-arm64-v8a-release.apk"),
                asset("OmniTune-v1.2.0-pre5-universal-release.apk"),
            ),
            tagName = "v1.2.0-pre5",
        )

        assertEquals("OmniTune-v1.2.0-pre5-universal-release.apk", selected?.name)
    }

    @Test
    fun selectSha256AssetAllowsChecksumNamingVariants() {
        val apk = asset("OmniTune-v1.2.0-pre5-universal-release.apk")
        val selected = checker.selectSha256Asset(
            assets = listOf(
                apk,
                asset("checksums-sha256.txt"),
            ),
            apkAsset = apk,
        )

        assertEquals("checksums-sha256.txt", selected?.name)
    }

    @Test
    fun releaseApkNameUsedByPreReleaseWorkflowIsCompatible() {
        val selected = checker.selectApkAsset(
            assets = listOf(
                asset("OmniTune-v1.2.0-pre12-release.apk"),
                asset("OmniTune-v1.2.0-pre12-release.apk.sha256"),
            ),
            tagName = "v1.2.0-pre12",
        )

        assertEquals("OmniTune-v1.2.0-pre12-release.apk", selected?.name)
    }

    @Test
    fun noCompatibleApkAssetProducesNoUpdateCandidate() {
        val update = checker.findBestUpdate(
            releases = listOf(
                release(
                    tag = "v1.3.0",
                    prerelease = false,
                    assets = listOf(asset("OmniTune-v1.3.0-release.apk.sha256")),
                ),
            ),
            channel = UpdateChannel.STABLE,
            currentVersionName = "1.2.0",
            currentVersionCode = 120,
        )

        assertNull(update)
    }

    @Test
    fun olderAndEqualVersionsAreIgnored() {
        val update = checker.findBestUpdate(
            releases = listOf(
                release(tag = "v1.2.0", prerelease = false),
                release(tag = "v1.1.9", prerelease = false),
            ),
            channel = UpdateChannel.STABLE,
            currentVersionName = "1.2.0",
            currentVersionCode = 120,
        )

        assertNull(update)
    }

    @Test
    fun newerStableReleaseIsSelectedOnStableChannel() {
        val update = checker.findBestUpdate(
            releases = listOf(
                release(tag = "v1.2.1", prerelease = false),
                release(tag = "v1.2.0-pre9", prerelease = true),
            ),
            channel = UpdateChannel.STABLE,
            currentVersionName = "1.2.0",
            currentVersionCode = 120,
        )

        assertEquals("1.2.1", update?.versionName)
    }

    private fun release(
        tag: String,
        prerelease: Boolean,
        assets: List<GitHubReleaseAsset> = listOf(
            asset("OmniTune-$tag-universal-release.apk"),
            asset("OmniTune-$tag-universal-release.apk.sha256"),
        ),
    ): GitHubRelease = GitHubRelease(
        tagName = tag,
        name = tag,
        body = "notes",
        publishedAt = "2026-09-08T00:00:00Z",
        prerelease = prerelease,
        draft = false,
        assets = assets,
    )

    private fun asset(name: String): GitHubReleaseAsset =
        GitHubReleaseAsset(
            name = name,
            browserDownloadUrl = "https://example.test/$name",
            size = 42L,
            contentType = "application/vnd.android.package-archive",
            digest = null,
        )
}
