/*
 * This file was adapted from SuvMusic.
 * Original copyright follows:
 *
 * Copyright (C) Suvojeet
 * Licensed under the GNU General Public License v3.0 (GPLv3)
 */

package com.omnitune.app.ui.screens.settings

import android.content.ActivityNotFoundException
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.omnitune.app.BuildConfig
import com.omnitune.app.constants.LastUpdateCheckKey
import com.omnitune.app.constants.UpdateChannel
import com.omnitune.app.constants.UpdateChannelKey
import com.omnitune.app.update.ApkInstallLauncher
import com.omnitune.app.update.ChangelogRelease
import com.omnitune.app.update.ChangelogSource
import com.omnitune.app.update.ChangelogViewModel
import com.omnitune.app.update.DownloadedUpdate
import com.omnitune.app.update.UpdateState
import com.omnitune.app.update.UpdateViewModel
import com.omnitune.app.utils.rememberEnumPreference
import com.omnitune.app.utils.rememberPreference
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val UpdaterSquircleShape = RoundedCornerShape(28.dp)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UpdaterScreen(
    viewModel: UpdateViewModel = hiltViewModel(),
    changelogViewModel: ChangelogViewModel = hiltViewModel(),
    onBackClick: () -> Unit,
) {
    val context = LocalContext.current
    val updateState by viewModel.state.collectAsStateWithLifecycle()
    val changelogState by changelogViewModel.state.collectAsStateWithLifecycle()
    var updateChannel by rememberEnumPreference(UpdateChannelKey, UpdateChannel.STABLE)
    val (lastCheckedAt) = rememberPreference(LastUpdateCheckKey, 0L)
    var installMessage by remember { mutableStateOf<String?>(null) }

    val installPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
    ) {
        val downloaded = (viewModel.state.value as? UpdateState.Downloaded)?.update
        when {
            downloaded == null -> Unit
            !ApkInstallLauncher.canRequestPackageInstalls(context) -> {
                installMessage = "Android did not grant install permission. Allow it and try again."
            }
            else -> {
                installMessage = null
                runCatching {
                    context.startActivity(ApkInstallLauncher.installIntent(context, downloaded.apkFile))
                }.onFailure {
                    installMessage = updaterInstallLaunchError(it)
                }
            }
        }
    }

    fun checkForUpdates() {
        installMessage = null
        viewModel.checkForUpdates(updateChannel)
        changelogViewModel.refreshLatestRelease()
    }

    fun installUpdate(downloaded: DownloadedUpdate) {
        installMessage = null
        runCatching {
            if (ApkInstallLauncher.canRequestPackageInstalls(context)) {
                context.startActivity(ApkInstallLauncher.installIntent(context, downloaded.apkFile))
            } else {
                installPermissionLauncher.launch(ApkInstallLauncher.installPermissionIntent(context))
            }
        }.onFailure { error ->
            installMessage = updaterInstallLaunchError(error)
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("System Update", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .clickable(onClick = onBackClick)
                            .padding(8.dp),
                    ) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = ::checkForUpdates) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                ),
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                UpdaterStatusCard(
                    currentVersionName = BuildConfig.VERSION_NAME,
                    currentVersionCode = BuildConfig.VERSION_CODE,
                    updateChannel = updateChannel,
                    updateState = updateState,
                    lastUpdated = lastCheckedAt.takeIf { it > 0L },
                    installMessage = installMessage,
                    onCheckUpdate = ::checkForUpdates,
                    onDownloadUpdate = { confirmMetered -> viewModel.downloadUpdate(confirmMetered) },
                    onInstallUpdate = ::installUpdate,
                    onDismiss = viewModel::reset,
                )
            }

            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "History & Release Notes",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                    )

                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = CircleShape,
                    ) {
                        Text(
                            text = when (changelogState.release.source) {
                                ChangelogSource.Bundled -> "bundled"
                                ChangelogSource.GitHub -> "GitHub"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }

            if (changelogState.loading) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        UpdaterPulseLoadingIndicator()
                    }
                }
            } else {
                item {
                    UpdaterChangelogItem(release = changelogState.release)
                }
            }

            changelogState.errorMessage?.let { message ->
                item {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = UpdaterSquircleShape,
                        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.55f),
                    ) {
                        Text(
                            text = message,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(20.dp),
                        )
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(80.dp)) }
        }
    }
}

@Composable
private fun UpdaterStatusCard(
    currentVersionName: String,
    currentVersionCode: Int,
    updateChannel: UpdateChannel,
    updateState: UpdateState,
    lastUpdated: Long?,
    installMessage: String?,
    onCheckUpdate: () -> Unit,
    onDownloadUpdate: (confirmMetered: Boolean) -> Unit,
    onInstallUpdate: (DownloadedUpdate) -> Unit,
    onDismiss: () -> Unit,
) {
    Surface(
        shape = UpdaterSquircleShape,
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        tonalElevation = 1.dp,
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
        ),
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(UpdaterSquircleShape)
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                MaterialTheme.colorScheme.primaryContainer,
                                MaterialTheme.colorScheme.secondaryContainer,
                            ),
                        ),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Default.SystemUpdate,
                    contentDescription = null,
                    modifier = Modifier.size(40.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "OmniTune $currentVersionName",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = "Build $currentVersionCode • ${updateChannel.displayName}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                modifier = Modifier.padding(top = 4.dp),
            )

            if (lastUpdated != null) {
                val timeFormat = remember {
                    SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())
                }
                Text(
                    text = "Last checked: ${timeFormat.format(Date(lastUpdated))}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.padding(top = 4.dp),
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            when (updateState) {
                UpdateState.Idle -> {
                    Text(
                        text = "Ready to check GitHub releases",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = onCheckUpdate,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = UpdaterSquircleShape,
                    ) {
                        Text("Check for Updates", fontWeight = FontWeight.Bold)
                    }
                }

                UpdateState.Checking -> {
                    UpdaterPulseLoadingIndicator()
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "Checking for updates...",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                    )
                }

                UpdateState.NoUpdate -> {
                    UpdaterInlineState(
                        iconTint = Color(0xFF4CAF50),
                        containerColor = Color(0xFF4CAF50).copy(alpha = 0.1f),
                        icon = Icons.Default.CheckCircle,
                        text = "You're using the latest version",
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = onCheckUpdate,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = UpdaterSquircleShape,
                    ) {
                        Text("Check Again", fontWeight = FontWeight.Bold)
                    }
                }

                is UpdateState.UpdateAvailable -> {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.Info,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    "New version ${updateState.update.versionName} is available!",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                )
                            }
                            Text(
                                text = "Size: ${formatUpdaterBytes(updateState.update.apkAsset.size)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                                modifier = Modifier.padding(start = 36.dp, top = 4.dp),
                            )
                            if (updateState.requireMeteredConfirmation) {
                                Text(
                                    text = "Mobile data connection detected. Tap Download again to confirm.",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.padding(start = 36.dp, top = 8.dp),
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        OutlinedButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .weight(1f)
                                .height(56.dp),
                            shape = UpdaterSquircleShape,
                        ) {
                            Text("Later", fontWeight = FontWeight.Bold)
                        }
                        Button(
                            onClick = { onDownloadUpdate(updateState.requireMeteredConfirmation) },
                            modifier = Modifier
                                .weight(1f)
                                .height(56.dp),
                            shape = UpdaterSquircleShape,
                        ) {
                            Text("Download", fontWeight = FontWeight.Bold)
                        }
                    }
                }

                is UpdateState.Downloading -> {
                    UpdaterDownloadProgressView(updateState.progress.coerceIn(0f, 1f))
                }

                is UpdateState.Downloaded -> {
                    UpdaterInlineState(
                        iconTint = Color(0xFF4CAF50),
                        containerColor = Color(0xFF4CAF50).copy(alpha = 0.1f),
                        icon = Icons.Default.CheckCircle,
                        text = "Update downloaded and verified",
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = { onInstallUpdate(updateState.update) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = UpdaterSquircleShape,
                    ) {
                        Text("Install Now", fontWeight = FontWeight.Bold)
                    }
                }

                is UpdateState.Error -> {
                    UpdaterInlineState(
                        iconTint = MaterialTheme.colorScheme.error,
                        containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.1f),
                        icon = Icons.Default.Error,
                        text = updateState.message,
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = onCheckUpdate,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = UpdaterSquircleShape,
                    ) {
                        Text("Try Again", fontWeight = FontWeight.Bold)
                    }
                }
            }

            installMessage?.let { message ->
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}

@Composable
private fun UpdaterInlineState(
    iconTint: Color,
    containerColor: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(containerColor)
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(text, color = iconTint, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun UpdaterDownloadProgressView(progress: Float) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom,
        ) {
            Text(
                text = "Downloading...",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = "${(progress * 100).toInt()}%",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.primary,
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(12.dp)
                .clip(CircleShape),
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
        )
    }
}

@Composable
private fun UpdaterChangelogItem(release: ChangelogRelease) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = UpdaterSquircleShape,
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f),
        ),
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = release.releaseName,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = release.publishedAt?.take(10) ?: "Installed release notes",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        fontWeight = FontWeight.Medium,
                    )
                }

                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.padding(start = 12.dp),
                ) {
                    Text(
                        text = when (release.source) {
                            ChangelogSource.Bundled -> "LOCAL"
                            ChangelogSource.GitHub -> "LATEST"
                        },
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = release.body.ifBlank { "No release notes available." },
                style = MaterialTheme.typography.bodyLarge,
                lineHeight = 24.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                maxLines = 16,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun UpdaterPulseLoadingIndicator(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
) {
    val infiniteTransition = rememberInfiniteTransition(label = "PulseTransition")
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "PulseScale",
    )
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "PulseAlpha",
    )

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(60.dp)
                .scale(scale)
                .background(color.copy(alpha = alpha * 0.5f), CircleShape),
        )
        Box(
            modifier = Modifier
                .size(40.dp)
                .scale(scale * 0.9f)
                .background(color.copy(alpha = alpha), CircleShape),
        )
        Icon(
            imageVector = Icons.Default.MusicNote,
            contentDescription = "Loading",
            tint = Color.White,
            modifier = Modifier.size(24.dp),
        )
    }
}

private val UpdateChannel.displayName: String
    get() = when (this) {
        UpdateChannel.STABLE -> "Stable"
        UpdateChannel.BETA -> "Beta"
        UpdateChannel.NIGHTLY -> "Prerelease"
    }

private fun formatUpdaterBytes(bytes: Long): String {
    if (bytes <= 0L) return "Unknown size"
    val mb = bytes / (1024.0 * 1024.0)
    return "%.1f MB".format(Locale.US, mb)
}

private fun updaterInstallLaunchError(error: Throwable): String =
    if (error is ActivityNotFoundException) {
        "No Android package installer is available on this device."
    } else {
        "Could not open the Android package installer."
    }
