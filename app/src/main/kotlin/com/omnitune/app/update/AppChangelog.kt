package com.omnitune.app.update

import com.omnitune.app.BuildConfig

object AppChangelog {
    val bundled = ChangelogRelease(
        versionName = BuildConfig.VERSION_NAME,
        releaseName = "OmniTune v${BuildConfig.VERSION_NAME}",
        source = ChangelogSource.Bundled,
        body = """
# OmniTune v${BuildConfig.VERSION_NAME}

OmniTune polish prerelease.

## Fixes

- Restored a visible App Updates entry in Settings and defaulted prerelease builds to the prerelease update channel.
- Updated the default launcher, splash, in-app, and notification logo assets to the new OmniTune mark.
- Rebranded the first-run welcome screen so its logo and copy are clearly OmniTune-owned.
- Restored real download state, retry, cancel, delete, storage, and video mapping.
- Persisted music-video metadata so downloaded video tabs, local filtering, backups, and PiP gating stay consistent.
- Repaired PiP, deep links, media library browsing, output routing, volume-key behavior, and keep-screen-on handling.
- Wired customization, launcher icon variants, route-aware mini-player insets, prerelease updates, and the AI equalizer shortcut.
- Wired SponsorBlock skipping into playback and retired inert Discord/AI provider settings.
- Cleaned stale Last.fm settings state and incomplete localization resources.
- Reduced lint issues from blocking errors to non-blocking warnings.

## Verification

- `assembleDebug`: passed
- `testDebugUnitTest`: passed
- `lintDebug`: passed
- `assembleRelease`: passed

## Build

- Version: `${BuildConfig.VERSION_NAME}`
- Version code: `${BuildConfig.VERSION_CODE}`
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
