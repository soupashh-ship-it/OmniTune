package com.omnitune.app.update

import com.omnitune.app.BuildConfig

object AppChangelog {
    val bundled = ChangelogRelease(
        versionName = BuildConfig.VERSION_NAME,
        releaseName = "OmniTune v${BuildConfig.VERSION_NAME}",
        source = ChangelogSource.Bundled,
        body = """
# OmniTune v0.13.4

Short donation reliability hotfix.

## Fixes

- Added a visible UPI ID in Settings > About donation card.
- Added a Copy UPI fallback for payment apps that reject the direct UPI intent.
- Kept donation amount entry inside the user's UPI app so donors can choose their own amount.

## Verification

- Focused About metadata unit test: passed
- `compileDebugKotlin`: passed

## Build

- Version: `0.13.4`
- Version code: `73`
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
