package com.omnitune.app.ui.shell

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.omnitune.app.ui.theme.OmniColors

@Composable
fun OmniShellBackground(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(OmniColors.OmniBackgroundBase)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(OmniColors.BackgroundGradient)
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            OmniColors.OmniAccentPrimary.copy(alpha = 0.18f),
                            Color.Transparent,
                        ),
                        radius = 980f,
                    )
                )
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            OmniColors.OmniBackgroundBase.copy(alpha = 0.72f),
                        )
                    )
                )
        )
        content()
    }
}
