package com.omnitune.app.diagnostics

import org.junit.Assert.assertEquals
import org.junit.Test

class DiagnosticReportExporterTest {

    @Test
    fun `sanitize redacts urls with queries`() {
        val input = "Requesting https://music.youtube.com/v1?key=AIzaSyA for data"
        val expected = "Requesting <REDACTED_URL> for data"
        val actual = DiagnosticReportExporter.sanitize(input)
        assertEquals(expected, actual)
    }

    @Test
    fun `sanitize redacts sensitive tokens`() {
        val input = """
            Headers:
            authorization: secret_key
            cookie=abcdef123;
            visitor_id: 9999
            X-Some-Other-Header: safe_value
        """.trimIndent()
        
        val expected = """
            Headers:
            authorization: <REDACTED>
            cookie: <REDACTED>;
            visitor: <REDACTED>
            X-Some-Other-Header: safe_value
        """.trimIndent()

        val actual = DiagnosticReportExporter.sanitize(input)
        assertEquals(expected, actual)
    }

    @Test
    fun `sanitize limits lines`() {
        val input = (1..300).joinToString("\n") { "Line $it" }
        val actual = DiagnosticReportExporter.sanitize(input)
        val lineCount = actual.lines().size
        assertEquals(200, lineCount)
    }

    @Test
    fun `sanitize keeps safe logs intact`() {
        val input = "Network state: CONNECTED\nPlaying song ID 123"
        val actual = DiagnosticReportExporter.sanitize(input)
        assertEquals(input, actual)
    }
}
