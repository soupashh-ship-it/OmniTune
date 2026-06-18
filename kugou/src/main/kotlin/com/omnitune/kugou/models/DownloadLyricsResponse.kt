/*
 * OmniTune - based on Velune
 * Nikhil / Licensed Under GPL-3.0
 */

package com.omnitune.kugou.models

import kotlinx.serialization.Serializable

@Serializable
data class DownloadLyricsResponse(
    val content: String,
)
