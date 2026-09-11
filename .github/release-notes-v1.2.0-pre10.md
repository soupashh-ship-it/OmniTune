OmniTune 1.2.0-pre10 prerelease.

This supersedes v1.2.0-pre9 with a larger remediation batch focused on real music-video metadata, downloaded video behavior, and PiP reliability.

Highlights since pre9:
- Persisted music-video metadata in Room, backups, restore flows, search/result mapping, download grouping, and local library filters.
- Fixed downloaded videos so the Videos tab is driven by actual video metadata instead of playlist/set-video side effects.
- Hardened PiP playback actions by mapping them through the playback-service action namespace and starting the service safely from the receiver.
- Made PiP parameter updates and PiP entry failures visible in logs instead of swallowing broad exceptions.
- Added regression coverage for video metadata mapping, download item grouping, local video filtering, PiP action mapping, backup schema drift, and database migration behavior.

Validation:
- Local: assembleDebug, testDebugUnitTest, lintDebug, and assembleRelease were run before tagging.
- GitHub Actions Android Release verifies tests, Android-test compilation, lintRelease, signs the release APK, verifies it with apksigner, and uploads the APK plus SHA-256 file.
- Package: com.omnitune.app
- Version: 1.2.0-pre10
- Version code: 128

Manual install note:
This prerelease APK is attached for manual download/install. Android may require allowing installs from the browser or file manager.
