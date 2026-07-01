# Home Feed Structure Audit

Branch: `release/phase-5-rc-qa-offline-downloads`

Starting commit: `fc5e94deffccff315d298b080e535df6f59cf2b3`

## Findings

1. The visible `Home Discovery` title was rendered by `HeroCarousel()` in `HomeDiscoveryScreen.kt`.
2. Static curated content lived in `HomeDefaultCatalog.kt`: hero items, curated quick picks, fresh discovery, mood chips, genre grid, and fallback shelves.
3. Provider-backed Home support existed in Innertube through `YouTube.home()`, `YouTube.explore()`, and `YouTube.moodAndGenres()`, but the Home view model did not consume those APIs.
4. Home cards still fell back to Search through `onNavigateToSearchQuery` when they were not local songs or known catalog collections.
5. Quick Picks mixed real database songs with curated query cards from `HomeDefaultCatalog.quickPicks`.
6. Provider browse, artist, album, and playlist APIs existed, but Home did not preserve provider ids through typed Home actions.
7. Local Home sources existed for recent events, DB quick picks, liked songs, downloaded songs, library songs, forgotten favorites, skip counts, and most-played songs.
8. Several sections were rendered as full-width glass row cards, including fallback discovery, downloads, library, and collection rows.
9. Thumbnail jank risks came from startup thumbnail prewarming and search-based hydration for curated Home content, plus generated fallback artwork under every image.
10. Composition hotspots included chunking mood/genre lists inside composables, card-level hydration effects, and nav item model creation inside the bottom dock.
