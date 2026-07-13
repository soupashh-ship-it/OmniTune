## OmniTune v0.8.3 — UI Refinement & Settings Cleanup

### Settings Refactoring
- Modularized Settings into per-section files (Appearance, Playback, Storage, Notifications, Content, Lyrics, About, Updates, Diagnostics)
- Removed "Not yet implemented" placeholder rows from all visible settings sections
- Removed Scrobbling (Last.fm / ListenBrainz) from the main settings navigation
- Cleaned Appearance section to show only working controls
- Removed unused imports for non-functional features (PureBlack, DisableBlur, HideExplicit, etc.)

### Home Screen Improvements
- Migrated from GlassCard/GlassSurface/AccentPill to Omni component system
- Reduced dashboard-card appearance with cleaner rows
- Improved feed rhythm in lower sections
- Reduced borders and glass on repeated items

### MiniPlayer & Bottom Nav Balance
- Refined MiniPlayer height and artwork sizing
- Reduced GlassBottomDock visual weight
- Cleaner active-tab indicator with gradient pill
- Smoother state transitions

### Player Screen
- Reorganized controls and metadata layout
- Improved action button spacing
- Safer bottom-area padding across devices

### Navigation & Safe Area
- Centralized bottom chrome padding in navigation graph
- Better handling of MiniPlayer + dock combined height
- Safer system bar padding for Settings expanded sections

### Theme & Motion
- Added surface tokens for floating controls, panels, and quiet states (OmniColors)
- Added omniPressScale/omniPressScaleBounce modifiers for consistent press feedback
- Added omniPremiumGradientBackground modifier for subtle depth
- Enhanced OmniMotion with screenEnter/screenExit/popEnter/popExit transitions
- Added miniPlayerEnter/miniPlayerExit transitions for smooth show/hide
- Added listItemDelayMs staggered entry timing

### Stats & Library Polish
- Cleaner summary cards with refined layout
- Better empty-state messaging
- Fixed horizontal padding consistency

### Search & Collection
- Refined search result rows for better thumbnail stability
- Collection page header and track list consistency

### Technical
- Bump version to 0.8.3 (versionCode 36)
- Improved key/contentType usage in Lazy layouts
- Removed dead imports and legacy code paths
- Performance: stable image sizes, no shimmer animation in repeated rows
