/*
 * OmniTune - An open-source music player for Android
 * Licensed under GPL-3.0
 */

package com.omnitune.app.ui.component

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.omnitune.app.ui.theme.OmniColors
import com.omnitune.app.ui.theme.OmniShapes

// ── GlassSurface ──
@Composable
fun GlassSurface(
    modifier: Modifier = Modifier,
    cornerRadius: RoundedCornerShape = OmniShapes.MD,
    backgroundAlpha: Float = 0.06f,
    borderAlpha: Float = 0.14f,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(
        modifier = modifier
            .shadow(
                elevation = 8.dp,
                shape = cornerRadius,
                ambientColor = Color.Black.copy(alpha = 0.3f),
            )
            .clip(cornerRadius)
            .border(
                width = 1.dp,
                color = Color.White.copy(alpha = borderAlpha),
                shape = cornerRadius,
            )
            .background(Color.White.copy(alpha = backgroundAlpha)),
        content = content,
    )
}

// ── GlassCard ──
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    cornerRadius: RoundedCornerShape = OmniShapes.LG,
    content: @Composable ColumnScope.() -> Unit,
) {
    val baseModifier = modifier
        .shadow(
            elevation = 6.dp,
            shape = cornerRadius,
            ambientColor = Color.Black.copy(alpha = 0.25f),
        )
        .clip(cornerRadius)
        .border(
            width = 1.dp,
            color = OmniColors.GlassBorder,
            shape = cornerRadius,
        )
        .background(OmniColors.GlassSurface)

    Column(
        modifier = if (onClick != null) {
            baseModifier.clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = androidx.compose.material3.ripple(bounded = true, color = Color.White.copy(alpha = 0.15f)),
                onClick = onClick,
            )
        } else {
            baseModifier
        },
        content = content,
    )
}

// ── GlassRow ──
@Composable
fun GlassRow(
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit,
) {
    Row(
        modifier = modifier
            .clip(OmniShapes.MD)
            .border(1.dp, OmniColors.GlassBorderLight, OmniShapes.MD)
            .background(OmniColors.GlassSurface),
        content = content,
    )
}

// ── GlassIconButton ──
@Composable
fun GlassIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = OmniColors.TextPrimary,
    size: Dp = 40.dp,
    iconSize: Dp = 20.dp,
) {
    IconButton(
        onClick = onClick,
        modifier = modifier
            .size(size)
            .clip(OmniShapes.SM)
            .border(1.dp, OmniColors.GlassBorderLight, OmniShapes.SM)
            .background(OmniColors.GlassSurface),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.size(iconSize),
        )
    }
}

// ── GradientIconButton ──
@Composable
fun GradientIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 56.dp,
    iconSize: Dp = 28.dp,
    gradientColors: List<Color> = listOf(OmniColors.Primary, OmniColors.Secondary),
) {
    IconButton(
        onClick = onClick,
        modifier = modifier
            .size(size)
            .shadow(
                elevation = 12.dp,
                shape = OmniShapes.Circle,
                ambientColor = OmniColors.Primary.copy(alpha = 0.3f),
                spotColor = OmniColors.Primary.copy(alpha = 0.3f),
            )
            .clip(OmniShapes.Circle)
            .background(Brush.linearGradient(gradientColors)),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = Color.White,
            modifier = Modifier.size(iconSize),
        )
    }
}

// ── Accent Pill (for active tab indicator, chips, etc.) ──
@Composable
fun AccentPill(
    text: String,
    modifier: Modifier = Modifier,
    gradientColors: List<Color> = listOf(OmniColors.Primary, OmniColors.Secondary),
) {
    Box(
        modifier = modifier
            .clip(OmniShapes.Pill)
            .background(Brush.horizontalGradient(gradientColors))
            .padding(horizontal = 14.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            color = Color.White,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

// ── Section Header ──
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
            style = androidx.compose.material3.MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = OmniColors.TextPrimary,
            modifier = Modifier.weight(1f),
        )
        if (action != null && onAction != null) {
            Text(
                text = action,
                style = androidx.compose.material3.MaterialTheme.typography.labelLarge,
                color = OmniColors.Secondary,
                modifier = Modifier.clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = androidx.compose.material3.ripple(bounded = false, color = OmniColors.Secondary.copy(alpha = 0.2f)),
                    onClick = onAction,
                ),
            )
        }
    }
}

// ── Shimmer loading bar ──
@Composable
fun ShimmerBar(
    modifier: Modifier = Modifier,
) {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val alpha by transition.animateFloat(
        initialValue = 0.15f,
        targetValue = 0.35f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "shimmer_alpha",
    )

    Box(
        modifier = modifier
            .clip(OmniShapes.SM)
            .background(OmniColors.GlassSurface.copy(alpha = alpha)),
    )
}
