/*
 * OmniTune - An open-source music player for Android
 * Licensed under GPL-3.0
 */

package com.omnitune.app.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.omnitune.app.BuildConfig
import com.omnitune.app.R
import com.omnitune.app.ui.theme.OmniColors
import com.omnitune.app.ui.theme.OmniShapes
import com.omnitune.app.ui.theme.OmniSpacing
import kotlinx.coroutines.launch

@Composable
fun AboutSettings() {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val installedDate = remember(context) { installedDateLabel(context) }

    fun open(url: String) {
        if (!context.openExternalUrl(url)) {
            scope.launch {
                snackbarHostState.showSnackbar("Could not open link.")
            }
        }
    }

    Box(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(OmniSpacing.large),
        ) {
            AboutIdentityCard()

            AboutSection(title = "DEVELOPER") {
                AboutExternalLinkRow(
                    entry = AboutDestinations.developer,
                    onOpen = { open(it) },
                )
            }

            AboutSection(title = "INSPIRATION") {
                AboutDestinations.inspiration.forEach { entry ->
                    AboutExternalLinkRow(
                        entry = entry,
                        onOpen = { open(it) },
                    )
                }
            }

            AboutSection(title = "COMMUNITY") {
                AboutExternalLinkRow(
                    entry = AboutLinkEntry(
                        title = "GitHub Repository",
                        subtitle = "View source code",
                        url = OmniTuneRepositoryUrl,
                        iconRes = R.drawable.ic_info,
                    ),
                    onOpen = { open(it) },
                )
                AboutDestinations.discordUrl?.let { url ->
                    AboutExternalLinkRow(
                        entry = AboutLinkEntry(
                            title = "Discord Server",
                            subtitle = "Join the community to chat and report bugs",
                            url = url,
                            iconRes = R.drawable.ic_discord,
                        ),
                        onOpen = { uriHandler.openUri(it) },
                    )
                }
            }

            DonationSection(
                onSnackbar = { message ->
                    scope.launch { snackbarHostState.showSnackbar(message) }
                },
            )

            AboutSection(title = "APP INFO") {
                AboutInfoRow(
                    title = "Installed date",
                    subtitle = installedDate,
                    iconRes = R.drawable.ic_calendar,
                )
                AboutInfoRow(
                    title = "Version",
                    subtitle = "v${BuildConfig.VERSION_NAME}",
                    iconRes = R.drawable.ic_info,
                )
                AboutInfoRow(
                    title = "Version code",
                    subtitle = BuildConfig.VERSION_CODE.toString(),
                    iconRes = R.drawable.ic_list,
                )
                AboutExternalLinkRow(
                    entry = AboutLinkEntry(
                        title = "GNU General Public License v3.0",
                        subtitle = "GPL-3.0 • Free Open Source Software",
                        url = OmniTuneLicenseUrl,
                        iconRes = R.drawable.ic_info,
                    ),
                    onOpen = { open(it) },
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}

@Composable
private fun AboutIdentityCard() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp),
        shape = OmniShapes.ExtraLarge,
        colors = CardDefaults.cardColors(containerColor = OmniColors.SurfacePanel),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.radialGradient(
                        listOf(
                            OmniColors.OmniAccentPrimary.copy(alpha = 0.22f),
                            Color.Transparent,
                        )
                    )
                )
                .padding(horizontal = OmniSpacing.section, vertical = OmniSpacing.hero),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(OmniSpacing.medium),
        ) {
            Text(
                text = "OMNITUNE",
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 2.sp,
                color = OmniColors.TextPrimary,
                textAlign = TextAlign.Center,
            )
            Row(
                modifier = Modifier
                    .clip(OmniShapes.Pill)
                    .border(1.dp, OmniColors.OmniGlassBorderStrong.copy(alpha = 0.75f), OmniShapes.Pill)
                    .background(OmniColors.OmniGlassStrong)
                    .padding(horizontal = OmniSpacing.medium, vertical = OmniSpacing.compact),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(OmniSpacing.compact),
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_info),
                    contentDescription = null,
                    tint = OmniColors.TextSecondary,
                    modifier = Modifier.size(16.dp),
                )
                Text(
                    text = "v${BuildConfig.VERSION_NAME} • ${if (BuildConfig.DEBUG) "DEBUG" else "RELEASE"}",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = OmniColors.TextSecondary,
                )
            }
        }
    }
}

@Composable
private fun AboutSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(OmniSpacing.compact),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.4.sp,
            color = OmniColors.OmniAccentWarm.copy(alpha = 0.78f),
            modifier = Modifier.padding(horizontal = 20.dp),
        )
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(OmniSpacing.micro),
            content = content,
        )
    }
}

@Composable
private fun AboutExternalLinkRow(
    entry: AboutLinkEntry,
    onOpen: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    AboutBaseRow(
        title = entry.title,
        subtitle = entry.subtitle,
        iconRes = entry.iconRes,
        initials = entry.initials,
        imageUrl = entry.imageUrl,
        modifier = modifier.clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = androidx.compose.material3.ripple(
                bounded = true,
                color = Color.White.copy(alpha = 0.08f),
            ),
            onClick = { onOpen(entry.url) },
        ),
    )
}

@Composable
private fun AboutInfoRow(
    title: String,
    subtitle: String,
    iconRes: Int,
    modifier: Modifier = Modifier,
) {
    AboutBaseRow(
        title = title,
        subtitle = subtitle,
        iconRes = iconRes,
        initials = null,
        imageUrl = null,
        modifier = modifier,
    )
}

@Composable
private fun AboutBaseRow(
    title: String,
    subtitle: String,
    iconRes: Int?,
    initials: String?,
    imageUrl: String?,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 64.dp)
            .padding(horizontal = 20.dp, vertical = OmniSpacing.compact),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(OmniSpacing.medium),
    ) {
        AboutLeadingMark(iconRes = iconRes, initials = initials, imageUrl = imageUrl)
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium,
                color = OmniColors.TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = OmniColors.TextSecondary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun AboutLeadingMark(
    iconRes: Int?,
    initials: String?,
    imageUrl: String?,
) {
    Box(
        modifier = Modifier
            .size(46.dp)
            .clip(CircleShape)
            .background(
                Brush.linearGradient(
                    listOf(
                        OmniColors.OmniAccentPrimary.copy(alpha = 0.22f),
                        OmniColors.OmniGlassStrong,
                    )
                )
            ),
        contentAlignment = Alignment.Center,
    ) {
        when {
            !imageUrl.isNullOrBlank() -> AsyncImage(
                model = imageUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape),
            )
            iconRes != null -> Icon(
                painter = painterResource(iconRes),
                contentDescription = null,
                tint = OmniColors.TextPrimary.copy(alpha = 0.86f),
                modifier = Modifier.size(22.dp),
            )
            !initials.isNullOrBlank() -> Text(
                text = initials.take(2).uppercase(),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                color = OmniColors.TextPrimary,
            )
        }
    }
}

@Composable
private fun DonationSection(
    onSnackbar: (String) -> Unit,
) {
    val context = LocalContext.current
    val destination = AboutDestinations.supportUpi ?: return

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        shape = OmniShapes.Large,
        colors = CardDefaults.cardColors(containerColor = OmniColors.SurfacePanel),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        listOf(
                            OmniColors.OmniAccentPrimary.copy(alpha = 0.16f),
                            OmniColors.OmniGlassStrong.copy(alpha = 0.42f),
                        ),
                    ),
                )
                .padding(OmniSpacing.medium),
            verticalArrangement = Arrangement.spacedBy(OmniSpacing.small),
        ) {
            Text(
                text = "Support OmniTune",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = OmniColors.TextPrimary,
            )
            Text(
                text = "If you enjoy OmniTune, consider buying me a chai. Tap below to donate via UPI, Cards, or NetBanking.",
                style = MaterialTheme.typography.bodyMedium,
                color = OmniColors.TextSecondary,
            )

            Button(
                onClick = {
                    if (!context.openExternalUrl(INSTAMOJO_DONATION_URL)) {
                        onSnackbar("Could not open payment page")
                    }
                },
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = OmniShapes.Medium,
                colors = ButtonDefaults.buttonColors(
                    containerColor = OmniColors.OmniAccentPrimary.copy(alpha = 0.92f),
                    contentColor = if (OmniColors.OmniAccentPrimary.luminance() > 0.52f) Color.Black else Color.White,
                ),
            ) {
                Text(
                    text = "Donate via UPI / Cards",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                )
            }

            TextButton(
                onClick = {
                    context.getSystemService(android.content.ClipboardManager::class.java)
                        ?.setPrimaryClip(
                            android.content.ClipData.newPlainText("OmniTune UPI ID", destination.upiId),
                        )
                    onSnackbar("UPI ID copied")
                },
                modifier = Modifier.align(Alignment.CenterHorizontally),
            ) {
                Text(
                    text = "Or pay via UPI directly",
                    style = MaterialTheme.typography.labelMedium,
                    color = OmniColors.TextMuted,
                )
            }
        }
    }
}