package com.omnitune.app.update

import com.omnitune.app.BuildConfig

object AppChangelog {
    val bundled = ChangelogRelease(
        versionName = BuildConfig.VERSION_NAME,
        releaseName = "OmniTune v${BuildConfig.VERSION_NAME}",
        source = ChangelogSource.Bundled,
        body = """
# OmniTune v0.11.3

This release polishes the full-screen player and remasters the Settings About page with accurate app metadata, verified links, and a cleaner OmniTune-native layout.

## Fixes & Improvements

- Fixed the full-screen player header so it shows the current song title instead of the app name.
- Added inline synced lyric subtitles under the main song title when real LRC or TTML lyrics are available.
- Improved inline lyric readability with album-art-matched accent color, smooth color transitions, and a subtle tinted backing on the active lyric.
- Kept artist and album metadata as the fallback when lyrics are missing, loading, failed, or unsynced.
- Added smooth lyric line transitions and a tap target that opens the existing full lyrics sheet.
- Refined album-art-based dynamic song colors with stronger swatch filtering, dark-safe tone mapping, richer player gradients, and readable control surfaces.
- Remastered Settings > About with a premium OmniTune identity card, verified developer and inspiration links, dynamic install/version details, and accurate GPL-3.0 license information.

## Verification

- `clean assembleDebug`: passed
- `testDebugUnitTest`: passed
- `lintDebug`: passed

## Build

- Version: `0.11.3`
- Version code: `53`
        """.trimIndent(),
    )
}

data class ChangelogRelease(
    val versionName: String,
    val releaseName: String,
    val source: ChangelogSource,
    val body: String,
    val publishedAt: String? = null,
)

enum class ChangelogSource {
    Bundled,
    GitHub,
}
