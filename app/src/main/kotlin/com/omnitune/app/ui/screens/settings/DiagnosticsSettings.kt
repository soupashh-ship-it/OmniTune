package com.omnitune.app.ui.screens.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.omnitune.app.diagnostics.DiagnosticReportExporter
import com.omnitune.app.ui.theme.OmniColors

@Composable
fun DiagnosticsSettings() {
    val context = LocalContext.current
    var message by remember { mutableStateOf<String?>(null) }

    SettingsInfoBlock(
        title = "Export diagnostic report",
        body = "Useful for debugging playback, downloads, update checks, and device-specific behavior. This uses the existing exporter and does not collect new data.",
        accent = OmniColors.Hot,
    )
    SettingsActionButton("Export diagnostic report") {
        runCatching {
            context.startActivity(DiagnosticReportExporter.createShareIntent(context))
            message = "Share sheet opened for the diagnostic report."
        }.onFailure {
            message = "Could not export diagnostic report."
        }
    }
    message?.let { UpdateMessage(it) }
}


