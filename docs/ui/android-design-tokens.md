# OmniTune Android design tokens

## 1. Color primitives

| Token | Hex | Role |
|-------|-----|------|
| Canvas | `#111217` | Stable near-black navy/charcoal base |
| CanvasAlt | `#15151C` | Slightly lighter canvas for tonal depth |
| Surface | `#17161D` | Default card/panel surface |
| SurfaceRaised | `#1A1921` | Elevated cards, track rows |
| SurfaceFloating | `#15141B` | Mini-player, dock background |
| SurfacePanel | `#1A1921` | Settings group cards |
| SurfaceHairline | `rgba(255,255,255,0.06)` | Subtle borders |
| SurfacePressed | `rgba(255,255,255,0.08)` | Press feedback |
| GlassDock | `rgba(24,23,30,0.96)` | Bottom navigation |
| GlassPlayer | `rgba(26,24,32,0.97)` | Full player background |
| AccentPrimary | `#E47A82` | Coral — active state, play buttons, selected chips |
| AccentSecondary | `#FF9AA2` | Salmon — secondary highlights, gradients |
| AccentTertiary | `#F99392` | Dusty rose — tertiary accents |
| AccentMuted | `#B35C64` | Muted coral for inactive states |
| AccentGlow | `rgba(228,122,130,0.30)` | Glow behind active elements |
| AccentSoft | `rgba(228,122,130,0.12)` | Soft chip/card tint |
| AccentOnPrimary | `#05060A` | Text on accent surfaces |
| TextPrimary | `#F3F0F3` | Warm off-white headings |
| TextSecondary | `#B7B1B8` | Gray-lilac body text |
| TextTertiary | `#8F8992` | Muted metadata |
| TextDisabled | `#444B5C` | Disabled state |
| BorderSubtle | `rgba(255,255,255,0.07)` | Hairline borders |
| Success | `#4EDB8F` | Download complete, scrobble success |
| Warning | `#FFC46B` | Warning state |
| Error | `#FF6363` | Error state |
| Hot | `#FF5C93` | Likes/favorites heart |

## 2. Typography

| Style | Size | Weight | Tracking | Use |
|-------|------|--------|----------|-----|
| displayTitle | 34sp | Bold | -0.5 | Screen titles (Explore, Stats) |
| sectionHeader | 22sp | SemiBold | 0 | Section headers (Quick Picks, New Releases) |
| mediaTitle | 20sp | SemiBold | 0 | Album/song/playlist titles |
| songTitle | 16sp | Medium | 0 | Track row titles |
| metadata | 13sp | Normal | 0 | Artist names, durations, counts |
| caption | 11sp | Medium | 0.5 | Tab labels, timestamps |
| eyebrow | 11sp | Bold | 1.5 | UPPERCASE coral labels (CURATED PLAYLIST, PLAYBACK) |
| heroTitle | 28sp | Bold | -0.5 | Hero card titles |

## 3. Shape scale

| Token | Value | Use |
|-------|-------|-----|
| Tiny | 6dp | Badges, tags |
| Small | 12dp | Artwork thumbnails |
| Medium | 18dp | Media cards |
| Large | 24dp | Hero cards, player artwork |
| ExtraLarge | 32dp | Full player |
| Pill | 999dp | Chips, action buttons |
| Dock | 28dp | Bottom navigation |
| ArtworkSmall | 12dp | Song row artwork |
| ArtworkMedium | 16dp | Rail artwork |
| ArtworkLarge | 24dp | Player artwork |

## 4. Spacing rhythm

| Token | Value |
|-------|-------|
| micro | 4dp |
| compact | 8dp |
| small | 12dp |
| medium | 16dp |
| large | 20dp |
| section | 24dp |
| hero | 32dp |
| screen | 40dp |

## 5. Component dimensions

| Component | Height | Corner |
|-----------|--------|--------|
| Chip | 48dp | Pill |
| Primary action pill | 56dp | Pill |
| Search bar | 56dp | 28dp |
| Song row (compact) | 64dp | 12dp |
| Song row (default) | 72dp | 12dp |
| Song row (playlist) | 80dp | 12dp |
| Media card | 160-200dp | 18dp |
| Hero card | 130-160dp | 24dp |
| Mini-player | 76dp | 28dp |
| Bottom dock | 80dp | 28dp |
| Artwork (song row) | 48dp | 12dp |
| Artwork (rail) | 140dp | 16dp |
| Artwork (player) | 280dp | 24dp |

## 6. Motion

| Transition | Duration | Easing |
|------------|----------|--------|
| Press feedback | 80ms | fast out |
| Tab selection | 200ms | spring no-bouncy |
| Mini-player expand | 300ms | spring gentle |
| Screen enter | 350ms | fade + slide |
| Artwork crossfade | 200ms | fade |

Reduced motion: all animations respect `animatorDurationScale`.