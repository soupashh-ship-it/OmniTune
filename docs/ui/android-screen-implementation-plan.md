# OmniTune Android screen implementation plan

## Active reference-family sequence

1. **Shared secondary-reference shell** — Home / Stats / History / Library dock, tall mini-player, graphite/plum/coral surfaces, screen inset contract.
2. **Home** — header, mood pills, horizontal continue card, quick-pick rail, data-backed shelves.
3. **Search and Library** — header search entry, landing/result states, segmented Library categories, image-rich real-data shortcuts.
4. **Stats, History, Downloads, Settings** — derive every metric/count from persisted data and maintain each existing action.
5. **Playlist and player/lyrics** — editorial playlist density; immersive real playback and lyrics panel.
6. **Undepicted functional destinations** — make Explore, artist, album, and deep settings inherit the same tokens/shell without false sample metadata.
7. **QA** — repeated device screenshots against this ten-image canonical set, functional regression matrix, build/lint/test evidence, logical commits.

## Navigation contract

- Persistent dock: `Home`, `Stats`, `History`, `Library`.
- Header search is a persistent-shell destination that highlights Home in the dock.
- Downloads highlights Library. Playlist detail is a persistent-shell editorial destination and highlights Home when no library-specific selection is justified.
- Full player and queue suppress shell chrome; all other mapped reference routes retain the mini-player and dock.

## Safety boundaries

- No repository, Room, migration, preference, playback-service, download-manager, or provider contract changes solely for visual presentation.
- Never use reference-only Kanye titles, images, counts, or lyrics as runtime fallbacks.
- Empty, loading, offline, and error states remain explicit and use the same visual system.
