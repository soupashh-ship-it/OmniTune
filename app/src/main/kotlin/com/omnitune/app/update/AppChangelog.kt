package com.omnitune.app.update

import com.omnitune.app.BuildConfig

object AppChangelog {
    val bundled = ChangelogRelease(
        versionName = BuildConfig.VERSION_NAME,
        releaseName = "OmniTune v${BuildConfig.VERSION_NAME}",
        source = ChangelogSource.Bundled,
        body = """
# OmniTune v${BuildConfig.VERSION_NAME}

OmniTune 1.5.1 focuses on account sign-in, playlist import, update checks, and final app identity polish after the 1.5.0 stable release.

## Fixes

- Fixed YouTube Music sign-in persistence by saving session cookies, visitor data, data-sync ID, and account metadata before leaving the login screen.
- Added support for secure YouTube SAPISID cookie variants used by newer sign-in flows.
- Improved private or account-owned YouTube Music playlist import after sign-in, with clearer errors for unavailable playlists.
- Allowed playlist import from pasted playlist IDs as well as full YouTube and YouTube Music links.
- Added a visible Stable / Prerelease selector to the in-app updater.
- Fixed prerelease update checks so they can also offer newer stable releases.
- Reset older launcher aliases to the new default OmniTune app icon on first launch after this update.
- Updated the playback notification resources to use the new transparent OmniTune notification mark.

## Verification

- `innertube:test`: passed
- `testDebugUnitTest`: passed
- `compileDebugKotlin`: passed
- `compileDebugAndroidTestKotlin`: passed
- `lintRelease`: passed
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
