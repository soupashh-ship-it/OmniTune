package com.omnitune.app.ui.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

data class NewAction(
    val icon: @Composable () -> Unit,
    val text: String,
    val onClick: () -> Unit
)

/**
 * Canonical factory for menu grid tiles. Menus must build their [NewAction]s
 * through this (or a thin wrapper of it) so icon sizing, tinting and the
 * dismiss-then-act order stay consistent across the app.
 *
 * Deliberately not @Composable so it can be used inside `remember` blocks;
 * theming resolves when the icon slot renders.
 */
fun menuAction(
    iconRes: Int,
    label: String,
    onDismiss: (() -> Unit)? = null,
    iconSize: Dp = 28.dp,
    onClick: () -> Unit,
): NewAction = NewAction(
    icon = {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(iconSize),
        )
    },
    text = label,
    onClick = {
        onDismiss?.invoke()
        onClick()
    },
)

@Composable
fun NewActionGrid(
    actions: List<NewAction>,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        actions.forEach { action ->
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .weight(1f)
                    .clickable(onClick = action.onClick)
                    .padding(vertical = 8.dp)
            ) {
                action.icon()
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = action.text,
                    style = MaterialTheme.typography.labelMedium
                )
            }
        }
    }
}
