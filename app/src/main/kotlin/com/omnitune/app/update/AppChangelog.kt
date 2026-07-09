package com.omnitune.app.update

import com.omnitune.app.BuildConfig

object AppChangelog {
    val bundled = ChangelogRelease(
        versionName = BuildConfig.VERSION_NAME,
        releaseName = "OmniTune v${BuildConfig.VERSION_NAME}",
        source = ChangelogSource.Bundled,
        body = """
# OmniTune v0.11.6

This hotfix ships the Settings visual refresh and improves Home artwork sharpness for the large carousel above Quick Picks.

## Fixes & Improvements

- Remastered the Settings hub with a cleaner OmniTune identity row, flatter settings list, lighter section structure, and less bulky card styling.
- Preserved all existing Settings destinations while improving readability, spacing, chevrons, icon treatment, and mini-player-safe bottom padding.
- Fixed Home carousel thumbnail upgrades for `i.ytimg.com` URLs so large artwork above Quick Picks can load `maxresdefault` images instead of staying on lower-resolution defaults.
- Fixed `yt3.ggpht.com` artwork resizing so the requested size replaces the old size parameter instead of appending a broken suffix.
- Added focused tests for YouTube thumbnail URL upgrades.

## Verification

- `clean assembleDebug`: passed
- `testDebugUnitTest`: passed
- `lintDebug`: passed

## Build

- Version: `0.11.6`
- Version code: `56`
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
