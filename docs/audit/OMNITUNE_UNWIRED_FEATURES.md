# OmniTune unwired, static, and unreachable inventory

> Historical baseline note (2026-07-28): Together, Discord RPC/Kizzy, and the duplicate
> `ForYouSuggestionEngine` were subsequently removed; the fixed-artist Home fallback was
> removed; backup copy/metadata and signed-out sync behavior were corrected. Retired preference
> values now have a scoped cleanup migration. Retain this document as the evidence that prompted
> the work, not as a current feature inventory.

This inventory is evidence-based static analysis of the audited checkout. It does not call a visible screen “working” merely because its composable exists. Runtime was unavailable, so non-proven UI flows are not included as verified.

## Highest user impact

| ID | Status | Finding | Evidence | User impact | Resolution |
| --- | --- | --- | --- | --- | --- |
| UNW-001 | BROKEN | Completed download cache is not the playback cache. | `DownloadUtil.kt:62-82` writes `downloadCache`; `PlayerFactory.kt:97-100` reads `playbackCache`; resolver claims offline hit at `StreamUrlResolver.kt:67-80`. | Offline playback cannot access completed bytes. | Introduce cache routing/unify correctly; device-test offline playback. |
| UNW-002 | UI-ONLY | Backup UI says it backs up preferences, but snapshot contains only `BackupSettingsSection()` metadata and no queue. | `SettingsScreen.kt:118`; `OmniBackupRepository.kt:123-168`; `OmniBackupModels.kt:51-53`. | Users can believe settings/queue are recoverable when they are not. | Correct wording; implement/version selected preferences and queue only after safety design. |
| UNW-003 | PARTIALLY IMPLEMENTED | Notification route is system-media-control help, not application alerts/in-app messages. | `OmniNavGraph.kt:704`, `NotificationSettings.kt`, `SettingsScreen.kt:94`. | Product notification expectation is unfulfilled. | Rename scope or implement actual categories/delivery. |
| UNW-004 | BACKEND-ONLY | Together listening-party client/server/online API have no UI route or external caller. | `app/together/*.kt`; `rg` finds only declarations inside the package; no Together nav destination. | Large dormant feature footprint. | Remove or ship a complete, tested flow. |
| UNW-005 | BACKEND-ONLY | `ForYouSuggestionEngine` is a second recommendation implementation with no consumer. | only declaration reference: `utils/ForYouSuggestionEngine.kt:32`. | Maintenance drift; future engineers may modify dead recommendations. | Consolidate with `HomeRecommendationEngine` or delete. |
| UNW-006 | OBSOLETE OR UNUSED | Discord presence code is injected but forcibly disabled/cleared at service start; no settings path exists. | `MusicService.kt:397-404`; `discord/*.kt`; only UI Discord reference is community URL. | Dead code/legacy preference and privacy surface. | Remove/migrate in dedicated cleanup. |
| UNW-007 | PARTIALLY IMPLEMENTED | Cold-start home recommendations are generic popular-artist searches, not personalised provider recommendations. | `HomeDiscoveryViewModel.kt:568-608`. | "For you" can be generic/misaligned. | Label as discovery and improve fallback/persistence. |
| UNW-008 | PARTIALLY IMPLEMENTED | YouTube Sync switch is interactive while signed out; worker then returns success without syncing. | `OmniTuneAccountSettingsScreen.kt:130-138`; `YouTubePlaylistSync.kt:72-75`. | A switch can look enabled without any sync result. | Disable/annotate when unauthenticated; show explicit status. |

## Static/reference-driven home content

| Status | Surface | Evidence | Meaning |
| --- | --- | --- | --- |
| PARTIALLY IMPLEMENTED | Home shelves/moods/genres | Initial `HomeDiscoveryUiState` references `HomeDefaultCatalog` in `HomeDiscoveryViewModel.kt:54-63`; catalog cards carry query metadata. | The content is not fake track data, but the initial cards/labels are static and hydrate through generic/provider search. |
| PARTIALLY IMPLEMENTED | Quick Picks | `startQuickPickHydration` searches local signals, then fixed popular artist queries on no signal (`HomeDiscoveryViewModel.kt:532-608`). | It is real provider search after selection, not an independently curated/personalised feed. |
| PARTIALLY IMPLEMENTED | Provider home feed | `HomeFeedRepository.loadProviderFeed()` sequentially invokes `YouTube.home`, `explore`, `moodAndGenres`. | A thrown early call prevents later calls and yields an empty feed/error rather than independent partial shelves. |

## Declared preferences with no Kotlin reader outside their declaration

Static inventory: `PreferenceKeys.kt` declares 230 keys; 87 identifiers have no Kotlin source reference outside that file. This proves the listed keys have no behavior consumer in the audited sources. It does **not** prove each is currently displayed in UI.

| Group | Keys with no source reader |
| --- | --- |
| Appearance/player legacy | `DynamicThemeKey`, `DefaultOpenTabKey`, `SlimNavBarKey`, `SliderStyleKey`, `SwipeToSongKey`, `PlayerDesignStyleKey`, `UseNewMiniPlayerDesignKey`, `OmniTuneCanvasKey`, `ThumbnailCornerRadiusKey`, `CropThumbnailToSquareKey`, `GlassNavigationBarKey`, `GlassMiniPlayerKey`, `AppLanguageKey`, `SwipeThumbnailKey` |
| Together dormant settings | `TogetherDisplayNameKey`, `TogetherClientIdKey`, `TogetherDefaultPortKey`, `TogetherAllowGuestsToAddTracksKey`, `TogetherAllowGuestsToControlPlaybackKey`, `TogetherRequireHostApprovalToJoinKey`, `TogetherLastJoinLinkKey`, `TogetherWelcomeShownKey`, `TogetherOnlineEndpointCacheKey`, `TogetherOnlineEndpointLastCheckedAtKey` |
| Playback/equalizer/cache | `AutoLoadMoreKey`, `ShowHomeCategoryChipsKey`, `EqualizerOutputGainEnabledKey`, `EqualizerOutputGainMbKey`, `EqualizerCustomProfilesJsonKey`, `MaxSongCacheSizeKey`, `MaxCanvasCacheSizeKey`, `SleepTimerEnabledKey`, `SleepTimerMinutesKey`, `SleepTimerEndOfSongKey`, `EqualizerBandsKey`, `EqualizerPresetKey` |
| Privacy/Discord/translation | `DisableScreenshotKey`, `DiscordInfoDismissedKey`, `DiscordUsernameKey`, `DiscordNameKey`, `DiscordActivityPlatformKey`, `TranslatorContextsKey`, `TranslatorTargetLangKey`, `EnableTranslatorKey` |
| Library sorting/filter/view state | `ChipSortTypeKey`, `PlaylistSongSortTypeKey`, `PlaylistSongSortDescendingKey`, `AutoPlaylistSongSortTypeKey`, `AutoPlaylistSongSortDescendingKey`, `ArtistSortTypeKey`, `ArtistSortDescendingKey`, `AlbumSortTypeKey`, `AlbumSortDescendingKey`, `PlaylistSortDescendingKey`, `ArtistSongSortTypeKey`, `ArtistSongSortDescendingKey`, `SongFilterKey`, `ArtistFilterKey`, `AlbumFilterKey`, `ArtistViewTypeKey`, `PlaylistViewTypeKey`, `PlaylistEditLockKey`, `QueueEditLockKey` |
| Legacy YouTube sync markers | `LastLikeSongSyncKey`, `LastLibSongSyncKey`, `LastAlbumSyncKey`, `LastArtistSyncKey`, `LastPlaylistSyncKey` |
| Lyrics/search/provider switches | `LyricsAnimationStyleKey`, `PlayerButtonsStyleKey`, `ShowLyricsKey`, `TranslateLyricsKey`, `UseLyricsV2Key`, `SearchSourceKey`, `WebClientPoTokenEnabledKey`, `UseVisitorDataKey`, `PoTokenSourceUrlKey` |
| Update cache/reminders | `RemindAfterKey`, `EnableUpdateNotificationKey`, `LastNotifiedVersionKey`, `GitHubContributorsEtagKey`, `GitHubContributorsJsonKey`, `GitHubContributorsLastCheckedAtKey`, `GitHubReleasesEtagKey`, `GitHubReleasesJsonKey`, `GitHubReleasesLastCheckedAtKey`, `GitHubReleasesFingerprintKey` |

**Recommended policy:** visible settings must have (1) one persisted key, (2) at least one runtime reader, (3) a behavior test, and (4) a migration/removal rule. Run a source-level registry test in CI to prevent more orphaned keys.

## UI callbacks and placeholder scan

- `rg` found no exact empty `onClick = {}` / `onCheckedChange = {}` callbacks in production Kotlin. This is a **static absence check only**; it does not prove every callback completes its operation.
- `ListQueue.nextPage()` intentionally throws `UnsupportedOperationException` (`playback/queues/ListQueue.kt:25`). Its queue type reports no pagination; callers must remain guarded. Treat as `REGRESSION RISK`, not a currently proven user-visible failure.
- Broad `catch (Exception)` / `runCatching` use is concentrated in provider, hydration, backup, playback and settings paths. Several report/log errors, but Home hydration and search cancellation need the specific fixes in the main audit.
- No hardcoded song objects were found being rendered as final playback data. Home's static objects are query/card metadata rather than static audio. Do not conflate these two cases.

## Routes and components that need runtime proof

The following have valid navigation/manifest wiring but have not been executed in this audit and therefore remain `IMPLEMENTED BUT UNVERIFIED`: account login, PoToken extraction, backup/restore, downloads, equalizer, widget, playback notification controls, update installation, Last.fm/ListenBrainz authentication, and donation UPI action.

Manifest components have concrete declarations: non-exported music/download services and PoToken activity, a non-exported FileProvider, and an exported widget receiver with the normal AppWidget update action. No exported component is a proven exploit from this static pass; intent and widget runtime behavior still need device testing.
