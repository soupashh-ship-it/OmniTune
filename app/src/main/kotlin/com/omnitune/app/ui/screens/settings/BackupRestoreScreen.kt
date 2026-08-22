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
import com.omnitune.app.backup.OmniRestoreSelection
import com.omnitune.app.ui.theme.OmniColors
import com.omnitune.app.viewmodels.BackupRestoreResult
import com.omnitune.app.viewmodels.BackupRestoreViewModel
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun BackupRestoreScreen(
    viewModel: BackupRestoreViewModel = hiltViewModel(),
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val progress by viewModel.progress.collectAsStateWithLifecycle()
    val result by viewModel.result.collectAsStateWithLifecycle()
    val lastBackupAt by viewModel.lastBackupAt.collectAsStateWithLifecycle()
    val latestSafetyBackup by viewModel.latestSafetyBackup.collectAsStateWithLifecycle()
    var includeDownloadedAudio by remember { mutableStateOf(false) }
    var replaceExistingOnImport by remember { mutableStateOf(false) }
    var mergeLibraryAndLikes by remember { mutableStateOf(true) }
    var mergePlaylists by remember { mutableStateOf(true) }
    var mergeHistoryAndStats by remember { mutableStateOf(true) }
    var pendingRestoreSelection by remember { mutableStateOf(OmniRestoreSelection.ALL) }
    var showSafetyRecoveryConfirmation by remember { mutableStateOf(false) }

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
            viewModel.previewRestore(
                context,
                uri,
                replaceExisting = replaceExistingOnImport,
                selection = pendingRestoreSelection,
            )
        }
    }

    LaunchedEffect(result) {
        if (result is BackupRestoreResult.Success) {
            kotlinx.coroutines.delay(10_000)
            viewModel.clearResult()
        }
    }

    (result as? BackupRestoreResult.Preview)?.let { preview ->
        AlertDialog(
            onDismissRequest = viewModel::clearResult,
            title = {
                Text(if (preview.replaceExisting) "Replace library with this backup?" else "Merge this library backup?")
            },
            text = {
                BackupPreviewDetails(preview.details, preview.replaceExisting)
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.restore(
                            context,
                            preview.uri,
                            replaceExisting = preview.replaceExisting,
                            selection = preview.selection,
                        )
                    },
                ) {
                    Text(if (preview.replaceExisting) "Create safety backup & replace" else "Merge")
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::clearResult) {
                    Text("Cancel")
                }
            },
        )
    }

    if (showSafetyRecoveryConfirmation) {
        AlertDialog(
            onDismissRequest = { showSafetyRecoveryConfirmation = false },
            title = { Text("Recover the retained safety backup?") },
            text = {
                Text(
                    "This replaces the current restorable library with the retained pre-Replace state. OmniTune first creates another verified safety backup of the current library.",
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showSafetyRecoveryConfirmation = false
                    viewModel.recoverLatestSafetyBackup()
                }) {
                    Text("Recover")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSafetyRecoveryConfirmation = false }) {
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
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            text = res.message,
                            style = MaterialTheme.typography.bodyMedium,
                            color = OmniColors.Warning,
                            textAlign = TextAlign.Center,
                        )
                        if (res.retryAvailable) {
                            TextButton(onClick = { viewModel.retryRestore(context) }) {
                                Text("Retry safely")
                            }
                        }
                    }
                }
                is BackupRestoreResult.Preview -> Unit
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
                    "Preview an OmniTune backup, then replace existing library records after a verified safety backup is saved."
                } else {
                    "Merge an OmniTune JSON or full ZIP backup without deleting existing data."
                },
                iconRes = R.drawable.ic_share,
                accent = OmniColors.OmniAccentPrimary,
                onClick = {
                    val selection = if (replaceExistingOnImport) {
                        OmniRestoreSelection.ALL
                    } else {
                        OmniRestoreSelection(
                            libraryAndLikes = mergeLibraryAndLikes,
                            playlists = mergePlaylists,
                            historyAndStats = mergeHistoryAndStats,
                            downloads = false,
                        )
                    }
                    pendingRestoreSelection = selection
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
                description = "Advanced mode. A verified app-private safety archive is created before replacing library, playlists, history, stats, tags, downloads, and queue state.",
                iconRes = R.drawable.ic_share,
                accent = OmniColors.Warning,
                checked = replaceExistingOnImport,
                onCheckedChange = { replaceExistingOnImport = it },
            )
            if (!replaceExistingOnImport) {
                OmniSwitchPreference(
                    title = "Merge library and liked songs",
                    description = "Imports saved songs, metadata, artists, albums, and liked state.",
                    iconRes = R.drawable.ic_info,
                    accent = OmniColors.OmniAccentPrimary,
                    checked = mergeLibraryAndLikes,
                    onCheckedChange = { mergeLibraryAndLikes = it },
                )
                OmniSwitchPreference(
                    title = "Merge playlists",
                    description = "Imports playlists, ordering, tags, and the track records they require.",
                    iconRes = R.drawable.ic_info,
                    accent = OmniColors.OmniAccentPrimary,
                    checked = mergePlaylists,
                    onCheckedChange = { mergePlaylists = it },
                )
                OmniSwitchPreference(
                    title = "Merge history and statistics",
                    description = "Imports listening records, play counts, and any track records they require.",
                    iconRes = R.drawable.ic_info,
                    accent = OmniColors.OmniAccentPrimary,
                    checked = mergeHistoryAndStats,
                    onCheckedChange = { mergeHistoryAndStats = it },
                )
            }
            OmniPreferenceEntry(
                title = "Last backup",
                description = lastBackupAt?.let(::formatBackupTime) ?: "Never",
                iconRes = R.drawable.ic_info,
                accent = OmniColors.TextSecondary,
            )
            OmniPreferenceEntry(
                title = "Replace safety backup",
                description = latestSafetyBackup?.let {
                    "Recover retained backup from ${formatBackupTime(it.createdAtEpochMillis)}."
                } ?: "No automatic Replace safety backup has been created yet.",
                iconRes = R.drawable.ic_info,
                accent = if (latestSafetyBackup != null) OmniColors.Warning else OmniColors.TextSecondary,
                onClick = {
                    if (latestSafetyBackup != null) showSafetyRecoveryConfirmation = true
                },
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
private fun BackupPreviewDetails(
    preview: com.omnitune.app.backup.OmniBackupPreview,
    replaceExisting: Boolean,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = if (replaceExisting) {
                "The current restorable library will be replaced only after OmniTune writes and verifies an automatic safety archive."
            } else {
                "Existing library records are retained. Conflicting playlists use deterministic restored names."
            },
            style = MaterialTheme.typography.bodyMedium,
        )
        if (!replaceExisting) {
            val categories = buildList {
                if (preview.selection.libraryAndLikes) add("library and likes")
                if (preview.selection.playlists) add("playlists")
                if (preview.selection.historyAndStats) add("history and statistics")
            }
            Text(
                text = "Merge selection: ${categories.joinToString()}.",
                style = MaterialTheme.typography.bodySmall,
                color = OmniColors.TextSecondary,
            )
        }
        if (replaceExisting) {
            Text(
                text = "Items replaced: saved library records, playlists and order, artists, albums, tags, history, statistics, and download index/files. The session queue is cleared because queue restore is not supported yet.",
                style = MaterialTheme.typography.bodySmall,
                color = OmniColors.TextSecondary,
            )
        }
        Text(
            text = preview.counts.summaryText(),
            style = MaterialTheme.typography.bodySmall,
            color = OmniColors.TextSecondary,
        )
        Text(
            text = if (preview.archiveIsFull && "Offline audio files and Media3 download index" !in preview.unavailableItems) {
                "Full archive: ${preview.counts.downloadedAudioFiles} offline files (${formatFileSize(preview.counts.downloadedAudioBytes)}) are staged and verified before completion."
            } else if (preview.archiveIsFull) {
                "Full archive: offline media is present but not restored during Merge, so existing downloads remain untouched."
            } else {
                "JSON library backup: no offline audio files are included."
            },
            style = MaterialTheme.typography.bodySmall,
            color = OmniColors.TextSecondary,
        )
        if (preview.warnings.isNotEmpty()) {
            Text("Warnings", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
            preview.warnings.forEach { warning ->
                Text("• $warning", style = MaterialTheme.typography.bodySmall, color = OmniColors.Warning)
            }
        }
        if (preview.unavailableItems.isNotEmpty()) {
            Text(
                "Not restored: ${preview.unavailableItems.joinToString()}",
                style = MaterialTheme.typography.bodySmall,
                color = OmniColors.TextTertiary,
            )
        }
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

private fun formatFileSize(bytes: Long): String = when {
    bytes < 1024 -> "$bytes B"
    bytes < 1024 * 1024 -> "${bytes / 1024} KB"
    bytes < 1024 * 1024 * 1024 -> "${"%.1f".format(bytes.toDouble() / (1024 * 1024))} MB"
    else -> "${"%.2f".format(bytes.toDouble() / (1024 * 1024 * 1024))} GB"
}
