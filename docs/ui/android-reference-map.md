# OmniTune Android reference map

## Active visual authority

The user replaced the reference folder on 2026-07-26. The ten images currently present in `C:\Users\soupa\Downloads\Omnitune android new ui` are the sole authoritative visual family. All are `863 × 1822` portrait captures from the `09_04_52 PM` batch. Earlier `09_05_16 PM` material is superseded and must not drive new UI decisions.

The shared shell is: graphite/navy canvas, restrained plum-to-coral ambience, a tall rounded mini-player, and a rounded four-item dock in this order: **Home, Stats, History, Library**. Search is a header destination and retains the Home dock selection. Downloads retains the Library selection.

| # | File | Screen | Implementation target | Key visual contract |
| ---: | --- | --- | --- | --- |
| 1 | `ChatGPT Image Jul 22, 2026, 09_04_52 PM (1).png` | Home | `HomeDiscoveryScreen.kt` | Wordmark/subtitle, mood pills, horizontal continue card, four-up Quick Picks, shelves, persistent shell |
| 2 | `… (2).png` | Stats | `StatsScreen.kt` | Two-column metric cards, yearly feature card, overview metrics/chart, ranked song list |
| 3 | `… (3).png` | Settings | `settings/SettingsScreen.kt` | Uppercase coral groups, deliberately larger icon rows and text inside bordered sections, persistent shell |
| 4 | `… (4).png` | Library | `LibraryScreen.kt` | Header, four-segment category control, quick-access rail, two-column collection cards |
| 5 | `… (5).png` | Playlist detail | `PlaylistDetailScreen.kt` | Full-width editorial artwork, title/meta/actions, sorting control, dense numbered track list |
| 6 | `… (6).png` | Downloads | `DownloadsScreen.kt` | Four status cards, segmented Ready/Active/Failed tabs, real empty/download states |
| 7 | `… (7).png` | History | `HistoryScreen.kt` | Date groups, count labels, dense rows in rounded groups, destructive clear action |
| 8 | `… (8).png` | Search landing | `search/SearchScreen.kt` | Back/header, large outlined field, filter pills, discovery card, recent/trending chips, mood rail |
| 9 | `… (9).png` | Search results | `search/SearchScreen.kt` | Query field, filters, top artist treatment, song rows, album/artist rails |
| 10 | `… (10).png` | Now playing + lyrics | `ui/player/PlayerScreen.kt`, `LyricsBottomSheet.kt` | Artwork ambience, transport, pink seekbar, integrated lyrics/queue panel |

## Settings-detail reference family

The ten captures in `C:\Users\soupa\Downloads\Omnitune android new ui\Omnitune Settings Inside` extend the Settings visual authority to the existing functional settings routes. They establish the shared detail-screen treatment: burgundy-to-navy ambience, circular outlined back affordance, title/subtitle header, compact outlined preference cards, coral values, and large controls.

| # | File | Reference title | Functional target |
| ---: | --- | --- | --- |
| 1 | `ChatGPT Image Jul 26, 2026, 08_25_54 PM (1).png` | Playback | `settings/PlaybackSettings.kt` |
| 2 | `…08_25_54 PM (2).png` | Player Appearance | `settings/AppearanceSettings.kt` |
| 3 | `…08_25_55 PM (3).png` | Behavior | Existing playback behavior preferences |
| 4 | `…08_25_55 PM (4).png` | Downloads | Existing storage/download preferences |
| 5 | `…08_25_55 PM (5).png` | Library | `settings/ContentSettings.kt` |
| 6 | `…08_25_56 PM (6).png` | Parental Controls | Visual reference only; no route or controls are fabricated because this feature is not implemented |
| 7 | `…08_25_56 PM (7).png` | Notifications | Existing system media-notification guidance route |
| 8 | `…08_25_56 PM (8).png` | Storage | `settings/StorageSettings.kt` |
| 9 | `…08_25_57 PM (9).png` | Scrobbling & Integrations | `settings/ScrobblingSettings.kt` |
| 10 | `…08_25_57 PM (10).png` | Updates | `settings/UpdatesSettings.kt` |

## Binding and safety constraints

- Reference artists, covers, playlist names, counters, dates, and lyrics are composition samples only; every rendered value continues to come from real OmniTune state.
- The dock and mini-player remain hidden only for full player/queue overlays. They remain visible on the mapped utility, search, and playlist routes where the reference shows them.
- Screens not depicted (for example Explore, album, artist, and deep settings routes) retain their existing functional routes and inherit this same shell without fabricating reference-only data.
- Detail references change presentation only. Preferences, toggles, choices, accounts, notifications, storage actions, and update behavior remain backed by their pre-existing providers and system intents.
