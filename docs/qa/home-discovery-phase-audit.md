# Home Discovery Phase Audit

Starting commit: `177306c908414367f736a11c74e58bfb9ae6f2f8`

## Phase 0 Findings

### Home routing

- Top Home search button opens Search directly. This is the intended direct Search entry point.
- Mood chips and Mood and Genres grid open `homeCollection/{collectionId}`.
- Curated hero cards open `homeCollection/{collectionId}` when the catalog contains the id.
- Curated Quick Picks open `homeCollection/{collectionId}` when the catalog contains the id.
- Curated horizontal shelf cards open `homeCollection/{collectionId}` when the catalog contains the id.
- Real user-data cards from recent, downloaded, liked, or library songs play the real song directly.
- The New or Trending Searches section still opens Search because those rows are search history or seed searches, not curated playlist identities.

### Native collection behavior

- `HomeCollectionViewModel` loads real `SongItem` results through the existing `YouTube.search(query, FILTER_SONG)` path.
- Results are deduplicated by media id and capped by catalog `maxItems`.
- The screen shows catalog header immediately, then loads tracks asynchronously.
- Stream URLs are not resolved on collection open.
- Track tap, Play, and Shuffle queue the loaded collection through `ListQueue`.
- Offline/provider failure keeps the header visible and shows retry/search actions.
- Song count is honest: it reports loaded songs only, or a generic loading/empty label.

### Artwork and thumbnails

- Home defaults render immediately from the curated catalog.
- Runtime thumbnail hydration uses provider result thumbnails and generated fallback artwork.
- Shelf cards support collage artwork.
- No bundled album, YouTube, or Velune artwork was found in the Home catalog.
- Initials/label fallback remains only when remote artwork is missing or fails.

### Settings, loading, and visual system

- Active navigation imports `com.omnitune.app.ui.screens.settings.SettingsScreen`.
- The active Settings screen has compact quick actions and row-based categories.
- Legacy `ui/screens/SettingsScreen.kt` remains in the tree but is not the active nav target.
- `OmniTuneLoader` now uses OmniTune waveform/disc pulse components.
- `ShimmerBar` is now a static premium placeholder instead of a heavy repeated shimmer animation.

### Remaining phase risks

- Artist/profile pages are still the safe minimum: artist-like cards open a native `ArtistMix` collection with top songs, not a rich provider artist profile.
- Collection pagination/load-more is not implemented; collections show the first provider result page capped by `maxItems`.
- Library still uses simple shelves/rows, not full Playlists/Songs/Albums/Artists tabs.
- Thumbnail prewarm is intentionally aggressive from the previous speed pass and should be watched for provider throttling on slow networks.
- Search history and seed rows intentionally remain Search actions because they are not collection catalog entries.
