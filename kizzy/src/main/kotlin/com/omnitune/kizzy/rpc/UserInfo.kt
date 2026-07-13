/*
 * OmniTune - An open-source music player for Android
 * Licensed under GPL-3.0
 */

package com.omnitune.kizzy.rpc

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UserInfo(
    val id: String = "",
    val username: String = "",
    @SerialName("global_name") val globalName: String? = null,
    val avatar: String? = null,
    val discriminator: String = "0",
)
