/*
 * OmniTune - An open-source music player for Android
 * Licensed under GPL-3.0
 */

package com.omnitune.app.ui.theme

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

fun Modifier.omniGlassSurface(
    shape: Shape,
    background: Color = OmniColors.OmniGlassMedium,
    borderColor: Color = OmniColors.OmniGlassBorderSubtle,
    borderWidth: Dp = 1.dp,
): Modifier = this
    .clip(shape)
    .background(background)
    .border(borderWidth, borderColor, shape)

fun Modifier.omniSoftBorder(
    shape: Shape,
    color: Color = OmniColors.OmniGlassBorderSubtle,
    width: Dp = 1.dp,
): Modifier = this.border(width, color, shape)

fun Modifier.omniPremiumGradientBackground(
    shape: Shape = RoundedCornerShape(0.dp),
    top: Color = OmniColors.OmniBackgroundGradientTop,
    bottom: Color = OmniColors.OmniBackgroundGradientBottom,
): Modifier = this
    .clip(shape)
    .background(Brush.verticalGradient(listOf(top, bottom)))

@Composable
fun Modifier.omniPressScale(
    interactionSource: InteractionSource,
    pressedScale: Float = 0.97f,
): Modifier {
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) pressedScale else 1f,
        animationSpec = OmniMotion.pressSpring(),
        label = "omni_press_scale",
    )
    return this.graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
}

@Composable
fun Modifier.omniPressScaleBounce(
    interactionSource: InteractionSource,
    pressedScale: Float = 0.94f,
): Modifier {
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) pressedScale else 1f,
        animationSpec = OmniMotion.pressSpring(),
        label = "omni_press_scale_bounce",
    )
    return this.graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
}

fun Modifier.omniArtworkGlow(
    shape: Shape,
    elevation: Dp = 18.dp,
    color: Color = OmniColors.OmniAccentGlow,
): Modifier = this.shadow(
    elevation = elevation,
    shape = shape,
    ambientColor = color,
    spotColor = color,
)

fun Modifier.omniDisabledAlpha(
    enabled: Boolean,
    disabledAlpha: Float = 0.38f,
): Modifier = this.alpha(if (enabled) 1f else disabledAlpha)
