package com.omnitune.app.ui.screens.settings

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.omnitune.app.ui.component.LeadingIconBox
import com.omnitune.app.ui.component.SettingsCard as PortedSettingsCard
import com.omnitune.app.ui.component.SettingsSectionTitle as PortedSettingsSectionTitle
import com.omnitune.app.ui.component.SettingsSwitchRow as PortedSettingsSwitchRow
import com.omnitune.app.ui.theme.SquircleShape

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PoTokenScreen(
    navController: NavController,
    viewModel: PoTokenViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var gvsInput by rememberSaveable { mutableStateOf("") }
    var playerInput by rememberSaveable { mutableStateOf("") }
    var initializedInputs by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(state.gvsToken, state.playerToken) {
        if (!initializedInputs) {
            gvsInput = state.gvsToken
            playerInput = state.playerToken
            initializedInputs = true
        }
    }

    LaunchedEffect(state.message, state.errorMessage) {
        val message = state.message ?: state.errorMessage
        if (!message.isNullOrBlank()) {
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            viewModel.clearMessages()
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("YouTube PO Token", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 100.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                PortedSettingsSectionTitle(
                    title = "Status",
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                )
                PortedSettingsCard {
                    PortedSettingsSwitchRow(
                        title = "Web PO tokens",
                        subtitle = if (state.webClientPoTokensEnabled) {
                            "Stored tokens are used for Web playback requests"
                        } else {
                            "Playback falls back to OmniTune's built-in token path"
                        },
                        icon = Icons.Default.Settings,
                        checked = state.webClientPoTokensEnabled,
                        onCheckedChange = viewModel::setWebClientPoTokensEnabled,
                        subtitleMaxLines = 2,
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                    ListItem(
                        headlineContent = { Text("Visitor Data", fontWeight = FontWeight.Medium) },
                        supportingContent = { Text(maskPoTokenSecret(state.visitorData), maxLines = 1) },
                        leadingContent = { LeadingIconBox(Icons.Default.Key) },
                        colors = ListItemDefaults.colors(containerColor = androidx.compose.ui.graphics.Color.Transparent),
                    )
                }
            }

            item {
                PortedSettingsSectionTitle(
                    title = "Manual Tokens",
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                )
                PortedSettingsCard {
                    Column(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        PoTokenTextField(
                            value = gvsInput,
                            onValueChange = { gvsInput = it },
                            label = "GVS token",
                        )
                        PoTokenTextField(
                            value = playerInput,
                            onValueChange = { playerInput = it },
                            label = "Player token",
                        )

                        Row(modifier = Modifier.fillMaxWidth()) {
                            TextButton(
                                onClick = {
                                    gvsInput = ""
                                    playerInput = ""
                                    viewModel.clearTokens()
                                },
                            ) {
                                Text("Clear")
                            }
                            Spacer(modifier = Modifier.weight(1f))
                            Button(
                                onClick = { viewModel.saveTokens(gvsInput, playerInput) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary,
                                ),
                                shape = SquircleShape,
                            ) {
                                Text("Save", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            item {
                PortedSettingsSectionTitle(
                    title = "Session",
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                )
                PortedSettingsCard {
                    Column(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Button(
                            onClick = viewModel::refreshVisitorData,
                            enabled = !state.isRefreshingVisitorData,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.secondary,
                                contentColor = MaterialTheme.colorScheme.onSecondary,
                            ),
                            shape = SquircleShape,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            if (state.isRefreshingVisitorData) {
                                CircularProgressIndicator(
                                    modifier = Modifier.height(18.dp).width(18.dp),
                                    strokeWidth = 2.dp,
                                )
                                Spacer(Modifier.width(8.dp))
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                )
                                Spacer(Modifier.width(8.dp))
                            }
                            Text("Refresh Visitor Data", fontWeight = FontWeight.Bold)
                        }
                        Button(
                            onClick = viewModel::generateSessionToken,
                            enabled = !state.isRefreshingVisitorData,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary,
                            ),
                            shape = SquircleShape,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text("Generate Session Token", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PoTokenTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = true,
        visualTransformation = PasswordVisualTransformation(),
        shape = SquircleShape,
        modifier = Modifier.fillMaxWidth(),
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = MaterialTheme.colorScheme.onSurface,
            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
            cursorColor = MaterialTheme.colorScheme.primary,
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
            focusedLabelColor = MaterialTheme.colorScheme.primary,
            unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
        ),
    )
}

private fun maskPoTokenSecret(value: String): String =
    when {
        value.isBlank() -> "Not set"
        value.length <= 12 -> "Saved (${value.length} chars)"
        else -> "${value.take(6)}...${value.takeLast(4)}"
    }
