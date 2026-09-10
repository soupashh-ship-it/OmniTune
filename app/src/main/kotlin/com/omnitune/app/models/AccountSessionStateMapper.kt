package com.omnitune.app.models

import com.omnitune.innertube.utils.parseCookieString

data class AccountSessionState(
    val isLoggedIn: Boolean,
    val userName: String?,
    val userAvatarUrl: String?,
)

object AccountSessionStateMapper {
    fun fromStoredAccount(
        plainCookie: String?,
        accountName: String?,
        accountEmail: String?,
        channelHandle: String?,
        avatarUrl: String? = null,
    ): AccountSessionState {
        val isLoggedIn = hasSignedInCookie(plainCookie)
        val displayName = if (isLoggedIn) {
            firstNonBlank(accountName, channelHandle, accountEmail, "YouTube Music")
        } else {
            null
        }

        return AccountSessionState(
            isLoggedIn = isLoggedIn,
            userName = displayName,
            userAvatarUrl = avatarUrl?.takeIf { isLoggedIn && it.isNotBlank() },
        )
    }

    fun hasSignedInCookie(plainCookie: String?): Boolean =
        plainCookie?.let { "SAPISID" in parseCookieString(it) } ?: false

    private fun firstNonBlank(vararg values: String?): String? =
        values.firstOrNull { !it.isNullOrBlank() }?.trim()
}
