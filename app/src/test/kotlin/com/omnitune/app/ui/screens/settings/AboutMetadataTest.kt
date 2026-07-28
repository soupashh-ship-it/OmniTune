/*
 * OmniTune - An open-source music player for Android
 * Licensed under GPL-3.0
 */

package com.omnitune.app.ui.screens.settings

import java.math.BigDecimal
import java.util.Locale
import java.util.TimeZone
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AboutMetadataTest {
    @Test
    fun repositoryAndDeveloperUrlsUseVerifiedOwner() {
        assertEquals("https://github.com/soupashh-ship-it/OmniTune", OmniTuneRepositoryUrl)
        assertEquals("https://github.com/soupashh-ship-it", AboutDestinations.developer.url)
    }

    @Test
    fun discordRowUsesConfiguredDestination() {
        assertEquals("https://discord.gg/aDhxBnfNpX", AboutDestinations.discordUrl)
        assertNull(AboutDestinations.supportUrl)
    }

    @Test
    fun supportUpiUsesConfiguredOmniTuneDestination() {
        assertEquals("shashankbisht352612@oksbi", AboutDestinations.supportUpi?.upiId)
        assertEquals("Shashank Bisht", AboutDestinations.supportUpi?.payeeName)
    }

    @Test
    fun inspirationContainsOnlyVerifiedProjectUrls() {
        val urls = AboutDestinations.inspiration.map { it.url }

        assertTrue(urls.contains(VeluneRepositoryUrl))
        assertTrue(urls.contains(ArchiveTuneRepositoryUrl))
        assertFalse(urls.any { it.isBlank() || it == "#" })
    }

    @Test
    fun installedDateFormattingUsesLocaleDateInsteadOfTimestamp() {
        val originalTz = TimeZone.getDefault()
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"))
        try {
            val formatted = formatInstallDate(1_783_536_000_000L, Locale.US)
            assertEquals("Jul 8, 2026", formatted)
        } finally {
            TimeZone.setDefault(originalTz)
        }
    }

    @Test
    fun invalidInstallDateReturnsUnknown() {
        assertEquals("Unknown", formatInstallDate(0L, Locale.US))
    }

    @Test
    fun upiPaymentUriIncludesDeterministicReferenceAndEncodedParameters() {
        val destination = UpiPaymentDestination(
            upiId = "omnitune@example",
            payeeName = "Omni Tune",
            note = "Support OmniTune development",
        )

        assertEquals(
            "upi://pay?pa=omnitune%40example&pn=Omni%20Tune&am=100.00&cu=INR&tn=Support%20OmniTune%20development&tr=OMNITEST123",
            buildUpiPaymentUri(
                destination = destination,
                amountInr = BigDecimal("100"),
                transactionRef = "OMNITEST123",
            ),
        )
    }

    @Test
    fun upiPaymentUriEncodesUnicodeAndReservedCharactersExactlyOnce() {
        val destination = UpiPaymentDestination(
            upiId = "donor@oksbi",
            payeeName = "Omni & Tune",
            note = "चाय & ☕",
        )

        assertEquals(
            "upi://pay?pa=donor%40oksbi&pn=Omni%20%26%20Tune&am=12.50&cu=INR&tn=%E0%A4%9A%E0%A4%BE%E0%A4%AF%20%26%20%E2%98%95&tr=OMNIUNICODE",
            buildUpiPaymentUri(destination, BigDecimal("12.5"), "OMNIUNICODE"),
        )
    }

    @Test
    fun upiPaymentUriOmitsAmountOnlyWhenCallerDeliberatelyPassesNull() {
        val uri = buildUpiPaymentUri(
            destination = UpiPaymentDestination("omnitune@example", "Omni Tune"),
            transactionRef = "OMNINOAMOUNT",
        )

        assertEquals(
            "upi://pay?pa=omnitune%40example&pn=Omni%20Tune&cu=INR&tn=Support%20OmniTune%20development&tr=OMNINOAMOUNT",
            uri,
        )
    }

    @Test
    fun upiAmountParserUsesLocaleIndependentTwoDecimalFormatting() {
        assertEquals(BigDecimal("12.50"), parseUpiAmount("12.5"))
        assertEquals("12.50", formatUpiAmount(BigDecimal("12.5")))
        assertEquals("100.00", formatUpiAmount(BigDecimal("100")))
    }

    @Test
    fun upiAmountParserRejectsEmptyInvalidAndNonPositiveValues() {
        listOf("", " ", "0", "-1", "1,50", "1.234", ".5", "₹50").forEach { value ->
            assertNull("Expected '$value' to be rejected", parseUpiAmount(value))
        }
        assertNull(formatUpiAmount(BigDecimal("0")))
        assertNull(formatUpiAmount(BigDecimal("1.234")))
    }

    @Test
    fun upiBuilderRejectsInvalidDestinationOrAmount() {
        val valid = UpiPaymentDestination("omnitune@example", "Omni Tune")

        assertNull(buildUpiPaymentUri(valid, BigDecimal("0"), "OMNI1"))
        assertNull(buildUpiPaymentUri(UpiPaymentDestination("not-a-vpa", "Omni Tune"), BigDecimal.ONE, "OMNI1"))
        assertNull(buildUpiPaymentUri(valid, BigDecimal.ONE, " "))
    }

    @Test
    fun launchClassificationNeverTreatsMissingHandlerAsSuccess() {
        assertEquals(UpiPaymentLaunchResult.InvalidRequest, classifyUpiLaunchRequest(null, hasHandler = true))
        assertEquals(UpiPaymentLaunchResult.NoHandler, classifyUpiLaunchRequest("upi://pay?pa=x", hasHandler = false))
        assertEquals(UpiPaymentLaunchResult.LaunchInitiated, classifyUpiLaunchRequest("upi://pay?pa=x", hasHandler = true))
    }
}
