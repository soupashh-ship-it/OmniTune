package com.omnitune.app.ui.screens.settings

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import android.widget.Toast
import androidx.compose.material3.TextButton
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationManagerCompat
import androidx.datastore.preferences.core.Preferences
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.omnitune.app.BuildConfig
import com.omnitune.app.R
import com.omnitune.app.constants.AudioCrossfadeDurationKey
import com.omnitune.app.constants.AudioQuality
import com.omnitune.app.constants.AudioQualityKey
import com.omnitune.app.constants.PlaybackQualityModeKey
import com.omnitune.app.models.PlaybackQualityMode
import com.omnitune.app.constants.AutoSkipNextOnErrorKey
import com.omnitune.app.constants.DisableBlurKey
import com.omnitune.app.constants.EnableBetterLyricsKey
import com.omnitune.app.constants.EnableKugouKey
import com.omnitune.app.constants.EnableLastFMScrobblingKey
import com.omnitune.app.constants.EnableLrcLibKey
import com.omnitune.app.constants.EnableSimpMusicLyricsKey
import com.omnitune.app.constants.GridItemSize
import com.omnitune.app.constants.GridItemsSizeKey
import com.omnitune.app.constants.HideExplicitKey
import com.omnitune.app.constants.HideVideoKey
import com.omnitune.app.constants.LastFMUseNowPlaying
import com.omnitune.app.constants.ListenBrainzEnabledKey
import com.omnitune.app.constants.LyricsAnimationStyle
import com.omnitune.app.constants.LyricsAnimationStyleKey
import com.omnitune.app.constants.PauseListenHistoryKey
import com.omnitune.app.constants.PauseOnDeviceMuteKey
import com.omnitune.app.constants.PauseSearchHistoryKey
import com.omnitune.app.constants.PureBlackKey
import com.omnitune.app.constants.SkipSilenceKey
import com.omnitune.app.constants.SmartTrimmerKey
import com.omnitune.app.diagnostics.DiagnosticReportExporter
import com.omnitune.app.playback.MusicService
import com.omnitune.app.ui.component.OmniChrome
import com.omnitune.app.ui.component.GlassCard
import com.omnitune.app.ui.theme.OmniColors
import com.omnitune.app.ui.theme.OmniShapes
import com.omnitune.app.ui.theme.OmniSpacing
import com.omnitune.app.update.ApkInstallLauncher
import com.omnitune.app.update.UpdateState
import com.omnitune.app.update.UpdateViewModel
import com.omnitune.app.utils.rememberEnumPreference
import com.omnitune.app.utils.rememberPreference

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
