package com.omnitune.app.ui.screens.settings

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.omnitune.app.R
import com.omnitune.app.constants.*
import com.omnitune.app.ui.theme.OmniColors
import com.omnitune.app.utils.rememberPreference

@Composable
fun OmniTuneAccountSettingsScreen(navController: NavController) {
    var accountName by rememberPreference(AccountNameKey, "")
    var accountEmail by rememberPreference(AccountEmailKey, "")
    var accountHandle by rememberPreference(AccountChannelHandleKey, "")
    var useLoginForBrowse by rememberPreference(UseLoginForBrowse, true)
    var ytmSync by rememberPreference(YtmSyncKey, false)
    var innerTubeCookie by rememberPreference(InnerTubeCookieKey, "")

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
                description = "Sync liked songs, playlists, and subscriptions with YouTube Music",
                iconRes = R.drawable.ic_settings,
                checked = ytmSync,
                onCheckedChange = { ytmSync = it }
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
}
