/*
 * OmniTune - An open-source music player for Android
 * Licensed under GPL-3.0
 */

package com.omnitune.app.ui.component

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.omnitune.app.R
import com.omnitune.app.ui.theme.OmniColors
import com.omnitune.app.ui.theme.OmniShapes
import com.omnitune.app.ui.theme.OmniSpacing
import com.omnitune.app.ui.theme.OmniTextStyles

object OmniChrome {
    val MiniPlayerHeight = 68.dp
    val MiniPlayerArtwork = 48.dp
    val MiniPlayerButton = 42.dp
    val BottomDockHeight = 60.dp
    val BottomDockHorizontalPadding = 18.dp
    val BottomContentPadding = 156.dp
    val BottomContentPaddingWithPlayer = 172.dp
}

@Composable
fun OmniScreen(
    modifier: Modifier = Modifier,
    horizontalPadding: Dp = OmniSpacing.section,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(OmniColors.OmniBackgroundBase)
            .background(OmniColors.BackgroundGradient)
            .padding(horizontal = horizontalPadding),
        content = content,
    )
}

@Composable
fun OmniNavigationSpacer(height: Dp = OmniChrome.BottomContentPadding) {
    Column {
        Spacer(modifier = Modifier.height(height))
        Spacer(modifier = Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars.only(WindowInsetsSides.Bottom)))
    }
}

@Composable
fun OmniTopHeader(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    @DrawableRes leadingIcon: Int? = null,
    onBack: (() -> Unit)? = null,
    actions: @Composable (() -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(top = OmniSpacing.small, bottom = OmniSpacing.compact),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (onBack != null) {
            OmniIconButton(iconRes = R.drawable.ic_arrow_back, contentDescription = "Back", onClick = onBack)
            Spacer(modifier = Modifier.width(OmniSpacing.small))
        } else if (leadingIcon != null) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(OmniShapes.Small)
                    .background(OmniColors.OmniAccentSoft),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(leadingIcon),
                    contentDescription = null,
                    tint = OmniColors.OmniAccentSecondary,
                    modifier = Modifier.size(19.dp),
                )
            }
            Spacer(modifier = Modifier.width(OmniSpacing.small))
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = OmniColors.TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = OmniTextStyles.metadata,
                    color = OmniColors.TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        actions?.invoke()
    }
}

@Composable
fun OmniIconButton(
    @DrawableRes iconRes: Int,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = OmniColors.TextPrimary,
    size: Dp = 40.dp,
) {
    IconButton(
        onClick = onClick,
        modifier = modifier
            .size(size)
            .clip(OmniShapes.Pill)
            .background(OmniColors.SurfaceSubtle),
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.size(20.dp),
        )
    }
}

@Composable
fun OmniMusicRow(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    thumbnailUrl: String? = null,
    @DrawableRes placeholderIcon: Int = R.drawable.ic_album,
    @DrawableRes trailingIcon: Int? = null,
    trailingText: String? = null,
    onClick: (() -> Unit)? = null,
) {
    val interactionSource = remember { MutableInteractionSource() }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 64.dp)
            .clip(OmniShapes.Small)
            .background(OmniColors.SurfaceSubtle.copy(alpha = 0.52f))
            .then(
                if (onClick != null) {
                    Modifier.clickable(
                        interactionSource = interactionSource,
                        indication = androidx.compose.material3.ripple(
                            bounded = true,
                            color = OmniColors.OmniAccentSecondary.copy(alpha = 0.12f),
                        ),
                        onClick = onClick,
                    )
                } else {
                    Modifier
                },
            )
            .padding(horizontal = OmniSpacing.compact, vertical = OmniSpacing.compact),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        OmniThumbnail(thumbnailUrl = thumbnailUrl, placeholderIcon = placeholderIcon, size = 48.dp)
        Spacer(modifier = Modifier.width(OmniSpacing.small))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = OmniTextStyles.songTitle,
                color = OmniColors.TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = subtitle,
                style = OmniTextStyles.metadata,
                color = OmniColors.TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (trailingText != null) {
            Text(
                text = trailingText,
                style = MaterialTheme.typography.labelMedium,
                color = OmniColors.OmniAccentSecondary,
                maxLines = 1,
            )
        }
        if (trailingIcon != null) {
            Spacer(modifier = Modifier.width(OmniSpacing.compact))
            Icon(
                painter = painterResource(trailingIcon),
                contentDescription = null,
                tint = OmniColors.TextSecondary,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

@Composable
fun OmniShelfCard(
    title: String,
    subtitle: String,
    thumbnailUrl: String?,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Column(
        modifier = modifier
            .width(144.dp)
            .clip(OmniShapes.Small)
            .clickable(onClick = onClick),
    ) {
        OmniThumbnail(
            thumbnailUrl = thumbnailUrl,
            placeholderIcon = R.drawable.ic_album,
            size = 144.dp,
            shape = OmniShapes.ArtworkSmall,
            modifier = Modifier.aspectRatio(1f),
        )
        Spacer(modifier = Modifier.height(OmniSpacing.small))
        Text(title, style = OmniTextStyles.songTitle, maxLines = 2, overflow = TextOverflow.Ellipsis)
        Text(subtitle, style = OmniTextStyles.metadata, color = OmniColors.TextSecondary, maxLines = 2, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
fun OmniSettingsRow(
    @DrawableRes iconRes: Int,
    title: String,
    subtitle: String,
    accent: Color,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    trailing: @Composable (() -> Unit)? = null,
) {
    val interactionSource = remember { MutableInteractionSource() }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 64.dp)
            .clip(OmniShapes.Small)
            .then(
                if (onClick != null) {
                    Modifier.clickable(
                        interactionSource = interactionSource,
                        indication = androidx.compose.material3.ripple(
                            bounded = true,
                            color = Color.White.copy(alpha = 0.08f),
                        ),
                        onClick = onClick,
                    )
                } else {
                    Modifier
                },
            )
            .padding(horizontal = OmniSpacing.compact, vertical = OmniSpacing.small),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(OmniShapes.Small)
                .background(accent.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(iconRes),
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(19.dp),
            )
        }
        Spacer(modifier = Modifier.width(OmniSpacing.medium))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = OmniColors.TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = OmniColors.TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        trailing?.invoke()
    }
}

@Composable
fun OmniThumbnail(
    thumbnailUrl: String?,
    @DrawableRes placeholderIcon: Int,
    size: Dp,
    modifier: Modifier = Modifier,
    shape: Shape = OmniShapes.ArtworkSmall,
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(shape)
            .background(
                Brush.linearGradient(
                    listOf(
                        OmniColors.SurfaceRaised,
                        OmniColors.OmniBackgroundElevated,
                        OmniColors.OmniAccentSecondary.copy(alpha = 0.08f),
                    ),
                ),
            ),
        contentAlignment = Alignment.Center,
    ) {
        if (!thumbnailUrl.isNullOrBlank()) {
            AsyncImage(
                model = thumbnailUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            Icon(
                painter = painterResource(placeholderIcon),
                contentDescription = null,
                tint = OmniColors.TextTertiary.copy(alpha = 0.56f),
                modifier = Modifier.size(size * 0.44f),
            )
        }
    }
}

@Composable
fun OmniFloatingSurface(
    modifier: Modifier = Modifier,
    shape: RoundedCornerShape = OmniShapes.Dock,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(
        modifier = modifier
            .shadow(
                elevation = 8.dp,
                shape = shape,
                ambientColor = Color.Black.copy(alpha = 0.28f),
                spotColor = OmniColors.OmniAccentGlow.copy(alpha = 0.08f),
            )
            .clip(shape)
            .background(OmniColors.SurfaceFloating),
        content = content,
    )
}
