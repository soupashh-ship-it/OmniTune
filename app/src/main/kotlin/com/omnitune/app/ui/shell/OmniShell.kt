package com.omnitune.app.ui.shell

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.omnitune.app.LocalPlayerConnection
import com.omnitune.app.ui.player.rememberPlayerGradient
import com.omnitune.app.ui.theme.OmniColors
import kotlinx.coroutines.flow.flowOf

@Composable
fun OmniShellBackground(content: @Composable () -> Unit) {
    val playerConnection = LocalPlayerConnection.current
    val mediaMetadata by (playerConnection?.mediaMetadata ?: flowOf(null)).collectAsState(initial = null)

    val gradientState = rememberPlayerGradient(
        thumbnailUrl = mediaMetadata?.thumbnailUrl,
        videoId = mediaMetadata?.id,
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(OmniColors.OmniBackgroundBase)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(gradientState.backgroundBrush)
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
        content()
    }
}
