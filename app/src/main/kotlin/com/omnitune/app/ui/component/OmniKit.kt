/*
 * OmniTune - An open-source music player for Android
 * Licensed under GPL-3.0
 */

package com.omnitune.app.ui.component

import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer

import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.omnitune.app.R
import com.omnitune.app.ui.theme.LocalOmniColors
import com.omnitune.app.ui.theme.OmniMotion
import com.omnitune.app.ui.theme.OmniShapes
import com.omnitune.app.ui.theme.OmniSpacing

/**
 * Fixed-ratio artwork container: placeholder while loading, crossfade-in on success,
 * icon fallback on failure. Guarantees stable layout (no shift) at any size.
 */
@Composable
fun OmniArtwork(
    data: Any?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    size: Dp = 56.dp,
    cornerRadius: Dp = 12.dp,
    @DrawableRes fallbackIcon: Int = R.drawable.ic_music_note,
    contentScale: ContentScale = ContentScale.Crop,
) {
    val scheme = LocalOmniColors.current
    var loadFailed by remember(data) { mutableStateOf(false) }
    val shape = RoundedCornerShape(cornerRadius)

    Box(
        modifier = modifier
            .size(size)
            .clip(shape)
            .background(scheme.surfaceQuiet),
        contentAlignment = Alignment.Center,
    ) {
        if (!loadFailed && data != null) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(data)
                    .crossfade(OmniMotion.FastFadeMillis)
                    .build(),
                contentDescription = contentDescription,
                contentScale = contentScale,
                error = painterResource(fallbackIcon),
                onError = { loadFailed = true },
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            Icon(
                painter = painterResource(if (loadFailed) fallbackIcon else R.drawable.ic_album),
                contentDescription = null,
                tint = scheme.textTertiary,
                modifier = Modifier.size(size * 0.42f),
            )
        }
    }
}

/**
 * Primary list row for tracks/items: fixed-height scannable layout with leading artwork,
 * two-line text block, and trailing slot. Enforces minimum touch height.
 *
 * [isPlaying] swaps title color to accent and shows [playingIndicator] when provided.
 */
@Composable
fun OmniListRow(
    title: String,
    subtitle: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    artworkData: Any? = null,
    artworkContentDescription: String? = null,
    artworkSize: Dp = 56.dp,
    leading: (@Composable () -> Unit)? = null,
    isPlaying: Boolean = false,
    playingIndicator: (@Composable () -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
    titleStyle: TextStyle = omniTypeSongTitle(),
    subtitleStyle: TextStyle = omniTypeMetadata(),
) {
    val scheme = LocalOmniColors.current
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.98f else 1f,
        animationSpec = tween(OmniMotion.PressMillis),
        label = "omniRowPress",
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 64.dp)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = OmniSpacing.medium, vertical = OmniSpacing.small),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(OmniSpacing.medium),
    ) {
        if (leading != null) {
            Box(modifier = Modifier.size(artworkSize)) { leading() }
        } else {
            OmniArtwork(
                data = artworkData,
                contentDescription = artworkContentDescription,
                size = artworkSize,
                cornerRadius = if (artworkSize <= 72.dp) 8.dp else 12.dp,
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = title,
                    style = titleStyle,
                    color = if (isPlaying) scheme.accent else scheme.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                if (playingIndicator != null && isPlaying) {
                    Spacer(modifier = Modifier.width(OmniSpacing.compact))
                    AnimatedVisibility(visible = true, enter = fadeIn()) {
                        playingIndicator()
                    }
                }
            }
            if (!subtitle.isNullOrBlank()) {
                Text(
                    text = subtitle,
                    style = subtitleStyle,
                    color = scheme.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        trailing?.invoke()
    }
}

/**
 * Unified status view for empty/error/offline moments. Explains what happened and offers
 * the next action. Replaces ad-hoc "No data" placeholders across screens.
 */
@Composable
fun OmniStatusView(
    @DrawableRes iconRes: Int,
    title: String,
    modifier: Modifier = Modifier,
    message: String? = null,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    val scheme = LocalOmniColors.current
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = OmniSpacing.section, vertical = OmniSpacing.hero),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(OmniShapes.Circle)
                .background(scheme.accentSoft),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(iconRes),
                contentDescription = null,
                tint = scheme.accent,
                modifier = Modifier.size(28.dp),
            )
        }
        Spacer(Modifier.height(OmniSpacing.medium))
        Text(
            text = title,
            style = omniTypeStatusTitle(),
            color = scheme.textPrimary,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        if (!message.isNullOrBlank()) {
            Spacer(Modifier.height(OmniSpacing.compact))
            Text(
                text = message,
                style = omniTypeMetadata(),
                color = scheme.textSecondary,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (actionLabel != null && onAction != null) {
            Spacer(Modifier.height(OmniSpacing.large))
            Box(
                modifier = Modifier
                    .clip(OmniShapes.Small)
                    .background(scheme.accent)
                    .clickable(onClick = onAction)
                    .defaultMinSize(minHeight = OmniSpacing.touchTarget)
                    .padding(horizontal = OmniSpacing.large, vertical = OmniSpacing.small),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = actionLabel,
                    style = omniTypeMetadata().copy(fontWeight = FontWeight.SemiBold),
                    color = scheme.textOnAccent,
                )
            }
        }
    }
}

/**
 * Type roles resolved from MaterialTheme (which carries the active font family and honors
 * the user's system-font setting). Components must use these instead of OmniTextStyles.
 */
@Composable
fun omniTypeSongTitle(): TextStyle =
    MaterialTheme.typography.titleMedium.copy(fontSize = 16.sp, lineHeight = 22.sp)

@Composable
fun omniTypeMetadata(): TextStyle = MaterialTheme.typography.bodySmall.copy(
    fontSize = 13.sp,
    lineHeight = 18.sp,
)

@Composable
private fun omniTypeStatusTitle(): TextStyle = MaterialTheme.typography.titleMedium