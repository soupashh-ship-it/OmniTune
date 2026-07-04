/*
 * OmniTune - An open-source music player for Android
 * Licensed under GPL-3.0
 */

package com.omnitune.app.ui.screens.settings

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.omnitune.app.R
import com.omnitune.app.ui.theme.OmniColors
import com.omnitune.app.ui.theme.OmniShapes
import com.omnitune.app.viewmodels.BackupRestoreProgress
import com.omnitune.app.viewmodels.BackupRestoreResult
import com.omnitune.app.viewmodels.BackupRestoreScope
import com.omnitune.app.viewmodels.BackupRestoreViewModel
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Composable
fun BackupRestoreScreen(
    viewModel: BackupRestoreViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val progress by viewModel.progress.collectAsState()
    val result by viewModel.result.collectAsState()
    var selectedScope by rememberSaveable { mutableStateOf(BackupRestoreScope.ALL) }

    val backupLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/octet-stream")
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.backup(context, uri, selectedScope)
        }
    }

    val restoreLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.restore(context, uri) {
                viewModel.restartApp(context)
            }
        }
    }

    LaunchedEffect(result) {
        if (result is BackupRestoreResult.Success) {
            kotlinx.coroutines.delay(8000)
            viewModel.clearResult()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 12.dp),
    ) {
        Spacer(Modifier.height(8.dp))

        // Progress indicator
        AnimatedVisibility(visible = progress != null, enter = fadeIn(), exit = fadeOut()) {
            progress?.let { p ->
                Column(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    if (p.indeterminate) {
                        CircularProgressIndicator(
                            color = OmniColors.OmniAccentPrimary,
                            strokeWidth = 3.dp,
                            modifier = Modifier.size(40.dp),
                        )
                    } else {
                        LinearProgressIndicator(
                            progress = { p.percent / 100f },
                            modifier = Modifier.fillMaxWidth().height(6.dp),
                            color = OmniColors.OmniAccentPrimary,
                            trackColor = OmniColors.OmniGlassMedium,
                        )
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = p.title,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = OmniColors.TextPrimary,
                        )
                        Text(
                            text = p.step,
                            style = MaterialTheme.typography.bodySmall,
                            color = OmniColors.TextTertiary,
                        )
                        if (!p.indeterminate) {
                            Text(
                                text = "${p.percent}%",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = OmniColors.OmniAccentPrimary,
                            )
                        }
                    }
                }
            }
        }

        // Result banner
        AnimatedVisibility(visible = result !is BackupRestoreResult.Idle, enter = fadeIn(), exit = fadeOut()) {
            when (val res = result) {
                is BackupRestoreResult.Success -> {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        Text(
                            text = "✅ ${res.message}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = OmniColors.Downloaded,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
                is BackupRestoreResult.Error -> {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        Text(
                            text = "⚠️ ${res.message}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = OmniColors.Warning,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
                else -> {}
            }
        }

        // Backup card
        OmniPreferenceCard(title = "Backup") {
            OmniPreferenceEntry(
                title = "Create backup",
                description = when (selectedScope) {
                    BackupRestoreScope.ALL -> "Settings + database"
                    BackupRestoreScope.SETTINGS_ONLY -> "App preferences only"
                    BackupRestoreScope.DATABASE_ONLY -> "Library and playlists only"
                },
                iconRes = R.drawable.ic_download,
                accent = OmniColors.Downloaded,
                onClick = {
                    val timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
                    backupLauncher.launch("OmniTune_$timestamp.backup")
                },
            )
        }

        Spacer(Modifier.height(12.dp))

        // Backup scope selector
        OmniPreferenceCard(title = "Backup scope") {
            BackupScopeOption(
                label = "Everything",
                description = "Settings, library, playlists, and history",
                isSelected = selectedScope == BackupRestoreScope.ALL,
                onClick = { selectedScope = BackupRestoreScope.ALL },
            )
            BackupScopeOption(
                label = "Settings only",
                description = "App preferences and configuration",
                isSelected = selectedScope == BackupRestoreScope.SETTINGS_ONLY,
                onClick = { selectedScope = BackupRestoreScope.SETTINGS_ONLY },
            )
            BackupScopeOption(
                label = "Library only",
                description = "Songs, playlists, and listening history",
                isSelected = selectedScope == BackupRestoreScope.DATABASE_ONLY,
                onClick = { selectedScope = BackupRestoreScope.DATABASE_ONLY },
            )
        }

        Spacer(Modifier.height(12.dp))

        // Restore card
        OmniPreferenceCard(title = "Restore") {
            OmniPreferenceEntry(
                title = "Restore from backup",
                description = "Overwrites current settings and library. App will restart.",
                iconRes = R.drawable.ic_share,
                accent = OmniColors.Warning,
                onClick = { restoreLauncher.launch(arrayOf("application/octet-stream")) },
            )
        }

        Spacer(Modifier.height(12.dp))

        // Info card
        OmniPreferenceCard(title = "About backups") {
            OmniPreferenceEntry(
                title = "What's included",
                description = "Full backups contain preferences, song library, playlists, and listening history. Database-only backups exclude settings.",
                iconRes = R.drawable.ic_info,
                accent = OmniColors.TextSecondary,
            )
            OmniPreferenceEntry(
                title = "Restore safety",
                description = "Files are validated before applying. If restore fails, original data is preserved automatically.",
                iconRes = R.drawable.ic_info,
                accent = OmniColors.TextSecondary,
            )
            OmniPreferenceEntry(
                title = "File format",
                description = "Backups use the .backup extension and are standard ZIP archives.",
                iconRes = R.drawable.ic_info,
                accent = OmniColors.TextSecondary,
            )
        }

        Spacer(Modifier.height(32.dp))
    }
}

@Composable
private fun BackupScopeOption(
    label: String,
    description: String,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(OmniShapes.Small)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(bounded = true, color = Color.White.copy(alpha = 0.08f)),
                onClick = onClick,
            )
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Canvas(modifier = Modifier.size(22.dp)) {
            val strokeWidth = 2.dp.toPx()
            drawCircle(
                color = if (isSelected) OmniColors.OmniAccentPrimary else OmniColors.TextTertiary.copy(alpha = 0.4f),
                radius = (size.minDimension - strokeWidth) / 2f,
                style = Stroke(width = strokeWidth),
            )
            if (isSelected) {
                drawCircle(
                    color = OmniColors.OmniAccentPrimary,
                    radius = size.minDimension / 4f,
                )
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = if (isSelected) OmniColors.OmniAccentPrimary else OmniColors.TextPrimary,
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = OmniColors.TextTertiary,
            )
        }
    }
}
