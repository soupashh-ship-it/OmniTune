/*
 * OmniTune - An open-source music player for Android
 * Licensed under GPL-3.0
 */

package com.omnitune.app.ui.screens

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
import com.omnitune.app.ui.component.GlassCard
import com.omnitune.app.ui.theme.OmniColors
import com.omnitune.app.ui.theme.OmniShapes
import com.omnitune.app.ui.theme.OmniSpacing
import com.omnitune.app.update.ApkInstallLauncher
import com.omnitune.app.update.UpdateState
import com.omnitune.app.update.UpdateViewModel
import com.omnitune.app.utils.rememberEnumPreference
import com.omnitune.app.utils.rememberPreference

private enum class SettingsSection(
    val label: String,
    val description: String,
    val iconRes: Int,
    val accent: Color,
) {
    APPEARANCE(
        "Appearance",
        "OLED, glass, and layout preferences",
        R.drawable.ic_repeat,
        OmniColors.OmniAccentPrimary,
    ),
    PLAYBACK(
        "Playback",
        "Quality, crossfade, equalizer, and playback behavior",
        R.drawable.ic_play_arrow,
        OmniColors.ActivePlayback,
    ),
    STORAGE(
        "Downloads & cache",
        "Cache policy and offline-library guidance",
        R.drawable.ic_download,
        OmniColors.Downloaded,
    ),
    NOTIFICATIONS(
        "Notifications & lock screen",
        "Device-specific media control guidance",
        R.drawable.ic_notification_play,
        OmniColors.Warning,
    ),
    UPDATES(
        "Updates",
        "Check GitHub releases without changing update logic",
        R.drawable.ic_download,
        OmniColors.OmniAccentSecondary,
    ),
    DIAGNOSTICS(
        "Diagnostics",
        "Export the existing diagnostic report",
        R.drawable.ic_share,
        OmniColors.Hot,
    ),
    CONTENT(
        "Content & history",
        "Explicit filters, video results, and history controls",
        R.drawable.ic_search,
        OmniColors.OmniAccentMuted,
    ),
    LYRICS(
        "Lyrics providers",
        "Provider toggles and lyric animation preference",
        R.drawable.ic_list,
        OmniColors.Hot,
    ),
    SCROBBLING(
        "Scrobbling",
        "Last.fm and ListenBrainz preferences",
        R.drawable.ic_favorite,
        OmniColors.HotLight,
    ),
    ABOUT(
        "About",
        "Version, source, license, and credits",
        R.drawable.ic_info,
        OmniColors.TextSecondary,
    ),
}

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
        verticalArrangement = Arrangement.spacedBy(OmniSpacing.medium),
    ) {
        item {
            Spacer(modifier = Modifier.statusBarsPadding())
            Spacer(modifier = Modifier.height(OmniSpacing.medium))
            SettingsHeader(onBack = onBack)
        }

        item {
            SettingsQuickSummary()
        }

        SettingsSection.entries.forEach { section ->
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

        item { Spacer(modifier = Modifier.height(104.dp)) }
    }
}

@Composable
private fun SettingsHeader(onBack: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(OmniShapes.ExtraLarge)
            .background(
                Brush.linearGradient(
                    listOf(
                        OmniColors.OmniGlassPlayer,
                        OmniColors.OmniGlassSubtle,
                    )
                )
            )
            .border(BorderStroke(1.dp, OmniColors.OmniGlassBorderSubtle), OmniShapes.ExtraLarge)
            .padding(OmniSpacing.section),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .size(44.dp)
                    .clip(OmniShapes.Small)
                    .background(OmniColors.OmniGlassMedium),
            ) {
                Icon(
                    painterResource(R.drawable.ic_arrow_back),
                    contentDescription = "Back",
                    tint = OmniColors.TextPrimary,
                    modifier = Modifier.size(22.dp),
                )
            }
            Spacer(modifier = Modifier.width(OmniSpacing.medium))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Settings",
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold,
                    color = OmniColors.TextPrimary,
                )
                Text(
                    text = "Playback, appearance, updates, and diagnostics",
                    style = MaterialTheme.typography.bodyMedium,
                    color = OmniColors.TextSecondary,
                )
            }
        }

        Spacer(modifier = Modifier.height(OmniSpacing.large))
        SettingsStatusPill(
            label = "Installed",
            value = "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
        )
    }
}

@Composable
private fun SettingsQuickSummary() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(OmniSpacing.small),
    ) {
        SettingsMiniCard(
            label = "Updates",
            value = "GitHub release check",
            iconRes = R.drawable.ic_download,
            accent = OmniColors.OmniAccentSecondary,
            modifier = Modifier.weight(1f),
        )
        SettingsMiniCard(
            label = "Diagnostics",
            value = "Share report",
            iconRes = R.drawable.ic_share,
            accent = OmniColors.Hot,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun SettingsMiniCard(
    label: String,
    value: String,
    iconRes: Int,
    accent: Color,
    modifier: Modifier = Modifier,
) {
    GlassCard(modifier = modifier, cornerRadius = OmniShapes.Large) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(OmniSpacing.medium),
        ) {
            SettingsIconBadge(iconRes = iconRes, accent = accent, size = 40.dp)
            Spacer(modifier = Modifier.height(OmniSpacing.medium))
            Text(
                text = label,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = OmniColors.TextPrimary,
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodySmall,
                color = OmniColors.TextTertiary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun SettingsSectionCard(
    section: SettingsSection,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    onNavigateToEqualizer: () -> Unit,
) {
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = OmniShapes.Large,
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = androidx.compose.material3.ripple(
                            bounded = true,
                            color = Color.White.copy(alpha = 0.08f),
                        ),
                        onClick = onToggle,
                    )
                    .padding(OmniSpacing.medium),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SettingsIconBadge(iconRes = section.iconRes, accent = section.accent)
                Spacer(modifier = Modifier.width(OmniSpacing.medium))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = section.label,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = OmniColors.TextPrimary,
                    )
                    Text(
                        text = section.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = OmniColors.TextTertiary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Text(
                    text = if (isExpanded) "Hide" else "Open",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isExpanded) section.accent else OmniColors.TextTertiary,
                )
            }

            if (isExpanded) {
                Divider()
                Column(
                    modifier = Modifier.padding(
                        horizontal = OmniSpacing.medium,
                        vertical = OmniSpacing.small,
                    ),
                    verticalArrangement = Arrangement.spacedBy(OmniSpacing.compact),
                ) {
                    when (section) {
                        SettingsSection.APPEARANCE -> AppearanceSettings()
                        SettingsSection.PLAYBACK -> PlaybackSettings(onNavigateToEqualizer)
                        SettingsSection.STORAGE -> StorageSettings()
                        SettingsSection.NOTIFICATIONS -> MediaControlsHelp()
                        SettingsSection.UPDATES -> UpdatesSettings()
                        SettingsSection.DIAGNOSTICS -> DiagnosticsSettings()
                        SettingsSection.CONTENT -> ContentSettings()
                        SettingsSection.LYRICS -> LyricsSettings()
                        SettingsSection.SCROBBLING -> ScrobblingSettings()
                        SettingsSection.ABOUT -> AboutSettings()
                    }
                    Spacer(modifier = Modifier.height(OmniSpacing.compact))
                }
            }
        }
    }
}

@Composable
private fun PlaybackSettings(onNavigateToEqualizer: () -> Unit) {
    val audioQuality by rememberEnumPreference(AudioQualityKey, AudioQuality.AUTO)

    SettingsCategoryLabel("Audio quality")
    EnumPreferenceRow(
        label = "Stream quality",
        description = "Current: ${audioQuality.displayName()}",
        options = AudioQuality.entries.toList(),
        current = audioQuality,
        key = AudioQualityKey,
    )

    SettingsCategoryLabel("Playback behavior")
    SettingsActionRow(
        iconRes = R.drawable.ic_settings,
        label = "Equalizer",
        description = "Open the existing Android audio effects screen",
        accent = OmniColors.ActivePlayback,
        onClick = onNavigateToEqualizer,
    )
    IntPreferenceSliderRow(
        label = "Crossfade duration",
        description = "Fade between songs without changing playback logic",
        key = AudioCrossfadeDurationKey,
        defaultValue = 0,
        valueRange = 0f..15f,
        steps = 14,
        valueFormat = { if (it == 0) "Off" else "${it}s" },
    )
    TogglePreferenceRow(
        label = "Skip silence",
        description = "Automatically skip silent parts",
        key = SkipSilenceKey,
        defaultValue = false,
    )
    TogglePreferenceRow(
        label = "Auto-skip on error",
        description = "Skip to the next song if playback fails",
        key = AutoSkipNextOnErrorKey,
        defaultValue = true,
    )
    TogglePreferenceRow(
        label = "Pause on device mute",
        description = "Pause when the device is muted",
        key = PauseOnDeviceMuteKey,
        defaultValue = false,
    )
}

@Composable
private fun AppearanceSettings() {
    val gridItemSize by rememberEnumPreference(GridItemsSizeKey, GridItemSize.BIG)

    SettingsCategoryLabel("Display")
    TogglePreferenceRow(
        label = "Pure black mode",
        description = "Use true black for OLED screens",
        key = PureBlackKey,
        defaultValue = false,
    )
    TogglePreferenceRow(
        label = "Disable blur effects",
        description = "Reduce GPU work by disabling blur where supported",
        key = DisableBlurKey,
        defaultValue = false,
    )

    SettingsCategoryLabel("Library layout")
    EnumPreferenceRow(
        label = "Grid item size",
        description = "Current: ${gridItemSize.displayName()}",
        options = GridItemSize.entries.toList(),
        current = gridItemSize,
        key = GridItemsSizeKey,
    )
}

@Composable
private fun StorageSettings() {
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
}

@Composable
private fun LyricsSettings() {
    val lyricsAnim by rememberEnumPreference(LyricsAnimationStyleKey, LyricsAnimationStyle.KARAOKE)

    SettingsCategoryLabel("Lyrics providers")
    TogglePreferenceRow(
        label = "LrcLib",
        description = "Use the existing LrcLib provider",
        key = EnableLrcLibKey,
        defaultValue = true,
    )
    TogglePreferenceRow(
        label = "KuGou",
        description = "Use the existing KuGou provider",
        key = EnableKugouKey,
        defaultValue = true,
    )
    TogglePreferenceRow(
        label = "Better Lyrics",
        description = "Use the existing Better Lyrics provider",
        key = EnableBetterLyricsKey,
        defaultValue = true,
    )
    TogglePreferenceRow(
        label = "SimpMusic",
        description = "Use the existing SimpMusic provider",
        key = EnableSimpMusicLyricsKey,
        defaultValue = true,
    )

    SettingsCategoryLabel("Animation")
    EnumPreferenceRow(
        label = "Lyrics animation",
        description = "Current: ${lyricsAnim.displayName()}",
        options = LyricsAnimationStyle.entries.toList(),
        current = lyricsAnim,
        key = LyricsAnimationStyleKey,
    )
}

@Composable
private fun ContentSettings() {
    SettingsCategoryLabel("Search and content")
    TogglePreferenceRow(
        label = "Hide explicit content",
        description = "Filter out explicit songs",
        key = HideExplicitKey,
        defaultValue = false,
    )
    TogglePreferenceRow(
        label = "Hide video results",
        description = "Only show audio tracks",
        key = HideVideoKey,
        defaultValue = false,
    )

    SettingsCategoryLabel("History")
    TogglePreferenceRow(
        label = "Pause search history",
        description = "Stop saving search history",
        key = PauseSearchHistoryKey,
        defaultValue = false,
    )
    TogglePreferenceRow(
        label = "Pause listen history",
        description = "Stop saving listening history",
        key = PauseListenHistoryKey,
        defaultValue = false,
    )
}

@Composable
private fun UpdatesSettings(
    viewModel: UpdateViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val state by viewModel.state.collectAsState()

    SettingsInfoBlock(
        title = "Installed version",
        body = viewModel.currentVersionLabel,
        accent = OmniColors.OmniAccentSecondary,
    )

    when (val current = state) {
        UpdateState.Idle -> {
            UpdateStateCard(
                title = "Ready to check",
                body = "Uses the existing GitHub latest-release check. No release status is assumed until you run it.",
                accent = OmniColors.OmniAccentSecondary,
            )
            SettingsActionButton("Check for updates") {
                viewModel.checkForUpdates()
            }
        }
        UpdateState.Checking -> {
            UpdateStateCard(
                title = "Checking GitHub releases",
                body = "Waiting for the existing update checker to return.",
                accent = OmniColors.OmniAccentSecondary,
                progress = null,
            )
        }
        UpdateState.NoUpdate -> {
            UpdateStateCard(
                title = "Already latest",
                body = "The update checker did not find a newer public release.",
                accent = OmniColors.Success,
            )
            SettingsActionButton("Check again") {
                viewModel.checkForUpdates()
            }
        }
        is UpdateState.UpdateAvailable -> {
            UpdateDetails(current)
            if (current.requireMeteredConfirmation) {
                UpdateMessage("Mobile data connection detected. Tap again to confirm download.")
            }
            SettingsActionButton(
                if (current.requireMeteredConfirmation) "Download on mobile data" else "Download update"
            ) {
                viewModel.downloadUpdate(confirmMetered = current.requireMeteredConfirmation)
            }
        }
        is UpdateState.Downloading -> {
            val progress = current.progress.coerceIn(0f, 1f)
            val percent = (progress * 100).toInt().coerceIn(0, 100)
            UpdateStateCard(
                title = "Downloading update",
                body = "$percent% complete",
                accent = OmniColors.OmniAccentSecondary,
                progress = progress,
            )
        }
        is UpdateState.Downloaded -> {
            UpdateStateCard(
                title = "Update downloaded and verified",
                body = "Package: ${current.update.packageName}, code ${current.update.versionCode}",
                accent = OmniColors.Success,
            )
            SettingsActionButton("Install now") {
                runCatching {
                    if (ApkInstallLauncher.canRequestPackageInstalls(context)) {
                        context.startActivity(ApkInstallLauncher.installIntent(context, current.update.apkFile))
                    } else {
                        context.startActivity(ApkInstallLauncher.installPermissionIntent(context))
                    }
                }.onFailure {
                    viewModel.showError("Could not open Android installer.")
                }
            }
            if (!ApkInstallLauncher.canRequestPackageInstalls(context)) {
                UpdateMessage("Install permission is required to continue.")
            }
        }
        is UpdateState.Error -> {
            UpdateStateCard(
                title = "Update check failed",
                body = current.message,
                accent = OmniColors.Error,
            )
            SettingsActionButton("Try again") {
                viewModel.checkForUpdates()
            }
        }
    }
}

@Composable
private fun DiagnosticsSettings() {
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

@Composable
private fun MediaControlsHelp() {
    val context = LocalContext.current
    var message by remember { mutableStateOf<String?>(null) }
    val notificationsEnabled = remember {
        NotificationManagerCompat.from(context).areNotificationsEnabled()
    }
    val channelStatus = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(NotificationManager::class.java)
            when (manager.getNotificationChannel(MusicService.CHANNEL_ID)?.importance) {
                null -> "Playback channel will be created after playback starts."
                NotificationManager.IMPORTANCE_NONE -> "Playback notification channel is blocked."
                NotificationManager.IMPORTANCE_MIN -> "Playback channel is set very low; controls may be hidden."
                else -> "Playback notification channel is allowed."
            }
        } else {
            "Notification channels are not used on this Android version."
        }
    }
    val batteryStatus = remember {
        val powerManager = context.getSystemService(PowerManager::class.java)
        if (powerManager.isIgnoringBatteryOptimizations(context.packageName)) {
            "Battery optimization is unrestricted for OmniTune."
        } else {
            "Battery restrictions may affect background playback and media controls."
        }
    }

    SettingsInfoBlock(
        title = "Notification and lock-screen controls",
        body = "Behavior can vary by device and OEM. Battery restrictions may affect background playback.",
        accent = OmniColors.Warning,
    )
    UpdateMessage(
        "Notification permission: ${if (notificationsEnabled) "Allowed" else "Blocked"}\n" +
            "$channelStatus\n$batteryStatus"
    )
    UpdateMessage(
        "On Vivo/iQOO/Funtouch OS and similar Android skins, allow notifications, lock-screen notifications, background activity, unrestricted battery, and autostart if your device offers those options."
    )
    SettingsActionButton("Open notification settings") {
        openSettingsIntent(context, openNotificationSettingsIntent(context)) {
            message = "Could not open notification settings."
        }
    }
    SettingsActionButton("Open app settings") {
        openSettingsIntent(context, openAppDetailsIntent(context)) {
            message = "Could not open app settings."
        }
    }
    SettingsActionButton("Open battery settings") {
        openSettingsIntent(context, Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)) {
            message = "Could not open battery settings."
        }
    }
    message?.let { UpdateMessage(it) }
}

@Composable
private fun ScrobblingSettings() {
    val lastfmEnabled by rememberPreference(EnableLastFMScrobblingKey, false)

    SettingsCategoryLabel("Last.fm")
    TogglePreferenceRow(
        label = "Enable scrobbling",
        description = "Scrobble plays to Last.fm",
        key = EnableLastFMScrobblingKey,
        defaultValue = false,
    )
    if (lastfmEnabled) {
        TogglePreferenceRow(
            label = "Now playing",
            description = "Share now playing to Last.fm",
            key = LastFMUseNowPlaying,
            defaultValue = false,
        )
    }

    SettingsCategoryLabel("ListenBrainz")
    TogglePreferenceRow(
        label = "Enable scrobbling",
        description = "Scrobble plays to ListenBrainz",
        key = ListenBrainzEnabledKey,
        defaultValue = false,
    )
}

@Composable
private fun AboutSettings() {
    val context = LocalContext.current
    var message by remember { mutableStateOf<String?>(null) }

    SettingsInfoBlock(
        title = "OmniTune",
        body = "Version ${BuildConfig.VERSION_NAME} (code ${BuildConfig.VERSION_CODE})\nOpen-source music player for Android\nLicense: GPL-3.0",
        accent = OmniColors.TextSecondary,
    )
    SettingsInfoBlock(
        title = "Legal access",
        body = "License and credits remain in the project repository as LICENSE and CREDITS.md. Phase 10 does not rewrite or remove legal text.",
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

@Composable
private fun UpdateDetails(state: UpdateState.UpdateAvailable) {
    val update = state.update
    UpdateStateCard(
        title = "Update available",
        body = "Latest version: ${update.versionName}\nAPK: ${formatBytes(update.apkAsset.size)}",
        accent = OmniColors.Success,
    )
    if (update.releaseNotes.isNotBlank()) {
        Text(
            text = update.releaseNotes,
            style = MaterialTheme.typography.bodySmall,
            color = OmniColors.TextSecondary,
            maxLines = 6,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = OmniSpacing.medium, vertical = OmniSpacing.compact),
        )
    }
}

@Composable
private fun UpdateStateCard(
    title: String,
    body: String,
    accent: Color,
    progress: Float? = null,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(OmniShapes.Medium)
            .background(OmniColors.OmniGlassMedium)
            .border(BorderStroke(1.dp, OmniColors.OmniGlassBorderSubtle), OmniShapes.Medium)
            .padding(OmniSpacing.medium),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(accent),
            )
            Spacer(modifier = Modifier.width(OmniSpacing.compact))
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = OmniColors.TextPrimary,
            )
        }
        Spacer(modifier = Modifier.height(OmniSpacing.compact))
        Text(
            text = body,
            style = MaterialTheme.typography.bodySmall,
            color = OmniColors.TextSecondary,
        )
        progress?.let {
            Spacer(modifier = Modifier.height(OmniSpacing.small))
            LinearProgressIndicator(
                progress = { it.coerceIn(0f, 1f) },
                color = accent,
                trackColor = OmniColors.OmniGlassSubtle,
                modifier = Modifier.fillMaxWidth(),
            )
        } ?: if (title.startsWith("Checking")) {
            Spacer(modifier = Modifier.height(OmniSpacing.small))
            LinearProgressIndicator(
                color = accent,
                trackColor = OmniColors.OmniGlassSubtle,
                modifier = Modifier.fillMaxWidth(),
            )
        } else {
            Unit
        }
    }
}

@Composable
private fun SettingsInfoBlock(
    title: String,
    body: String,
    accent: Color,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(OmniShapes.Medium)
            .background(OmniColors.OmniGlassSubtle)
            .border(BorderStroke(1.dp, OmniColors.OmniGlassBorderSubtle), OmniShapes.Medium)
            .padding(OmniSpacing.medium),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = accent,
        )
        Spacer(modifier = Modifier.height(OmniSpacing.micro))
        Text(
            text = body,
            style = MaterialTheme.typography.bodySmall,
            color = OmniColors.TextSecondary,
        )
    }
}

@Composable
private fun SettingsStatusPill(
    label: String,
    value: String,
) {
    Column(
        modifier = Modifier
            .clip(OmniShapes.Pill)
            .background(OmniColors.OmniGlassMedium)
            .border(BorderStroke(1.dp, OmniColors.OmniGlassBorderSubtle), OmniShapes.Pill)
            .padding(horizontal = OmniSpacing.medium, vertical = OmniSpacing.compact),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = OmniColors.TextTertiary,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = OmniColors.TextPrimary,
        )
    }
}

@Composable
private fun SettingsIconBadge(
    iconRes: Int,
    accent: Color,
    size: androidx.compose.ui.unit.Dp = 44.dp,
) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(OmniShapes.Small)
            .background(accent.copy(alpha = 0.16f))
            .border(BorderStroke(1.dp, accent.copy(alpha = 0.34f)), OmniShapes.Small),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = null,
            tint = accent,
            modifier = Modifier.size(size * 0.46f),
        )
    }
}

@Composable
private fun SettingsCategoryLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.SemiBold,
        color = OmniColors.OmniAccentSecondary,
        modifier = Modifier.padding(horizontal = OmniSpacing.compact, vertical = OmniSpacing.small),
    )
}

@Composable
private fun SettingsActionRow(
    iconRes: Int,
    label: String,
    description: String,
    accent: Color,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 64.dp)
            .clip(OmniShapes.Medium)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = androidx.compose.material3.ripple(
                    bounded = true,
                    color = Color.White.copy(alpha = 0.08f),
                ),
                onClick = onClick,
            )
            .padding(OmniSpacing.medium),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SettingsIconBadge(iconRes = iconRes, accent = accent, size = 38.dp)
        Spacer(modifier = Modifier.width(OmniSpacing.medium))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = OmniColors.TextPrimary,
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = OmniColors.TextTertiary,
            )
        }
        Icon(
            painter = painterResource(R.drawable.ic_arrow_back),
            contentDescription = "Open $label",
            tint = OmniColors.TextTertiary,
            modifier = Modifier
                .size(18.dp)
                .graphicsLayer { rotationZ = 180f },
        )
    }
}

@Composable
private fun SettingsActionButton(
    label: String,
    onClick: () -> Unit,
) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = OmniColors.OmniAccentPrimary,
            contentColor = OmniColors.OmniAccentOnPrimary,
        ),
        shape = OmniShapes.Pill,
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 48.dp)
            .padding(vertical = OmniSpacing.micro),
    ) {
        Text(label, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun TogglePreferenceRow(
    label: String,
    description: String,
    key: Preferences.Key<Boolean>,
    defaultValue: Boolean,
) {
    var value by rememberPreference(key, defaultValue)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 64.dp)
            .clip(OmniShapes.Medium)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = androidx.compose.material3.ripple(
                    bounded = true,
                    color = Color.White.copy(alpha = 0.08f),
                ),
                onClick = { value = !value },
            )
            .padding(OmniSpacing.medium),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = OmniColors.TextPrimary,
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = OmniColors.TextTertiary,
            )
        }
        Spacer(modifier = Modifier.width(OmniSpacing.medium))
        Switch(
            checked = value,
            onCheckedChange = { newValue -> value = newValue },
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = OmniColors.OmniAccentPrimary,
                uncheckedThumbColor = OmniColors.TextTertiary,
                uncheckedTrackColor = OmniColors.OmniGlassMedium,
                uncheckedBorderColor = OmniColors.OmniGlassBorderSubtle,
            ),
        )
    }
}

@Composable
private fun <T : Enum<T>> EnumPreferenceRow(
    label: String,
    description: String,
    options: List<T>,
    current: T,
    key: Preferences.Key<String>,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    var showDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 64.dp)
            .clip(OmniShapes.Medium)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = androidx.compose.material3.ripple(
                    bounded = true,
                    color = Color.White.copy(alpha = 0.08f),
                ),
                onClick = { showDialog = true },
            )
            .padding(OmniSpacing.medium),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = OmniColors.TextPrimary,
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = OmniColors.TextTertiary,
            )
        }
        Text(
            text = "Change",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = OmniColors.OmniAccentSecondary,
        )
    }

    if (showDialog) {
        EnumSelectionDialog(
            title = label,
            options = options,
            current = current,
            onDismiss = { showDialog = false },
            onSelected = { selected ->
                showDialog = false
                viewModel.updatePreference(context, key, selected.name)
            },
        )
    }
}

@Composable
private fun <T : Enum<T>> EnumSelectionDialog(
    title: String,
    options: List<T>,
    current: T,
    onDismiss: () -> Unit,
    onSelected: (T) -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = OmniColors.OmniBackgroundElevated,
        titleContentColor = OmniColors.TextPrimary,
        textContentColor = OmniColors.TextSecondary,
        title = {
            Text(title, fontWeight = FontWeight.Bold)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(OmniSpacing.micro)) {
                options.forEach { option ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .defaultMinSize(minHeight = 48.dp)
                            .clip(OmniShapes.Small)
                            .clickable { onSelected(option) }
                            .padding(vertical = OmniSpacing.small, horizontal = OmniSpacing.compact),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(20.dp)
                                .clip(CircleShape)
                                .border(
                                    2.dp,
                                    if (option == current) OmniColors.OmniAccentPrimary else OmniColors.OmniGlassBorderSubtle,
                                    CircleShape,
                                ),
                            contentAlignment = Alignment.Center,
                        ) {
                            if (option == current) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(OmniColors.OmniAccentPrimary),
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(OmniSpacing.small))
                        Text(
                            text = option.displayName(),
                            style = MaterialTheme.typography.bodyLarge,
                            color = if (option == current) OmniColors.OmniAccentPrimary else OmniColors.TextPrimary,
                        )
                    }
                }
            }
        },
        confirmButton = {},
    )
}

@Composable
private fun Divider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(OmniColors.OmniGlassBorderSubtle),
    )
}

@Composable
private fun IntPreferenceSliderRow(
    label: String,
    description: String,
    key: Preferences.Key<Int>,
    defaultValue: Int,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int = 0,
    valueFormat: (Int) -> String = { it.toString() },
) {
    var value by rememberPreference(key, defaultValue)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 64.dp)
            .clip(OmniShapes.Medium)
            .padding(OmniSpacing.medium),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = OmniColors.TextPrimary,
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = OmniColors.TextTertiary,
                )
            }
            Text(
                text = valueFormat(value),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = OmniColors.OmniAccentSecondary,
            )
        }
        Slider(
            value = value.toFloat(),
            onValueChange = { value = it.toInt() },
            valueRange = valueRange,
            steps = steps,
            colors = SliderDefaults.colors(
                thumbColor = OmniColors.OmniAccentPrimary,
                activeTrackColor = OmniColors.OmniAccentPrimary,
                inactiveTrackColor = OmniColors.OmniGlassMedium,
            ),
            modifier = Modifier.padding(top = OmniSpacing.compact),
        )
    }
}

@Composable
private fun UpdateMessage(message: String) {
    Text(
        text = message,
        style = MaterialTheme.typography.bodySmall,
        color = OmniColors.TextSecondary,
        modifier = Modifier.padding(horizontal = OmniSpacing.compact, vertical = OmniSpacing.micro),
    )
}

private fun formatBytes(bytes: Long): String {
    if (bytes <= 0L) return "Unknown size"
    val mb = bytes / (1024.0 * 1024.0)
    return "%.1f MB".format(mb)
}

private fun openNotificationSettingsIntent(context: Context): Intent {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
            .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
    } else {
        openAppDetailsIntent(context)
    }
}

private fun openAppDetailsIntent(context: Context): Intent {
    return Intent(
        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
        Uri.fromParts("package", context.packageName, null),
    )
}

private fun openSettingsIntent(
    context: Context,
    intent: Intent,
    onFailure: () -> Unit,
) {
    runCatching { context.startActivity(intent) }.onFailure { onFailure() }
}

private fun openUrl(
    context: Context,
    url: String,
    onFailure: () -> Unit,
) {
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
    runCatching { context.startActivity(intent) }.onFailure { onFailure() }
}

private fun Enum<*>.displayName(): String {
    return name
        .replace("_", " ")
        .lowercase()
        .replaceFirstChar { it.uppercase() }
}
