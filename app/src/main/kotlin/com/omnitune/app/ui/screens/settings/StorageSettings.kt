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
import com.omnitune.app.utils.rememberPreference

@Composable
fun StorageSettings(
    viewModel: SettingsViewModel = androidx.hilt.navigation.compose.hiltViewModel()
) {
    val context = LocalContext.current
    var showClearCacheDialog by remember { mutableStateOf(false) }
    var smartTrimmer by rememberPreference(SmartTrimmerKey, true)

    if (showClearCacheDialog) {
        AlertDialog(
            onDismissRequest = { showClearCacheDialog = false },
            containerColor = OmniColors.OmniBackgroundElevated,
            titleContentColor = OmniColors.TextPrimary,
            textContentColor = OmniColors.TextSecondary,
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
        )
    }

    OmniPreferenceCard(title = "Cache management") {
        OmniSwitchPreference(
            title = "Smart cache trimmer",
            description = "Automatically clear old cache",
            iconRes = R.drawable.ic_download,
            accent = OmniColors.OmniAccentSecondary,
            checked = smartTrimmer,
            onCheckedChange = { smartTrimmer = it },
        )
        OmniPreferenceEntry(
            title = "Clear cache",
            description = "Free up space used by temporary files",
            iconRes = R.drawable.ic_settings,
            accent = OmniColors.Warning,
            onClick = { showClearCacheDialog = true },
        )
    }

    OmniPreferenceCard(title = "Downloads") {
        OmniPreferenceEntry(
            title = "Downloads are managed by the offline library",
            description = "Completed downloads remain playable from Downloads when the download state marks them ready.",
            iconRes = R.drawable.ic_download,
            accent = OmniColors.Downloaded,
        )
    }

    OmniPreferenceCard(title = "Limits") {
        OmniPreferenceEntry(
            title = "Current cache limits",
            description = "Image cache: 128 MB max\nSong cache: 2 GB max",
            iconRes = R.drawable.ic_download,
            accent = OmniColors.OmniAccentMuted,
        )
    }
}
