/*
 * OmniTune - An open-source music player for Android
 * Licensed under GPL-3.0
 */

package com.omnitune.kizzy.gateway.entities

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Identify(
    val capabilities: Int = 65,
    val compress: Boolean = false,
    @SerialName("large_threshold") val largeThreshold: Int = 100,
    val properties: Properties = Properties(),
    val token: String,
)

@Serializable
data class Properties(
    val browser: String = "omnitune",
    val device: String = "omnitune",
    val os: String = "android",
)

fun String.toIdentifyPayload() = Identify(token = this)
