# OmniTune UI Direction

## Final Direction

OmniTune should feel like a clean, dark, image-led Android music app with a subtle futuristic Omni identity.

The visual system should use an OLED-friendly near-black/deep navy base, compact music-first layouts, clean typography, minimal borders, and calm cyan/blue-violet accents. Album, playlist, artist, and song artwork should provide most of the screen color. UI surfaces should support playback and discovery rather than compete with the content.

## Avoid

- Dashboard-style layouts and oversized title blocks.
- Heavy glass cards around every row or section.
- Thick borders on repeated list items.
- Random gradients on repeated cards.
- Neon cyan or purple used as decoration everywhere.
- Fake songs, fake stats, fake counts, fake charts, or fake provider shelves.
- Search as a default destination for normal Home cards.
- Animated placeholders in every row.
- Slow, bouncy, or theatrical motion.

## Screen Audit

### Home

Home is already provider-first and closer to the target rhythm: compact header, chips, image-led hero, real Quick Picks only, horizontal shelves, native actions, and fallback content demoted. Remaining risk is mostly visual: shared glass defaults can still make rows and fallback states feel heavier than the desired music-feed surface.

### Search

Search uses the correct explicit destination model and now keeps query text order stable. It still relies on glass search/header surfaces and repeated result components that should stay lightweight, fixed-height, and thumbnail-led.

### Collection, Playlist, Artist, Album, Browse

Native collection pages load real provider/local tracks and avoid Search fallback. The main polish risk is visual weight: headers and action rows should stay image-led, with compact track rows and minimal repeated borders.

### MiniPlayer and Bottom Nav

MiniPlayer and bottom nav are the correct places for polished floating surfaces. They should remain compact, dark, readable, and responsive without becoming bulky or over-glassed.

### Library

Library should remain organized around real local data: liked, downloaded, recently played, playlists, songs, albums, and artists where available. It should use compact rows and honest empty states.

### Stats

Stats must remain honest. It should show only real listening/library data. If insufficient data exists, the empty state should be quiet and useful instead of becoming a fake analytics dashboard.

### History

History should be a clean recently played music list with compact rows and direct playback. Borders and large card shells are the primary visual risk.

### Settings

Settings currently has the largest dashboard risk because it uses many card-like rows and borders. The target is a compact settings list: icon, title, short subtitle, and chevron/open label with consistent row height and restrained surfaces.

### Player

Player should keep artwork, title, artist, and controls as the focus. Accent and loader usage should be calm. Repeated control surfaces should avoid strong borders and gradients.

## Visual System Tasks

- Lower default glass border/shadow strength.
- Keep `GlassCard` available for special surfaces, but make the default calmer.
- Prefer subtle dark row backgrounds over outlined cards.
- Keep accent colors for action and state.
- Keep fixed artwork and row dimensions.
- Preserve stable Lazy keys/content types where they already exist.

## Deferred Risks

- Full replacement of every legacy list row is intentionally deferred unless runtime QA confirms a regression or obvious visual conflict.
- Playback, queue, downloads, lyrics, notifications, and database behavior should not be refactored during UI polish.
- Release packaging is not part of this pass unless explicitly requested.
