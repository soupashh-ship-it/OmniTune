package com.omnitune.app.ui.screens.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import com.omnitune.app.R
import com.omnitune.app.constants.EnableBetterLyricsKey
import com.omnitune.app.constants.EnableKugouKey
import com.omnitune.app.constants.EnableLrcLibKey
import com.omnitune.app.constants.EnableSimpMusicLyricsKey
import com.omnitune.app.constants.LyricsScrollKey
import com.omnitune.app.ui.theme.OmniColors
import com.omnitune.app.utils.rememberPreference

@Composable
fun LyricsSettings() {
    var lrcLib by rememberPreference(EnableLrcLibKey, true)
    var kugou by rememberPreference(EnableKugouKey, true)
    var betterLyrics by rememberPreference(EnableBetterLyricsKey, true)
    var simpMusic by rememberPreference(EnableSimpMusicLyricsKey, true)
    var autoScroll by rememberPreference(LyricsScrollKey, true)

    OmniPreferenceCard(title = "Display") {
        OmniSwitchPreference(
            title = "Auto-scroll synced lyrics",
            description = "Keep the current lyric line in view during playback",
            iconRes = R.drawable.ic_lyrics,
            accent = OmniColors.OmniAccentSecondary,
            checked = autoScroll,
            onCheckedChange = { autoScroll = it },
        )
    }

    OmniPreferenceCard(title = "Lyrics providers") {
        OmniSwitchPreference(
            title = "LrcLib",
            description = "Use the existing LrcLib provider",
            iconRes = R.drawable.ic_list,
            accent = OmniColors.Hot,
            checked = lrcLib,
            onCheckedChange = { lrcLib = it },
        )
        OmniSwitchPreference(
            title = "KuGou",
            description = "Use the existing KuGou provider",
            iconRes = R.drawable.ic_list,
            accent = OmniColors.Hot,
            checked = kugou,
            onCheckedChange = { kugou = it },
        )
        OmniSwitchPreference(
            title = "Better Lyrics",
            description = "Use the existing Better Lyrics provider",
            iconRes = R.drawable.ic_list,
            accent = OmniColors.Hot,
            checked = betterLyrics,
            onCheckedChange = { betterLyrics = it },
        )
        OmniSwitchPreference(
            title = "SimpMusic",
            description = "Use the existing SimpMusic provider",
            iconRes = R.drawable.ic_list,
            accent = OmniColors.Hot,
            checked = simpMusic,
            onCheckedChange = { simpMusic = it },
        )
    }
}
