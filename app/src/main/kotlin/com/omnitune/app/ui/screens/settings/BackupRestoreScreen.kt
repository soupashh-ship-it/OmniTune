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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.omnitune.app.R
import com.omnitune.app.backup.OmniBackupCounts
import com.omnitune.app.ui.theme.OmniColors
import com.omnitune.app.viewmodels.BackupRestoreResult
import com.omnitune.app.viewmodels.BackupRestoreViewModel
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun BackupRestoreScreen(
    viewModel: BackupRestoreViewModel = hiltViewModel(),
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val progress by viewModel.progress.collectAsState()
    val result by viewModel.result.collectAsState()
    val lastBackupAt by viewModel.lastBackupAt.collectAsState()
    var pendingImportUri by remember { mutableStateOf<Uri?>(null) }
    var includeDownloadedAudio by remember { mutableStateOf(false) }
    var replaceExistingOnImport by remember { mutableStateOf(false) }

    val jsonBackupLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json"),
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.backup(context, uri, includeDownloadedAudio = false)
        }
    }

    val fullBackupLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/zip"),
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.backup(context, uri, includeDownloadedAudio = true)
        }
    }

    val restoreLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri: Uri? ->
        if (uri != null) {
            pendingImportUri = uri
        }
    }

    LaunchedEffect(result) {
        if (result is BackupRestoreResult.Success) {
            kotlinx.coroutines.delay(10_000)
            viewModel.clearResult()
        }
    }

    pendingImportUri?.let { uri ->
        AlertDialog(
            onDismissRequest = { pendingImportUri = null },
            title = { Text("Import library backup?") },
            text = {
                Text(
                    if (replaceExistingOnImport) {
                        "This will replace your current liked songs, playlists, saved artists, albums, history, and stats with the selected backup. Cache data is not cleared. Offline audio from a full archive will be applied after app restart."
                    } else {
                        "This will merge liked songs, playlists, saved artists, albums, history, and stats into your current library. Existing data will not be deleted. Offline audio from a full archive will be applied after app restart."
                    },
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        pendingImportUri = null
                        viewModel.restore(context, uri, replaceExisting = replaceExistingOnImport)
                    },
                ) {
                    Text("Import")
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingImportUri = null }) {
                    Text("Cancel")
                }
            },
        )
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp),
    ) {
        Spacer(Modifier.height(8.dp))

        AnimatedVisibility(visible = progress != null, enter = fadeIn(), exit = fadeOut()) {
            progress?.let { p ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
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
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp),
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
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }
        }

        AnimatedVisibility(visible = result !is BackupRestoreResult.Idle, enter = fadeIn(), exit = fadeOut()) {
            when (val res = result) {
                is BackupRestoreResult.Success -> ResultSummary(
                    title = res.title,
                    message = res.message,
                    counts = res.counts,
                )
                is BackupRestoreResult.Error -> {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        Text(
                            text = res.message,
                            style = MaterialTheme.typography.bodyMedium,
                            color = OmniColors.Warning,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
                else -> Unit
            }
        }

        OmniPreferenceCard(title = "Backup & Restore") {
            OmniPreferenceEntry(
                title = "Export library backup",
                description = if (includeDownloadedAudio) {
                    "Save library data plus app-managed offline audio to a full ZIP archive."
                } else {
                    "Save liked songs, playlists, saved artists, albums, history, and stats to a versioned JSON file."
                },
                iconRes = R.drawable.ic_download,
                accent = OmniColors.Downloaded,
                onClick = {
                    val timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd-HH-mm"))
                    if (includeDownloadedAudio) {
                        fullBackupLauncher.launch("omnitune-library-backup-$timestamp.zip")
                    } else {
                        jsonBackupLauncher.launch("omnitune-library-backup-$timestamp.json")
                    }
                },
            )
            OmniSwitchPreference(
                title = "Include downloaded audio",
                description = "Creates a larger ZIP backup with app-managed offline audio and the Media3 download index.",
                iconRes = R.drawable.ic_download,
                accent = OmniColors.Downloaded,
                checked = includeDownloadedAudio,
                onCheckedChange = { includeDownloadedAudio = it },
            )
            OmniPreferenceEntry(
                title = "Import library backup",
                description = if (replaceExistingOnImport) {
                    "Import an OmniTune backup and replace existing library records."
                } else {
                    "Merge an OmniTune JSON or full ZIP backup without deleting existing data."
                },
                iconRes = R.drawable.ic_share,
                accent = OmniColors.OmniAccentPrimary,
                onClick = {
                    restoreLauncher.launch(
                        arrayOf(
                            "application/json",
                            "application/zip",
                            "text/*",
                            "application/octet-stream",
                        ),
                    )
                },
            )
            OmniSwitchPreference(
                title = "Replace existing library on import",
                description = "Advanced mode. Clears current library, playlists, history, stats, and tags before importing the backup.",
                iconRes = R.drawable.ic_share,
                accent = OmniColors.Warning,
                checked = replaceExistingOnImport,
                onCheckedChange = { replaceExistingOnImport = it },
            )
            OmniPreferenceEntry(
                title = "Last backup",
                description = lastBackupAt?.let(::formatBackupTime) ?: "Never",
                iconRes = R.drawable.ic_info,
                accent = OmniColors.TextSecondary,
            )
        }

        Spacer(Modifier.height(12.dp))

        OmniPreferenceCard(title = "Included data") {
            OmniPreferenceEntry(
                title = "Library and relationships",
                description = "Liked songs, saved library songs, playlists, playlist order, saved artists, saved albums, and playlist tags.",
                iconRes = R.drawable.ic_info,
                accent = OmniColors.TextSecondary,
            )
            OmniPreferenceEntry(
                title = "History and stats",
                description = "Listening history, recently played records, play counts, and last-played timestamps.",
                iconRes = R.drawable.ic_info,
                accent = OmniColors.TextSecondary,
            )
        }

        Spacer(Modifier.height(12.dp))

        OmniPreferenceCard(title = "Not included") {
            OmniPreferenceEntry(
                title = "Temporary cache",
                description = "Stream URLs, extractor cache, image cache, and session-only queue state are not exported.",
                iconRes = R.drawable.ic_info,
                accent = OmniColors.TextSecondary,
            )
            OmniPreferenceEntry(
                title = "Private account data",
                description = "Secrets, API keys, login tokens, and device-specific file paths are never included in manual library backups.",
                iconRes = R.drawable.ic_info,
                accent = OmniColors.TextSecondary,
            )
        }

        Spacer(Modifier.height(32.dp))
    }
}

@Composable
private fun ResultSummary(
    title: String,
    message: String,
    counts: OmniBackupCounts,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = OmniColors.Downloaded,
            textAlign = TextAlign.Center,
        )
        Text(
            text = message,
            style = MaterialTheme.typography.bodySmall,
            color = OmniColors.TextSecondary,
            textAlign = TextAlign.Center,
        )
        Text(
            text = counts.summaryText(),
            style = MaterialTheme.typography.bodySmall,
            color = OmniColors.TextTertiary,
            textAlign = TextAlign.Center,
        )
    }
}

private fun OmniBackupCounts.summaryText(): String = buildList {
    add("$songs songs")
    add("$likedSongs liked")
    add("$playlists playlists")
    add("$playlistEntries playlist entries")
    add("$artists artists")
    add("$albums albums")
    add("$historyItems history")
    add("$statRecords stats")
    if (tags > 0) add("$tags tags")
    if (playlistTags > 0) add("$playlistTags playlist tags")
    if (downloadedAudioFiles > 0) add("$downloadedAudioFiles offline files")
    if (skippedDuplicates > 0) add("$skippedDuplicates duplicates skipped")
    if (skippedInvalidRows > 0) add("$skippedInvalidRows invalid rows skipped")
}.joinToString(" · ")

private fun formatBackupTime(epochMillis: Long): String =
    DateTimeFormatter.ofPattern("MMM d, yyyy h:mm a")
        .format(Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault()))
