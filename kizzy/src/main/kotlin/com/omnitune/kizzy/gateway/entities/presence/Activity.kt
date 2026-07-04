/*
 * OmniTune - An open-source music player for Android
 * Licensed under GPL-3.0
 */

package com.omnitune.kizzy.gateway.entities.presence

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Activity(
    val name: String = "Listening to OmniTune",
    val type: Int = 2,
    val url: String? = null,
    val state: String? = null,
    val details: String? = null,
    val assets: Assets? = null,
    val timestamps: Timestamps? = null,
    val buttons: List<String>? = null,
    val metadata: Metadata? = null,
)
