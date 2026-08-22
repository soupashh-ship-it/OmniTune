package com.omnitune.app.ui.screens.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.omnitune.app.R
import com.omnitune.app.diagnostics.DiagnosticReportExporter
import com.omnitune.app.ui.theme.OmniColors
import com.omnitune.app.ui.theme.OmniShapes

@Composable
fun DiagnosticsSettings() {
    val context = LocalContext.current
    var message by remember { mutableStateOf<String?>(null) }

    OmniPreferenceCard(title = "Diagnostics") {
        OmniPreferenceEntry(
            title = "Export diagnostic report",
            description = "Includes app/device state and up to 200 redacted log lines. Account data, query text, URLs, and credentials are removed.",
            iconRes = R.drawable.ic_share,
            accent = OmniColors.Hot,
        )
        OmniPreferenceEntry(
            title = "Delete diagnostic data",
            description = "Remove local diagnostic reports and stored crash summaries from this device.",
            iconRes = R.drawable.ic_trash,
            accent = OmniColors.Warning,
            onClick = {
                DiagnosticReportExporter.clearStoredDiagnostics(context)
                message = "Diagnostic data deleted."
            },
        )
    }

    Spacer(Modifier.height(8.dp))

    Button(
        onClick = {
            runCatching {
                context.startActivity(DiagnosticReportExporter.createShareIntent(context))
                message = "Share sheet opened for the diagnostic report."
            }.onFailure {
                message = "Could not export diagnostic report."
            }
        },
        colors = ButtonDefaults.buttonColors(
            containerColor = OmniColors.OmniAccentPrimary,
            contentColor = OmniColors.OmniAccentOnPrimary,
        ),
        shape = OmniShapes.Pill,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .height(50.dp),
    ) {
        Text("Export diagnostic report", fontWeight = FontWeight.Bold)
    }

    message?.let {
        Text(
            text = it,
            style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
            color = OmniColors.TextSecondary,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
        )
    }
}
