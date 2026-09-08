/*
 * Adapted from SuvMusic (https://github.com/SuvojeetDev/SuvMusic)
 * Copyright (c) SuvMusic contributors
 * Licensed under GPL-3.0
 */

package com.omnitune.app.ui.player.components

import android.content.Context
import android.media.AudioManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.omnitune.app.ui.component.DominantColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharedFlow

/**
 * Self-contained volume control that manages its own state internally.
 */
@Composable
fun VolumeControl(
    dominantColors: DominantColors,
    volumeKeyEvents: SharedFlow<Unit>?,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val audioManager = remember {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    }

    var maxVolume by remember {
        mutableIntStateOf(audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC))
    }
    var currentVolume by remember {
        mutableIntStateOf(audioManager.getStreamVolume(AudioManager.STREAM_MUSIC))
    }
    var showVolumeIndicator by remember { mutableStateOf(false) }
    var lastVolumeChangeTime by remember { mutableLongStateOf(0L) }

    SystemVolumeObserver(
        context = LocalContext.current
    ) { newVol, newMax ->
        maxVolume = newMax
        if (currentVolume != newVol) {
            currentVolume = newVol
            lastVolumeChangeTime = System.currentTimeMillis()
        }
    }

    LaunchedEffect(volumeKeyEvents) {
        volumeKeyEvents?.collect {
            currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
            lastVolumeChangeTime = System.currentTimeMillis()
        }
    }

    LaunchedEffect(lastVolumeChangeTime) {
        if (lastVolumeChangeTime > 0) {
            showVolumeIndicator = true
            delay(2000)
            showVolumeIndicator = false
        }
    }

    VolumeIndicator(
        isVisible = showVolumeIndicator,
        currentVolume = currentVolume,
        maxVolume = maxVolume,
        dominantColors = dominantColors,
        onVolumeChange = { newVolume ->
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, newVolume, 0)
            currentVolume = newVolume
            lastVolumeChangeTime = System.currentTimeMillis()
        },
        modifier = modifier
    )
}
