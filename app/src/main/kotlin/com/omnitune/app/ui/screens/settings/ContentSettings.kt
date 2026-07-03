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
import com.omnitune.app.ui.screens.SettingsViewModel
import com.omnitune.app.ui.theme.OmniColors

@Composable
fun ContentSettings(
    viewModel: SettingsViewModel = androidx.hilt.navigation.compose.hiltViewModel()
) {
    val context = LocalContext.current
    var showClearSearchHistoryDialog by remember { mutableStateOf(false) }
    var showClearListenHistoryDialog by remember { mutableStateOf(false) }

    if (showClearSearchHistoryDialog) {
        AlertDialog(
            onDismissRequest = { showClearSearchHistoryDialog = false },
            title = { Text("Clear search history?", fontWeight = FontWeight.Bold) },
            text = { Text("This cannot be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.clearSearchHistory()
                    Toast.makeText(context, "Search history cleared", Toast.LENGTH_SHORT).show()
                    showClearSearchHistoryDialog = false
                }) { Text("Clear") }
            },
            dismissButton = {
                TextButton(onClick = { showClearSearchHistoryDialog = false }) { Text("Cancel") }
            },
            containerColor = OmniColors.OmniBackgroundElevated,
            titleContentColor = OmniColors.TextPrimary,
            textContentColor = OmniColors.TextSecondary,
        )
    }

    if (showClearListenHistoryDialog) {
        AlertDialog(
            onDismissRequest = { showClearListenHistoryDialog = false },
            title = { Text("Clear listen history?", fontWeight = FontWeight.Bold) },
            text = { Text("This cannot be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.clearListenHistory()
                    Toast.makeText(context, "Listen history cleared", Toast.LENGTH_SHORT).show()
                    showClearListenHistoryDialog = false
                }) { Text("Clear") }
            },
            dismissButton = {
                TextButton(onClick = { showClearListenHistoryDialog = false }) { Text("Cancel") }
            },
            containerColor = OmniColors.OmniBackgroundElevated,
            titleContentColor = OmniColors.TextPrimary,
            textContentColor = OmniColors.TextSecondary,
        )
    }
    SettingsCategoryLabel("History")
    SettingsActionRow(
        iconRes = R.drawable.ic_history,
        label = "Clear search history",
        description = "Remove all past searches",
        accent = OmniColors.Warning,
        onClick = { showClearSearchHistoryDialog = true },
    )
    SettingsActionRow(
        iconRes = R.drawable.ic_history,
        label = "Clear listen history",
        description = "Remove all past listens",
        accent = OmniColors.Warning,
        onClick = { showClearListenHistoryDialog = true },
    )
}
