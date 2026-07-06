package com.omnitune.app.ui.shell

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.omnitune.app.ui.navigation.LocalPlayerConnection
import com.omnitune.app.ui.player.rememberPlayerGradient
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import com.omnitune.app.ui.theme.OmniColors

@Composable
fun OmniShellBackground(content: @Composable () -> Unit) {
    val playerConnection = LocalPlayerConnection.current
    val mediaMetadata by (playerConnection?.mediaMetadata ?: kotlinx.coroutines.flow.flowOf(null)).collectAsState(initial = null)
    
    val gradientState = rememberPlayerGradient(
        thumbnailUrl = mediaMetadata?.thumbnailUrl,
        videoId = mediaMetadata?.id,
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(OmniColors.OmniBackgroundBase)
    ) {
        AnimatedContent(
            targetState = gradientState,
            transitionSpec = {
                fadeIn(tween(1000)) togetherWith fadeOut(tween(1000))
            },
            label = "GlobalBackgroundAnimation"
        ) { state ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(state.backgroundBrush)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    OmniColors.OmniBackgroundBase.copy(alpha = 0.85f),
                                )
                            )
                        )
                )
            }
        }
        content()
    }
}
