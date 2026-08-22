package com.omnitune.app.update

import android.content.Context
import android.os.Build
import androidx.core.content.pm.PackageInfoCompat
import com.omnitune.app.BuildConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ApkDownloadManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val okHttpClient: OkHttpClient,
) {
    suspend fun downloadUpdate(
        updateInfo: AppUpdateInfo,
        onProgress: (Float) -> Unit,
    ): DownloadedUpdate = withContext(Dispatchers.IO) {
        val updateDir = File(context.cacheDir, UPDATE_CACHE_DIR)
        if (!updateDir.exists()) updateDir.mkdirs()
        updateDir.listFiles()?.forEach { file ->
            if (file.isFile) file.delete()
        }

        val apkFile = File(updateDir, updateInfo.apkAsset.name)
        downloadFile(updateInfo.apkAsset.browserDownloadUrl, apkFile, updateInfo.apkAsset.size, onProgress)
        verifyDownloadedFile(updateInfo.apkAsset, apkFile)
        verifySha256(updateInfo, apkFile)
        verifyPackage(updateInfo, apkFile)
    }

    private fun downloadFile(
        url: String,
        target: File,
        expectedSize: Long,
        onProgress: (Float) -> Unit,
    ) {
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "OmniTune-Android")
            .build()
        okHttpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IllegalStateException("Could not download update. HTTP ${response.code}")
            }
            val body = response.body
            val totalBytes = expectedSize.takeIf { it > 0 } ?: body.contentLength()
            var downloaded = 0L
            target.outputStream().use { output ->
                body.byteStream().use { input ->
                    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                    while (true) {
                        val read = input.read(buffer)
                        if (read == -1) break
                        output.write(buffer, 0, read)
                        downloaded += read
                        if (totalBytes > 0) {
                            onProgress((downloaded.toFloat() / totalBytes).coerceIn(0f, 1f))
                        }
                    }
                }
            }
        }
        onProgress(1f)
    }

    private fun verifyDownloadedFile(asset: GitHubReleaseAsset, apkFile: File) {
        if (!apkFile.exists() || apkFile.length() <= 0L) {
            throw IllegalStateException("Downloaded update is invalid.")
        }
        if (asset.size > 0L && apkFile.length() != asset.size) {
            apkFile.delete()
            throw IllegalStateException("Downloaded update size does not match GitHub release asset.")
        }
    }

    private fun verifySha256(updateInfo: AppUpdateInfo, apkFile: File) {
        val digest = updateInfo.apkAsset.digest.orEmpty()
        val expected = if (digest.startsWith("sha256:", ignoreCase = true)) {
            digest.substringAfter("sha256:").lowercase()
        } else {
            updateInfo.sha256Asset?.let { asset ->
                downloadSha256(asset.browserDownloadUrl)
            }
        }

        // Fail closed: never hand an unverifiable APK to the package installer.
        if (expected.isNullOrBlank()) {
            apkFile.delete()
            throw IllegalStateException(
                "This update cannot be verified because no SHA-256 checksum was published with the release."
            )
        }
        if (!expected.matches(Regex("^[a-f0-9]{64}$"))) {
            apkFile.delete()
            throw IllegalStateException("Update checksum format is invalid.")
        }

        val actual = sha256(apkFile)
        if (actual != expected) {
            apkFile.delete()
            throw IllegalStateException("Downloaded update failed SHA-256 verification.")
        }
    }

    private fun downloadSha256(url: String): String {
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "OmniTune-Android")
            .build()
        okHttpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IllegalStateException("Could not verify update checksum.")
            }
            val body = response.body.string().trim()
            return body.split(Regex("\\s+")).firstOrNull()
                ?.lowercase()
                ?.takeIf { it.matches(Regex("^[a-f0-9]{64}$")) }
                ?: throw IllegalStateException("Update checksum file is invalid.")
        }
    }

    private fun verifyPackage(updateInfo: AppUpdateInfo, apkFile: File): DownloadedUpdate {
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            android.content.pm.PackageManager.GET_SIGNING_CERTIFICATES
        } else {
            @Suppress("DEPRECATION")
            android.content.pm.PackageManager.GET_SIGNATURES
        }
        val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.packageManager.getPackageArchiveInfo(
                apkFile.path,
                android.content.pm.PackageManager.PackageInfoFlags.of(flags.toLong()),
            )
        } else {
            @Suppress("DEPRECATION")
            context.packageManager.getPackageArchiveInfo(apkFile.path, flags)
        } ?: throw IllegalStateException("Downloaded update is invalid.")

        if (!packageInfo.hasSigningCertificate()) {
            apkFile.delete()
            throw IllegalStateException("Downloaded update is not signed.")
        }

        val downloadedPackageName = packageInfo.packageName
        if (downloadedPackageName != context.packageName) {
            apkFile.delete()
            throw IllegalStateException("This update package does not match OmniTune.")
        }

        val downloadedVersionCode = PackageInfoCompat.getLongVersionCode(packageInfo)
        if (downloadedVersionCode <= BuildConfig.VERSION_CODE.toLong()) {
            apkFile.delete()
            throw IllegalStateException("This update is not newer than your installed version.")
        }

        val installedPackageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.packageManager.getPackageInfo(
                context.packageName,
                android.content.pm.PackageManager.PackageInfoFlags.of(flags.toLong()),
            )
        } else {
            @Suppress("DEPRECATION")
            context.packageManager.getPackageInfo(context.packageName, flags)
        }
        if (packageInfo.signingCertificateHashes() != installedPackageInfo.signingCertificateHashes()) {
            apkFile.delete()
            throw IllegalStateException(
                "Android blocked this update because the installed app and the update APK are signed differently. " +
                    "If you installed an older test build, uninstall it once and install the new secure release."
            )
        }

        return DownloadedUpdate(
            updateInfo = updateInfo,
            apkFile = apkFile,
            packageName = downloadedPackageName,
            versionCode = downloadedVersionCode,
        )
    }

    private fun android.content.pm.PackageInfo.hasSigningCertificate(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val info = signingInfo ?: return false
            info.apkContentsSigners?.isNotEmpty() == true ||
                info.signingCertificateHistory?.isNotEmpty() == true
        } else {
            @Suppress("DEPRECATION")
            signatures?.isNotEmpty() == true
        }
    }

    private fun android.content.pm.PackageInfo.signingCertificateHashes(): Set<String> {
        val certificates = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val info = signingInfo ?: return emptySet()
            val signers = info.apkContentsSigners?.takeIf { it.isNotEmpty() }
                ?: info.signingCertificateHistory
            signers.orEmpty().map { it.toByteArray() }
        } else {
            @Suppress("DEPRECATION")
            signatures.orEmpty().map { it.toByteArray() }
        }
        return certificates.map { bytes ->
            MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }
        }.toSet()
    }

    private fun sha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            while (true) {
                val read = input.read(buffer)
                if (read == -1) break
                digest.update(buffer, 0, read)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    private companion object {
        const val UPDATE_CACHE_DIR = "updates"
    }
}
