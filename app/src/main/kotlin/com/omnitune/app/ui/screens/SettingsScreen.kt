/*
 * OmniTune - An open-source music player for Android
 * Licensed under GPL-3.0
 */

package com.omnitune.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.graphicsLayer
import com.omnitune.app.R
import com.omnitune.app.constants.*
import com.omnitune.app.ui.component.GlassCard
import com.omnitune.app.ui.theme.OmniColors
import com.omnitune.app.ui.theme.OmniShapes
import androidx.compose.ui.platform.LocalContext
import com.omnitune.app.utils.rememberEnumPreference
import com.omnitune.app.utils.rememberPreference
import com.omnitune.app.utils.dataStore
import com.omnitune.app.utils.PreferenceStore

// ── Settings Sections ──

private enum class SettingsSection(val label: String, val iconRes: Int) {
    PLAYBACK("Playback", R.drawable.ic_play_arrow),
    APPEARANCE("Appearance", R.drawable.ic_repeat),
    LYRICS("Lyrics", R.drawable.ic_list),
    CONTENT("Content", android.R.drawable.ic_menu_search),
    STORAGE("Storage & Cache", android.R.drawable.ic_menu_save),
    SCROBBLING("Scrobbling", R.drawable.ic_favorite),
}

// ── Main Settings Screen ──

@Composable
fun SettingsScreen(
    onBack: () -> Unit = {},
    onNavigateToEqualizer: () -> Unit = {},
) {
    var expandedSection by remember { mutableStateOf<SettingsSection?>(null) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(OmniColors.Background)
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        item {
            Spacer(modifier = Modifier.statusBarsPadding())
            Spacer(modifier = Modifier.height(12.dp))
            // Top bar
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(OmniShapes.SM)
                        .background(OmniColors.GlassSurface),
                ) {
                    Icon(
                        painterResource(R.drawable.ic_arrow_back),
                        contentDescription = "Back",
                        tint = OmniColors.TextPrimary,
                        modifier = Modifier.size(20.dp),
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    "Settings",
                    style = androidx.compose.material3.MaterialTheme.typography.displayMedium,
                    fontWeight = FontWeight.Bold,
                    color = OmniColors.TextPrimary,
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        // Settings sections
        SettingsSection.entries.forEach { section ->
            item {
                SettingsSectionCard(
                    section = section,
                    isExpanded = expandedSection == section,
                    onToggle = {
                        expandedSection = if (expandedSection == section) null else section
                    },
                    onNavigateToEqualizer = onNavigateToEqualizer
                )
            }
        }

        item { Spacer(modifier = Modifier.height(80.dp)) }
    }
}

// ── Section Card ──

@Composable
private fun SettingsSectionCard(
    section: SettingsSection,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    onNavigateToEqualizer: () -> Unit = {},
) {
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = OmniShapes.LG,
    ) {
        // Section header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = androidx.compose.material3.ripple(bounded = true, color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.1f)),
                    onClick = onToggle,
                )
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        Brush.linearGradient(
                            when (section) {
                                SettingsSection.PLAYBACK -> listOf(OmniColors.Primary, OmniColors.Secondary)
                                SettingsSection.APPEARANCE -> listOf(OmniColors.Secondary, OmniColors.Primary)
                                SettingsSection.LYRICS -> listOf(OmniColors.Hot, OmniColors.Primary)
                                SettingsSection.CONTENT -> listOf(OmniColors.Primary, OmniColors.Hot)
                                SettingsSection.STORAGE -> listOf(OmniColors.Warning, OmniColors.Hot)
                                SettingsSection.SCROBBLING -> listOf(OmniColors.Hot, OmniColors.Secondary)
                            }
                        )
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painterResource(section.iconRes),
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(18.dp),
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Text(
                section.label,
                style = androidx.compose.material3.MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = OmniColors.TextPrimary,
                modifier = Modifier.weight(1f),
            )
            Text(
                if (isExpanded) "▾" else "▸",
                fontSize = 14.sp,
                color = OmniColors.TextMuted,
            )
        }

        // Expanded content
        if (isExpanded) {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                when (section) {
                    SettingsSection.PLAYBACK -> PlaybackSettings(onNavigateToEqualizer)
                    SettingsSection.APPEARANCE -> AppearanceSettings()
                    SettingsSection.LYRICS -> LyricsSettings()
                    SettingsSection.CONTENT -> ContentSettings()
                    SettingsSection.STORAGE -> StorageSettings()
                    SettingsSection.SCROBBLING -> ScrobblingSettings()
                }
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }
}

// ── Playback Settings ──

@Composable
private fun PlaybackSettings(onNavigateToEqualizer: () -> Unit) {
    val audioQuality by rememberEnumPreference(AudioQualityKey, AudioQuality.AUTO)
    val skipSilence by rememberPreference(SkipSilenceKey, false)
    val autoSkipOnError by rememberPreference(AutoSkipNextOnErrorKey, true)
    val permanentShuffle by rememberPreference(PermanentShuffleKey, false)
    val pauseOnMute by rememberPreference(PauseOnDeviceMuteKey, false)

    SettingsCategoryLabel("Audio Quality")
    EnumPreferenceRow(
        label = "Stream Quality",
        description = "Current: ${audioQuality.name}",
        options = AudioQuality.entries.toList(),
        current = audioQuality,
        key = AudioQualityKey,
    )
    Divider()

    SettingsCategoryLabel("Playback Behavior")
    
    // OMNITUNE: Equalizer row
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onNavigateToEqualizer() }
            .padding(vertical = 14.dp, horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(painterResource(R.drawable.ic_settings), "Equalizer", tint = OmniColors.Primary, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(12.dp))
        Text("Equalizer", color = OmniColors.TextPrimary, fontSize = 15.sp, modifier = Modifier.weight(1f))
        Icon(painterResource(R.drawable.ic_arrow_back), "Open", tint = OmniColors.TextPrimary.copy(alpha = 0.4f),
             modifier = Modifier.size(16.dp).graphicsLayer { rotationZ = 180f })
    }
    
    TogglePreferenceRow(
        label = "Skip Silence",
        description = "Automatically skip silent parts",
        key = SkipSilenceKey,
        defaultValue = false,
    )
    TogglePreferenceRow(
        label = "Auto-Skip on Error",
        description = "Skip to next song if playback fails",
        key = AutoSkipNextOnErrorKey,
        defaultValue = true,
    )
    TogglePreferenceRow(
        label = "Permanent Shuffle",
        description = "Keep shuffle on always",
        key = PermanentShuffleKey,
        defaultValue = false,
    )
    TogglePreferenceRow(
        label = "Pause on Device Mute",
        description = "Pause when device is muted",
        key = PauseOnDeviceMuteKey,
        defaultValue = false,
    )
}

// ── Appearance Settings ──

@Composable
private fun AppearanceSettings() {
    val pureBlack by rememberPreference(PureBlackKey, false)
    val disableBlur by rememberPreference(DisableBlurKey, false)
    val gridItemSize by rememberEnumPreference(GridItemsSizeKey, GridItemSize.BIG)

    TogglePreferenceRow(
        label = "Pure Black Mode",
        description = "Use true black for OLED screens",
        key = PureBlackKey,
        defaultValue = false,
    )
    TogglePreferenceRow(
        label = "Disable Blur Effects",
        description = "Reduce GPU usage by disabling blur",
        key = DisableBlurKey,
        defaultValue = false,
    )

    SettingsCategoryLabel("Grid Layout")
    EnumPreferenceRow(
        label = "Grid Item Size",
        description = "Current: ${gridItemSize.name}",
        options = GridItemSize.entries.toList(),
        current = gridItemSize,
        key = GridItemsSizeKey,
    )
}

// ── Lyrics Settings ──

@Composable
private fun LyricsSettings() {
    val enableLrcLib by rememberPreference(EnableLrcLibKey, true)
    val enableKugou by rememberPreference(EnableKugouKey, true)
    val enableBetterLyrics by rememberPreference(EnableBetterLyricsKey, true)
    val enableSimpMusic by rememberPreference(EnableSimpMusicLyricsKey, true)
    val lyricsAnim by rememberEnumPreference(LyricsAnimationStyleKey, LyricsAnimationStyle.KARAOKE)

    SettingsCategoryLabel("Lyrics Providers")
    TogglePreferenceRow(
        label = "LrcLib",
        description = "Synced lyrics from LrcLib",
        key = EnableLrcLibKey,
        defaultValue = true,
    )
    TogglePreferenceRow(
        label = "KuGou",
        description = "Synced lyrics from KuGou",
        key = EnableKugouKey,
        defaultValue = true,
    )
    TogglePreferenceRow(
        label = "Better Lyrics",
        description = "Enhanced lyrics source",
        key = EnableBetterLyricsKey,
        defaultValue = true,
    )
    TogglePreferenceRow(
        label = "SimpMusic",
        description = "SimpMusic lyrics provider",
        key = EnableSimpMusicLyricsKey,
        defaultValue = true,
    )
    Divider()

    SettingsCategoryLabel("Animation")
    EnumPreferenceRow(
        label = "Lyrics Animation",
        description = "Current: ${lyricsAnim.name}",
        options = LyricsAnimationStyle.entries.toList(),
        current = lyricsAnim,
        key = LyricsAnimationStyleKey,
    )
}

// ── Content Settings ──

@Composable
private fun ContentSettings() {
    val hideExplicit by rememberPreference(HideExplicitKey, false)
    val hideVideo by rememberPreference(HideVideoKey, false)
    val pauseSearchHistory by rememberPreference(PauseSearchHistoryKey, false)
    val pauseListenHistory by rememberPreference(PauseListenHistoryKey, false)

    TogglePreferenceRow(
        label = "Hide Explicit Content",
        description = "Filter out explicit songs",
        key = HideExplicitKey,
        defaultValue = false,
    )
    TogglePreferenceRow(
        label = "Hide Video Results",
        description = "Only show audio tracks",
        key = HideVideoKey,
        defaultValue = false,
    )
    Divider()

    SettingsCategoryLabel("History")
    TogglePreferenceRow(
        label = "Pause Search History",
        description = "Stop saving search history",
        key = PauseSearchHistoryKey,
        defaultValue = false,
    )
    TogglePreferenceRow(
        label = "Pause Listen History",
        description = "Stop saving listening history",
        key = PauseListenHistoryKey,
        defaultValue = false,
    )
}

// ── Storage Settings ──

@Composable
private fun StorageSettings() {
    val smartTrimmer by rememberPreference(SmartTrimmerKey, true)

    TogglePreferenceRow(
        label = "Smart Cache Trimmer",
        description = "Automatically clear old cache",
        key = SmartTrimmerKey,
        defaultValue = true,
    )

    SettingsCategoryLabel("Cache Limits")
    Text(
        "Image cache: 128 MB max",
        style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
        color = OmniColors.TextMuted,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
    )
    Text(
        "Song cache: 2 GB max",
        style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
        color = OmniColors.TextMuted,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
    )
}

// ── Scrobbling Settings ──

@Composable
private fun ScrobblingSettings() {
    val lastfmEnabled by rememberPreference(EnableLastFMScrobblingKey, false)
    val listenbrainzEnabled by rememberPreference(ListenBrainzEnabledKey, false)
    val lastfmNowPlaying by rememberPreference(LastFMUseNowPlaying, false)

    SettingsCategoryLabel("Last.fm")
    TogglePreferenceRow(
        label = "Enable Scrobbling",
        description = "Scrobble plays to Last.fm",
        key = EnableLastFMScrobblingKey,
        defaultValue = false,
    )
    if (lastfmEnabled) {
        TogglePreferenceRow(
            label = "Now Playing",
            description = "Share now playing to Last.fm",
            key = LastFMUseNowPlaying,
            defaultValue = false,
        )
    }
    Divider()

    SettingsCategoryLabel("ListenBrainz")
    TogglePreferenceRow(
        label = "Enable Scrobbling",
        description = "Scrobble plays to ListenBrainz",
        key = ListenBrainzEnabledKey,
        defaultValue = false,
    )
}

// ── Reusable Settings Components ──

@Composable
private fun SettingsCategoryLabel(text: String) {
    Text(
        text = text,
        style = androidx.compose.material3.MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.SemiBold,
        color = OmniColors.Secondary,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
    )
}

@Composable
private fun TogglePreferenceRow(
    label: String,
    description: String,
    key: androidx.datastore.preferences.core.Preferences.Key<Boolean>,
    defaultValue: Boolean,
) {
    var value by rememberPreference(key, defaultValue)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(OmniShapes.SM)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = androidx.compose.material3.ripple(bounded = true, color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.1f)),
                onClick = { value = !value },
            )
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                label,
                style = androidx.compose.material3.MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = OmniColors.TextPrimary,
            )
            Text(
                description,
                style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                color = OmniColors.TextMuted,
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Switch(
            checked = value,
            onCheckedChange = { newValue -> value = newValue },
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = OmniColors.Primary,
                uncheckedThumbColor = OmniColors.TextMuted,
                uncheckedTrackColor = OmniColors.GlassSurface,
                uncheckedBorderColor = OmniColors.GlassBorder,
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
    key: androidx.datastore.preferences.core.Preferences.Key<String>,
) {
    var showDialog by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(OmniShapes.SM)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = androidx.compose.material3.ripple(bounded = true, color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.1f)),
                onClick = { showDialog = true },
            )
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                label,
                style = androidx.compose.material3.MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = OmniColors.TextPrimary,
            )
            Text(
                description,
                style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                color = OmniColors.TextMuted,
            )
        }
        Text(
            "▸",
            fontSize = 14.sp,
            color = OmniColors.TextMuted,
        )
    }

    val context = LocalContext.current
    if (showDialog) {
        EnumSelectionDialog(
            title = label,
            options = options,
            current = current,
            onDismiss = { showDialog = false },
            onSelected = { selected ->
                showDialog = false
                PreferenceStore.launchEdit(context.dataStore) {
                    this[key] = selected.name
                }
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
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = OmniColors.SurfaceElevated,
        titleContentColor = OmniColors.TextPrimary,
        textContentColor = OmniColors.TextSecondary,
        title = {
            Text(title, fontWeight = FontWeight.Bold)
        },
        text = {
            Column {
                options.forEach { option ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(OmniShapes.SM)
                            .clickable { onSelected(option) }
                            .padding(vertical = 12.dp, horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(20.dp)
                                .clip(RoundedCornerShape(50))
                                .border(
                                    2.dp,
                                    if (option == current) OmniColors.Primary else OmniColors.GlassBorder,
                                    RoundedCornerShape(50),
                                ),
                            contentAlignment = Alignment.Center,
                        ) {
                            if (option == current) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(RoundedCornerShape(50))
                                        .background(OmniColors.Primary),
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            option.name.replace("_", " ").lowercase()
                                .replaceFirstChar { it.uppercase() },
                            style = androidx.compose.material3.MaterialTheme.typography.bodyLarge,
                            color = if (option == current) OmniColors.Primary else OmniColors.TextPrimary,
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
            .padding(horizontal = 16.dp)
            .background(OmniColors.GlassBorderLight),
    )
}
