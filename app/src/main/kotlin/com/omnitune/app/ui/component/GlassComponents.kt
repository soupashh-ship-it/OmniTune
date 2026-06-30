/*
 * OmniTune - An open-source music player for Android
 * Licensed under GPL-3.0
 */

package com.omnitune.app.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.omnitune.app.ui.theme.OmniColors
import com.omnitune.app.ui.theme.OmniShapes
import com.omnitune.app.ui.theme.OmniSpacing
import com.omnitune.app.ui.theme.OmniTextStyles
import com.omnitune.app.ui.theme.omniPressScale
import com.omnitune.app.ui.theme.omniSoftBorder

enum class GlassTone {
    Subtle,
    Medium,
    Strong,
    Dock,
    Player,
}

private fun glassColorFor(tone: GlassTone): Color =
    when (tone) {
        GlassTone.Subtle -> OmniColors.OmniGlassSubtle
        GlassTone.Medium -> OmniColors.OmniGlassMedium
        GlassTone.Strong -> OmniColors.OmniGlassStrong
        GlassTone.Dock -> OmniColors.OmniGlassDock
        GlassTone.Player -> OmniColors.OmniGlassPlayer
    }

private fun glassBorderFor(tone: GlassTone): Color =
    when (tone) {
        GlassTone.Subtle,
        GlassTone.Medium,
        -> OmniColors.OmniGlassBorderSubtle
        GlassTone.Strong,
        GlassTone.Dock,
        GlassTone.Player,
        -> OmniColors.OmniGlassBorderStrong
    }

@Composable
fun GlassSurface(
    modifier: Modifier = Modifier,
    cornerRadius: RoundedCornerShape = OmniShapes.Medium,
    backgroundAlpha: Float = 0.08f,
    borderAlpha: Float = 0.18f,
    tone: GlassTone = GlassTone.Medium,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(
        modifier = modifier
            .shadow(
                elevation = 3.dp,
                shape = cornerRadius,
                ambientColor = Color.Black.copy(alpha = 0.18f),
                spotColor = OmniColors.OmniAccentGlow.copy(alpha = 0.08f),
            )
            .clip(cornerRadius)
            .omniSoftBorder(
                shape = cornerRadius,
                color = glassBorderFor(tone).copy(alpha = borderAlpha),
            )
            .background(glassColorFor(tone).copy(alpha = backgroundAlpha)),
        content = content,
    )
}

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    cornerRadius: RoundedCornerShape = OmniShapes.Large,
    tone: GlassTone = GlassTone.Medium,
    content: @Composable ColumnScope.() -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val baseModifier = modifier
        .shadow(
            elevation = 3.dp,
            shape = cornerRadius,
            ambientColor = Color.Black.copy(alpha = 0.18f),
            spotColor = OmniColors.OmniAccentGlow.copy(alpha = 0.07f),
        )
        .clip(cornerRadius)
        .omniSoftBorder(cornerRadius, glassBorderFor(tone).copy(alpha = 0.70f))
        .background(
            Brush.verticalGradient(
                colors = listOf(
                    OmniColors.SurfacePanel.copy(alpha = 0.34f),
                    glassColorFor(tone).copy(alpha = 0.84f),
                    OmniColors.OmniBackgroundBase.copy(alpha = 0.62f),
                )
            )
        )

    Column(
        modifier = if (onClick != null) {
            baseModifier
                .omniPressScale(interactionSource)
                .clickable(
                    interactionSource = interactionSource,
                    indication = androidx.compose.material3.ripple(
                        bounded = true,
                        color = OmniColors.OmniAccentSecondary.copy(alpha = 0.14f),
                    ),
                    onClick = onClick,
                )
        } else {
            baseModifier
        },
        content = content,
    )
}

@Composable
fun GlassRow(
    modifier: Modifier = Modifier,
    tone: GlassTone = GlassTone.Subtle,
    content: @Composable RowScope.() -> Unit,
) {
    Row(
        modifier = modifier
            .clip(OmniShapes.Medium)
            .omniSoftBorder(OmniShapes.Medium, glassBorderFor(tone))
            .background(glassColorFor(tone)),
        content = content,
    )
}

@Composable
fun GlassIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = OmniColors.TextPrimary,
    size: Dp = 40.dp,
    iconSize: Dp = 20.dp,
    tone: GlassTone = GlassTone.Subtle,
) {
    val interactionSource = remember { MutableInteractionSource() }

    IconButton(
        onClick = onClick,
        modifier = modifier
            .size(size)
            .clip(OmniShapes.Small)
            .omniSoftBorder(OmniShapes.Small, glassBorderFor(tone))
            .background(glassColorFor(tone))
            .omniPressScale(interactionSource),
        interactionSource = interactionSource,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.size(iconSize),
        )
    }
}

@Composable
fun GradientIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 56.dp,
    iconSize: Dp = 28.dp,
    gradientColors: List<Color> = OmniColors.PrimaryGradientColors,
) {
    val interactionSource = remember { MutableInteractionSource() }

    IconButton(
        onClick = onClick,
        modifier = modifier
            .size(size)
            .shadow(
                elevation = 14.dp,
                shape = OmniShapes.Circle,
                ambientColor = OmniColors.OmniAccentGlow,
                spotColor = OmniColors.OmniAccentGlow,
            )
            .clip(OmniShapes.Circle)
            .background(Brush.linearGradient(gradientColors))
            .omniPressScale(interactionSource),
        interactionSource = interactionSource,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = OmniColors.TextOnAccent,
            modifier = Modifier.size(iconSize),
        )
    }
}

@Composable
fun AccentPill(
    text: String,
    modifier: Modifier = Modifier,
    gradientColors: List<Color> = OmniColors.PrimaryGradientColors,
) {
    Box(
        modifier = modifier
            .clip(OmniShapes.Pill)
            .background(Brush.horizontalGradient(gradientColors))
            .padding(horizontal = 14.dp, vertical = OmniSpacing.compact),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            color = OmniColors.TextOnAccent,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
fun OmniSectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    action: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            style = OmniTextStyles.sectionTitle,
            modifier = Modifier.weight(1f),
        )
        if (action != null && onAction != null) {
            Text(
                text = action,
                style = MaterialTheme.typography.labelLarge,
                color = OmniColors.OmniAccentSecondary,
                modifier = Modifier.clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = androidx.compose.material3.ripple(
                        bounded = false,
                        color = OmniColors.OmniAccentSecondary.copy(alpha = 0.2f),
                    ),
                    onClick = onAction,
                ),
            )
        }
    }
}

@Composable
fun ShimmerBar(
    modifier: Modifier = Modifier,
    tone: GlassTone = GlassTone.Subtle,
) {
    Box(
        modifier = modifier
            .clip(OmniShapes.Small)
            .background(
                Brush.linearGradient(
                    listOf(
                        glassColorFor(tone).copy(alpha = 0.18f),
                        OmniColors.OmniAccentSecondary.copy(alpha = 0.08f),
                    ),
                ),
            ),
    )
}
