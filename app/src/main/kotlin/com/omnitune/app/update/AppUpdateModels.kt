package com.omnitune.app.update

import java.io.File

data class GitHubRelease(
    val tagName: String,
    val name: String,
    val body: String,
    val publishedAt: String,
    val prerelease: Boolean,
    val draft: Boolean,
    val assets: List<GitHubReleaseAsset>,
)

data class GitHubReleaseAsset(
    val name: String,
    val browserDownloadUrl: String,
    val size: Long,
    val contentType: String?,
    val digest: String?,
)

data class AppUpdateInfo(
    val versionName: String,
    val releaseName: String,
    val releaseNotes: String,
    val publishedAt: String,
    val apkAsset: GitHubReleaseAsset,
    val sha256Asset: GitHubReleaseAsset?,
)

data class DownloadedUpdate(
    val updateInfo: AppUpdateInfo,
    val apkFile: File,
    val packageName: String,
    val versionCode: Long,
)

sealed class UpdateState {
    data object Idle : UpdateState()
    data object Checking : UpdateState()
    data object NoUpdate : UpdateState()
    data class UpdateAvailable(
        val update: AppUpdateInfo,
        val requireMeteredConfirmation: Boolean = false,
    ) : UpdateState()
    data class Downloading(val progress: Float) : UpdateState()
    data class Downloaded(val update: DownloadedUpdate) : UpdateState()
    data class Error(val message: String) : UpdateState()
}
