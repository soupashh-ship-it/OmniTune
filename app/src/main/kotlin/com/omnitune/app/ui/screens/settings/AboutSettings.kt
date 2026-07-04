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
import com.omnitune.app.BuildConfig
import com.omnitune.app.R
import com.omnitune.app.ui.theme.OmniColors
import com.omnitune.app.ui.theme.OmniShapes

@Composable
fun AboutSettings() {
    val context = LocalContext.current
    var message by remember { mutableStateOf<String?>(null) }

    OmniPreferenceCard(title = "About") {
        OmniPreferenceEntry(
            title = "OmniTune",
            description = "Version ${BuildConfig.VERSION_NAME} (code ${BuildConfig.VERSION_CODE})\nOpen-source music player for Android\nLicense: GPL-3.0",
            iconRes = R.drawable.ic_info,
            accent = OmniColors.TextSecondary,
        )
    }

    OmniPreferenceCard(title = "Legal") {
        OmniPreferenceEntry(
            title = "License and credits",
            description = "See LICENSE and CREDITS.md in the project repository.",
            iconRes = R.drawable.ic_info,
            accent = OmniColors.Warning,
        )
    }

    Spacer(Modifier.height(8.dp))

    Button(
        onClick = {
            openUrl(context, "https://github.com/soupashh-ship-it/OmniTune") {
                message = "Could not open project repository."
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
        Text("Open project repository", fontWeight = FontWeight.Bold)
    }

    Spacer(Modifier.height(8.dp))

    Button(
        onClick = {
            openUrl(context, "https://github.com/soupashh-ship-it/OmniTune/blob/main/LICENSE") {
                message = "Could not open license link."
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
        Text("Open GPL license", fontWeight = FontWeight.Bold)
    }

    Spacer(Modifier.height(8.dp))

    Button(
        onClick = {
            openUrl(context, "https://github.com/soupashh-ship-it/OmniTune/blob/main/CREDITS.md") {
                message = "Could not open credits link."
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
        Text("Open credits", fontWeight = FontWeight.Bold)
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
