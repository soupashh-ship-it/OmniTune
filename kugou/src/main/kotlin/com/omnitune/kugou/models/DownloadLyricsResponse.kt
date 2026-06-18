/*
 * OmniTune - An open-source music player for Android
 * Licensed under GPL-3.0 / Licensed Under GPL-3.0
 */

package com.omnitune.kugou.models

import kotlinx.serialization.Serializable

@Serializable
data class DownloadLyricsResponse(
    val content: String,
)
