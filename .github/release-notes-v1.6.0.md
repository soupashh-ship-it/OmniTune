# OmniTune 1.6 Release — Official iOS Launch & Multiplatform Debut

We are thrilled to announce **OmniTune 1.6**, marking the official debut of OmniTune on Apple iOS! 

Powered by **Kotlin Multiplatform (KMP)** and **Compose Multiplatform**, OmniTune brings its modern music player experience, rich YouTube Music streaming engine, and synchronized lyrics to iPhone and iPad devices—while preserving independent release cycles and uninterrupted stability for our Android community.

---

## 🚀 What's New in 1.6

### 📱 Official iOS Debut (`iosApp`)
- **Native iOS Target**: Complete, standalone native Xcode application target (`iosApp`) leveraging SwiftUI and Compose Multiplatform.
- **Apple AVFoundation Audio Engine**: Native background audio playback powered by Apple's `AVPlayer` and `AVAudioSessionCategoryPlayback`.
- **System Media Integration**: Full Control Center, Lock Screen, and Dynamic Island controls via `MPRemoteCommandCenter` and `MPNowPlayingInfoCenter` with live artwork, elapsed time, and scrubber seeking.
- **Background Playback**: Seamless background streaming with automatic route-change handling (auto-pauses when headphones disconnect).

### 🎨 Signature SuvMusic Experience on iOS
- **SuvMusicTheme & Palettes**: 5 expressive color schemes (Default/Purple, Ocean, Sunset, Nature, Love) with dynamic accent adaptation.
- **Outfit Variable Typography**: Clean, expressive Material 3 typography scaled specifically for mobile displays.
- **Continuous-Curvature Squircles**: Smooth iOS-style rounded geometry (`SquircleShape`) applied across cards, search bars, and action chips.
- **Pure Black AMOLED Mode**: Deep contrast optimized for OLED displays with WCAG AA compliance.
- **Signature Player Suite**:
  - MiniPlayer with smooth rotating vinyl album art animation and marquee text.
  - Full-screen Expandable Player Sheet with album hero backdrops.
  - 9 visualizer waveform seeker styles with live playback progress.
  - Modern Reorderable Queue with equalizer animations.

### 🎵 Enhanced Streaming & Synchronized Lyrics
- **Direct AAC Stream Resolution**: High-fidelity YouTube Music stream resolution prioritizing native AAC (`itag 140` and `itag 139`) for smooth, hardware-accelerated decoding on iOS.
- **Pure Kotlin PoToken Generator**: Cryptographically secure Proof-of-Origin token generation (binary packing, XOR salt masking, HMAC-SHA256) running natively without WebViews or external engines.
- **Multi-Provider Synchronized Lyrics**: Unified lyrics engine fetching from LRCLIB, KuGou, and SimpMusic with millisecond timestamp precision, active line highlighting, auto-scrolling, and tap-to-seek playback.

### 🛡️ Independent Release Architecture & Zero Regressions
- **Decoupled Release Pipelines**: Android (`:app`) and iOS (`iosApp`) remain completely independent application targets. Updates to either platform can be released without cross-coupling.
- **Android Stability Preserved**: The native Android application remains 100% intact with zero breaking changes or regressions.

---

## 🧪 Quality & Verification Matrix

- **Master E2E Test Suite**: 265 / 265 tests passing across all 4 tiers (Feature, Boundary, Integration, and Real-World Scenarios).
- **Multiplatform & Stress Tests**: 182 / 182 desktop tests passing across 21 test suites, including 13 Tier 5 adversarial stress tests.
- **Android Unit Tests**: 333 / 333 unit tests passing (`:app:testDebugUnitTest`).
- **Independent Victory Audit**: Verified `CLEAN` and `VICTORY CONFIRMED` by independent audit protocols.

---

## 📦 Packages & Build Targets

- **Android Target**: `:app` (Package: `com.omnitune.app`, Version: `1.6.0`, Code: `160`)
- **iOS Target**: `iosApp` (Bundle ID: `com.omnitune.iosApp`, Xcode Project: `iosApp/iosApp.xcodeproj`)
- **Shared Framework**: `:shared` (Kotlin Multiplatform / Compose Multiplatform)
- **Status**: Stable Release

---
*OmniTune — Free, open-source music streaming for Android & iOS.*
