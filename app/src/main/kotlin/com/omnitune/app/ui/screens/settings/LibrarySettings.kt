package com.omnitune.app.ui.screens.settings

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.omnitune.app.R
import com.omnitune.app.constants.OmniLibraryDesign
import com.omnitune.app.constants.OmniLibraryDesignKey
import com.omnitune.app.constants.ShowCachedPlaylistKey
import com.omnitune.app.constants.ShowDownloadedPlaylistKey
import com.omnitune.app.constants.ShowLikedPlaylistKey
import com.omnitune.app.constants.ShowTagsInLibraryKey
import com.omnitune.app.constants.ShowTopPlaylistKey
import com.omnitune.app.ui.theme.OmniColors
import com.omnitune.app.utils.rememberEnumPreference
import com.omnitune.app.utils.rememberPreference

@Composable
fun LibrarySettings() {
    var libraryDesign by rememberEnumPreference(OmniLibraryDesignKey, OmniLibraryDesign.DEFAULT)
    var showLiked by rememberPreference(ShowLikedPlaylistKey, true)
    var showDownloads by rememberPreference(ShowDownloadedPlaylistKey, true)
    var showTop by rememberPreference(ShowTopPlaylistKey, true)
    var showCached by rememberPreference(ShowCachedPlaylistKey, true)
    var showFolders by rememberPreference(ShowTagsInLibraryKey, true)

    OmniPreferenceCard(title = "ORGANIZATION") {
        OmniEnumPreference(
            title = "Library layout",
            description = "Choose the standard hub or a compact list",
            iconRes = R.drawable.ic_grid,
            accent = OmniColors.OmniAccentPrimary,
            selectedValue = libraryDesign,
            values = OmniLibraryDesign.entries,
            valueText = {
                when (it) {
                    OmniLibraryDesign.DEFAULT -> "Standard"
                    OmniLibraryDesign.COMPACT_LIST -> "Compact"
                }
            },
            onValueSelected = { libraryDesign = it },
        )
        OmniSwitchPreference(
            title = "Playlist folders",
            description = "Show folder filters on the playlists screen",
            iconRes = R.drawable.ic_list,
            accent = OmniColors.OmniAccentPrimary,
            checked = showFolders,
            onCheckedChange = { showFolders = it },
        )
    }

    Spacer(Modifier.height(12.dp))

    OmniPreferenceCard(title = "QUICK ACCESS") {
        OmniSwitchPreference(
            title = "Liked songs",
            description = "Show the liked-songs shortcut",
            iconRes = R.drawable.ic_favorite,
            accent = OmniColors.OmniAccentPrimary,
            checked = showLiked,
            onCheckedChange = { showLiked = it },
        )
        OmniSwitchPreference(
            title = "Downloads",
            description = "Show the offline-downloads shortcut",
            iconRes = R.drawable.ic_download,
            accent = OmniColors.OmniAccentPrimary,
            checked = showDownloads,
            onCheckedChange = { showDownloads = it },
        )
        OmniSwitchPreference(
            title = "Top songs",
            description = "Show the generated top-songs shortcut",
            iconRes = R.drawable.ic_insights,
            accent = OmniColors.OmniAccentPrimary,
            checked = showTop,
            onCheckedChange = { showTop = it },
        )
        OmniSwitchPreference(
            title = "Cached songs",
            description = "Show the cached-songs shortcut",
            iconRes = R.drawable.ic_storage,
            accent = OmniColors.OmniAccentPrimary,
            checked = showCached,
            onCheckedChange = { showCached = it },
        )
    }
}
