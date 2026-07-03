package com.omnitune.app.ui.screens.settings

import com.omnitune.app.ui.screens.SettingsViewModel
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel

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
import com.omnitune.app.ui.component.GlassCard
import com.omnitune.app.ui.component.GlassTone
import com.omnitune.app.ui.theme.OmniColors
import com.omnitune.app.ui.theme.OmniShapes
import com.omnitune.app.ui.theme.OmniSpacing
import com.omnitune.app.update.ApkInstallLauncher
import com.omnitune.app.update.UpdateState
import com.omnitune.app.update.UpdateViewModel
import com.omnitune.app.utils.rememberEnumPreference
import com.omnitune.app.utils.rememberPreference

@Composable
fun SettingsHeader(onBack: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(top = OmniSpacing.small),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .size(44.dp)
                    .clip(OmniShapes.Pill),
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
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = OmniColors.TextPrimary,
                )
                Text(
                    text = "OmniTune preferences",
                    style = MaterialTheme.typography.bodyMedium,
                    color = OmniColors.TextSecondary,
                )
            }
        }

        Spacer(modifier = Modifier.height(OmniSpacing.small))
        SettingsStatusPill(
            label = "OmniTune",
            value = "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
        )
    }
}


@Composable
fun SettingsQuickSummary(
    onUpdates: () -> Unit,
    onDiagnostics: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth(),
    ) {
        SettingsMiniCard(
            label = "Updates",
            value = "GitHub release check",
            iconRes = R.drawable.ic_download,
            accent = OmniColors.OmniAccentSecondary,
            onClick = onUpdates,
        )
        Divider()
        SettingsMiniCard(
            label = "Diagnostics",
            value = "Share report",
            iconRes = R.drawable.ic_share,
            accent = OmniColors.Hot,
            onClick = onDiagnostics,
        )
    }
}


@Composable
fun SettingsMiniCard(
    label: String,
    value: String,
    iconRes: Int,
    accent: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 64.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = androidx.compose.material3.ripple(
                    bounded = true,
                    color = Color.White.copy(alpha = 0.08f),
                ),
                onClick = onClick,
            )
            .padding(horizontal = OmniSpacing.medium, vertical = OmniSpacing.small),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SettingsIconBadge(iconRes = iconRes, accent = accent, size = 34.dp)
        Spacer(modifier = Modifier.width(OmniSpacing.medium))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = OmniColors.TextPrimary,
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodySmall,
                color = OmniColors.TextTertiary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        SettingsChevron(open = false, tint = OmniColors.TextTertiary)
    }
}


@Composable
fun SettingsSectionCard(
    section: SettingsSection,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    onNavigateToEqualizer: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 68.dp)
                .clip(OmniShapes.Small)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = androidx.compose.material3.ripple(
                        bounded = true,
                        color = Color.White.copy(alpha = 0.08f),
                    ),
                    onClick = onToggle,
                )
                .padding(horizontal = OmniSpacing.compact, vertical = OmniSpacing.small),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SettingsIconBadge(iconRes = section.iconRes, accent = section.accent, size = 36.dp)
            Spacer(modifier = Modifier.width(OmniSpacing.medium))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = section.label,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = OmniColors.TextPrimary,
                )
                Text(
                    text = section.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = OmniColors.TextTertiary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            SettingsChevron(open = isExpanded, tint = if (isExpanded) section.accent else OmniColors.TextTertiary)
        }

        if (isExpanded) {
            Column(
                modifier = Modifier
                    .padding(top = OmniSpacing.micro, bottom = OmniSpacing.small)
                    .padding(horizontal = OmniSpacing.compact, vertical = OmniSpacing.small),
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
        } else {
            Divider()
        }
    }
}


@Composable
fun SettingsInfoBlock(
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
fun SettingsStatusPill(
    label: String,
    value: String,
) {
    Row(
        modifier = Modifier
            .clip(OmniShapes.Pill)
            .background(OmniColors.SurfaceQuiet)
            .border(BorderStroke(1.dp, OmniColors.SurfaceHairline), OmniShapes.Pill)
            .padding(horizontal = OmniSpacing.medium, vertical = OmniSpacing.compact),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = OmniColors.TextSecondary,
        )
        Spacer(modifier = Modifier.width(OmniSpacing.small))
        Text(
            text = value,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = OmniColors.OmniAccentSecondary,
        )
    }
}


@Composable
fun SettingsIconBadge(
    iconRes: Int,
    accent: Color,
    size: androidx.compose.ui.unit.Dp = 44.dp,
) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(OmniShapes.Small),
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
fun SettingsChevron(
    open: Boolean,
    tint: Color,
) {
    Icon(
        painter = painterResource(R.drawable.ic_arrow_back),
        contentDescription = if (open) "Collapse" else "Open",
        tint = tint,
        modifier = Modifier
            .size(18.dp)
            .graphicsLayer { rotationZ = if (open) 90f else 180f },
    )
}


@Composable
fun SettingsCategoryLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.SemiBold,
        color = OmniColors.OmniAccentSecondary,
        modifier = Modifier.padding(horizontal = OmniSpacing.compact, vertical = OmniSpacing.small),
    )
}


@Composable
fun SettingsActionRow(
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
fun SettingsActionButton(
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
fun TogglePreferenceRow(
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
fun <T : Enum<T>> EnumPreferenceRow(
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
fun <T : Enum<T>> EnumSelectionDialog(
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
fun Divider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(OmniColors.OmniGlassBorderSubtle),
    )
}


@Composable
fun IntPreferenceSliderRow(
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




fun openNotificationSettingsIntent(context: Context): Intent {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
            .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
    } else {
        openAppDetailsIntent(context)
    }
}

fun openAppDetailsIntent(context: Context): Intent {
    return Intent(
        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
        Uri.fromParts("package", context.packageName, null),
    )
}

fun openSettingsIntent(
    context: Context,
    intent: Intent,
    onFailure: () -> Unit,
) {
    runCatching { context.startActivity(intent) }.onFailure { onFailure() }
}

fun openUrl(
    context: Context,
    url: String,
    onFailure: () -> Unit,
) {
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
    runCatching { context.startActivity(intent) }.onFailure { onFailure() }
}

fun Enum<*>.displayName(): String {
    return name
        .replace("_", " ")
        .lowercase()
        .replaceFirstChar { it.uppercase() }
}

