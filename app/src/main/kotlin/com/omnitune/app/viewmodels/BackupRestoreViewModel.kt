/*
 * OmniTune - An open-source music player for Android
 * Licensed under GPL-3.0
 */

package com.omnitune.app.viewmodels

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import javax.inject.Inject

data class BackupRestoreProgress(
    val title: String,
    val step: String,
    val percent: Int,
    val indeterminate: Boolean = false,
)

sealed class BackupRestoreResult {
    data object Idle : BackupRestoreResult()
    data class Success(val message: String) : BackupRestoreResult()
    data class Error(val message: String) : BackupRestoreResult()
}

enum class BackupRestoreScope {
    ALL,
    SETTINGS_ONLY,
    DATABASE_ONLY,
}

@HiltViewModel
class BackupRestoreViewModel @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) : ViewModel() {

    private val _progress = MutableStateFlow<BackupRestoreProgress?>(null)
    val progress: StateFlow<BackupRestoreProgress?> = _progress.asStateFlow()

    private val _result = MutableStateFlow<BackupRestoreResult>(BackupRestoreResult.Idle)
    val result: StateFlow<BackupRestoreResult> = _result.asStateFlow()

    private var rollbackFile: File? = null

    fun backup(
        context: Context,
        uri: Uri,
        scope: BackupRestoreScope = BackupRestoreScope.ALL,
    ) = viewModelScope.launch {
        _result.value = BackupRestoreResult.Idle
        _progress.value = BackupRestoreProgress(
            title = "Creating backup...",
            step = "Preparing files",
            percent = 0,
        )

        try {
            val contentResolver = context.contentResolver
            val outputStream = contentResolver.openOutputStream(uri)
                ?: throw IllegalStateException("Could not open output file")

            withContext(Dispatchers.IO) {
                ZipOutputStream(BufferedOutputStream(outputStream)).use { zos ->
                    if (scope == BackupRestoreScope.ALL || scope == BackupRestoreScope.SETTINGS_ONLY) {
                        _progress.value = BackupRestoreProgress(
                            title = "Creating backup...",
                            step = "Exporting settings",
                            percent = 10,
                        )
                        val settingsXml = exportSettingsToXml()
                        zos.putNextEntry(ZipEntry("settings.xml"))
                        zos.write(settingsXml.toByteArray(Charsets.UTF_8))
                        zos.closeEntry()
                    }

                    if (scope == BackupRestoreScope.ALL || scope == BackupRestoreScope.DATABASE_ONLY) {
                        val dbDir = context.getDatabasePath("song.db").parentFile
                            ?: throw IllegalStateException("Cannot locate database directory")

                        val dbFiles = listOf("song.db", "song.db-wal", "song.db-shm")
                            .map { File(dbDir, it) }
                            .filter { it.exists() }

                        dbFiles.forEachIndexed { index, file ->
                            val percentBase = if (scope == BackupRestoreScope.ALL) 20 else 10
                            val percentRange = if (scope == BackupRestoreScope.ALL) 70 else 80
                            val pct = percentBase + (index * percentRange / dbFiles.size)
                            _progress.value = BackupRestoreProgress(
                                title = "Creating backup...",
                                step = "Archiving ${file.name}",
                                percent = pct.coerceIn(10, 90),
                            )
                            zos.putNextEntry(ZipEntry(file.name))
                            file.inputStream().use { input ->
                                input.copyTo(zos, bufferSize = 8192)
                            }
                            zos.closeEntry()
                        }
                    }

                    zos.putNextEntry(ZipEntry(".backup_meta"))
                    val meta = buildString {
                        appendLine("version=1")
                        appendLine("app=OmniTune")
                        appendLine("created=${LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)}")
                        appendLine("scope=${scope.name}")
                        appendLine("has_settings=${scope == BackupRestoreScope.ALL || scope == BackupRestoreScope.SETTINGS_ONLY}")
                        appendLine("has_database=${scope == BackupRestoreScope.ALL || scope == BackupRestoreScope.DATABASE_ONLY}")
                    }
                    zos.write(meta.toByteArray(Charsets.UTF_8))
                    zos.closeEntry()
                }

                val fileSize = contentResolver.openInputStream(uri)?.use { stream ->
                    val buffer = ByteArray(8192)
                    var total = 0L
                    var read: Int
                    while (stream.read(buffer).also { read = it } != -1) {
                        total += read
                    }
                    total
                } ?: 0L

                _progress.value = null
                _result.value = BackupRestoreResult.Success(
                    "Backup complete (${formatFileSize(fileSize)})"
                )
            }
        } catch (e: Exception) {
            _progress.value = null
            _result.value = BackupRestoreResult.Error(
                "Backup failed: ${e.message ?: "Unknown error"}"
            )
        }
    }

    fun restore(
        context: Context,
        uri: Uri,
        onRestartRequired: () -> Unit,
    ) = viewModelScope.launch {
        _result.value = BackupRestoreResult.Idle
        _progress.value = BackupRestoreProgress(
            title = "Validating backup...",
            step = "Checking file integrity",
            percent = 0,
            indeterminate = true,
        )

        try {
            val contentResolver = context.contentResolver

            withContext(Dispatchers.IO) {
                var hasSettings = false
                var hasDatabase = false
                var validated = false
                val tempDir = File(context.cacheDir, "restore_temp_${System.currentTimeMillis()}")
                tempDir.mkdirs()

                try {
                    contentResolver.openInputStream(uri)?.use { input ->
                        ZipInputStream(BufferedInputStream(input)).use { zis ->
                            var entry: ZipEntry?
                            while (zis.nextEntry.also { entry = it } != null) {
                                val e = entry!!
                                when (e.name) {
                                    ".backup_meta" -> {
                                        val meta = zis.readBytes().toString(Charsets.UTF_8)
                                        hasSettings = meta.contains("has_settings=true")
                                        hasDatabase = meta.contains("has_database=true")
                                        validated = true
                                    }
                                    "settings.xml" -> {
                                        val target = File(tempDir, "settings.xml")
                                        target.outputStream().use { zis.copyTo(it) }
                                        hasSettings = true
                                        validated = true
                                    }
                                    "song.db" -> {
                                        val target = File(tempDir, "song.db")
                                        target.outputStream().use { zis.copyTo(it) }
                                        hasDatabase = true
                                        validated = true
                                    }
                                    "song.db-wal" -> {
                                        val target = File(tempDir, "song.db-wal")
                                        target.outputStream().use { zis.copyTo(it) }
                                    }
                                    "song.db-shm" -> {
                                        val target = File(tempDir, "song.db-shm")
                                        target.outputStream().use { zis.copyTo(it) }
                                    }
                                }
                                zis.closeEntry()
                            }
                        }
                    }

                    if (!validated) {
                        throw IllegalStateException("Invalid or corrupted backup file")
                    }

                    _progress.value = BackupRestoreProgress(
                        title = "Restoring...",
                        step = "Applying data",
                        percent = 70,
                    )

                    val restoredSettings = File(tempDir, "settings.xml")
                    if (hasSettings && restoredSettings.exists()) {
                        val xml = restoredSettings.readText(Charsets.UTF_8)
                        importSettingsFromXml(xml)
                    }

                    _progress.value = BackupRestoreProgress(
                        title = "Restoring...",
                        step = "Applying database",
                        percent = 85,
                    )

                    if (hasDatabase) {
                        val dbDir = context.getDatabasePath("song.db").parentFile ?: error("No db dir")
                        val currentDb = File(dbDir, "song.db")
                        if (currentDb.exists()) {
                            rollbackFile = File(context.cacheDir, "restore_rollback_${System.currentTimeMillis()}.db")
                            currentDb.copyTo(rollbackFile!!, overwrite = true)
                        }

                        listOf("song.db", "song.db-wal", "song.db-shm").forEach { name ->
                            val restored = File(tempDir, name)
                            val target = File(dbDir, name)
                            if (restored.exists()) {
                                target.delete()
                                restored.copyTo(target, overwrite = true)
                            }
                        }
                    }

                    tempDir.deleteRecursively()

                    _progress.value = null
                    _result.value = BackupRestoreResult.Success("Restore complete — restarting to apply changes")
                    onRestartRequired()

                } catch (e: Exception) {
                    rollbackFile?.let { rollback ->
                        try {
                            val dbDir = context.getDatabasePath("song.db").parentFile ?: return@let
                            val target = File(dbDir, "song.db")
                            target.delete()
                            rollback.copyTo(target, overwrite = true)
                            rollback.delete()
                            rollbackFile = null
                        } catch (_: Exception) { }
                    }
                    tempDir.deleteRecursively()
                    throw e
                }
            }
        } catch (e: Exception) {
            _progress.value = null
            _result.value = BackupRestoreResult.Error(
                "Restore failed: ${e.message ?: "Unknown error"}. Your data has been preserved."
            )
        }
    }

    fun restartApp(context: Context) {
        val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)
            ?.apply { addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK) }
        if (intent != null) {
            context.startActivity(intent)
        }
        Runtime.getRuntime().exit(0)
    }

    fun clearResult() {
        _result.value = BackupRestoreResult.Idle
    }

    private suspend fun exportSettingsToXml(): String {
        val prefs = dataStore.data.first()
        val sb = StringBuilder()
        sb.appendLine("<?xml version=\"1.0\" encoding=\"UTF-8\"?>")
        sb.appendLine("<omnitune_preferences>")
        prefs.asMap().forEach { (key, value) ->
            sb.appendLine("  <preference name=\"${escapeXml(key.name)}\" type=\"${value::class.qualifiedName}\">")
            sb.append("    <![CDATA[")
            sb.append(value.toString())
            sb.appendLine("]]>")
            sb.appendLine("  </preference>")
        }
        sb.appendLine("</omnitune_preferences>")
        return sb.toString()
    }

    private suspend fun importSettingsFromXml(xml: String) {
        val regex = Regex(
            "<preference name=\"([^\"]+)\" type=\"[^\"]+\">\\s*<!\\[CDATA\\[(.*?)\\]\\]>\\s*</preference>",
            RegexOption.DOT_MATCHES_ALL,
        )
        val entries = regex.findAll(xml).map { match ->
            match.groupValues[1] to match.groupValues[2]
        }.toList()

        dataStore.edit { current ->
            entries.forEach { (name, rawValue) ->
                val existingKey = current.asMap().keys.find { it.name == name } ?: return@forEach
                val valueType = current.asMap()[existingKey]?.let { it::class.java } ?: return@forEach
                val converted = convertRawValue(rawValue, valueType)
                if (converted != null) {
                    @Suppress("UNCHECKED_CAST")
                    current.set(existingKey as Preferences.Key<Any>, converted)
                }
            }
        }
    }

    private fun convertRawValue(raw: String, clazz: Class<*>): Any? {
        return when (clazz) {
            String::class.java -> raw
            Boolean::class.javaPrimitiveType, Boolean::class.java -> raw.toBooleanStrictOrNull()
            Int::class.javaPrimitiveType, Int::class.java -> raw.toIntOrNull()
            Long::class.javaPrimitiveType, Long::class.java -> raw.toLongOrNull()
            Float::class.javaPrimitiveType, Float::class.java -> raw.toFloatOrNull()
            Double::class.javaPrimitiveType, Double::class.java -> raw.toDoubleOrNull()
            else -> null
        }
    }

    private fun escapeXml(s: String): String =
        s.replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;")

    private fun formatFileSize(bytes: Long): String = when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> "${bytes / 1024} KB"
        bytes < 1024 * 1024 * 1024 -> "${"%.1f".format(bytes.toDouble() / (1024 * 1024))} MB"
        else -> "${"%.2f".format(bytes.toDouble() / (1024 * 1024 * 1024))} GB"
    }
}
