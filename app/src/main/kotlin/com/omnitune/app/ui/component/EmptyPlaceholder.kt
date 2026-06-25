/*
 * OmniTune - An open-source music player for Android
 * Licensed under GPL-3.0
 */

package com.omnitune.app.ui.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.omnitune.app.ui.theme.OmniColors
import com.omnitune.app.ui.theme.OmniShapes
import com.omnitune.app.ui.theme.OmniSpacing
import com.omnitune.app.ui.theme.OmniTextStyles
import com.omnitune.app.ui.theme.omniGlassSurface

@Composable
fun EmptyPlaceholder(
    icon: Int,
    text: String,
    modifier: Modifier = Modifier,
    action: @Composable (() -> Unit)? = null
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = OmniSpacing.section, vertical = OmniSpacing.screen),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(96.dp)
                    .clip(OmniShapes.Pill)
                    .omniGlassSurface(
                        shape = OmniShapes.Pill,
                        background = OmniColors.OmniGlassSubtle,
                        borderColor = OmniColors.OmniGlassBorderSubtle,
                    )
            ) {
                Image(
                    painter = painterResource(icon),
                    contentDescription = null,
                    colorFilter = ColorFilter.tint(OmniColors.OmniAccentSecondary.copy(alpha = 0.62f)),
                    modifier = Modifier.size(48.dp),
                )
            }

            Spacer(Modifier.height(OmniSpacing.large))

            Text(
                text = text,
                style = OmniTextStyles.metadata,
                fontWeight = FontWeight.Medium,
                color = OmniColors.TextSecondary,
                textAlign = TextAlign.Center,
            )

            if (action != null) {
                Spacer(Modifier.height(OmniSpacing.section))
                action()
            }
        }
    }
}
