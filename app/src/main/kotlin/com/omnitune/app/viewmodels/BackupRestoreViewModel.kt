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
import com.omnitune.app.backup.OmniBackupRepository
import com.omnitune.app.backup.OmniRestoreMode
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
    data class Success(
        val title: String,
        val message: String,
        val counts: OmniBackupCounts,
    ) : BackupRestoreResult()
    data class Error(val message: String) : BackupRestoreResult()
}

@HiltViewModel
class BackupRestoreViewModel @Inject constructor(
    private val backupRepository: OmniBackupRepository,
    private val dataStore: DataStore<Preferences>,
) : ViewModel() {

    private val _progress = MutableStateFlow<BackupRestoreProgress?>(null)
    val progress: StateFlow<BackupRestoreProgress?> = _progress.asStateFlow()

    private val _result = MutableStateFlow<BackupRestoreResult>(BackupRestoreResult.Idle)
    val result: StateFlow<BackupRestoreResult> = _result.asStateFlow()

    val lastBackupAt: StateFlow<Long?> = dataStore.data
        .map { it[LastLibraryBackupAtKey] }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun backup(context: Context, uri: Uri) = viewModelScope.launch {
        _result.value = BackupRestoreResult.Idle
        _progress.value = BackupRestoreProgress(
            title = "Creating library backup",
            step = "Collecting library, playlists, history, and stats",
            percent = 0,
            indeterminate = true,
        )

        try {
            val outputStream = context.contentResolver.openOutputStream(uri)
                ?: throw IllegalStateException("Could not open backup destination")
            val exportResult = backupRepository.exportBackup(outputStream)

            dataStore.edit { prefs ->
                prefs[LastLibraryBackupAtKey] = exportResult.createdAtEpochMillis
            }

            _progress.value = null
            _result.value = BackupRestoreResult.Success(
                title = "Backup created",
                message = "Saved ${formatFileSize(exportResult.byteCount)} JSON backup.",
                counts = exportResult.counts,
            )
        } catch (e: Exception) {
            _progress.value = null
            _result.value = BackupRestoreResult.Error(
                "Backup failed: ${e.message ?: "Unknown error"}",
            )
        }
    }

    fun restore(context: Context, uri: Uri) = viewModelScope.launch {
        _result.value = BackupRestoreResult.Idle
        _progress.value = BackupRestoreProgress(
            title = "Importing library backup",
            step = "Validating JSON and merging records",
            percent = 0,
            indeterminate = true,
        )

        try {
            val inputStream = context.contentResolver.openInputStream(uri)
                ?: throw IllegalStateException("Could not open backup file")
            val importResult = backupRepository.importBackup(inputStream, OmniRestoreMode.MERGE)

            _progress.value = null
            _result.value = BackupRestoreResult.Success(
                title = "Backup imported",
                message = "Merged OmniTune backup format v${importResult.formatVersion}.",
                counts = importResult.counts,
            )
        } catch (e: Exception) {
            _progress.value = null
            _result.value = BackupRestoreResult.Error(
                "Import failed: ${e.message ?: "Unknown error"}. Existing data was preserved.",
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
