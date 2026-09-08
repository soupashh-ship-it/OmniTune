/*
 * Adapted from SuvMusic (https://github.com/SuvojeetDev/SuvMusic)
 * Copyright (c) SuvMusic contributors
 * Licensed under GPL-3.0
 */

package com.omnitune.app.ui.player.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.omnitune.app.ui.component.DominantColors
import com.omnitune.app.ui.component.glass.ArtworkBlurBackdrop

/**
 * Transparent, iOS-style "liquid glass" player backdrop.
 */
@Composable
fun GlassArtBackground(
    thumbnailUrl: String?,
    isDarkTheme: Boolean,
    isVideoMode: Boolean,
    dominantColors: DominantColors,
    modifier: Modifier = Modifier,
    blurRadius: Float = 60f,
    intensity: Float = 1f
) {
    Box(modifier = modifier.fillMaxSize()) {
        if (!thumbnailUrl.isNullOrBlank() && !isVideoMode) {
            ArtworkBlurBackdrop(
                artworkUrl = thumbnailUrl,
                isDarkTheme = isDarkTheme,
                dominantColors = dominantColors,
                modifier = Modifier.fillMaxSize(),
                blurRadius = blurRadius,
                intensity = intensity
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(if (isDarkTheme) Color(0xFF0B0B0F) else Color(0xFFF2F2F6))
            )
        }
    }
}
