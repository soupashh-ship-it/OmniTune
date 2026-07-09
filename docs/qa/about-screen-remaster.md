# About Screen Remaster

## Current OmniTune behavior before fix

- `Settings -> About` used a basic `OmniPreferenceCard` with app name, version text, license text, and three large buttons.
- Version name/code came from `BuildConfig`.
- Repository, license, and credits opened GitHub URLs through the shared `openUrl` helper.
- The screen did not show install date, a structured developer section, community section, or premium identity card.

## Reference findings

- The reference screen uses a large identity/version card, compact all-caps section headers, circular row artwork/icons, a support card only when it has a real UPI target, and app-info rows for install date, version code, and license.
- The reference hardcodes its own developer, Discord, UPI, and inspiration data. OmniTune does not copy those values.

## OmniTune implementation

- Added a large OmniTune identity card using the bundled launcher icon and dynamic `BuildConfig.VERSION_NAME`.
- Shows `DEBUG` or `RELEASE` based on `BuildConfig.DEBUG`; no fake `STABLE` label is hardcoded.
- Replaced duplicate buttons with reusable row treatment for external links and info rows.
- Added a safe `Context.openExternalUrl(url)` helper that validates schemes and returns false on launch failure.
- Failed external launches show a snackbar message instead of crashing.

## Verified entries

| Name | Reason for attribution | URL | Verified source |
| --- | --- | --- | --- |
| soupashh-ship-it | Repository owner and OmniTune maintainer profile | https://github.com/soupashh-ship-it | `git remote -v`; HTTP 200 profile check |
| Velune | `README.md` and `CREDITS.md` say OmniTune includes code derived from or inspired by Velune | https://github.com/nikhilvishwakarma00/Velune | `CREDITS.md`; `git ls-remote` |
| ArchiveTune | Upstream framework inspiration acknowledged by the reference About implementation | https://github.com/koiverse/ArchiveTune | Reference `AboutScreen.kt`; `git ls-remote` |
| GitHub Repository | OmniTune source repository | https://github.com/soupashh-ship-it/OmniTune | `git remote -v`; `git ls-remote` |
| License | Repository license is GPL-3.0 | https://www.gnu.org/licenses/gpl-3.0.html | `LICENSE`; `README.md` |

## Omitted rows

- Discord server: omitted because no valid OmniTune invite URL was found.
- Support card: omitted because no valid OmniTune donation destination was found.
- Developer avatar: omitted because no bundled personal avatar asset exists; the screen uses a clean initials treatment instead.

## App info

- Installed date is read from `PackageManager` `firstInstallTime` and formatted with the user locale.
- Version name uses `BuildConfig.VERSION_NAME`.
- Version code uses `BuildConfig.VERSION_CODE`.
- License row opens the official GPL-3.0 license page.

## Tests

- `AboutMetadataTest`
  - Verifies repository/developer URLs use the real owner.
  - Verifies Discord/support rows remain hidden without real destinations.
  - Verifies inspiration URLs are present and non-placeholder.
  - Verifies install-date formatting and invalid-date fallback.

## Manual QA

- Runtime device QA was not completed during implementation.
- `adb devices` was checked and no device was attached.
- Final command verification passed: `clean assembleDebug`, `testDebugUnitTest`, and `lintDebug`.
- If a device is available, manually open Settings -> About and test every visible external row.

## Known limitations

- No Discord row is shown until a valid OmniTune community invite exists.
- No support card is shown until a real support or donation destination exists.
- The ArchiveTune row is marked as upstream/reference-project inspiration, not direct OmniTune code derivation.
