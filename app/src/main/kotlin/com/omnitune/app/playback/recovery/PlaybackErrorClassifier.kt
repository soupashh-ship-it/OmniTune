package com.omnitune.app.playback.recovery

import androidx.media3.common.PlaybackException
import androidx.media3.datasource.HttpDataSource

enum class PlaybackErrorType {
    Forbidden403,
    NotFound404,
    Error2000,
    SignatureExpired,
    NetworkError,
    Timeout,
    BotCheck,
    Unknown
}

object PlaybackErrorClassifier {
    fun classify(error: PlaybackException): PlaybackErrorType {
        val cause = error.cause
        
        if (error.errorCode == PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED ||
            error.errorCode == PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT) {
            return PlaybackErrorType.NetworkError
        }

        val message = "${error.message.orEmpty()} ${cause?.message.orEmpty()}".lowercase()
        if (message.contains("error 2000") || message.contains("errorcode=2000")) {
            return PlaybackErrorType.Error2000
        }
        
        if (cause is HttpDataSource.InvalidResponseCodeException) {
            when (cause.responseCode) {
                403 -> return PlaybackErrorType.Forbidden403
                404 -> return PlaybackErrorType.NotFound404
                429 -> return PlaybackErrorType.BotCheck
            }
        }

        if (error.errorCode == PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS) {
            return PlaybackErrorType.NetworkError
        }
        
        if (message.contains("signature") && message.contains("expired")) {
            return PlaybackErrorType.SignatureExpired
        }
        if (message.contains("bot") || message.contains("captcha") || message.contains("sign in to confirm")) {
            return PlaybackErrorType.BotCheck
        }
        if (message.contains("timeout")) {
            return PlaybackErrorType.Timeout
        }

        return PlaybackErrorType.Unknown
    }
}
