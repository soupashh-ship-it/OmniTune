# Home Mood and Genre Category Results QA

Date: 2026-07-08
Branch: `feature/playback-continuation-autoplay`

## Current Behavior Before Fix

Home mood and genre chips used a single loose query per category, such as `chill mix` or `gaming music`, then accepted the provider/search result stream without semantic scoring. This could let categories feel generic when the provider returned unrelated or broad results.

## Implementation

- Added `MoodGenreCategory` profiles for visible Home moods and activity categories.
- Added `MoodGenreResolver` to run multiple category-specific primary queries, then fallback queries only when the primary pool is too small.
- Added deterministic relevance scoring using song title, artist, album, query source, preferred terms, and excluded terms.
- Added duplicate removal by stable song ID, with title/artist/duration fallback.
- Wired `HomeCollectionViewModel` so profiled mood/genre categories use the resolver before generic search or provider browse fallback.
- Cache keys now include category ID and query profile version.

## Category Profiles Added

| Category | Primary query examples | Fallback behavior |
| --- | --- | --- |
| Chill | `chill songs playlist`, `lofi chill songs`, `acoustic chill songs` | Relaxing/mellow queries |
| Relax | `relaxing songs playlist`, `calm songs playlist` | Peaceful/mellow queries |
| Gaming | `gaming music playlist`, `edm gaming music`, `phonk gaming music` | Gaming montage/no-copyright queries |
| Workout | `workout songs playlist`, `gym music mix`, `workout edm songs` | Cardio/hip-hop/EDM workout queries |
| Focus | `focus music playlist`, `study music playlist`, `lofi study beats` | Deep work/study beats queries |
| Romance | `romantic songs playlist`, `love songs playlist` | Hindi/Bollywood romantic queries |
| Sad | `sad songs playlist`, `heartbreak songs` | Emotional/Hindi sad queries |
| Party | `party songs playlist`, `dance party music`, `club songs playlist` | Punjabi/Bollywood/dance hit queries |
| Feel good | `feel good songs playlist`, `happy songs playlist` | Positive/happy music queries |
| Energize | `high energy songs playlist`, `hype songs playlist` | EDM/dance/motivation queries |
| Commute | `road trip songs playlist`, `driving songs playlist` | Drive/travel queries |
| Sleep | `sleep music`, `ambient sleep music` | Peaceful/acoustic sleep queries |
| Lo-fi | `lofi songs playlist`, `lofi study beats` | Hindi lo-fi/lo-fi beats queries |
| Electronic | `edm songs playlist`, `electronic music playlist` | EDM energy/electronic workout queries |
| Bollywood / Hindi | `bollywood songs playlist`, `hindi songs playlist` | Popular/new Hindi queries |

## Filtering Rules

- Positive score from category title, preferred terms, primary/fallback source, and include keywords.
- Negative score for excluded terms and unrelated-content markers.
- Obvious non-music or wrong-content markers are penalized: news, podcast, full movie, trailer, reaction, tutorial, ringtone, shorts compilation, kids/nursery content.
- Results below the minimum accepted score are not returned.

## Language Handling

- Hindi/Bollywood searches are only part of Hindi/Bollywood-specific or explicitly Hindi fallback profiles.
- Global mood categories do not default to Hindi or English. They use mood/activity-specific query profiles.
- Mood/activity labels are not treated as real track genre metadata unless OmniTune receives/stores verified metadata elsewhere.

## Manual QA Status

Automated resolver tests passed. Device smoke testing was limited to install/startup for the current debug APK during this pass; full category-by-category live result inspection still needs network/provider runtime QA.

## Known Limitations

- Relevance uses provider-returned song metadata only: title, artist, and album. It does not infer genre from artwork or language.
- Provider search quality can still vary by region/account, but categories no longer use one generic random query.
- Unknown one-word categories without a real query profile remain unprofiled instead of generating fake results.

