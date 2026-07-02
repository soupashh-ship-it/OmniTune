# OmniTune Final UI Overhaul Audit

Date: 2026-07-02
Branch: release/phase-5-rc-qa-offline-downloads
Starting commit: 010eb1e

## Visual Sources

- Velune reference screenshots: `C:\Users\soupa\Downloads\Velune Ui`
- Latest OmniTune screenshots: `C:\Users\soupa\Downloads\Omnitune Latest`
- Old OmniTune screenshots: `C:\Users\soupa\Downloads\Omnitune Ui old`
- Contact sheets stored in `docs/qa/final-ui-overhaul/references/`

Velune is used only as a reference for density, feed rhythm, and settings completeness. No Velune code, assets, exact layout, logo, text system, or color identity should be copied.

## 1. Status Bar And Safe Area Issues

- Latest OmniTune generally avoids direct status-bar overlap on Home, Library, Stats, History, and Settings main.
- Settings expanded content still feels visually tight near the top because expansion happens inline inside one long settings list instead of a stable subpage/header pattern.
- Several screens implement top padding independently with local `statusBarsPadding()` calls. This is fragile and should be centralized through shared screen/header scaffolding.

## 2. Content Hidden Behind MiniPlayer And Bottom Dock

- Latest OmniTune screenshots show MiniPlayer plus dock consuming too much vertical space as a stacked playback/navigation system.
- Library, Stats, History, and Mood/Genres lower content all rely on manual bottom spacers. This is repeated and easy to under-size when MiniPlayer is visible.
- MiniPlayer has enough readability, but the total chrome feels bulky and reduces visible feed content.

## 3. Screens That Are Too Flat

- Settings expanded sections look like plain text groups with weak grouping and unfinished labels.
- Library is clean but sparse when counts are zero.
- Stats rows are readable, but summary chips and top lists need a little more hierarchy.
- History is functional but sparse, with rows floating on the base background.
- Search/results and collection rows should keep lightweight separation without returning to heavy card borders.

## 4. Screens That Are Still Too Boxed

- Old OmniTune screenshots show the previous risk clearly: large bordered cards, heavy panels, and dashboard surfaces should not return.
- Latest OmniTune has mostly reduced that successfully. Any added depth should be low-alpha and targeted to controls, rows, and floating chrome.

## 5. Rows And Cards With Weak Hierarchy

- Mood and Genres currently reads as a two-column wall of plain pills.
- Settings category rows need consistent icon/title/subtitle/chevron treatment and cleaner expanded content.
- Library/Stats/History rows need subtle row surfaces or separators so they feel intentional without becoming boxes.
- MiniPlayer and bottom dock need a shared compact visual language.

## 6. Non-Functional Or Unfinished Settings

Confirmed code-visible normal rows still use `Not yet implemented`:

- Appearance: Pure black mode, Disable blur effects, Grid item size
- Content/history: Hide explicit content, Hide video results, Pause search history, Pause listen history
- Lyrics: Lyrics animation
- Scrobbling: Last.fm and ListenBrainz placeholder controls
- Playback root also contains placeholder rows in the legacy settings file

These should not appear as primary functional settings. Working controls can remain. Non-functional controls should be hidden or moved out of the normal settings path.

## 7. Repeated Components Needing Design-System Cleanup

- Screen background, safe top padding, horizontal padding, and bottom chrome padding.
- Compact top headers with optional back/search/settings actions.
- Music row with thumbnail, title, subtitle, trailing action, pressed state, and dark placeholder.
- Image-led shelf card.
- Settings row with icon, title, subtitle, optional action/chevron.
- Compact MiniPlayer and floating bottom dock surfaces.
- Static dark placeholders for missing thumbnails.

## 8. Risky Changes To Avoid

- Do not alter package/applicationId, playback service, queue logic, search/provider logic, downloads/offline state, lyrics provider logic, notification/background playback, licensing, credits, or attribution.
- Do not add fake songs, fake stats, fake charts, fake Quick Picks, or fake provider sections.
- Do not return to old dashboard-card styling.
- Do not use screenshots as proof unless captured from a real running app after implementation.
