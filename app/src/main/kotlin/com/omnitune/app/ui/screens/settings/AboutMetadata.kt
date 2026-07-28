/*
 * OmniTune - An open-source music player for Android
 * Licensed under GPL-3.0
 */

package com.omnitune.app.ui.screens.settings

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import java.math.BigDecimal
import java.math.RoundingMode
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.text.DateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

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

internal sealed interface UpiPaymentLaunchResult {
    data object LaunchInitiated : UpiPaymentLaunchResult
    data object InvalidRequest : UpiPaymentLaunchResult
    data object NoHandler : UpiPaymentLaunchResult
}

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

/**
 * Parses a user-entered INR amount using a fixed dot-decimal representation.
 *
 * The UI deliberately does not use the device locale here: a UPI URI must use a
 * stable decimal separator regardless of the user's locale.
 */
internal fun parseUpiAmount(value: String): BigDecimal? {
    val normalized = value.trim()
    if (!UPI_AMOUNT_PATTERN.matches(normalized)) return null

    return normalized.toBigDecimalOrNull()
        ?.takeIf { it.signum() > 0 }
        ?.let { amount ->
            runCatching { amount.setScale(2, RoundingMode.UNNECESSARY) }.getOrNull()
        }
}

internal fun formatUpiAmount(amountInr: BigDecimal): String? =
    if (amountInr.signum() <= 0) {
        null
    } else {
        runCatching {
            amountInr
                .setScale(2, RoundingMode.UNNECESSARY)
                .toPlainString()
        }.getOrNull()
    }

/**
 * Creates a UPI URI without Android framework dependencies so the payment
 * contract can be covered by plain JVM tests.
 *
 * - `pa`: payee VPA
 * - `pn`: payee name
 * - `am`: optional, positive INR amount with exactly two decimal places
 * - `cu`: INR currency
 * - `tn`: optional transaction note
 * - `tr`: OmniTune's unique transaction reference; launching an app does not
 *   mean the transaction succeeded or was completed.
 */
internal fun buildUpiPaymentUri(
    destination: UpiPaymentDestination,
    amountInr: BigDecimal? = null,
    transactionRef: String = newUpiTransactionReference(),
): String? {
    val upiId = destination.upiId.trim()
    val payeeName = destination.payeeName.trim()
    val reference = transactionRef.trim()
    if (!isValidUpiDestination(upiId, payeeName) || reference.isBlank()) return null

    val amount = amountInr?.let(::formatUpiAmount) ?: if (amountInr == null) null else return null
    val parameters = buildList {
        add("pa" to upiId)
        add("pn" to payeeName)
        amount?.let { add("am" to it) }
        add("cu" to "INR")
        destination.note.trim().takeIf { it.isNotBlank() }?.let { add("tn" to it) }
        add("tr" to reference)
    }

    return "upi://pay?" + parameters.joinToString("&") { (name, value) ->
        "${name.encodeUriComponent()}=${value.encodeUriComponent()}"
    }
}

internal fun classifyUpiLaunchRequest(uri: String?, hasHandler: Boolean): UpiPaymentLaunchResult =
    when {
        uri.isNullOrBlank() -> UpiPaymentLaunchResult.InvalidRequest
        !hasHandler -> UpiPaymentLaunchResult.NoHandler
        else -> UpiPaymentLaunchResult.LaunchInitiated
    }

/**
 * Opens a payment application only. A successful return means Android accepted
 * the activity launch; it never means the user completed a payment.
 */
internal fun Context.launchUpiPayment(
    destination: UpiPaymentDestination,
    amountInr: BigDecimal,
): UpiPaymentLaunchResult {
    val uriText = buildUpiPaymentUri(destination, amountInr)
    val uri = uriText?.let { runCatching { Uri.parse(it) }.getOrNull() }
        ?: return UpiPaymentLaunchResult.InvalidRequest
    val intent = Intent(Intent.ACTION_VIEW, uri)
    val preflight = classifyUpiLaunchRequest(
        uri = uriText,
        hasHandler = runCatching {
            // resolveActivity() can return Android's chooser/resolver when no
            // payment app is installed. queryIntentActivities() can also list
            // disabled payment activities. Only an enabled, concrete activity
            // is a handler that can receive this UPI request.
            packageManager
                .queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY)
                .any { resolveInfo ->
                    resolveInfo.activityInfo.enabled &&
                        resolveInfo.activityInfo.applicationInfo.enabled
                }
        }.getOrDefault(false),
    )
    if (preflight != UpiPaymentLaunchResult.LaunchInitiated) return preflight

    return try {
        startActivity(intent)
        UpiPaymentLaunchResult.LaunchInitiated
    } catch (_: ActivityNotFoundException) {
        UpiPaymentLaunchResult.NoHandler
    } catch (_: SecurityException) {
        UpiPaymentLaunchResult.NoHandler
    }
}

internal fun Context.copyUpiId(upiId: String): Boolean {
    if (upiId.isBlank()) return false
    return runCatching {
        val clipboard = getSystemService(android.content.ClipboardManager::class.java) ?: return false
        clipboard.setPrimaryClip(android.content.ClipData.newPlainText("OmniTune UPI ID", upiId))
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

private fun isValidUpiDestination(upiId: String, payeeName: String): Boolean =
    upiId.isNotBlank() &&
        payeeName.isNotBlank() &&
        upiId.contains('@') &&
        upiId.none(Char::isWhitespace)

private fun newUpiTransactionReference(): String =
    "OMNI${UUID.randomUUID().toString().replace("-", "").take(20)}"

private val UPI_AMOUNT_PATTERN = Regex("""(?:0|[1-9]\d*)(?:\.\d{1,2})?""")
