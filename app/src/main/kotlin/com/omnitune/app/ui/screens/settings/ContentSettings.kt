package com.omnitune.app.ui.screens.settings

import android.widget.Toast
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.omnitune.app.R
import com.omnitune.app.constants.QuickPicks
import com.omnitune.app.constants.QuickPicksKey
import com.omnitune.app.ui.screens.SettingsViewModel
import com.omnitune.app.ui.theme.OmniColors
import com.omnitune.app.utils.dataStore
import kotlinx.coroutines.flow.map

@Composable
fun ContentSettings(
    viewModel: SettingsViewModel = hiltViewModel(),
    onNavigateToPoToken: () -> Unit = {},
) {
    val context = LocalContext.current
    var showClearSearchHistoryDialog by remember { mutableStateOf(false) }
    var showClearListenHistoryDialog by remember { mutableStateOf(false) }
    var showQuickPicksDialog by remember { mutableStateOf(false) }

    val quickPicksMode by remember {
        context.dataStore.data.map { prefs ->
            val value = prefs[QuickPicksKey] ?: QuickPicks.QUICK_PICKS.name
            try { QuickPicks.valueOf(value) } catch (_: Exception) { QuickPicks.QUICK_PICKS }
        }
    }.collectAsState(QuickPicks.QUICK_PICKS)

    if (showClearSearchHistoryDialog) {
        AlertDialog(
            onDismissRequest = { showClearSearchHistoryDialog = false },
            containerColor = OmniColors.OmniBackgroundElevated,
            titleContentColor = OmniColors.TextPrimary,
            textContentColor = OmniColors.TextSecondary,
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
        )
    }

    if (showClearListenHistoryDialog) {
        AlertDialog(
            onDismissRequest = { showClearListenHistoryDialog = false },
            containerColor = OmniColors.OmniBackgroundElevated,
            titleContentColor = OmniColors.TextPrimary,
            textContentColor = OmniColors.TextSecondary,
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
        )
    }

    if (showQuickPicksDialog) {
        AlertDialog(
            onDismissRequest = { showQuickPicksDialog = false },
            containerColor = OmniColors.OmniBackgroundElevated,
            titleContentColor = OmniColors.TextPrimary,
            textContentColor = OmniColors.TextSecondary,
            title = { Text("Quick Picks mode", fontWeight = FontWeight.Bold) },
            text = { Text("Choose how Quick Picks are generated on the Home screen.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.updatePreference(context, QuickPicksKey, QuickPicks.QUICK_PICKS.name)
                    showQuickPicksDialog = false
                }) { Text("Related to your listening") }
            },
            dismissButton = {
                TextButton(onClick = {
                    viewModel.updatePreference(context, QuickPicksKey, QuickPicks.LAST_LISTEN.name)
                    showQuickPicksDialog = false
                }) { Text("Related to last listen") }
            },
        )
    }

    OmniPreferenceCard(title = "Discovery") {
        OmniPreferenceEntry(
            title = "Quick Picks mode",
            description = when (quickPicksMode) {
                QuickPicks.QUICK_PICKS -> "Related songs from your listening"
                QuickPicks.LAST_LISTEN -> "Songs related to your last listen"
            },
            iconRes = R.drawable.ic_insights,
            accent = OmniColors.OmniAccentSecondary,
            onClick = { showQuickPicksDialog = true },
        )
    }

    OmniPreferenceCard(title = "History") {
        OmniPreferenceEntry(
            title = "Clear search history",
            description = "Remove all past searches",
            iconRes = R.drawable.ic_history,
            accent = OmniColors.Warning,
            onClick = { showClearSearchHistoryDialog = true },
        )
        OmniPreferenceEntry(
            title = "Clear listen history",
            description = "Remove all past listens",
            iconRes = R.drawable.ic_history,
            accent = OmniColors.Warning,
            onClick = { showClearListenHistoryDialog = true },
        )
    }

    OmniPreferenceCard(title = "API & Tokens") {
        OmniPreferenceEntry(
            title = "YouTube PO Token",
            description = "Bypass YouTube bot protections",
            iconRes = R.drawable.ic_settings,
            accent = OmniColors.OmniAccentSecondary,
            onClick = onNavigateToPoToken,
        )
    }
}
