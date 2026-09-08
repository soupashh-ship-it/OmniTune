package com.omnitune.app.ui.screens.settings

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import com.omnitune.app.ui.component.SettingsCard as PortedSettingsCard
import com.omnitune.app.ui.component.SettingsRow as PortedSettingsRow
import com.omnitune.app.ui.component.SettingsSwitchRow as PortedSettingsSwitchRow

@Composable
fun SettingsCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    PortedSettingsCard(
        modifier = modifier,
        content = content,
    )
}

@Composable
fun SettingsNavRow(
    title: String,
    subtitle: String? = null,
    icon: ImageVector? = null,
    trailingText: String? = null,
    onClick: () -> Unit,
) {
    PortedSettingsRow(
        title = title,
        subtitle = subtitle,
        subtitleMaxLines = 2,
        icon = icon,
        onClick = onClick,
        showChevron = trailingText == null,
        trailingContent = trailingText?.let { text ->
            {
                Text(
                    text = text,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        },
    )
}

@Composable
fun SettingsSwitchRow(
    title: String,
    subtitle: String? = null,
    icon: ImageVector? = null,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean = true,
) {
    PortedSettingsSwitchRow(
        title = title,
        checked = checked,
        onCheckedChange = onCheckedChange,
        icon = icon,
        subtitle = subtitle,
        subtitleMaxLines = 2,
        enabled = enabled,
    )
}
