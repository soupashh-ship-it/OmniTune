# Phase 6 UI Polish Audit

Root issues found:

- Home interaction is now native for curated content, but layout still relies on large dark surfaces and vertical row stacks.
- Loading language mixes shimmer placeholders, generic spinners, and older animated loader behavior; OmniTune needs one quieter loader family.
- Home and collection artwork fallbacks work, but too many surfaces read as boxed because every row uses strong glass treatment.
- Settings keeps useful functionality, but current structure overuses large cards and expanded panels, making scanning harder than needed.
- Bottom navigation and MiniPlayer are functional; polish target is lighter active state, clearer contrast, and safer spacing.

Safety constraints:

- Keep search, playback, queue, downloads, lyrics, notifications, and database behavior intact.
- Keep native collection route as primary Home card target.
- Do not fake collection counts or recommendations.
