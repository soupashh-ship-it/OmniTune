package com.omnitune.app.ui.screens.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.omnitune.app.R
import com.omnitune.app.ui.theme.OmniColors
import com.omnitune.app.ui.theme.OmniShapes
import com.omnitune.app.ui.theme.OmniSpacing
import com.omnitune.app.update.ApkInstallLauncher
import com.omnitune.app.update.UpdateState
import com.omnitune.app.update.UpdateViewModel

@Composable
fun UpdatesSettings(
    viewModel: UpdateViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val state by viewModel.state.collectAsState()

    OmniPreferenceCard(title = "Version") {
        OmniPreferenceEntry(
            title = "Installed version",
            description = viewModel.currentVersionLabel,
            iconRes = R.drawable.ic_download,
            accent = OmniColors.OmniAccentSecondary,
        )
    }

    Spacer(Modifier.height(8.dp))

    when (val current = state) {
        UpdateState.Idle -> {
            UpdateStateCard(
                title = "Ready to check",
                body = "Uses the existing GitHub latest-release check. No release status is assumed until you run it.",
                accent = OmniColors.OmniAccentSecondary,
            )
            Spacer(Modifier.height(8.dp))
            SettingsActionButton("Check for updates") {
                viewModel.checkForUpdates()
            }
        }
        UpdateState.Checking -> {
            UpdateStateCard(
                title = "Checking GitHub releases",
                body = "Waiting for the existing update checker to return.",
                accent = OmniColors.OmniAccentSecondary,
                progress = null,
            )
        }
        UpdateState.NoUpdate -> {
            UpdateStateCard(
                title = "Already latest",
                body = "The update checker did not find a newer public release.",
                accent = OmniColors.Success,
            )
            Spacer(Modifier.height(8.dp))
            SettingsActionButton("Check again") {
                viewModel.checkForUpdates()
            }
        }
        is UpdateState.UpdateAvailable -> {
            UpdateDetails(current)
            if (current.requireMeteredConfirmation) {
                UpdateMessage("Mobile data connection detected. Tap again to confirm download.")
            }
            Spacer(Modifier.height(8.dp))
            SettingsActionButton(
                if (current.requireMeteredConfirmation) "Download on mobile data" else "Download update"
            ) {
                viewModel.downloadUpdate(confirmMetered = current.requireMeteredConfirmation)
            }
        }
        is UpdateState.Downloading -> {
            val progress = current.progress.coerceIn(0f, 1f)
            val percent = (progress * 100).toInt().coerceIn(0, 100)
            UpdateStateCard(
                title = "Downloading update",
                body = "$percent% complete",
                accent = OmniColors.OmniAccentSecondary,
                progress = progress,
            )
        }
        is UpdateState.Downloaded -> {
            UpdateStateCard(
                title = "Update downloaded and verified",
                body = "Package: ${current.update.packageName}, code ${current.update.versionCode}",
                accent = OmniColors.Success,
            )
            Spacer(Modifier.height(8.dp))
            SettingsActionButton("Install now") {
                runCatching {
                    if (ApkInstallLauncher.canRequestPackageInstalls(context)) {
                        context.startActivity(ApkInstallLauncher.installIntent(context, current.update.apkFile))
                    } else {
                        context.startActivity(ApkInstallLauncher.installPermissionIntent(context))
                    }
                }.onFailure {
                    viewModel.showError("Could not open Android installer.")
                }
            }
            if (!ApkInstallLauncher.canRequestPackageInstalls(context)) {
                UpdateMessage("Install permission is required to continue.")
            }
        }
        is UpdateState.Error -> {
            UpdateStateCard(
                title = "Update check failed",
                body = current.message,
                accent = OmniColors.Error,
            )
            Spacer(Modifier.height(8.dp))
            SettingsActionButton("Try again") {
                viewModel.checkForUpdates()
            }
        }
    }
}


@Composable
fun UpdateDetails(state: UpdateState.UpdateAvailable) {
    val update = state.update
    UpdateStateCard(
        title = "Update available",
        body = "Latest version: ${update.versionName}\nAPK: ${formatBytes(update.apkAsset.size)}",
        accent = OmniColors.Success,
    )
    if (update.releaseNotes.isNotBlank()) {
        Text(
            text = update.releaseNotes,
            style = MaterialTheme.typography.bodySmall,
            color = OmniColors.TextSecondary,
            maxLines = 6,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
        )
    }
}


@Composable
fun UpdateStateCard(
    title: String,
    body: String,
    accent: Color,
    progress: Float? = null,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
            .clip(OmniShapes.Medium)
            .background(OmniColors.OmniGlassMedium)
            .border(BorderStroke(1.dp, OmniColors.OmniGlassBorderSubtle), OmniShapes.Medium)
            .padding(OmniSpacing.medium),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(accent),
            )
            Spacer(modifier = Modifier.width(OmniSpacing.compact))
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = OmniColors.TextPrimary,
            )
        }
        Spacer(modifier = Modifier.height(OmniSpacing.compact))
        Text(
            text = body,
            style = MaterialTheme.typography.bodySmall,
            color = OmniColors.TextSecondary,
        )
        progress?.let {
            Spacer(modifier = Modifier.height(OmniSpacing.small))
            LinearProgressIndicator(
                progress = { it.coerceIn(0f, 1f) },
                color = accent,
                trackColor = OmniColors.OmniGlassSubtle,
                modifier = Modifier.fillMaxWidth(),
            )
        } ?: if (title.startsWith("Checking")) {
            Spacer(modifier = Modifier.height(OmniSpacing.small))
            LinearProgressIndicator(
                color = accent,
                trackColor = OmniColors.OmniGlassSubtle,
                modifier = Modifier.fillMaxWidth(),
            )
        } else {
            Unit
        }
    }
}


@Composable
fun UpdateMessage(message: String) {
    Text(
        text = message,
        style = MaterialTheme.typography.bodySmall,
        color = OmniColors.TextSecondary,
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
    )
}

private fun formatBytes(bytes: Long): String {
    if (bytes <= 0L) return "Unknown size"
    val mb = bytes / (1024.0 * 1024.0)
    return "%.1f MB".format(mb)
}
