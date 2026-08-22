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
            access_token=abcd
            X-Some-Other-Header: safe_value
        """.trimIndent()
        
        val expected = """
            Headers:
            authorization: <REDACTED>
            cookie: <REDACTED>
            visitor_id: <REDACTED>
            access_token: <REDACTED>
            X-Some-Other-Header: safe_value
        """.trimIndent()

        val actual = DiagnosticReportExporter.sanitize(input)
        assertEquals(expected, actual)
    }

    @Test
    fun `sanitize redacts bearer and common auth formats`() {
        val input = """
            Authorization: Bearer abc.def-123
            Cookie: SID=secret; PREF=secret
            standalone bearer xyz.123_456
            X-Api-Token=top_secret
            api_key=AIzaSySecret
        """.trimIndent()

        val expected = """
            Authorization: <REDACTED>
            Cookie: <REDACTED>
            standalone Bearer <REDACTED>
            X-Api-Token: <REDACTED>
            api_key: <REDACTED>
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
    fun `sanitize redacts search query values`() {
        val input = "query=private song title\nsearch_query: private artist\nq=private album"

        val actual = DiagnosticReportExporter.sanitize(input)

        assertEquals("query: <REDACTED>\nsearch_query: <REDACTED>\nq: <REDACTED>", actual)
    }

    @Test
    fun `sanitize redacts account and provider identifiers`() {
        val input = "user_id=private-user\nchannelId: private-channel\nvisitorData=private-visitor"

        val actual = DiagnosticReportExporter.sanitize(input)

        assertEquals(
            "user_id: <REDACTED>\nchannelId: <REDACTED>\nvisitorData: <REDACTED>",
            actual,
        )
    }

    @Test
    fun `sanitize redacts quoted json credentials queries and identifiers`() {
        val input = """{"access_token":"secret token", "query":"private song title", "channelId":"UC-secret"}"""

        val actual = DiagnosticReportExporter.sanitize(input)

        assertEquals(
            """{"access_token":<REDACTED>, "query":<REDACTED>, "channelId":<REDACTED>}""",
            actual,
        )
    }

    @Test
    fun `sanitize keeps safe logs intact`() {
        val input = "Network state: CONNECTED\nPlaying song ID 123"
        val actual = DiagnosticReportExporter.sanitize(input)
        assertEquals(input, actual)
    }
}
