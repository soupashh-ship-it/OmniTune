# OmniTune v0.10.0
**Dynamic Accent Colors, Velune-Inspired Vibrancy, Premium Visual Polish**

### 🎨 Color System Overhaul
* **Fixed:** Secondary accent was incorrectly mapped to tertiary color, causing identical accent tones throughout the UI — now uses a proper distinct secondary seed for richer color variety
* **Added:** Saturation boosting on artwork-extracted theme colors (1.3× multiplier, Velune-style) — muted album art now produces punchy, vibrant UI accents instead of washed-out tones
* **Enhanced:** Dynamic accent colors now properly recompose via `compositionLocalOf` — the home screen, fullscreen player, miniplayer, buttons, and backgrounds all react instantly when a new song plays
* **Added:** Animated color scheme transitions — smooth `spring()`-based interpolation on every Material3 color property for buttery color changes between songs
* **Enhanced:** Background gradient brush is now fully dynamic (computed getter) — no more stale cached gradients, the entire background updates with the current accent
* **Enhanced:** Player background fallback includes the accent glow as a prominent top gradient stop — colored sheen on the fullscreen player even when artwork extraction fails
* **Increased:** Background surface tint from 4% to 15% for a visible but tasteful color shift that matches the current song

### 🖌️ UI & Layout Refinements
* **Redesigned:** Theme Creator, Palette Picker, and Theme Palettes screens — condensed, more intuitive flows for creating and managing custom theme seeds
* **Refined:** MiniPlayer layout and spacing for better visual balance
* **Updated:** Home screen discovery layout — accent-aware styling throughout
* **Improved:** Player screen button hierarchy and glow effects
* **Polished:** OmniShell background integration for seamless transitions

### 🔧 Fixes
* Fixed accent colors not updating on home screen and fullscreen player (CompositionLocal recomputation)
* Fixed `PlayerFallbackGradient` returning stale cached colors (changed from `val` to computed `get()`)
* Fixed `OmniBackgroundGradientBottom` not following tinted base color (changed to computed getter)
* Fixed lyrics repository synchronization edge case

### 🏗️ Build
* Version: **0.10.0** (code 46)
* Signed release APK via CI
