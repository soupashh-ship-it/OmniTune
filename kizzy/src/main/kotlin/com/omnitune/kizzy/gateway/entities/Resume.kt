/*
 * OmniTune - An open-source music player for Android
 * Licensed under GPL-3.0
 */

package com.omnitune.kizzy.gateway.entities

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Resume(
    @SerialName("seq") val seq: Int,
    @SerialName("session_id") val sessionId: String?,
    @SerialName("token") val token: String,
)
