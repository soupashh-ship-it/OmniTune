/*
 * OmniTune - An open-source music player for Android
 * Licensed under GPL-3.0
 * Licensed Under GPL-3.0
 */



package com.omnitune.app.models

import com.omnitune.app.innertube.models.YTItem

data class ItemsPage(
    val items: List<YTItem>,
    val continuation: String?,
)
