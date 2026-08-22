/*
 * OmniTune - An open-source music player for Android
 * Licensed under GPL-3.0
 */

package com.omnitune.app.viewmodels

import android.content.Context
import android.net.Uri
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.omnitune.app.backup.OmniBackupCounts
import com.omnitune.app.backup.OmniBackupPreview
import com.omnitune.app.backup.OmniBackupRepository
import com.omnitune.app.backup.OmniRestoreSelection
import com.omnitune.app.backup.OmniRestoreMode
import com.omnitune.app.backup.RestoreSafetyBackup
import com.omnitune.app.constants.LastLibraryBackupAtKey
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class BackupRestoreProgress(
    val title: String,
    val step: String,
    val percent: Int,
    val indeterminate: Boolean = false,
)

sealed class BackupRestoreResult {
    data object Idle : BackupRestoreResult()
    data class Preview(
        val uri: Uri,
        val details: OmniBackupPreview,
        val replaceExisting: Boolean,
        val selection: OmniRestoreSelection,
    ) : BackupRestoreResult()
    data class Success(
        val title: String,
        val message: String,
        val counts: OmniBackupCounts,
    ) : BackupRestoreResult()
    data class Error(
        val message: String,
        val retryAvailable: Boolean = false,
    ) : BackupRestoreResult()
}

private data class RestoreRequest(
    val uri: Uri,
    val replaceExisting: Boolean,
    val selection: OmniRestoreSelection,
)

@HiltViewModel
class BackupRestoreViewModel @Inject constructor(
    private val backupRepository: OmniBackupRepository,
    private val dataStore: DataStore<Preferences>,
) : ViewModel() {

    private val _progress = MutableStateFlow<BackupRestoreProgress?>(null)
    val progress: StateFlow<BackupRestoreProgress?> = _progress.asStateFlow()

    private val _result = MutableStateFlow<BackupRestoreResult>(BackupRestoreResult.Idle)
    val result: StateFlow<BackupRestoreResult> = _result.asStateFlow()

    private val _latestSafetyBackup = MutableStateFlow(backupRepository.latestSafetyBackup())
    val latestSafetyBackup: StateFlow<RestoreSafetyBackup?> = _latestSafetyBackup.asStateFlow()

    private var latestRestoreRequest: RestoreRequest? = null

    val lastBackupAt: StateFlow<Long?> = dataStore.data
        .map { it[LastLibraryBackupAtKey] }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun backup(
        context: Context,
        uri: Uri,
        includeDownloadedAudio: Boolean,
    ) = viewModelScope.launch {
        _result.value = BackupRestoreResult.Idle
        _progress.value = BackupRestoreProgress(
            title = "Creating library backup",
            step = if (includeDownloadedAudio) {
                "Collecting library and app-managed offline audio"
            } else {
                "Collecting library, playlists, history, and stats"
            },
            percent = 0,
            indeterminate = true,
        )

        try {
            val outputStream = context.contentResolver.openOutputStream(uri)
                ?: throw IllegalStateException("Could not open backup destination")
            val exportResult = backupRepository.exportBackup(
                outputStream = outputStream,
                includeDownloadedAudio = includeDownloadedAudio,
            )

            dataStore.edit { prefs ->
                prefs[LastLibraryBackupAtKey] = exportResult.createdAtEpochMillis
            }

            _progress.value = null
            _result.value = BackupRestoreResult.Success(
                title = "Backup created",
                message = if (includeDownloadedAudio) {
                    "Saved ${formatFileSize(exportResult.byteCount)} full backup archive."
                } else {
                    "Saved ${formatFileSize(exportResult.byteCount)} JSON backup."
                },
                counts = exportResult.counts,
            )
        } catch (e: Exception) {
            _progress.value = null
            _result.value = BackupRestoreResult.Error(
                "Backup failed: ${e.message ?: "Unknown error"}",
            )
        }
    }

    fun previewRestore(
        context: Context,
        uri: Uri,
        replaceExisting: Boolean,
        selection: OmniRestoreSelection = OmniRestoreSelection.ALL,
    ) = viewModelScope.launch {
        _result.value = BackupRestoreResult.Idle
        _progress.value = BackupRestoreProgress(
            title = "Reading library backup",
            step = if (replaceExisting) {
                "Validating the backup before showing Replace preview"
            } else {
                "Validating the backup before showing Merge preview"
            },
            percent = 0,
            indeterminate = true,
        )

        try {
            val inputStream = context.contentResolver.openInputStream(uri)
                ?: throw IllegalStateException("Could not open backup file")
            val preview = backupRepository.previewBackup(
                inputStream = inputStream,
                mode = if (replaceExisting) OmniRestoreMode.REPLACE else OmniRestoreMode.MERGE,
                selection = selection,
            )
            _progress.value = null
            _result.value = BackupRestoreResult.Preview(
                uri = uri,
                details = preview,
                replaceExisting = replaceExisting,
                selection = selection,
            )
        } catch (e: Exception) {
            _progress.value = null
            _result.value = BackupRestoreResult.Error(
                "Backup preview failed: ${e.message ?: "Unknown error"}. No library data was changed.",
            )
        }
    }

    fun restore(
        context: Context,
        uri: Uri,
        replaceExisting: Boolean,
        selection: OmniRestoreSelection = OmniRestoreSelection.ALL,
    ) = viewModelScope.launch {
        latestRestoreRequest = RestoreRequest(uri, replaceExisting, selection)
        _result.value = BackupRestoreResult.Idle
        _progress.value = BackupRestoreProgress(
            title = "Restoring library backup",
            step = if (replaceExisting) {
                "Creating verified safety backup, then replacing library records"
            } else {
                "Restoring validated backup with merge conflict protection"
            },
            percent = 0,
            indeterminate = true,
        )

        try {
            val inputStream = context.contentResolver.openInputStream(uri)
                ?: throw IllegalStateException("Could not open backup file")
            val importResult = backupRepository.importBackup(
                inputStream = inputStream,
                mode = if (replaceExisting) OmniRestoreMode.REPLACE else OmniRestoreMode.MERGE,
                selection = selection,
            )
            _latestSafetyBackup.value = importResult.safetyBackup ?: backupRepository.latestSafetyBackup()
            _progress.value = null
            _result.value = BackupRestoreResult.Success(
                title = "Backup restored",
                message = buildString {
                    append(
                        if (replaceExisting) {
                            "Replaced library with OmniTune backup format v${importResult.formatVersion}."
                        } else {
                            "Merged OmniTune backup format v${importResult.formatVersion}."
                        },
                    )
                    if (importResult.offlineAudioRestorePending) {
                        append(" Offline audio will be applied after app restart.")
                    }
                    importResult.safetyBackup?.let { safety ->
                        append(" Verified safety backup retained at ${safety.locationDescription}.")
                    }
                },
                counts = importResult.counts,
            )
        } catch (e: Exception) {
            _progress.value = null
            _result.value = BackupRestoreResult.Error(
                "Restore failed: ${e.message ?: "Unknown error"}. No success was recorded; retry from the backup screen when ready.",
                retryAvailable = true,
            )
        }
    }

    fun retryRestore(context: Context) {
        latestRestoreRequest?.let { request ->
            restore(context, request.uri, request.replaceExisting, request.selection)
        }
    }

    fun recoverLatestSafetyBackup() = viewModelScope.launch {
        _result.value = BackupRestoreResult.Idle
        _progress.value = BackupRestoreProgress(
            title = "Recovering safety backup",
            step = "Creating a safety backup of the current library before recovery",
            percent = 0,
            indeterminate = true,
        )
        try {
            val importResult = backupRepository.recoverLatestSafetyBackup()
            _latestSafetyBackup.value = importResult.safetyBackup ?: backupRepository.latestSafetyBackup()
            _progress.value = null
            _result.value = BackupRestoreResult.Success(
                title = "Safety backup recovered",
                message = "Recovered the retained Replace safety archive.",
                counts = importResult.counts,
            )
        } catch (e: Exception) {
            _progress.value = null
            _result.value = BackupRestoreResult.Error(
                "Safety recovery failed: ${e.message ?: "Unknown error"}. The retained archive was not deleted.",
            )
        }
    }

    fun clearResult() {
        _result.value = BackupRestoreResult.Idle
    }

    private fun formatFileSize(bytes: Long): String = when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> "${bytes / 1024} KB"
        bytes < 1024 * 1024 * 1024 -> "${"%.1f".format(bytes.toDouble() / (1024 * 1024))} MB"
        else -> "${"%.2f".format(bytes.toDouble() / (1024 * 1024 * 1024))} GB"
    }
}
