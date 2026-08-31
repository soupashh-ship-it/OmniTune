/*
 * This file was adapted from SuvMusic.
 * Original copyright follows:
 * 
 * Copyright (C) Suvojeet
 * Licensed under the GNU General Public License v3.0 (GPLv3)
 */

package com.omnitune.app.ui.player.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.LinearEasing
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Square
import androidx.compose.material.icons.rounded.RoundedCorner
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.omnitune.app.models.ArtworkShape
import com.omnitune.app.models.ArtworkSize
import com.omnitune.app.ui.component.DominantColors
import com.omnitune.app.ui.utils.ImageUtils

@Composable
fun AlbumArtwork(
    imageUrl: String?,
    title: String?,
    dominantColors: DominantColors,
    isLoading: Boolean = false,
    isPlaying: Boolean = false,
    isRotatingEnabled: Boolean = false,
    onSwipeLeft: () -> Unit = {},
    onSwipeRight: () -> Unit = {},
    initialShape: ArtworkShape = ArtworkShape.ROUNDED_SQUARE,
    artworkSize: ArtworkSize = ArtworkSize.FULL,
    onShapeChange: ((ArtworkShape) -> Unit)? = null,
    onDoubleTapLeft: () -> Unit = {},
    onDoubleTapRight: () -> Unit = {},
    songId: String? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val colorFlashingEnabled = false

    val glowActive = colorFlashingEnabled && isPlaying
    val glowColor = if (glowActive) {
        val pulseTransition = rememberInfiniteTransition(label = "art_color_pulse")
        val pulseAlpha by pulseTransition.animateFloat(
            initialValue = 0.25f,
            targetValue = 0.75f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 2400, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse,
            ),
            label = "pulse_alpha",
        )
        val hueShift by pulseTransition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 6000, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse,
            ),
            label = "pulse_hue",
        )
        androidx.compose.ui.graphics.lerp(
            dominantColors.primary,
            dominantColors.accent,
            hueShift,
        ).copy(alpha = pulseAlpha)
    } else {
        dominantColors.primary.copy(alpha = 0.25f)
    }

    var currentShape by remember { mutableStateOf(initialShape) }
    var showShapeMenu by remember { mutableStateOf(false) }
    
    LaunchedEffect(initialShape) {
        currentShape = initialShape
    }
    
    var vinylDragRotation by remember { mutableStateOf(0f) }
    val rotationAnimatable = remember { androidx.compose.animation.core.Animatable(0f) }
    
    LaunchedEffect(isPlaying, currentShape, isRotatingEnabled) {
        if (isPlaying && currentShape == ArtworkShape.VINYL && isRotatingEnabled) {
            try {
                while (true) {
                    rotationAnimatable.animateTo(
                        targetValue = rotationAnimatable.value + 360f,
                        animationSpec = tween(8000, easing = LinearEasing)
                    )
                }
            } finally {
                withContext(NonCancellable) { rotationAnimatable.snapTo(rotationAnimatable.value) }
            }
        } else {
            rotationAnimatable.stop()
        }
    }
    
    val currentRotation = when {
        currentShape == ArtworkShape.VINYL -> rotationAnimatable.value + vinylDragRotation
        else -> 0f
    }
    
    val targetCornerRadius = when (currentShape) {
        ArtworkShape.ROUNDED_SQUARE -> 16.dp
        ArtworkShape.CIRCLE, ArtworkShape.VINYL -> 500.dp
        ArtworkShape.SQUARE -> 0.dp
    }
    
    val cornerRadius by animateDpAsState(
        targetValue = targetCornerRadius,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "corner_radius"
    )
    
    val safeCornerRadius = cornerRadius.coerceAtLeast(0.dp)
    var offsetX by remember { mutableStateOf(0f) }
    val swipeThreshold = 150f

    val animatedOffsetX by animateFloatAsState(
        targetValue = offsetX,
        animationSpec = if (offsetX == 0f) spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ) else tween(durationMillis = 0),
        label = "swipe_offset"
    )

    val scale = 1f - (kotlin.math.abs(animatedOffsetX) / 800f).coerceIn(0f, 0.15f)
    val rotation = animatedOffsetX / 40f

    val currentOnDoubleTapLeft by rememberUpdatedState(onDoubleTapLeft)
    val currentOnDoubleTapRight by rememberUpdatedState(onDoubleTapRight)
    val currentOnSwipeLeft by rememberUpdatedState(onSwipeLeft)
    val currentOnSwipeRight by rememberUpdatedState(onSwipeRight)

    BoxWithConstraints(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        val isWideLayout = maxWidth > maxHeight
        val maxFraction = ArtworkSize.MAX_FRACTION

        Box(
            modifier = Modifier
                .then(
                    if (isWideLayout) {
                        Modifier.fillMaxHeight(maxFraction).aspectRatio(1f)
                    } else {
                        Modifier.fillMaxWidth(maxFraction).aspectRatio(1f)
                    }
                )
                .pointerInput(Unit) {
                    detectTapGestures(
                        onDoubleTap = { offset ->
                            if (offset.x < size.width / 2) {
                                currentOnDoubleTapLeft()
                            } else {
                                currentOnDoubleTapRight()
                            }
                        },
                        onLongPress = {
                            showShapeMenu = true
                        }
                    )
                }
                .pointerInput(Unit) {
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            when {
                                offsetX < -swipeThreshold -> currentOnSwipeLeft()
                                offsetX > swipeThreshold -> currentOnSwipeRight()
                            }
                            offsetX = 0f
                        },
                        onDragCancel = { offsetX = 0f },
                        onHorizontalDrag = { _, dragAmount ->
                            val resistance = 1f - (kotlin.math.abs(offsetX) / 600f).coerceIn(0f, 0.7f)
                            offsetX += (dragAmount * resistance)
                            if (currentShape == ArtworkShape.VINYL) {
                                vinylDragRotation += (dragAmount * 0.1f * resistance)
                            }
                        }
                    )
                }
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        val dynamicScale = (artworkSize.fraction / maxFraction) * scale
                        translationX = animatedOffsetX
                        scaleX = dynamicScale
                        scaleY = dynamicScale
                        rotationZ = rotation + currentRotation
                    }
                    .shadow(
                        elevation = if (colorFlashingEnabled && isPlaying) 28.dp else 8.dp,
                        shape = RoundedCornerShape(safeCornerRadius),
                        spotColor = glowColor,
                        ambientColor = glowColor
                    )
                    .clip(RoundedCornerShape(safeCornerRadius))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                if (!imageUrl.isNullOrEmpty()) {
                    androidx.compose.runtime.key(songId, imageUrl) {
                        var model by remember(imageUrl) { mutableStateOf<Any?>(ImageUtils.getHighResThumbnailUrl(imageUrl)) }

                        AsyncImage(
                            model = ImageRequest.Builder(context)
                                .data(model)
                                .crossfade(false)
                                .size(600)
                                .listener(
                                    onError = { _, _ ->
                                        if (model != imageUrl) {
                                            model = imageUrl
                                        }
                                    }
                                )
                                .build(),
                            contentDescription = title,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
                } else {
                    val fallbackUrl = remember(songId) {
                        if (!songId.isNullOrEmpty() && songId.length == 11) {
                            "https://img.youtube.com/vi/$songId/maxresdefault.jpg"
                        } else {
                            null
                        }
                    }

                    if (fallbackUrl != null) {
                        androidx.compose.runtime.key(songId, fallbackUrl) {
                            var model by remember(fallbackUrl) { mutableStateOf<Any?>(fallbackUrl) }

                            AsyncImage(
                                model = ImageRequest.Builder(context)
                                .data(model)
                                .crossfade(false)
                                .size(600)
                                .listener(
                                    onError = { _, _ ->
                                        if (model == fallbackUrl) {
                                            model = null
                                        }
                                    }
                                )
                                .build(),
                                contentDescription = title,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        }
                    } else {
                        Icon(
                            imageVector = Icons.Default.MusicNote,
                            contentDescription = null,
                            modifier = Modifier.size(100.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                if (currentShape == ArtworkShape.VINYL) {
                    VinylCenterOverlay(dominantColors = dominantColors)
                }

                M3ELoadingOverlay(
                    isLoading = isLoading,
                    dominantColors = dominantColors,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
        
        if (showShapeMenu) {
            Popup(
                alignment = Alignment.Center,
                onDismissRequest = { showShapeMenu = false },
                properties = PopupProperties(focusable = true)
            ) {
                ShapeSelectionMenu(
                    currentShape = currentShape,
                    dominantColors = dominantColors,
                    onShapeSelected = { shape ->
                        currentShape = shape
                        onShapeChange?.invoke(shape)
                        showShapeMenu = false
                    },
                    onDismiss = { showShapeMenu = false }
                )
            }
        }
    }
}

@Composable
private fun VinylCenterOverlay(dominantColors: DominantColors) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.9f),
                            Color.Black.copy(alpha = 0.7f),
                            dominantColors.primary.copy(alpha = 0.3f)
                        )
                    )
                )
                .border(2.dp, dominantColors.accent.copy(alpha = 0.5f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(Color.Black)
                    .border(1.dp, Color.White.copy(alpha = 0.3f), CircleShape)
            )
        }
        
        Box(
            modifier = Modifier
                .fillMaxSize()
                .border(
                    width = 1.dp,
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.1f),
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.05f),
                            Color.Transparent
                        )
                    ),
                    shape = CircleShape
                )
        )
    }
}

@Composable
private fun ShapeSelectionMenu(
    currentShape: ArtworkShape,
    dominantColors: DominantColors,
    onShapeSelected: (ArtworkShape) -> Unit,
    onDismiss: () -> Unit
) {
    Surface(
        modifier = Modifier.padding(16.dp),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shadowElevation = 16.dp,
        tonalElevation = 8.dp
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Artwork Style",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.SemiBold
                ),
                color = MaterialTheme.colorScheme.onSurface
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ShapeOption(
                    icon = Icons.Rounded.RoundedCorner,
                    label = "Rounded",
                    isSelected = currentShape == ArtworkShape.ROUNDED_SQUARE,
                    accentColor = dominantColors.accent,
                    onClick = { onShapeSelected(ArtworkShape.ROUNDED_SQUARE) }
                )
                
                ShapeOption(
                    icon = Icons.Default.Circle,
                    label = "Circle",
                    isSelected = currentShape == ArtworkShape.CIRCLE,
                    accentColor = dominantColors.accent,
                    onClick = { onShapeSelected(ArtworkShape.CIRCLE) }
                )
                
                ShapeOption(
                    icon = Icons.Default.Album,
                    label = "Vinyl",
                    isSelected = currentShape == ArtworkShape.VINYL,
                    accentColor = dominantColors.accent,
                    onClick = { onShapeSelected(ArtworkShape.VINYL) }
                )
                
                ShapeOption(
                    icon = Icons.Default.Square,
                    label = "Square",
                    isSelected = currentShape == ArtworkShape.SQUARE,
                    accentColor = dominantColors.accent,
                    onClick = { onShapeSelected(ArtworkShape.SQUARE) }
                )
            }
        }
    }
}

@Composable
private fun ShapeOption(
    icon: ImageVector,
    label: String,
    isSelected: Boolean,
    accentColor: Color,
    onClick: () -> Unit
) {
    val backgroundColor by animateColorAsState(
        targetValue = if (isSelected) accentColor.copy(alpha = 0.2f) else Color.Transparent,
        animationSpec = spring(),
        label = "bg_color"
    )
    
    val iconColor by animateColorAsState(
        targetValue = if (isSelected) accentColor else MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = spring(),
        label = "icon_color"
    )
    
    val borderColor by animateColorAsState(
        targetValue = if (isSelected) accentColor else Color.Transparent,
        animationSpec = spring(),
        label = "border_color"
    )
    
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(backgroundColor)
            .border(1.5.dp, borderColor, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = iconColor,
            modifier = Modifier.size(28.dp)
        )
        
        Spacer(modifier = Modifier.height(4.dp))
        
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = iconColor,
            fontSize = 10.sp
        )
    }
}
