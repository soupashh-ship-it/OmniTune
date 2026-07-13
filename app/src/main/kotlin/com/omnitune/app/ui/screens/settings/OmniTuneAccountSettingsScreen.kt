package com.omnitune.app.ui.screens.settings

import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.omnitune.app.LocalDatabase
import com.omnitune.app.R
import com.omnitune.app.constants.*
import com.omnitune.app.db.MusicDatabase
import com.omnitune.app.sync.parseSelectedYouTubePlaylists
import com.omnitune.app.sync.scheduleYouTubePlaylistSync
import com.omnitune.app.sync.syncYouTubePlaylist
import com.omnitune.app.ui.theme.OmniColors
import com.omnitune.app.utils.rememberPreference
import com.omnitune.innertube.YouTube
import com.omnitune.innertube.models.PlaylistItem
import com.omnitune.innertube.utils.completed
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val YTM_PLAYLISTS_BROWSE_ID = "FEmusic_liked_playlists"

@Composable
fun OmniTuneAccountSettingsScreen(navController: NavController) {
    val context = LocalContext.current
    val database = LocalDatabase.current
    val scope = rememberCoroutineScope()
    var accountName by rememberPreference(AccountNameKey, "")
    var accountEmail by rememberPreference(AccountEmailKey, "")
    var accountHandle by rememberPreference(AccountChannelHandleKey, "")
    var useLoginForBrowse by rememberPreference(UseLoginForBrowse, true)
    var ytmSync by rememberPreference(YtmSyncKey, false)
    var persistedPlaylistIds by rememberPreference(SelectedYtmPlaylistsKey, "")
    var innerTubeCookie by rememberPreference(InnerTubeCookieKey, "")
    var isSyncBusy by remember { mutableStateOf(false) }
    var showPlaylistPicker by remember { mutableStateOf(false) }
    var remotePlaylists by remember { mutableStateOf<List<PlaylistItem>>(emptyList()) }
    var selectedPlaylistIds by remember { mutableStateOf(setOf<String>()) }

    val isLoggedIn = innerTubeCookie.isNotBlank()

    SettingsSubScreenScaffold(
        title = "OmniTune Account",
        onBack = { navController.popBackStack() }
    ) {
        OmniPreferenceCard(title = "ACCOUNT") {
            if (isLoggedIn) {
                OmniPreferenceEntry(
                    title = accountName.ifBlank { "YouTube Music" },
                    description = accountEmail.ifBlank { accountHandle.ifBlank { "Signed in" } },
                    iconRes = R.drawable.ic_settings,
                    accent = OmniColors.OmniAccentPrimary,
                )
                OmniPreferenceEntry(
                    title = "Sign out",
                    description = "Remove your YouTube Music account from OmniTune",
                    iconRes = R.drawable.ic_settings,
                    accent = OmniColors.Hot,
                    onClick = {
                        // Clear account data
                        innerTubeCookie = ""
                        accountName = ""
                        accountEmail = ""
                        accountHandle = ""
                        ytmSync = false
                        scheduleYouTubePlaylistSync(context, false)
                        com.omnitune.innertube.YouTube.cookie = null
                    }
                )
            } else {
                OmniPreferenceEntry(
                    title = "Sign in to YouTube Music",
                    description = "Sign in to access your playlists, likes, and subscriptions",
                    iconRes = R.drawable.ic_settings,
                    accent = OmniColors.OmniAccentPrimary,
                    onClick = {
                        navController.navigate("login")
                    }
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        OmniPreferenceCard(title = "SYNC") {
            OmniSwitchPreference(
                title = "Use login for browsing",
                description = "Use your account for personalized recommendations and browsing",
                iconRes = R.drawable.ic_settings,
                checked = useLoginForBrowse,
                onCheckedChange = {
                    useLoginForBrowse = it
                    com.omnitune.innertube.YouTube.useLoginForBrowse = it
                }
            )
            OmniSwitchPreference(
                title = "YouTube Music Sync",
                description = if (isLoggedIn) "Import selected YouTube Music playlists" else "Sign in first to sync playlists",
                iconRes = R.drawable.ic_settings,
                checked = ytmSync,
                onCheckedChange = {
                    ytmSync = it
                    scheduleYouTubePlaylistSync(context, it)
                }
            )
            OmniPreferenceEntry(
                title = "Select playlists to sync",
                description = if (isSyncBusy) "Loading YouTube Music playlists..." else "Choose playlists from your YouTube Music library",
                iconRes = R.drawable.ic_list,
                accent = OmniColors.OmniAccentSecondary,
                onClick = if (isLoggedIn && !isSyncBusy) {
                    {
                        isSyncBusy = true
                        scope.launch(Dispatchers.IO) {
                            runCatching {
                                YouTube.library(YTM_PLAYLISTS_BROWSE_ID).completed().getOrThrow()
                                    .items.filterIsInstance<PlaylistItem>()
                            }.onSuccess { playlists ->
                                withContext(Dispatchers.Main) {
                                    remotePlaylists = playlists
                                    selectedPlaylistIds = parseSelectedYouTubePlaylists(persistedPlaylistIds)
                                        .ifEmpty { playlists.filter { it.id == "LM" }.map { it.id }.toSet() }
                                    showPlaylistPicker = true
                                }
                            }.onFailure { error ->
                                withContext(Dispatchers.Main) {
                                    Toast.makeText(context, error.message ?: "Could not load playlists", Toast.LENGTH_SHORT).show()
                                }
                            }
                            withContext(Dispatchers.Main) { isSyncBusy = false }
                        }
                    }
                } else null,
                trailing = {
                    if (isSyncBusy) {
                        CircularProgressIndicator(modifier = Modifier.height(24.dp))
                    }
                },
            )
        }

        Spacer(Modifier.height(12.dp))

        OmniPreferenceCard(title = "DATA") {
            OmniPreferenceEntry(
                title = "Backup & Restore",
                description = "Export or import your OmniTune data",
                iconRes = R.drawable.ic_settings,
                accent = OmniColors.OmniAccentSecondary,
                onClick = { navController.navigate("settings/backup_restore") }
            )
        }
    }

    if (showPlaylistPicker) {
        AlertDialog(
            onDismissRequest = { showPlaylistPicker = false },
            title = { Text("Sync playlists") },
            text = {
                if (remotePlaylists.isEmpty()) {
                    Text("No YouTube Music playlists found.")
                } else {
                    LazyColumn(modifier = Modifier.heightIn(max = 360.dp)) {
                        items(remotePlaylists, key = { it.id }) { playlist ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Checkbox(
                                    checked = playlist.id in selectedPlaylistIds,
                                    onCheckedChange = { checked ->
                                        selectedPlaylistIds = if (checked) {
                                            selectedPlaylistIds + playlist.id
                                        } else {
                                            selectedPlaylistIds - playlist.id
                                        }
                                    },
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(playlist.title)
                                    Text(
                                        playlist.songCountText ?: "Playlist",
                                        color = OmniColors.TextTertiary,
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    enabled = !isSyncBusy && selectedPlaylistIds.isNotEmpty(),
                    onClick = {
                        val selected = remotePlaylists.filter { it.id in selectedPlaylistIds }
                        persistedPlaylistIds = selectedPlaylistIds.joinToString(",")
                        showPlaylistPicker = false
                        isSyncBusy = true
                        scope.launch(Dispatchers.IO) {
                            runCatching {
                                selected.sumOf { playlist -> syncYouTubePlaylist(database, playlist) }
                            }.onSuccess { songCount ->
                                withContext(Dispatchers.Main) {
                                    ytmSync = true
                                    scheduleYouTubePlaylistSync(context, true)
                                    Toast.makeText(context, "Synced ${selected.size} playlists, $songCount songs", Toast.LENGTH_SHORT).show()
                                }
                            }.onFailure { error ->
                                withContext(Dispatchers.Main) {
                                    Toast.makeText(context, error.message ?: "Playlist sync failed", Toast.LENGTH_SHORT).show()
                                }
                            }
                            withContext(Dispatchers.Main) { isSyncBusy = false }
                        }
                    },
                ) {
                    Text("Sync")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPlaylistPicker = false }) {
                    Text("Cancel")
                }
            },
        )
    }
}
