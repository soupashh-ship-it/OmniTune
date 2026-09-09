/*
 * OmniTune - An open-source music player for Android
 * Licensed under GPL-3.0
 */

package com.omnitune.app.utils

internal data class SensitivePreferenceRead(
    val plainValue: String,
    val migratedStorageValue: String?,
)

internal object SensitivePreferenceCodec {
    fun decodeForRead(
        rawValue: String?,
        isEncrypted: (String?) -> Boolean,
        decryptOrPlain: (String?) -> String,
        encrypt: (String) -> String,
    ): SensitivePreferenceRead {
        val plainValue = decryptOrPlain(rawValue).trim()
        val migratedStorageValue = if (
            !rawValue.isNullOrBlank() &&
            !isEncrypted(rawValue) &&
            plainValue.isNotBlank()
        ) {
            encrypt(plainValue)
        } else {
            null
        }

        return SensitivePreferenceRead(
            plainValue = plainValue,
            migratedStorageValue = migratedStorageValue,
        )
    }

    fun encodeForStorage(
        plainValue: String,
        encrypt: (String) -> String,
    ): String? =
        plainValue.trim()
            .takeIf { it.isNotBlank() }
            ?.let(encrypt)

    fun maskedPreview(plainValue: String): String {
        val trimmed = plainValue.trim()
        if (trimmed.isBlank()) return ""
        val suffix = trimmed.takeLast(4)
        return if (trimmed.length <= 4) "****" else "****$suffix"
    }
}
