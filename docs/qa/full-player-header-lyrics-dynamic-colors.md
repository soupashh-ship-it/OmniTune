# Full Player Header, Inline Lyrics, and Dynamic Colors

## Current issue before fix

- The full-screen player top bar rendered `Now playing` with a hardcoded `OmniTune` second line.
- The player title area always showed artist and album metadata under the large song title, even when synced lyrics were available in OmniTune's cached lyrics table.
- Dynamic song colors already existed, but the artwork palette path could lean on weak, muddy, or overly bright swatches before mapping them into player surfaces.

## Reference findings

- The reference player keeps the now-playing context tied to the active track, not the app name.
- Its lyrics experience prioritizes synced lyric context near the player while preserving a full lyrics screen.
- Its color treatment favors high-resolution artwork, cached extraction, dark-safe gradients, and smooth transitions instead of raw artwork colors on large surfaces.

## Header behavior after fix

- `PlayerTopBar` now receives the current song title from `mediaMetadata`.
- The header still shows the compact `Now playing` label, but the second line is the active track title.
- Missing or blank titles fall back to `Now playing`.
- Long titles are ellipsized so they do not collide with the back or queue buttons.

## Inline lyric subtitles

- Added `InlineLyrics` and `InlineLyricState` as a small reusable lyric-subtitle resolver.
- The resolver reuses OmniTune's existing cached lyrics path through `PlayerConnection.currentLyrics`.
- Synced LRC and TTML lyrics are parsed with the existing `LyricsUtils` parser.
- The full player metadata block updates the current and next lyric line every 250 ms using the existing player position accessor.
- Tapping inline synced lyrics opens the existing lyrics bottom sheet.

## Lyrics fallback behavior

- Synced lyrics available: show current lyric line and the next line with softer emphasis.
- Lyrics missing, loading, failed, marked not found, or plain unsynced only: show the existing artist/album metadata fallback.
- No fake timing is generated for plain lyrics.
- Song changes reset parsing by media ID so old lyric entries are not reused for the next track.

## Dynamic color algorithm changes

- Palette extraction now prioritizes dark vibrant, vibrant, dark muted, muted, dominant, and then light swatches.
- Unusable colors are rejected when they are too gray, too dark, too bright, too close to white/black, or too close to the base surface.
- Artwork colors are tone-mapped into a controlled accent, then used to derive dark player surfaces.
- Background, secondary background, elevated surfaces, mini player surface, control surface, gradient start/end, accent, and soft accent remain dark-safe and readable.
- Player background now adds a restrained radial accent glow on dynamic-gradient styles.

## Settings compatibility

- Dynamic song colors disabled: existing `DynamicSongColorsKey` path returns the fallback/static palette.
- Solid dark player background: existing `OmniPlayerBackgroundStyle.SOLID_DARK` path still avoids the dynamic gradient/radial overlay.
- AMOLED/reduce-motion: no explicit reduce-motion or AMOLED player-specific preference was found in the current settings files during this task.

## Tests

- `InlineLyricsTest`
  - Synced lyrics return current and next line for playback position.
  - Unsynced lyrics do not create fake subtitles.
- `OmniDynamicSongPaletteTest`
  - Gray/white/black artwork swatches fall back.
  - Bright artwork is mapped to a dark-safe background and readable foreground control color.

## Manual QA result

- Device runtime QA was not completed in this edit pass.
- `adb devices` was checked and no device was attached.
- Final local verification passed: `clean assembleDebug`, `testDebugUnitTest`, and `lintDebug`.
- When a device is available, install and manually test header changes, inline lyric updates, seeking, skip behavior, queue button, mini player tap-to-open, dynamic colors on several artworks, and the dynamic color setting.

## Known limitations

- Inline subtitles only appear for real synced LRC or TTML lyrics already available through OmniTune's lyrics system.
- Plain lyrics intentionally fall back to artist/album metadata.
- No new lyric fetcher was added; this avoids duplicating the existing lyrics screen and cache pipeline.
