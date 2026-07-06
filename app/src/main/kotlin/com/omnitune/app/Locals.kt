package com.omnitune.app

import androidx.compose.runtime.compositionLocalOf
import com.omnitune.app.db.MusicDatabase
import com.omnitune.app.playback.DownloadUtil
import com.omnitune.app.utils.SyncUtils

val LocalDatabase = compositionLocalOf<MusicDatabase> { error("No database provided") }
val LocalDownloadUtil = compositionLocalOf<DownloadUtil> { error("No download util provided") }
val LocalSyncUtils = compositionLocalOf<SyncUtils> { error("No sync utils provided") }
