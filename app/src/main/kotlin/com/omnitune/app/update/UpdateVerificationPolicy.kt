package com.omnitune.app.update

import java.util.Locale

internal object UpdateVerificationPolicy {
    private val Sha256Regex = Regex("^[a-f0-9]{64}$")

    fun sha256FromDigest(digest: String?): String? =
        digest
            ?.takeIf { it.startsWith("sha256:", ignoreCase = true) }
            ?.substringAfter(':')
            ?.lowercase(Locale.ROOT)

    fun sha256FromChecksumFile(body: String): String? =
        body
            .trim()
            .split(Regex("\\s+"))
            .firstOrNull()
            ?.lowercase(Locale.ROOT)
            ?.takeIf(::isValidSha256)

    fun isValidSha256(value: String): Boolean =
        Sha256Regex.matches(value)

    fun isExpectedPackage(downloadedPackageName: String?, installedPackageName: String): Boolean =
        downloadedPackageName == installedPackageName

    fun isNewerVersion(downloadedVersionCode: Long, installedVersionCode: Long): Boolean =
        downloadedVersionCode > installedVersionCode
}
