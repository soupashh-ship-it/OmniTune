package com.omnitune.shared.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp
import com.omnitune.shared.ui.components.SquircleShape

// ============================================================================
// SuvMusic Geometry Tokens
// ============================================================================

val Shapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp)
)

val PillShape = RoundedCornerShape(50)
val MusicCardShape = RoundedCornerShape(20.dp)
val AlbumArtShape = RoundedCornerShape(16.dp)
val ChipShape = RoundedCornerShape(16.dp)
val SheetShapeToken = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
val CardShapeToken = RoundedCornerShape(20.dp)
val QuickAccessShape = RoundedCornerShape(topStart = 8.dp, bottomStart = 8.dp, topEnd = 20.dp, bottomEnd = 20.dp)
val NewReleaseCardShape = RoundedCornerShape(topStart = 20.dp, bottomStart = 20.dp, topEnd = 12.dp, bottomEnd = 12.dp)

val DefaultSquircleShape = SquircleShape(cornerRadius = 28.dp, cornerSmoothing = 0.6f)
