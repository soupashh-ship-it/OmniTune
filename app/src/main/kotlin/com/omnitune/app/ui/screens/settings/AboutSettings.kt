package com.omnitune.app.ui.screens.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.omnitune.app.BuildConfig
import com.omnitune.app.ui.theme.OmniColors

@Composable
fun AboutSettings() {
    val context = LocalContext.current
    var message by remember { mutableStateOf<String?>(null) }

    SettingsInfoBlock(
        title = "OmniTune",
        body = "Version ${BuildConfig.VERSION_NAME} (code ${BuildConfig.VERSION_CODE})\nOpen-source music player for Android\nLicense: GPL-3.0",
        accent = OmniColors.TextSecondary,
    )
    SettingsInfoBlock(
        title = "Legal access",
        body = "License and credits remain in the project repository as LICENSE and CREDITS.md.",
        accent = OmniColors.Warning,
    )
    SettingsActionButton("Open project repository") {
        openUrl(context, "https://github.com/soupashh-ship-it/OmniTune") {
            message = "Could not open project repository."
        }
    }
    SettingsActionButton("Open GPL license") {
        openUrl(context, "https://github.com/soupashh-ship-it/OmniTune/blob/main/LICENSE") {
            message = "Could not open license link."
        }
    }
    SettingsActionButton("Open credits") {
        openUrl(context, "https://github.com/soupashh-ship-it/OmniTune/blob/main/CREDITS.md") {
            message = "Could not open credits link."
        }
    }
    message?.let { UpdateMessage(it) }
}
