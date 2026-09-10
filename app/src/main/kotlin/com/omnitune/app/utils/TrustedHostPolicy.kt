package com.omnitune.app.utils

import java.net.IDN
import java.util.Locale

object TrustedHostPolicy {
    fun isExactHost(host: String?, expectedHost: String): Boolean {
        return normalizeHost(host) == normalizeHost(expectedHost)
    }

    fun isExactHostOrSubdomain(host: String?, rootDomain: String): Boolean {
        val normalizedHost = normalizeHost(host) ?: return false
        val normalizedRoot = normalizeHost(rootDomain) ?: return false
        return normalizedHost == normalizedRoot || normalizedHost.endsWith(".$normalizedRoot")
    }

    fun normalizeHost(host: String?): String? =
        host
            ?.trim()
            ?.trimEnd('.')
            ?.takeIf { it.isNotBlank() }
            ?.let { runCatching { IDN.toASCII(it) }.getOrDefault(it) }
            ?.lowercase(Locale.US)
}
