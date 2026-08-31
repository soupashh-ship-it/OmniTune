/*
 * This file was adapted from SuvMusic.
 * Original copyright follows:
 * 
 * Copyright (C) Suvojeet
 * Licensed under the GNU General Public License v3.0 (GPLv3)
 */

package com.omnitune.app.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * Material 3 Expressive shapes.
 * Combines standard Material shapes with custom music-focused variants.
 */
val SuvShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp)
)

// Custom shapes used throughout the app
val MusicCardShape = RoundedCornerShape(20.dp)
val PlayerCardShape = RoundedCornerShape(
    topStart = 32.dp,
    topEnd = 32.dp,
    bottomStart = 0.dp,
    bottomEnd = 0.dp
)
val AlbumArtShape = RoundedCornerShape(16.dp)
val PillShape = RoundedCornerShape(50)
val SquircleShape = RoundedCornerShape(28.dp)

// Asymmetric — QuickAccess card (image left, text right)
val QuickAccessShape = RoundedCornerShape(
    topStart = 8.dp,
    bottomStart = 8.dp,
    topEnd = 20.dp,
    bottomEnd = 20.dp
)

// Asymmetric — NewRelease card (text left, image right)
val NewReleaseCardShape = RoundedCornerShape(
    topStart = 20.dp,
    bottomStart = 20.dp,
    topEnd = 12.dp,
    bottomEnd = 12.dp
)

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
object ExpressiveShapes {
    val AlbumArt = MaterialShapes.Cookie9Sided
    val Fab = MaterialShapes.Clover4Leaf
    val MiniPlayer = MaterialShapes.Arch
    val NowPlaying = MaterialShapes.Cookie6Sided
    val ActionChip = MaterialShapes.Pill
    val GenreCard = MaterialShapes.Fan

    val Button = MaterialShapes.Pill
    val ButtonPressed = MaterialShapes.Cookie6Sided
    val FabResting = MaterialShapes.Clover4Leaf
    val FabPressed = MaterialShapes.Cookie9Sided
    val IconButton = MaterialShapes.Pill
    val IconButtonPressed = MaterialShapes.Cookie6Sided
}

val CardShapeToken = RoundedCornerShape(20.dp)
val SheetShapeToken = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
val ChipShapeToken = RoundedCornerShape(16.dp)

object OmniShapes {
    val Tiny = RoundedCornerShape(6.dp)
    val Small = RoundedCornerShape(10.dp)
    val Medium = RoundedCornerShape(14.dp)
    val Large = RoundedCornerShape(18.dp)
    val ExtraLarge = RoundedCornerShape(24.dp)
    val ArtworkSmall = RoundedCornerShape(8.dp)
    val ArtworkMedium = RoundedCornerShape(12.dp)
    val ArtworkLarge = RoundedCornerShape(16.dp)
    val Player = RoundedCornerShape(24.dp)
    val Pill = PillShape
    val Chip = ChipShapeToken

    val XS = RoundedCornerShape(6.dp)
    val SM = Small
    val MD = Medium
    val LG = Large
    val XL = ExtraLarge
    val XXL = ArtworkLarge
    val Dock = RoundedCornerShape(20.dp)
    val Circle = RoundedCornerShape(50)
}