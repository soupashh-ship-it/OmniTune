# OmniTune Android reference measurements

## Active image set

- 10 current canonical images, each `863 × 1822 px` (portrait, 0.474 aspect ratio).
- Measurements below are relative to that source, then expressed as responsive Compose targets. They are deliberately not raw screen pixels.

## Shared geometry

| Element | Canonical reference | Compose target |
| --- | --- | --- |
| Screen side gutter | 32–40 px | 20dp compact / 24dp regular |
| Status-to-header gap | ~70–90 px | status inset + 20dp |
| Wordmark logo | ~60 px | 36–40dp optical size |
| Header action circle | ~72 px | 44–48dp touch target |
| Standard chip | ~70 px high | 44–48dp |
| Primary pill | ~72 px high | 52–56dp |
| Home continue card | 790 × 274 px | full width × 132–150dp |
| Home quick-pick art | 175 px square | 82–96dp square, four visible at 360dp width |
| Playlist hero | 800 × 370 px | full width, 2.15:1 |
| Group/metric card | 370–390 px wide | two equal columns with 12dp gap |
| Track artwork | 104–112 px | 48–52dp |
| Track row | 120–140 px | 64–72dp |
| Mini-player | ~170 px visible | 76dp content + inset/padding |
| Bottom dock | ~150 px visible | 80dp content + system inset |
| Full-player art | ~580 px square | min(72vw, 320dp) |

## Palette sampled from the active reference

| Role | Target |
| --- | --- |
| Canvas | `#10131D` to `#16161E` |
| Elevated card | `#231D25` with a faint burgundy wash |
| Floating player/dock | `#2B1D27` at high opacity |
| Hairline | dusty coral around 25–35% opacity |
| Main coral | `#EF7886` / `#E47A82` |
| Highlight salmon | `#FF9AA2` |
| Primary text | `#F6F1F3` |
| Secondary text | `#B7B1C1` |
| Muted text | `#8E8A99` |

## Visual rules extracted from the active set

- Warm burgundy ambience comes from the upper-left and content artwork only; the canvas must not turn neutral gray.
- Major cards use a thin coral-brown outline, not a heavy shadow or generic Material elevation.
- The mini-player is materially taller than the dock and sits flush above it with a small centered drag handle.
- The selected dock item uses a small tinted capsule behind the icon and coral label. The Home item remains selected for header-only search and editorial detail destinations.
- Full player is the only mapped screen without the persistent mini-player/dock.
