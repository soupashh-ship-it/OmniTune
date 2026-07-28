# OmniTune reusable verification checklist

Run this on a disposable profile whenever a core feature changes. Record device model, Android version, app version/commit, network, exact steps, expected/actual result, screenshots/logcat, and whether user data was created or removed.

## Build and release

- [ ] `./gradlew.bat clean` completes.
- [ ] `./gradlew.bat :app:assembleDebug --no-daemon --max-workers=1 --console=plain` completes.
- [ ] `./gradlew.bat :app:testDebugUnitTest --no-daemon --max-workers=1 --console=plain` has zero failures.
- [ ] `./gradlew.bat :innertube:test --no-daemon --max-workers=1 --console=plain` has zero failures.
- [ ] `./gradlew.bat :app:lintDebug :app:lintRelease --no-daemon --max-workers=1 --console=plain` completes and report XML has no errors.
- [ ] `./gradlew.bat :app:compileDebugAndroidTestKotlin :app:assembleRelease --no-daemon --max-workers=1 --console=plain` completes.
- [ ] Protected CI runs the same test/lint gate with release secrets; signed APK passes `apksigner verify --verbose`.
- [ ] Release APK has no development token/bearer secret and uses the intended signing certificate.

## Existing-user/update and database

- [ ] Install new debug/release APK over a populated prior build without clearing data.
- [ ] Run each supported Room migration with `MusicDatabaseMigrationTest` on Android.
- [ ] Confirm songs, likes, playlists/order, history, queue, tags, downloads metadata, and settings survive.
- [ ] Test a corrupt/partial schema fixture and confirm recovery preserves a backup or fails safely.
- [ ] Fresh install starts without fatal crash and empty states are accurate.

## Search and provider browsing

- [ ] Empty query shows persisted history; clear history persists after relaunch.
- [ ] Rapidly type/change filters: no stale results, loading indicator, or result count is published.
- [ ] Search a song, artist, album, playlist, and video; each result opens/plays the right target.
- [ ] Pagination appends/deduplicates correctly.
- [ ] Offline, rate-limit, region-block and provider-parser failures show an actionable error/retry state.
- [ ] Home provider sections, moods, genres, quick picks, and personalised cards identify fallback vs provider content honestly.

## Playback and queue

- [ ] Select a search/home/library track; playback starts and remains playing for at least 30 seconds.
- [ ] Pause, resume, seek forward/back, previous/next, shuffle, repeat, speed, skip silence, equalizer, and radio behave in the actual player.
- [ ] Queue edit/reorder/remove/play-next/add-to-queue has one consistent order across player and queue screen.
- [ ] Kill process/force-stop and reopen: queue, selected item, position, and intended play state restore safely.
- [ ] Network loss/reconnect, expired stream URL, unavailable track, VPN/private DNS/WARP and rate-limit paths recover or show a clear error.
- [ ] Headphone unplug, Bluetooth connect/disconnect, audio focus interruption, notification actions, lock screen and Android Auto state stay synchronized.

## Library, likes, playlists, history and stats

- [ ] Like/unlike from player, rows, menus and album/artist screens updates all surfaces and persists after relaunch.
- [ ] Liked Songs play/shuffle/repeat/search/sort/bulk add/download/remove work without duplicates.
- [ ] Create, rename, delete, edit, reorder, filter, search, bulk-remove and play local playlists; validate counts/order after relaunch.
- [ ] Sync selected YouTube playlists while signed in; a signed-out toggle cannot claim success.
- [ ] History records only the intended listening threshold; clear history, stats/year data and restore behavior agree.

## Downloads and offline playback

- [ ] Start, cancel/remove, retry, resume after relaunch, and observe progress/completed/failed states.
- [ ] Confirm duplicate prevention and Wi-Fi-only behavior.
- [ ] Download a known track; disable all network access; play, seek, pause/resume, next/previous and relaunch successfully.
- [ ] Delete the completed download; cache/database state disappears and offline playback is rejected accurately.
- [ ] Test low storage, interrupted download, expired source, and file/cache mismatch.

## Lyrics, settings and integrations

- [ ] Synced, unsynced, missing and provider-error lyrics show appropriate state; active line, auto-scroll and manual recovery work.
- [ ] Test each visible setting: control is interactive, value persists, current value is correct, a runtime reader changes behavior, and restart requirement is disclosed.
- [ ] Verify account cookie/PoToken/Last.fm/ListenBrainz encryption migration, invalid token, disconnect/logout, re-auth and error states.
- [ ] UPI action builds the expected URI and fails gracefully with no handler; copy fallback works.
- [ ] When an external UPI app is used, verify the payee, VPA, amount, currency, note, and transaction reference before any payment is approved. Do this without USB-gated app restrictions and do not treat handler launch as payment success.
- [ ] Update check/download/signature/install flow behaves correctly with no unknown-source permission surprise.

## Backup, restore, notifications and security

- [ ] Export a backup; inspect documented contents, version, checksum/integrity manifest, and excluded secrets.
- [ ] Merge and Replace restore on disposable data; verify safety backup/rollback, corrupt/empty/old/large archive handling, queue/settings behavior, duplicate prevention and offline archive staging.
- [ ] Playback foreground notification shows correct metadata/artwork/actions; Android 13+ permission denial is clear.
- [ ] Notification settings wording matches the actual categories delivered.
- [ ] Verify no sensitive token/cookie is written to logs, crash snapshots, external files, backup, source, or distributed BuildConfig.
- [ ] Review exported components, FileProvider paths, cleartext policy, backup rules, and release signing after manifest/build changes.
