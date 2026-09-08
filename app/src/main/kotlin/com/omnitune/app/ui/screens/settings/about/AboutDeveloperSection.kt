/*
 * This file was adapted from SuvMusic.
 * Original copyright follows:
 *
 * Copyright (C) Suvojeet
 * Licensed under the GNU General Public License v3.0 (GPLv3)
 */

package com.omnitune.app.ui.screens.settings.about

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Code
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.omnitune.app.R
import com.omnitune.app.ui.screens.settings.AboutDestinations
import com.omnitune.app.ui.screens.settings.OmniTuneRepositoryUrl
import com.omnitune.app.ui.theme.SquircleShape
import com.omnitune.app.ui.utils.SocialIcons

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun AboutDeveloperSection(onOpenUri: (String) -> Unit) {
    val colorScheme = MaterialTheme.colorScheme
    val primaryColor = colorScheme.primary
    val onSurfaceVariant = colorScheme.onSurfaceVariant
    val developer = AboutDestinations.developer

    AboutSectionTitle("Developer")
    AboutCard(modifier = Modifier.padding(horizontal = 16.dp)) {
        Column(
            modifier = Modifier
                .padding(24.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            AsyncImage(
                model = developer.imageUrl,
                contentDescription = developer.title,
                modifier = Modifier
                    .size(100.dp)
                    .clip(SquircleShape),
                contentScale = ContentScale.Crop,
                error = painterResource(id = R.drawable.logo_pulse),
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = developer.title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = colorScheme.onSurface,
            )

            Text(
                text = developer.subtitle,
                style = MaterialTheme.typography.titleMedium,
                color = primaryColor,
                fontWeight = FontWeight.Medium,
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Maintaining OmniTune as a free, open-source music player focused on privacy, playback reliability and a clean listening experience.",
                style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 20.sp),
                color = onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 8.dp),
            )

            Spacer(modifier = Modifier.height(24.dp))

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                SocialIconBadge(
                    icon = SocialIcons.GitHub,
                    onClick = { onOpenUri(developer.url) },
                )
                SocialIconBadge(
                    icon = Icons.Default.Code,
                    onClick = { onOpenUri(OmniTuneRepositoryUrl) },
                )
            }
        }
    }
    Spacer(modifier = Modifier.height(24.dp))
}
