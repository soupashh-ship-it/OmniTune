/*
 * OmniTune - An open-source music player for Android
 * Licensed under GPL-3.0
 */

package com.omnitune.app.ui.screens.settings

import android.content.Context
import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.text.DateFormat
import java.util.Date
import java.util.Locale

internal const val OmniTuneRepositoryUrl = "https://github.com/soupashh-ship-it/OmniTune"
internal const val OmniTuneDeveloperUrl = "https://github.com/soupashh-ship-it"
internal const val OmniTuneDeveloperAvatarUrl = "https://github.com/soupashh-ship-it.png"
internal const val OmniTuneLicenseUrl = "https://www.gnu.org/licenses/gpl-3.0.html"
internal const val VeluneRepositoryUrl = "https://github.com/nikhilvishwakarma00/Velune"
internal const val VeluneAvatarUrl = "https://github.com/nikhilvishwakarma00.png"
internal const val ArchiveTuneRepositoryUrl = "https://github.com/koiverse/ArchiveTune"
internal const val ArchiveTuneAvatarUrl = "https://github.com/koiverse.png"

internal data class AboutLinkEntry(
    val title: String,
    val subtitle: String,
    val url: String,
    val iconRes: Int? = null,
    val initials: String? = null,
    val imageUrl: String? = null,
)

internal data class UpiPaymentDestination(
    val upiId: String,
    val payeeName: String,
    val note: String = "Support OmniTune development",
)

internal object AboutDestinations {
    val developer = AboutLinkEntry(
        title = "soupashh-ship-it",
        subtitle = "OmniTune maintainer",
        url = OmniTuneDeveloperUrl,
        initials = "S",
        imageUrl = OmniTuneDeveloperAvatarUrl,
    )

    val inspiration = listOf(
        AboutLinkEntry(
            title = "Velune",
            subtitle = "Open-source UI, playback, and queue inspiration",
            url = VeluneRepositoryUrl,
            initials = "V",
            imageUrl = VeluneAvatarUrl,
        ),
        AboutLinkEntry(
            title = "ArchiveTune",
            subtitle = "Upstream framework inspiration acknowledged by the reference project",
            url = ArchiveTuneRepositoryUrl,
            initials = "A",
            imageUrl = ArchiveTuneAvatarUrl,
        ),
    )

    val discordUrl: String? = "https://discord.gg/aDhxBnfNpX"
    val supportUrl: String? = null
    val supportUpi: UpiPaymentDestination? = UpiPaymentDestination(
        upiId = "shashankbisht352612@oksbi",
        payeeName = "Shashank Bisht",
    )
}

internal const val DEFAULT_DONATION_AMOUNT = 100
internal val DONATION_PRESET_AMOUNTS = listOf(50, 100, 250, 500)

internal fun Context.openExternalUrl(url: String): Boolean {
    if (url.isBlank()) return false
    val uri = runCatching { Uri.parse(url) }.getOrNull() ?: return false
    val scheme = uri.scheme?.lowercase(Locale.US)
    if (scheme !in setOf("http", "https", "upi")) return false

    val intent = Intent(Intent.ACTION_VIEW, uri)
    return runCatching {
        startActivity(intent)
        true
    }.getOrDefault(false)
}

internal fun buildUpiPaymentUri(
    destination: UpiPaymentDestination,
    amountInr: Int? = null,
    transactionRef: String? = null,
): String {
    return Uri.Builder()
        .scheme("upi")
        .authority("pay")
        .appendQueryParameter("pa", destination.upiId)
        .appendQueryParameter("pn", destination.payeeName)
        .appendQueryParameter("tn", destination.note)
        .appendQueryParameter("cu", "INR")
        .appendQueryParameter("tr", transactionRef ?: "OMNI${System.currentTimeMillis()}")
        .apply {
            if (amountInr != null && amountInr > 0) {
                appendQueryParameter("am", "${amountInr}.00")
            }
        }
        .build()
        .toString()
}

internal fun Context.openUpiPayment(
    destination: UpiPaymentDestination,
    amountInr: Int? = null,
): Boolean {
    if (destination.upiId.isBlank() || destination.payeeName.isBlank()) return false
    val uri = runCatching { Uri.parse(buildUpiPaymentUri(destination, amountInr)) }.getOrNull() ?: return false
    val intent = Intent(Intent.ACTION_VIEW, uri)
    return runCatching {
        startActivity(intent)
        true
    }.getOrDefault(false)
}


internal fun formatInstallDate(firstInstallTime: Long, locale: Locale = Locale.getDefault()): String {
    if (firstInstallTime <= 0L) return "Unknown"
    return DateFormat
        .getDateInstance(DateFormat.MEDIUM, locale)
        .format(Date(firstInstallTime))
}

internal fun installedDateLabel(context: Context): String =
    runCatching {
        formatInstallDate(context.packageManager.omniPackageInfo(context.packageName).firstInstallTime)
    }.getOrDefault("Unknown")

@Suppress("DEPRECATION")
private fun PackageManager.omniPackageInfo(packageName: String): PackageInfo =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(0))
    } else {
        getPackageInfo(packageName, 0)
    }

private fun String.encodeUriComponent(): String =
    URLEncoder.encode(this, StandardCharsets.UTF_8.toString()).replace("+", "%20")
