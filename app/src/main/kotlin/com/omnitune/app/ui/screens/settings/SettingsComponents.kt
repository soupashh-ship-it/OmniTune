package com.omnitune.app.ui.screens.settings

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.Preferences
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.omnitune.app.BuildConfig
import com.omnitune.app.R
import com.omnitune.app.ui.component.OmniChrome
import com.omnitune.app.ui.screens.SettingsViewModel
import com.omnitune.app.ui.theme.OmniColors
import com.omnitune.app.ui.theme.OmniShapes
import com.omnitune.app.ui.theme.OmniSpacing
import com.omnitune.app.utils.rememberPreference





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


@Suppress("UNCHECKED_CAST")
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
    var value by rememberPreference(key as Preferences.Key<Int>, defaultValue)
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
fun FloatPreferenceSliderRow(
    label: String,
    description: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int = 0,
    valueFormat: (Float) -> String = { it.toString() },
) {
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
            value = value,
            onValueChange = onValueChange,
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
    return Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
        .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
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

// ─── OmniTune preference components ──────────────────────────────────

@Composable
fun OmniPreferenceIcon(
    iconRes: Int,
    accent: Color,
    size: Dp = 32.dp,
    iconSize: Dp = 16.dp,
) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(OmniShapes.Pill)
            .background(
                Brush.linearGradient(
                    listOf(accent.copy(alpha = 0.22f), accent.copy(alpha = 0.08f))
                )
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = null,
            tint = accent,
            modifier = Modifier.size(iconSize),
        )
    }
}

@Composable
fun OmniPreferenceEntry(
    title: String,
    description: String? = null,
    iconRes: Int? = null,
    accent: Color = OmniColors.TextPrimary,
    onClick: (() -> Unit)? = null,
    trailing: @Composable RowScope.() -> Unit = {},
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(OmniShapes.Small)
            .then(
                if (onClick != null)
                    Modifier.clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = androidx.compose.material3.ripple(
                            bounded = true,
                            color = Color.White.copy(alpha = 0.08f),
                        ),
                        onClick = onClick,
                    )
                else Modifier
            )
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (iconRes != null) {
            OmniPreferenceIcon(iconRes = iconRes, accent = accent)
            Spacer(Modifier.width(OmniSpacing.compact))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = OmniColors.TextPrimary,
            )
            if (description != null) {
                Spacer(Modifier.height(1.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.labelSmall,
                    color = OmniColors.TextTertiary,
                )
            }
        }
        trailing()
    }
}

@Composable
fun OmniSwitchPreference(
    title: String,
    description: String? = null,
    iconRes: Int? = null,
    accent: Color = OmniColors.TextPrimary,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    OmniPreferenceEntry(
        title = title,
        description = description,
        iconRes = iconRes,
        accent = accent,
        onClick = { onCheckedChange(!checked) },
        trailing = {
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = OmniColors.OmniAccentPrimary,
                    uncheckedThumbColor = OmniColors.TextTertiary,
                    uncheckedTrackColor = OmniColors.OmniGlassMedium,
                    uncheckedBorderColor = OmniColors.OmniGlassBorderSubtle,
                ),
            )
        },
    )
}

@Composable
fun <T : Enum<T>> OmniEnumPreference(
    title: String,
    description: String? = null,
    iconRes: Int? = null,
    accent: Color = OmniColors.TextPrimary,
    selectedValue: T,
    values: List<T>,
    valueText: (T) -> String = { it.displayName() },
    onValueSelected: (T) -> Unit,
) {
    var showDialog by remember { mutableStateOf(false) }

    OmniPreferenceEntry(
        title = title,
        description = description,
        iconRes = iconRes,
        accent = accent,
        onClick = { showDialog = true },
        trailing = {
            Row(
                modifier = Modifier
                    .clip(OmniShapes.Pill)
                    .border(1.dp, OmniColors.OmniAccentPrimary.copy(alpha = 0.36f), OmniShapes.Pill)
                    .padding(horizontal = OmniSpacing.compact, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = valueText(selectedValue),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = OmniColors.OmniAccentSecondary,
                )
                Spacer(Modifier.width(OmniSpacing.micro))
                Icon(
                    painter = painterResource(R.drawable.ic_arrow_back),
                    contentDescription = null,
                    tint = OmniColors.TextTertiary.copy(alpha = 0.4f),
                    modifier = Modifier
                        .size(12.dp)
                        .graphicsLayer { rotationZ = 180f },
                )
            }
        },
    )

    if (showDialog) {
        EnumSelectionDialog(
            title = title,
            options = values,
            current = selectedValue,
            onDismiss = { showDialog = false },
            onSelected = { selected ->
                showDialog = false
                onValueSelected(selected)
            },
        )
    }
}

@Composable
fun OmniPreferenceGroupTitle(
    title: String,
) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.SemiBold,
        color = OmniColors.OmniAccentSecondary,
        modifier = Modifier
            .padding(top = 12.dp)
            .padding(vertical = 6.dp),
    )
}

@Composable
fun OmniPreferenceCard(
    title: String? = null,
    content: @Composable () -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        title?.let {
            OmniPreferenceGroupTitle(it)
        }
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, OmniColors.OmniAccentPrimary.copy(alpha = 0.26f), OmniShapes.Medium),
            shape = OmniShapes.Medium,
            colors = CardDefaults.cardColors(containerColor = OmniColors.SurfaceRaised),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        ) {
            Column(modifier = Modifier.padding(vertical = 2.dp)) {
                content()
            }
        }
    }
}

// ─── Sub-screen scaffold ─────────────────────────────────────────────

@Composable
fun SettingsSubScreenScaffold(
    title: String,
    onBack: () -> Unit,
    content: @Composable () -> Unit,
) {
    val (referenceTitle, subtitle) = settingsReferenceHeading(title)
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(OmniColors.OmniBackgroundBase)
            .background(OmniColors.BackgroundGradient),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
        ) {
            Spacer(modifier = Modifier.statusBarsPadding())
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = OmniSpacing.screenHorizontalCompact, vertical = OmniSpacing.compact),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(OmniShapes.Pill)
                        .border(1.dp, OmniColors.OmniAccentPrimary.copy(alpha = 0.25f), OmniShapes.Pill)
                        .clickable(onClick = onBack),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_arrow_back),
                        contentDescription = "Back",
                        tint = OmniColors.TextPrimary,
                        modifier = Modifier.size(18.dp),
                    )
                }
                Spacer(modifier = Modifier.width(OmniSpacing.small))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = referenceTitle,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = OmniColors.TextPrimary,
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = OmniColors.TextSecondary,
                    )
                }
            }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = OmniSpacing.screenHorizontalCompact),
            ) {
                content()
                Spacer(modifier = Modifier.height(OmniChrome.BottomContentPaddingWithPlayer))
            }
        }
    }
}

private fun settingsReferenceHeading(title: String): Pair<String, String> = when (title) {
    "Playback & Audio" -> "Playback" to "Audio quality, crossfade, equalizer"
    "Appearance" -> "Player Appearance" to "Now playing screen, themes, animations"
    "Behavior" -> "Behavior" to "Autoplay, resume, headset & car"
    "Downloads" -> "Downloads" to "Offline library and automatic downloads"
    "Library" -> "Library" to "Shortcuts, layout, and discovery"
    "Content" -> "Library" to "Manage tabs, hidden songs & filters"
    "Parental Controls" -> "Parental Controls" to "Limit explicit content and manage access"
    "Notifications" -> "Notifications" to "Manage alerts and in-app messages"
    "Storage" -> "Storage" to "Cache, downloads, and device storage"
    "Scrobbling" -> "Scrobbling & Integrations" to "Last.fm and ListenBrainz services"
    "Updates" -> "Updates" to "Check for updates and release notes"
    else -> title to "OmniTune settings"
}
