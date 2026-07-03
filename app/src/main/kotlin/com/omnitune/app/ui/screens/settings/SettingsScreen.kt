package com.omnitune.app.ui.screens.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.omnitune.app.ui.component.OmniChrome
import com.omnitune.app.ui.theme.OmniColors
import com.omnitune.app.ui.theme.OmniShapes
import com.omnitune.app.ui.theme.OmniSpacing

@Composable
fun SettingsScreen(
    onBack: () -> Unit = {},
    onNavigateToEqualizer: () -> Unit = {},
) {
    var expandedSection by remember { mutableStateOf<SettingsSection?>(null) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(OmniColors.OmniBackgroundBase)
            .background(OmniColors.BackgroundGradient)
            .padding(horizontal = OmniSpacing.section),
        verticalArrangement = Arrangement.spacedBy(OmniSpacing.compact),
    ) {
        item {
            SettingsHeader(onBack = onBack)
        }

        item {
            SettingsCategoryLabel("Quick actions")
            SettingsQuickSummary(
                onUpdates = {
                    expandedSection = if (expandedSection == SettingsSection.UPDATES) {
                        null
                    } else {
                        SettingsSection.UPDATES
                    }
                },
                onDiagnostics = {
                    expandedSection = if (expandedSection == SettingsSection.DIAGNOSTICS) {
                        null
                    } else {
                        SettingsSection.DIAGNOSTICS
                    }
                },
            )
        }

        if (expandedSection == SettingsSection.UPDATES || expandedSection == SettingsSection.DIAGNOSTICS) {
            item(key = "quick-action-detail-${expandedSection?.name}") {
                QuickActionSectionDetail(section = expandedSection!!)
            }
        }

        item {
            SettingsCategoryLabel("Categories")
        }

        SettingsSection.entries
            .filterNot {
                it == SettingsSection.UPDATES ||
                    it == SettingsSection.DIAGNOSTICS ||
                    it == SettingsSection.SCROBBLING
            }
            .forEach { section ->
            item {
                SettingsSectionCard(
                    section = section,
                    isExpanded = expandedSection == section,
                    onToggle = {
                        expandedSection = if (expandedSection == section) null else section
                    },
                    onNavigateToEqualizer = onNavigateToEqualizer,
                )
            }
        }

        item { Spacer(modifier = Modifier.height(OmniChrome.BottomContentPaddingWithPlayer)) }
    }
}

@Composable
private fun QuickActionSectionDetail(section: SettingsSection) {
    Column(
        modifier = Modifier
            .padding(top = OmniSpacing.micro, bottom = OmniSpacing.small)
            .clip(OmniShapes.Medium)
            .background(OmniColors.SurfaceQuiet)
            .border(BorderStroke(1.dp, OmniColors.SurfaceHairline), OmniShapes.Medium)
            .padding(horizontal = OmniSpacing.compact, vertical = OmniSpacing.small),
        verticalArrangement = Arrangement.spacedBy(OmniSpacing.compact),
    ) {
        when (section) {
            SettingsSection.UPDATES -> UpdatesSettings()
            SettingsSection.DIAGNOSTICS -> DiagnosticsSettings()
            else -> Unit
        }
        Spacer(modifier = Modifier.height(OmniSpacing.compact))
    }
}



enum class SettingsSection(
    val label: String,
    val description: String,
    val iconRes: Int,
    val accent: androidx.compose.ui.graphics.Color,
) {
    APPEARANCE(
        "Appearance",
        "Theme and layout preferences",
        com.omnitune.app.R.drawable.ic_repeat,
        com.omnitune.app.ui.theme.OmniColors.OmniAccentPrimary,
    ),
    PLAYBACK(
        "Playback",
        "Quality, crossfade, and equalizer",
        com.omnitune.app.R.drawable.ic_play_arrow,
        com.omnitune.app.ui.theme.OmniColors.ActivePlayback,
    ),
    STORAGE(
        "Downloads & cache",
        "Storage and offline guidance",
        com.omnitune.app.R.drawable.ic_download,
        com.omnitune.app.ui.theme.OmniColors.Downloaded,
    ),
    NOTIFICATIONS(
        "Notifications & lock screen",
        "Media controls and lock screen",
        com.omnitune.app.R.drawable.ic_notification_play,
        com.omnitune.app.ui.theme.OmniColors.Warning,
    ),
    UPDATES(
        "Updates",
        "Check GitHub releases without changing update logic",
        com.omnitune.app.R.drawable.ic_download,
        com.omnitune.app.ui.theme.OmniColors.OmniAccentSecondary,
    ),
    DIAGNOSTICS(
        "Diagnostics",
        "Export the existing diagnostic report",
        com.omnitune.app.R.drawable.ic_share,
        com.omnitune.app.ui.theme.OmniColors.Hot,
    ),
    CONTENT(
        "Content & history",
        "Filters and history controls",
        com.omnitune.app.R.drawable.ic_search,
        com.omnitune.app.ui.theme.OmniColors.OmniAccentMuted,
    ),
    LYRICS(
        "Lyrics providers",
        "Sources and lyric animation",
        com.omnitune.app.R.drawable.ic_list,
        com.omnitune.app.ui.theme.OmniColors.Hot,
    ),
    SCROBBLING(
        "Scrobbling",
        "Last.fm and ListenBrainz preferences",
        com.omnitune.app.R.drawable.ic_favorite,
        com.omnitune.app.ui.theme.OmniColors.HotLight,
    ),
    ABOUT(
        "About",
        "Version, source, license, and credits",
        com.omnitune.app.R.drawable.ic_info,
        com.omnitune.app.ui.theme.OmniColors.TextSecondary,
    ),
}
