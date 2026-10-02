package com.omnitune.shared.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.StartOffset
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.animateValue
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Animated equalizer bars displaying live audio playback activity.
 */
@Composable
fun NowPlayingAnimation(
    modifier: Modifier = Modifier,
    color: Color = Color.White,
    isPlaying: Boolean = true,
    barCount: Int = 3,
    barWidth: Dp = 4.dp,
    maxBarHeight: Dp = 18.dp,
    minBarHeight: Dp = 3.dp
) {
    Row(
        modifier = modifier.height(maxBarHeight),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        repeat(barCount) { index ->
            val transition = rememberInfiniteTransition(label = "eq_bar_$index")

            val duration = when (index % 3) {
                0 -> 400
                1 -> 600
                else -> 500
            }

            val heightState = if (isPlaying) {
                transition.animateValue(
                    initialValue = minBarHeight,
                    targetValue = maxBarHeight,
                    typeConverter = Dp.VectorConverter,
                    animationSpec = infiniteRepeatable(
                        animation = tween(
                            durationMillis = duration,
                            easing = FastOutSlowInEasing
                        ),
                        repeatMode = RepeatMode.Reverse,
                        initialStartOffset = StartOffset(index * 90)
                    ),
                    label = "bar_height_$index"
                )
            } else {
                remember {
                    mutableStateOf(
                        when (index % 3) {
                            0 -> maxBarHeight * 0.35f
                            1 -> maxBarHeight * 0.65f
                            else -> maxBarHeight * 0.45f
                        }
                    )
                }
            }

            Box(
                modifier = Modifier
                    .width(barWidth)
                    .layout { measurable, constraints ->
                        val h = heightState.value.roundToPx()
                        val placeable = measurable.measure(
                            constraints.copy(minHeight = h, maxHeight = h)
                        )
                        layout(placeable.width, placeable.height) {
                            placeable.place(0, 0)
                        }
                    }
                    .clip(RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp, bottomStart = 1.dp, bottomEnd = 1.dp))
                    .background(color)
            )
        }
    }
}
