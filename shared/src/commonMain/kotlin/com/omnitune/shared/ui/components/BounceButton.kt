package com.omnitune.shared.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp

/**
 * Interactive button with spring scale animation on press and subtle overlay.
 */
@Composable
fun BounceButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp? = null,
    shape: Shape = CircleShape,
    contentAlignment: Alignment = Alignment.Center,
    clickEnabled: Boolean = true,
    content: @Composable BoxScope.(isPressed: Boolean) -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.90f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "bounce_scale"
    )

    val overlayAlpha by animateFloatAsState(
        targetValue = if (isPressed) 0.12f else 0f,
        animationSpec = tween(durationMillis = if (isPressed) 60 else 180),
        label = "bounce_overlay"
    )

    val sizeModifier = if (size != null) Modifier.size(size) else Modifier

    Box(
        modifier = modifier
            .then(sizeModifier)
            .scale(scale)
            .clip(shape)
            .then(
                if (clickEnabled) {
                    Modifier.clickable(
                        interactionSource = interactionSource,
                        indication = null,
                        onClick = onClick
                    )
                } else {
                    Modifier
                }
            ),
        contentAlignment = contentAlignment
    ) {
        content(isPressed)

        if (overlayAlpha > 0f) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(Color.Black.copy(alpha = overlayAlpha))
            )
        }
    }
}
