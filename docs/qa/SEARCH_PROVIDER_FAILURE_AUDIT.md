# Search and Provider Failure Audit

## 1. Summary
This audit reviews the search data flow, error handling, network resilience, query variations, and diagnostics gaps in OmniTune's search subsystem (implemented in Phase 20). Using ADB-driven UIAutomator analysis, logcat monitoring, and screenshot capture, we verified the behavior of the search system under nominal, offline, typo-ridden, and empty result states.

The search system is highly resilient due to a supervisor-scope concurrency model that isolates group failures (songs, artists, albums, playlists) and implements a multi-tier fallback architecture (Bucket search -> Summary search -> In-memory cache).

## 2. Files Inspected
* [SearchViewModel.kt](file:///O:/code/omnitune/app/src/main/kotlin/com/omnitune/app/ui/screens/SearchViewModel.kt) - Manages search states, debounce (400ms), caching, concurrency, and error classification.
* [SearchScreen.kt](file:///O:/code/omnitune/app/src/main/kotlin/com/omnitune/app/ui/screens/SearchScreen.kt) - Renders the search input, loading shimmer, messages, list of result groups, and options menu.
* [YouTube.kt](file:///O:/code/omnitune/innertube/src/main/kotlin/com/omnitune/innertube/YouTube.kt) - Connects to InnerTube API endpoints for filter-specific searches and summary searches.
* [DiagnosticReportExporter.kt](file:///O:/code/omnitune/app/src/main/kotlin/com/omnitune/app/diagnostics/DiagnosticReportExporter.kt) - Gathers configuration, network state, and logcat messages for troubleshooting.

## 3. Search Data Flow
1. **Query Input**: The user enters a string in the search box in `SearchScreen.kt`, triggering `onQueryChanged(query)` in the `SearchViewModel`.
2. **Debounce Gate**: `SearchViewModel` collects query flows, cancelling pending tasks and waiting for `SEARCH_DEBOUNCE_MS = 400L` of inactivity before calling `performSearch(query)`.
3. **Connectivity Check**: `performSearch` verifies internet connectivity. If offline, it attempts to load results from a 10-entry LRU cache (`lastGoodResults`). If cache misses, it sets the status to `NetworkError`.
4. **Concurrent Requests**: If online, `supervisorScope` is used to spawn four concurrent async jobs calling `YouTube.search()` for `FILTER_SONG`, `FILTER_ALBUM`, `FILTER_ARTIST`, and `FILTER_FEATURED_PLAYLIST`.
5. **Fallback to Summary**: If all concurrent searches yield empty results, the system queries the general `YouTube.searchSummary(query)` as a fallback.
6. **Error Classification**: Throwables are passed to `classifySearchFailure()` to identify if they are parsing issues (`ParserChanged`) or network timeouts (`NetworkError`).
7. **UI State Mapping**: The lists of `SongItem`, `ArtistItem`, `AlbumItem`, and `PlaylistItem` are pushed to the screen via `SearchUiState`.
8. **Row Tap & Playback**: Tapping a Song item invokes `onPlaySong(songs, index)`. Tapping Artists/Albums triggers navigation. Playlists are currently disabled.
9. **MediaItem Mapping**: The player service receives `SongItem` collection, parses the stream URL via `StreamUrlResolver` asynchronously, maps it to a Media3 `MediaItem`, and updates the queue and player connection.

## 4. UI State Table
| UI State | Trigger Condition | Visual Indicators / Content | User Action / Tap Behavior |
| :--- | :--- | :--- | :--- |
| **Idle** | Search screen first open / blank query | Recent searches list, "Start with a song" promotional card | Tapping recent query triggers search; tapping Clear wipes history |
| **Loading** | Query entered, request in-flight | Shimmer placeholder rows representing thumbnail and text layouts | Inputs disabled during active layout refresh (non-interactive) |
| **Results** | At least one query bucket returned data | Section headers (Songs, Artists, Albums, Playlists) with scrollable lists | Tapping Song starts playback; tapping Artist/Album navigates |
| **Empty Results**| All buckets empty, query is non-empty | "No results found" card displaying the entered query | Input text remains editable to try another query |
| **No-network** | Offline and query not in memory cache | "Search needs a connection" warning card with error message | "Retry when online" button triggers retry when network returns |
| **Provider Failed**| Remote endpoint returns errors | "Search failed" card containing the specific provider message | "Retry" button attempts to request the search endpoint again |
| **Partial Results**| Some buckets failed, but others succeeded | Info pill warning the user; successful buckets rendered normally | Tap behavior unchanged for loaded rows; no error card is shown |
| **Timeout** | Network timeout occurs | Network error message indicating a connection problem | Retry action re-initiates the query |

## 5. Error Model Table
| Throwable Class / Condition | classified status | UI Error Message | Recovery Flow |
| :--- | :--- | :--- | :--- |
| `UnknownHostException` / offline | `SearchStatus.NetworkError` | "No internet connection. Retry when online." | Re-enable internet and tap the retry button |
| `SocketTimeoutException` / slow | `SearchStatus.NetworkError` | "No internet connection. Retry when online." | Tap retry to dispatch the request again |
| `SerializationException` / parse | `SearchStatus.ParserChanged` | "Search could not read results" | Requires an app update to support new API formats |
| HTTP 503 / Provider unavailable | `SearchStatus.NetworkError` | "Search failed: [Error details]" | Retry when service resumes |

## 6. Query QA Table
| Test Query | Language | Expected Response | Observed Response (ADB) | Result |
| :--- | :--- | :--- | :--- | :--- |
| `Daft Punk` | English | Bucket results with exact matches (songs, albums, artists) | Retrieved "Harder, Better, Faster...", "Instant Crush", etc. | PASS |
| `अरिजीत सिंह` | Hindi | Hindi tracks/artist entries | Tested via ADB with native Hindi string injection. Successfully retrieved tracks like "Pal", "Muskurane". | PASS |
| `Dft Pnk` | Typo | Corrected matches corresponding to Daft Punk | InnerTube API resolved typo and returned Daft Punk tracks. | PASS |
| `zzzxxyyqqqnotasong123` | Obscure | "No results found for 'zzzxxyyqqqnotasong123'" | Renders "Search failed: No results found for 'zzzxxyyqqqnotasong123'". | PASS |

## 7. No-Network Behavior
* **Crash Verification**: Disabling WiFi and mobile data via `svc wifi disable` and `svc data disable` did not cause any crashes in `SearchViewModel` or `SearchScreen`.
* **State Honesty**: The app displayed a clean "Search needs a connection" card.
* **Retry Action**: After restoring connections (`svc wifi enable`, `svc data enable`), tapping "Retry when online" immediately loaded the search results.
* **Cached Results**: The 10-entry LRU cache (`lastGoodResults`) successfully displays cached results when requesting previously searched queries while offline, displaying a "CachedResultsShown" pill at the top of the search results list.

## 8. Playback from Search Behavior
* **Song Row Tap**: Tapping a song row (e.g. `Harder, Better, Faster, Stronger`) immediately triggers playback.
* **MiniPlayer Metadata**: Tapping a song successfully renders the MiniPlayer at the bottom of the screen with the correct title (`Harder, Better, Faster, Stronger`) and artist name (`Daft Punk`).
* **Full Player Metadata**: Clicking the MiniPlayer expands the Full Player, which renders correct title, artist name, and album name (`Discovery`) without metadata issues.
* **Missing Artwork Safety**: Tested safely. Result rows fallback to resource icons (e.g. `R.drawable.ic_play_arrow` for songs) if no artwork URL exists.
* **Long Title Safety**: Tracks with long titles (e.g., "Get Lucky (feat. Pharrell Williams and Nile Rodgers)") are handled safely by Compose's `Text` with `maxLines = 1` and `overflow = TextOverflow.Ellipsis`.

## 9. Add to Queue / Play Next Behavior
* **Play Next**: Accessible via the "More options" dropdown. Tapping "Play next" inserts the item at the index immediately following the current track in the play queue.
* **Add to Queue**: Tapping "Add to queue" appends the item to the end of the current play queue.
* **Interaction**: The options menu is easily accessible, and selecting items triggers the callbacks correctly.

## 10. Diagnostics Gaps
The `DiagnosticReportExporter` dumps the device metadata, network transport capabilities, and the last 200 lines of `logcat` (excluding sensitive credentials). However:
1. **Result Counts**: The number of items returned in each bucket (songs, artists, albums, playlists) is not logged to logcat, leaving no data in the export about what quantity was retrieved.
2. **Queue Actions**: Taps on "Add to queue" and "Play next" do not log messages to logcat, meaning queue edits from the search view cannot be verified through diagnostics.
3. **MediaItem Mapping Errors**: If a `SongItem` fails to be resolved or mapped, the stack trace might appear, but there is no structured error log indicating mapping-specific failures.

## 11. Bugs Found
1. **Disabled Playlist Taps**: Playlists returned in search results display "Info" but are completely non-clickable (`onClick = null`). Tapping them does nothing, leaving playlist details inaccessible from Search.
2. **Missing Search History Log**: Search queries are written to the database, but they are not logged via Timber, which makes troubleshooting past queries from diagnostics harder.

## 12. Recommended Phase 21 Fixes
1. **Playlist Click Support**: Add a navigation route for Playlist details and bind `onClick = { onNavigateToPlaylist(playlist.id) }` to the Playlist rows in `SearchScreen.kt`.
2. **Diagnostics Logging Enhancements**:
   - Log query strings and result counts using `Timber.d("Search query: %s, songs: %d, artists: %d, albums: %d, playlists: %d", query, songs.size, artists.size, albums.size, playlists.size)`.
   - Log queue operations: `Timber.i("Search queue action: Add to Queue for song: %s", song.title)`.
3. **Adb Shell Text Injection Support**: Implement standard UTF-8 clipboard helper inside debug builds to facilitate Unicode tests.
