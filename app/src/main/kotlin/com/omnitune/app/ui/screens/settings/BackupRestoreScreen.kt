package com.omnitune.app.ui.screens.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.omnitune.app.backup.OmniBackupCounts
import com.omnitune.app.backup.OmniBackupPreview
import com.omnitune.app.backup.OmniRestoreSelection
import com.omnitune.app.ui.theme.SquircleShape
import com.omnitune.app.viewmodels.BackupRestoreProgress
import com.omnitune.app.viewmodels.BackupRestoreResult
import com.omnitune.app.viewmodels.BackupRestoreViewModel
import java.text.DateFormat
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackupRestoreScreen(
    onBackClick: () -> Unit,
    viewModel: BackupRestoreViewModel = hiltViewModel(),
) {
    val progress by viewModel.progress.collectAsStateWithLifecycle()
    val result by viewModel.result.collectAsStateWithLifecycle()
    val lastBackupAt by viewModel.lastBackupAt.collectAsStateWithLifecycle()
    val safetyBackup by viewModel.latestSafetyBackup.collectAsStateWithLifecycle()
    val context = androidx.compose.ui.platform.LocalContext.current
    var includeDownloadedAudio by remember { mutableStateOf(false) }
    var replaceExisting by remember { mutableStateOf(false) }

    val jsonBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json"),
    ) { uri ->
        uri?.let { viewModel.backup(context, it, includeDownloadedAudio = false) }
    }
    val archiveBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/zip"),
    ) { uri ->
        uri?.let { viewModel.backup(context, it, includeDownloadedAudio = true) }
    }
    val restorePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri ->
        uri?.let {
            viewModel.previewRestore(
                context = context,
                uri = it,
                replaceExisting = replaceExisting,
                selection = OmniRestoreSelection.ALL,
            )
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Backup & Restore", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                Text(
                    text = "Your library",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = "Backups include songs, likes, playlists, history, listening stats, artwork metadata and tags. YouTube sign-in and device-specific settings are never exported.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }

            item {
                BackupActionCard(
                    title = "Create backup",
                    subtitle = lastBackupAt?.let { "Last backup ${formatBackupDate(it)}" }
                        ?: "Save a portable OmniTune library backup",
                    icon = Icons.Default.Backup,
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Include downloaded audio", style = MaterialTheme.typography.bodyLarge)
                            Text(
                                text = "Creates a larger ZIP backup with OmniTune-managed offline audio.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Switch(
                            checked = includeDownloadedAudio,
                            onCheckedChange = { includeDownloadedAudio = it },
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = {
                            if (includeDownloadedAudio) {
                                archiveBackupLauncher.launch(defaultBackupFileName("zip"))
                            } else {
                                jsonBackupLauncher.launch(defaultBackupFileName("json"))
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(Icons.Default.Backup, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Create backup")
                    }
                }
            }

            item {
                BackupActionCard(
                    title = "Restore backup",
                    subtitle = "Review contents before any library data changes",
                    icon = Icons.Default.Restore,
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (replaceExisting) "Replace current library" else "Merge with current library",
                                style = MaterialTheme.typography.bodyLarge,
                            )
                            Text(
                                text = if (replaceExisting) {
                                    "A verified safety backup is created before replacing library records."
                                } else {
                                    "Keeps existing data and imports missing library records."
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Switch(
                            checked = replaceExisting,
                            onCheckedChange = { replaceExisting = it },
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedButton(
                        onClick = {
                            restorePicker.launch(
                                arrayOf(
                                    "application/json",
                                    "application/zip",
                                    "application/octet-stream",
                                    "*/*",
                                ),
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(Icons.Default.FolderOpen, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Choose backup")
                    }
                }
            }

            safetyBackup?.let { backup ->
                item {
                    BackupActionCard(
                        title = "Replace safety backup",
                        subtitle = "Created ${formatBackupDate(backup.createdAtEpochMillis)}. Recover this if a replace restore needs to be undone.",
                        icon = Icons.Default.WarningAmber,
                    ) {
                        OutlinedButton(
                            onClick = viewModel::recoverLatestSafetyBackup,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Icon(Icons.Default.Restore, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Recover safety backup")
                        }
                    }
                }
            }
        }
    }

    progress?.let { BackupProgressDialog(it) }
    when (val currentResult = result) {
        is BackupRestoreResult.Preview -> RestorePreviewDialog(
            preview = currentResult.details,
            replaceExisting = currentResult.replaceExisting,
            onDismiss = viewModel::clearResult,
            onConfirm = {
                viewModel.restore(
                    context = context,
                    uri = currentResult.uri,
                    replaceExisting = currentResult.replaceExisting,
                    selection = currentResult.selection,
                )
            },
        )
        is BackupRestoreResult.Success -> BackupResultDialog(
            title = currentResult.title,
            message = currentResult.message,
            counts = currentResult.counts,
            onDismiss = viewModel::clearResult,
        )
        is BackupRestoreResult.Error -> BackupErrorDialog(
            message = currentResult.message,
            retryAvailable = currentResult.retryAvailable,
            onDismiss = viewModel::clearResult,
            onRetry = { viewModel.retryRestore(context) },
        )
        BackupRestoreResult.Idle -> Unit
    }
}

@Composable
private fun BackupActionCard(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    content: @Composable () -> Unit,
) {
    Card(
        shape = SquircleShape,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            content()
        }
    }
}

@Composable
private fun BackupProgressDialog(progress: BackupRestoreProgress) {
    AlertDialog(
        onDismissRequest = {},
        title = { Text(progress.title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(progress.step, color = MaterialTheme.colorScheme.onSurfaceVariant)
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {},
    )
}

@Composable
private fun RestorePreviewDialog(
    preview: OmniBackupPreview,
    replaceExisting: Boolean,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.Restore, contentDescription = null) },
        title = { Text(if (replaceExisting) "Replace library?" else "Merge backup?") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(backupCountsSummary(preview.counts))
                Text(
                    text = if (replaceExisting) {
                        "Your current library will be replaced after OmniTune creates a verified safety backup."
                    } else {
                        "Existing records stay in place and missing backup records are added."
                    },
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                preview.warnings.take(2).forEach { warning ->
                    Text(
                        text = warning,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(if (replaceExisting) "Replace" else "Merge")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun BackupResultDialog(
    title: String,
    message: String,
    counts: OmniBackupCounts,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.CheckCircle, contentDescription = null) },
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(message)
                Text(
                    text = backupCountsSummary(counts),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Done") } },
    )
}

@Composable
private fun BackupErrorDialog(
    message: String,
    retryAvailable: Boolean,
    onDismiss: () -> Unit,
    onRetry: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.WarningAmber, contentDescription = null) },
        title = { Text("Backup action failed") },
        text = { Text(message) },
        confirmButton = {
            if (retryAvailable) {
                TextButton(onClick = onRetry) { Text("Retry") }
            } else {
                TextButton(onClick = onDismiss) { Text("Close") }
            }
        },
        dismissButton = if (retryAvailable) {
            { TextButton(onClick = onDismiss) { Text("Close") } }
        } else {
            null
        },
    )
}

private fun defaultBackupFileName(extension: String): String =
    "OmniTune-backup-${java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(Date())}.$extension"

private fun formatBackupDate(epochMillis: Long): String =
    DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(epochMillis))

private fun backupCountsSummary(counts: OmniBackupCounts): String = listOf(
    "${counts.songs} songs",
    "${counts.playlists} playlists",
    "${counts.likedSongs} likes",
    "${counts.historyItems} history items",
).joinToString(" | ")
