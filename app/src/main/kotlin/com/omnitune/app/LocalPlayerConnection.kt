package com.omnitune.app

import androidx.compose.runtime.compositionLocalOf
import com.omnitune.app.playback.PlayerConnection

val LocalPlayerConnection = compositionLocalOf<PlayerConnection?> { null }
