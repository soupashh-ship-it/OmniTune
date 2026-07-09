package com.omnitune.app.update

import com.omnitune.app.BuildConfig

object AppChangelog {
    val bundled = ChangelogRelease(
        versionName = BuildConfig.VERSION_NAME,
        releaseName = "OmniTune v${BuildConfig.VERSION_NAME}",
        source = ChangelogSource.Bundled,
        body = """
# OmniTune v0.11.2

This release focuses on playlist motion polish and makes the Updates changelog screen show real release changes instead of placeholder text.

## Fixes & Improvements

- Made playlist detail scrolling feel continuous by moving the hero, actions, custom order list, and suggestions into one scroll surface.
- Added a floating playlist toolbar with back, search, and menu controls that stays available while the playlist scrolls.
- Added a collapsing toolbar title that appears after the playlist header scrolls away.
- Added smooth lazy-list item placement for playlist rows and suggestions.
- Improved playlist detail bottom padding so content is less likely to sit behind system navigation or the mini player area.
- Replaced the Changelog placeholder in Settings with a real release-notes screen.
- Added bundled release notes for the installed app version so changes are visible even before checking for updates.
- Added a GitHub refresh action on the Changelog screen to show the latest published release notes when online.

## Verification

- `assembleDebug`: passed
- `testDebugUnitTest`: passed
- `lintDebug`: passed

## Build

- Version: `0.11.2`
- Version code: `52`
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
