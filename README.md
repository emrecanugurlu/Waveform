# Waveform

Waveform is a privacy-first, ad-free podcast player built with **Kotlin Multiplatform**, **Compose Multiplatform**, and **Material 3 Expressive**.

---

## Features

- **Material 3 Expressive UI & Theme System**
  - Supports Light, Dark, and System theme modes across 4 palettes (`Studio`, `Ocean`, `Sunset`, and `Forest`).
  - Uses expressive shapes, animated audio waveforms, wavy progress indicators, carousels, split buttons, and floating player toolbars.
- **Discovery & Subscriptions**
  - Live podcast search via the Apple iTunes Search API with recent search chips, category browsing, starter picks, and instant RSS URL detection.
  - Inline episode search, sorting, filtering, and unplayed badges per subscription.
- **Background Playback & Player Controls**
  - Background media playback and lock-screen/notification controls powered by **AndroidX Media3** (`MediaSessionService` + `ExoPlayer`) on Android and `AVPlayer` / `AVAudioSession` / `MPNowPlayingInfoCenter` on iOS.
  - Persistent playback speed (`0.75x` – `2.5x`), configurable skip-back/forward intervals (`5s`–`60s`), sleep timer (minutes or end-of-episode), interactive chapters parsed from show notes, and a manageable play queue.
- **Offline Downloads**
  - Stream or download episodes for offline playback with progress tracking and orphaned-file cleanup; downloaded audio is excluded from Android cloud backups.
- **Bookmarks & Detailed Listening History**
  - Save timestamped episode bookmarks with optional notes and jump back to exact positions.
  - Full chronological listening history grouped by date (`Today`, `Yesterday`, `This Week`, `Older`) with search, filters, active streak tracking, 7-day activity chart, and top podcasts breakdown.
- **OPML 2.0 Backup & Restore**
  - Export subscriptions to standard OPML 2.0 XML and bulk import feeds from OPML files.
- **Background Feed Refresh & Notifications**
  - Periodic background refresh on Android via **WorkManager** with opt-in notifications for newly published episodes.

---

## Tech Stack

| Layer | Technology | Version |
| --- | --- | --- |
| Language | Kotlin Multiplatform | `2.4.10` |
| UI | Compose Multiplatform | `1.11.1` |
| Design System | Material 3 (`material3`) | `1.11.0-alpha07` |
| Android Gradle Plugin | AGP | `9.0.1` |
| Android SDK | `minSdk` / `targetSdk` / `compileSdk` | `26` / `36` / `36` |
| Audio Playback | AndroidX Media3 (`exoplayer`, `session`) | `1.5.1` |
| Background Work | AndroidX WorkManager | `2.10.0` |
| Networking | Ktor Client (`OkHttp` / `Darwin`) | `3.1.1` |
| Image Loading | Coil 3 | `3.1.0` |
| Persistence / JSON | Kotlinx Serialization & Coroutines | `1.8.0` / `1.10.1` |

---

## Project Structure

```text
Waveform/
├── androidApp/                 # Android application entry point, manifest, launcher icons, R8 rules
├── shared/
│   └── src/
│       ├── commonMain/         # Shared ViewModel, RSS/OPML codecs, storage, models, Compose UI
│       ├── androidMain/        # Media3 service, WorkManager refresh worker, Android file/HTTP/prefs
│       ├── iosMain/            # AVPlayer audio engine, Darwin HTTP, NSFileManager storage
│       ├── commonTest/         # Shared unit tests (RSS, OPML, queue, formatting, store, ViewModel)
│       └── androidHostTest/    # Android storage safety tests
├── iosApp/                     # Xcode SwiftUI wrapper hosting ComposeUIViewController
├── gradle/libs.versions.toml   # Centralized dependency versions
├── keystore.properties.example # Template for signed release builds
└── PRIVACY_POLICY.md           # Bilingual privacy policy (TR / EN)
```

---

## Getting Started

### Prerequisites
- **JDK 11+** (JDK 21 / Android Studio JBR recommended)
- **Android Studio** (for Android builds and signing)
- **Xcode** on macOS (only required for building/running `iosApp`)

### Run Unit Tests

On Windows:
```powershell
.\gradlew.bat :shared:testAndroidHostTest
```

On macOS / Linux:
```bash
./gradlew :shared:testAndroidHostTest
```

### Build Debug APK

```bash
./gradlew :androidApp:assembleDebug
```

### Build Signed Release App Bundle (`.aab`)

1. Copy [`keystore.properties.example`](keystore.properties.example) to `keystore.properties` in the project root (ignored by Git).
2. Generate an upload keystore (or point to your existing `.jks`) and fill in `storeFile`, `storePassword`, `keyAlias`, and `keyPassword`.
3. Build the minified release bundle:
   ```bash
   ./gradlew :androidApp:bundleRelease
   ```

---

## Privacy

Waveform requires no account, runs no ads, and includes no analytics or tracking SDKs. All user state and offline audio files stay on-device; search queries are sent only to the Apple iTunes Search API, and feeds/media are fetched directly from the publisher URLs configured by the user.

See [`PRIVACY_POLICY.md`](PRIVACY_POLICY.md) for the full policy (Turkish & English).
