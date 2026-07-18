/*
 * OmniTune - An open-source music player for Android
 * Licensed under GPL-3.0
 */

package com.omnitune.app.diagnostics

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import androidx.core.content.FileProvider
import com.omnitune.app.BuildConfig
import java.io.File
import java.time.Instant

object DiagnosticReportExporter {
    private val queryUrlPattern = """https?://[^\s"']+\?[^\s"']+""".toRegex(RegexOption.IGNORE_CASE)
    private val sensitiveHeaderPattern = """(?i)\b(authorization|cookie)\s*[:=]\s*.*""".toRegex()
    private val sensitiveKeyValuePattern =
        """(?i)\b((?:access|refresh|id)[_-]?token|x[_-]?api[_-]?(?:key|token)|api[_-]?key|po[_-]?token|potoken|token|session|password|keystore|key_password|visitor)[\w-]*\s*[:=]\s*([^\s,;"']+)""".toRegex()
    private val bearerTokenPattern = """(?i)\bbearer\s+[A-Za-z0-9._~+/=-]+""".toRegex()

    fun createShareIntent(context: Context): Intent {
        val reportFile = writeReport(context)
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.updatefileprovider",
            reportFile,
        )

        return Intent.createChooser(
            Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_SUBJECT, "OmniTune diagnostic report")
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            },
            "Share diagnostic report",
        )
    }

    private fun writeReport(context: Context): File {
        val directory = File(context.cacheDir, "diagnostics").apply {
            deleteRecursively()
            mkdirs()
        }
        val reportFile = File(directory, "omnitune-diagnostic-report.txt")
        reportFile.writeText(buildReport(context))
        return reportFile
    }

    private fun buildReport(context: Context): String = buildString {
        appendLine("OmniTune Diagnostic Report")
        appendLine("Generated: ${Instant.now()}")
        appendLine()
        appendLine("App")
        appendLine("- package: ${context.packageName}")
        appendLine("- versionName: ${BuildConfig.VERSION_NAME}")
        appendLine("- versionCode: ${BuildConfig.VERSION_CODE}")
        appendLine("- buildType: ${BuildConfig.BUILD_TYPE}")
        appendLine("- debug: ${BuildConfig.DEBUG}")
        appendLine()
        appendLine("Device")
        appendLine("- manufacturer: ${Build.MANUFACTURER}")
        appendLine("- model: ${Build.MODEL}")
        appendLine("- sdk: ${Build.VERSION.SDK_INT}")
        appendLine("- android: ${Build.VERSION.RELEASE}")
        appendLine()
        appendLine("Network")
        appendLine("- state: ${networkState(context)}")
        appendLine()
        appendLine("Recent logs")
        appendLine(sanitizedLogs())
    }

    private fun networkState(context: Context): String {
        val connectivityManager = context.getSystemService(ConnectivityManager::class.java)
            ?: return "unknown"
        val network = connectivityManager.activeNetwork ?: return "offline"
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return "unknown"
        val transports = buildList {
            if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) add("wifi")
            if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) add("cellular")
            if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_VPN)) add("vpn")
            if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)) add("ethernet")
        }
        val validated = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
        return "${transports.ifEmpty { listOf("other") }.joinToString(",")}; validated=$validated"
    }

    private fun sanitizedLogs(): String = runCatching {
        val process = ProcessBuilder("logcat", "-d", "-t", "200").redirectErrorStream(true).start()
        val output = process.inputStream.bufferedReader().use { it.readText() }
        process.destroy()
        sanitize(output).ifBlank { "No recent app-readable logs available." }
    }.getOrElse {
        "Recent logs unavailable: ${it.javaClass.simpleName}"
    }

    @androidx.annotation.VisibleForTesting
    internal fun sanitize(text: String): String {
        return text.lineSequence().take(200).joinToString("\n")
            .replace(queryUrlPattern, "<REDACTED_URL>")
            .replace(sensitiveHeaderPattern) { "${it.groupValues[1]}: <REDACTED>" }
            .replace(sensitiveKeyValuePattern) { "${it.groupValues[1]}: <REDACTED>" }
            .replace(bearerTokenPattern, "Bearer <REDACTED>")
    }
}
