package com.omnitune.app.auth

internal object LoginSessionVerificationPolicy {
    fun failureMessage(
        cookieHeader: String,
        visitorData: String?,
        dataSyncId: String?,
        accountName: String?,
    ): String? = when {
        !LoginWebViewPolicy.hasRequiredSessionCookie(cookieHeader) ->
            "The YouTube Music session cookie was not available."
        visitorData.isNullOrBlank() ->
            "YouTube Music did not provide a visitor session."
        dataSyncId.isNullOrBlank() ->
            "YouTube Music did not provide an account sync session."
        accountName.isNullOrBlank() ->
            "YouTube Music did not confirm the signed-in account."
        else -> null
    }
}
