package com.omnitune.app.playback.recovery

import androidx.media3.common.PlaybackException
import androidx.media3.datasource.HttpDataSource
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.IOException

class PlaybackErrorClassifierTest {

    @Test
    fun testClassify_networkConnectionFailed_returnsNetworkError() {
        val error = PlaybackException("Network failed", null, PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED)
        val type = PlaybackErrorClassifier.classify(error)
        assertEquals(PlaybackErrorType.NetworkError, type)
    }

    @Test
    fun testClassify_networkConnectionTimeout_returnsNetworkError() {
        val error = PlaybackException("Network timeout", null, PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT)
        val type = PlaybackErrorClassifier.classify(error)
        assertEquals(PlaybackErrorType.NetworkError, type)
    }

    @Test
    fun testClassify_bufferingTimeoutWithNetworkTimeoutCode_returnsTimeout() {
        val error = PlaybackException("Buffering timeout", null, PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT)
        val type = PlaybackErrorClassifier.classify(error)
        assertEquals(PlaybackErrorType.Timeout, type)
    }

    @Test
    fun testClassify_badHttpStatus_returnsNetworkError() {
        val error = PlaybackException("Bad HTTP status", null, PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS)
        val type = PlaybackErrorClassifier.classify(error)
        assertEquals(PlaybackErrorType.NetworkError, type)
    }

    @Test
    fun testClassify_errorMessageContainsError2000_returnsError2000() {
        val error = PlaybackException("Some error 2000 occurred", null, PlaybackException.ERROR_CODE_UNSPECIFIED)
        val type = PlaybackErrorClassifier.classify(error)
        assertEquals(PlaybackErrorType.Error2000, type)
    }

    @Test
    fun testClassify_httpDataSourceInvalidResponseCode403_returnsForbidden403() {
        val cause = HttpDataSource.InvalidResponseCodeException(
            403,
            "Forbidden",
            null,
            emptyMap(),
            org.mockito.Mockito.mock(androidx.media3.datasource.DataSpec::class.java),
            ByteArray(0)
        )
        val error = PlaybackException("HTTP Error", cause, PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS)
        val type = PlaybackErrorClassifier.classify(error)
        assertEquals(PlaybackErrorType.Forbidden403, type)
    }

    @Test
    fun testClassify_httpDataSourceInvalidResponseCode404_returnsNotFound404() {
        val cause = HttpDataSource.InvalidResponseCodeException(
            404,
            "Not Found",
            null,
            emptyMap(),
            org.mockito.Mockito.mock(androidx.media3.datasource.DataSpec::class.java),
            ByteArray(0)
        )
        val error = PlaybackException("HTTP Error", cause, PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS)
        val type = PlaybackErrorClassifier.classify(error)
        assertEquals(PlaybackErrorType.NotFound404, type)
    }

    @Test
    fun testClassify_httpDataSourceInvalidResponseCode429_returnsBotCheck() {
        val cause = HttpDataSource.InvalidResponseCodeException(
            429,
            "Too Many Requests",
            null,
            emptyMap(),
            org.mockito.Mockito.mock(androidx.media3.datasource.DataSpec::class.java),
            ByteArray(0)
        )
        val error = PlaybackException("HTTP Error", cause, PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS)
        val type = PlaybackErrorClassifier.classify(error)
        assertEquals(PlaybackErrorType.BotCheck, type)
    }

    @Test
    fun testClassify_messageContainsSignatureExpired_returnsSignatureExpired() {
        val error = PlaybackException("signature has expired", null, PlaybackException.ERROR_CODE_UNSPECIFIED)
        val type = PlaybackErrorClassifier.classify(error)
        assertEquals(PlaybackErrorType.SignatureExpired, type)
    }

    @Test
    fun testClassify_messageContainsBotCheck_returnsBotCheck() {
        val error = PlaybackException("Sign in to confirm you're not a bot", null, PlaybackException.ERROR_CODE_UNSPECIFIED)
        val type = PlaybackErrorClassifier.classify(error)
        assertEquals(PlaybackErrorType.BotCheck, type)
    }

    @Test
    fun testClassify_messageContainsTimeout_returnsTimeout() {
        val cause = IOException("socket timeout")
        val error = PlaybackException("Something went wrong", cause, PlaybackException.ERROR_CODE_UNSPECIFIED)
        val type = PlaybackErrorClassifier.classify(error)
        assertEquals(PlaybackErrorType.Timeout, type)
    }

    @Test
    fun testClassify_unknownError_returnsUnknown() {
        val error = PlaybackException("Just some random error", null, PlaybackException.ERROR_CODE_UNSPECIFIED)
        val type = PlaybackErrorClassifier.classify(error)
        assertEquals(PlaybackErrorType.Unknown, type)
    }
}
