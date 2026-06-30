# Home, Search, and Native Discovery Architecture Audit

Baseline commit: `20eed33df8704bdc896d5236ca31a83ac7a0a408`

## Home routing

- Top header search intentionally opens `search` / `search?query=...`.
- Top header settings intentionally opens `settings`.
- Curated mood chips, hero cards, quick picks, shelf cards, and mood grid items already open `homeCollection/{collectionId}` when `HomeDefaultCatalog.findCollection(id)` returns metadata.
- Direct song rows from recently played, downloads, favorites, and user-data quick picks play immediately.
- Search-history shelf items still use `HomeActionType.Search` and route to Search. This is honest user-history behavior, but it is the remaining Home content that behaves like a search shortcut.
- Any catalog item with a query but no matching `HomeCollectionMetadata` falls back to Search. The default catalog currently covers the curated first-install items, so this fallback mainly protects incomplete or future data.

## Native pages and route support

- `homeCollection/{collectionId}?artworkUrl=...` exists and renders `HomeCollectionRoute`.
- `album/{albumId}` exists and loads `AlbumScreen` from `YouTube.album()` plus `YouTube.albumSongs()`.
- `artist/{artistId}` exists and loads `ArtistScreen` from `YouTube.artist()`.
- `playlist/{playlistId}` exists for local library playlists through `PlaylistDetailScreen`.
- Online provider playlists are parsed by Search as `PlaylistItem`, but Search currently does not navigate them to a native online playlist screen. Safe fallback should be `homeCollection` or a future provider playlist route using `YouTube.playlist()`.

## Collection behavior

- `HomeCollectionViewModel` shows catalog metadata immediately, then asynchronously loads songs with `YouTube.search(collection.query, FILTER_SONG)`.
- Collection tracks are deduplicated by song id and capped by catalog `maxItems`.
- Collection results are cached in a static in-memory `ConcurrentHashMap` by `collectionId`.
- Offline or provider failures keep the header and show a retryable error.
- Track tap, Play, and Shuffle call the navigation-level queue player with the loaded `SongItem` list and selected index.

## Search behavior

- Search currently has no visible filter chips.
- Every non-empty query triggers four concurrent provider searches: songs, albums, artists, and featured playlists.
- Results are already rendered in separated sections with stable keys for songs, artists, albums, and playlists.
- Song rows play a queue of visible song results.
- Album rows navigate to `album/{browseId}`.
- Artist rows navigate to `artist/{id}`.
- Playlist rows are non-clickable and show `Playlist details pending`.
- Search caches only the last good combined result per query in memory; it does not keep separate state per filter.
- The provider exposes typed filters for songs, videos, albums, artists, featured playlists, and community playlists, plus `searchContinuation()`.

## Provider model support

- Provider search items are typed as sealed `YTItem` subclasses: `SongItem`, `AlbumItem`, `ArtistItem`, and `PlaylistItem`.
- There is no standalone `VideoItem`; video results are represented through `SongItem` metadata/endpoints where available.
- `SearchResult` carries a provider continuation token.
- Album, artist, and playlist provider endpoints exist in `YouTube.kt`; only album and artist have native Search navigation today.

## Thumbnail and loading behavior

- Home thumbnail hydration is ViewModel-owned, guarded by `requestedThumbnailIds`, and processed by two workers.
- Home uses sized Coil `ImageRequest`s with memory/disk cache keys and short crossfade.
- Collection uses first loaded song thumbnail as header fallback and caches collection results.
- Search rows use sized Coil requests, but row thumbnails show an icon fallback rather than the richer Omni thumbnail placeholder.
- Album/Artist/History/Library screens still have several raw `AsyncImage` calls without explicit size requests.

## Smoothness risks

- Search does four network requests for each query before filters exist, which is expensive during navigation and typing.
- Search lacks per-filter cached state, so switching to future filters would refetch unless the ViewModel is changed.
- Some older screens keep local loading state in Composables instead of ViewModels, making reloads more likely on recreation.
- Home still updates the combined Home state as hydration results arrive, which can recompose multiple shelves, though requests are one-shot.
- Repeated glass/gradient surfaces in Home/Search rows can add overdraw on mid-range devices.
- Some lazy lists lack keys in older screens such as History and parts of Artist/Album.
- Animated loaders are used for central states; avoid adding them per thumbnail or per row.

## Safe implementation path

- Add explicit typed Home actions without changing playback resolution.
- Keep curated Home taps on native collection routes; use collection fallback when full provider-native pages are unsupported.
- Add Search filters by fetching only the active filter and caching state by `(query, filter)`.
- Make playlist Search rows open a safe native fallback rather than a dead row.
- Use existing album/artist routes only when provider ids are available.
- Keep stream resolution deferred until user taps play.
- Apply smoothness fixes to list keys, content types, thumbnail placeholders, and Search request fan-out without changing database schema.

## Deferred recommendation engine design

- Future personalized sections should use real local signals only: play time, recent events, liked songs, downloads, skips if tracked, and time of day.
- Provider related songs can hydrate future sections after local signals exist.
- Do not add `For You`, forgotten favorites, top artists, or similar sections unless the data source is honest and cached.
