package com.omnitune.app.ui.screens.settings

import android.widget.Toast
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import com.omnitune.app.R
import com.omnitune.app.constants.SmartTrimmerKey
import com.omnitune.app.ui.screens.SettingsViewModel
import com.omnitune.app.ui.theme.OmniColors

@Composable
fun StorageSettings(
    viewModel: SettingsViewModel = androidx.hilt.navigation.compose.hiltViewModel()
) {
    val context = LocalContext.current
    var showClearCacheDialog by remember { mutableStateOf(false) }

    if (showClearCacheDialog) {
        AlertDialog(
            onDismissRequest = { showClearCacheDialog = false },
            title = { Text("Clear cache?", fontWeight = FontWeight.Bold) },
            text = { Text("This clears stream cache, image cache, and temporary resolver cache. It does NOT delete completed downloads.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.clearAppCache(context)
                    Toast.makeText(context, "Cache cleared", Toast.LENGTH_SHORT).show()
                    showClearCacheDialog = false
                }) { Text("Clear") }
            },
            dismissButton = {
                TextButton(onClick = { showClearCacheDialog = false }) { Text("Cancel") }
            },
            containerColor = OmniColors.OmniBackgroundElevated,
            titleContentColor = OmniColors.TextPrimary,
            textContentColor = OmniColors.TextSecondary,
        )
    }

    SettingsCategoryLabel("Cache")
    TogglePreferenceRow(
        label = "Smart cache trimmer",
        description = "Automatically clear old cache",
        key = SmartTrimmerKey,
        defaultValue = true,
    )

    SettingsInfoBlock(
        title = "Downloads are managed by the offline library",
        body = "This section does not change completed-download playback or storage paths. Completed downloads remain playable from Downloads when the existing download state marks them ready.",
        accent = OmniColors.Downloaded,
    )
    SettingsInfoBlock(
        title = "Current cache limits",
        body = "Image cache: 128 MB max\nSong cache: 2 GB max",
        accent = OmniColors.OmniAccentMuted,
    )
    SettingsActionRow(
        iconRes = R.drawable.ic_settings,
        label = "Clear cache",
        description = "Free up space used by temporary files",
        accent = OmniColors.Warning,
        onClick = { showClearCacheDialog = true },
    )
}
