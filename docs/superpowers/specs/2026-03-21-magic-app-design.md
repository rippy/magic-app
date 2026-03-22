# Magic App — Design Spec
**Date:** 2026-03-21
**Target:** Magic Box 2.0 (Android 10, API 29, MTK6765 octa-core, 4GB RAM, 64GB storage)

---

## Overview

A native Android 10 companion app for the Magic Box 2.0 in-car device. The app serves two purposes: a day-to-day driver companion (media, news, voice assistant, AirPlay audio) and a diagnostic/showcase tool that surfaces all device capabilities. Built with Kotlin + Jetpack Compose. Distributed via GitHub Releases with a GitHub Pages sideload landing page and in-app auto-update.

---

## UI & Layout

The app has two modes, toggled by a single icon button (⊞) embedded in the top-right corner of the hero card alongside a mic mute button (🎙️) and settings (⚙️). These three controls are always visible in both modes with no dedicated status bar.

### Glance Mode (default — driving)
- **Hero card** (~60% of screen width): speed in large text, GPS coordinates, heading, altitude
- **Two small tiles** to the right of the hero: WiFi nearby count, BT device count
- **Bottom row** of four quick-launch tiles: News, YouTube, Web, AI
- Optimised for quick glances; minimal text, large values

### Detail Mode (parked / exploring)
- **Sidebar**: vertical column of icon-only nav items (all feature components)
- **Main panel**: selected feature fills the rest of the screen
- Toggle button returns to Glance Mode

### Full-screen features
- Any tile or sidebar item opens the feature full-screen
- Back arrow to return to previous mode
- Mic mute button floats as a small overlay in the corner

### Theme
Dark, high-contrast throughout. Safe for night driving.

---

## Feature Components

### Diagnostic / Showcase
| Component | Description |
|---|---|
| **GPS** | Live speed, coordinates, heading, altitude, accuracy, satellite count |
| **WiFi Scanner** | Nearby networks: SSID, signal strength, security type. Tap to refresh. |
| **Bluetooth Scanner** | Nearby and paired devices, signal strength. Tap to refresh. |
| **Microphone** | Live audio level meter, record/stop control, mute toggle. Foundation for voice. |
| **System Info** | CPU usage, RAM, storage, battery level/charging state, network type |
| **Compass** | Visual compass from device magnetometer. Useful at low speed when GPS heading stalls. |
| **Clock / Trip Timer** | Current time + start/stop trip timer |
| **Car Button Events** | Real-time log of incoming key events, hardware button presses, and broadcast intents. Shows event type, keycode, timestamp. Used to discover which car controls (e.g. mic button) send receivable events, and as a foundation for mapping them to app actions. |

### Media / Content
| Component | Description |
|---|---|
| **Web Browser** | Fullscreen WebView with URL bar, back/forward/refresh controls |
| **YouTube** | WebView pointed at YouTube mobile |
| **News Reader** | Fetches Reddit (configurable subreddit, default r/worldnews) and Hacker News top posts. Scrollable headline list. Two read-aloud modes (see below). |

#### News Reader — Read Aloud Modes
A toggle switches between two modes:
- **Headlines only**: TTS reads headline + one-sentence post description. Fast, no LLM.
- **Full summary**: fetches article content, sends to LLM for summary, TTS reads the result.

In both modes, auto-read plays continuously: TTS finishes one story, brief pause, advances to the next automatically. A pause button and story progress indicator (e.g. "3 / 15") are visible during playback.

### AI / Voice
| Component | Description |
|---|---|
| **Voice Assistant** | Push-to-talk or always-listening (when mic unmuted). Records audio → transcribes via Groq Whisper → sends to LLM → reads response via Android TTS. Conversation history held in memory for session context. |
| **LLM Settings** | Configure API keys for Groq, Gemini, OpenRouter. View per-provider usage counters. Drag to reorder failover priority. |
| **AirPlay Audio Receiver** | Advertises device as AirPlay receiver on local network via mDNS. Implements AirPlay 1 audio protocol using open-source library. Shows connection status. iPhone on shared WiFi can stream audio to the app. Full video/screen mirroring is a future stretch goal. |

---

## Architecture

### Code Structure
```
app/
  ui/
    glance/       # Glance mode layout + hero card
    detail/       # Detail mode layout + sidebar
    components/   # One subfolder per feature component
    theme/        # Colors, typography, dark theme
  features/
    gps/          # LocationManager wrapper, speed/heading/altitude StateFlow
    scanner/      # WiFi + BT scanning logic
    news/         # Reddit + HN API clients, TTS playback controller
    voice/        # SpeechRecognizer, audio recorder, mic state
    llm/          # Provider abstraction + failover manager
    airplay/      # mDNS advertisement + AirPlay 1 audio receiver
    updater/      # GitHub release checker + APK download/install
    events/       # Hardware key/button event capture
  data/
    prefs/        # API keys, settings (DataStore)
    usage/        # LLM usage tracking (Room DB)
```

### Key Principles
- Each feature exposes state as `StateFlow`. UI observes; features have no knowledge of UI.
- Single-module app. No feature-level modules needed at this stage.
- Minimum SDK: API 29 (Android 10). Target SDK: API 29.
- Language: Kotlin. UI: Jetpack Compose.

---

## LLM Provider Abstraction

### Interface
```
LLMProvider
  complete(prompt: String, history: List<Message>): String
  transcribe(audioFile: File): String
```

### Implementations
- `GroqProvider` — default. Offers Whisper transcription + LLaMA/Mixtral completion. Fast free tier.
- `GeminiProvider` — Google Gemini free tier. Fallback #1.
- `OpenRouterProvider` — access to multiple free models. Fallback #2.

### Failover Manager (`LLMManager`)
- Tries providers in configured priority order.
- On HTTP 429 (rate limit): marks provider as cooling down for 60 seconds, retries immediately with next provider.
- On non-rate-limit errors: logs and tries next provider.
- If all providers fail: surfaces error to UI with retry option.

### Usage Tracking
- Room DB stores: provider name, timestamp, token count (in/out), request count, error count.
- Visible per-provider in LLM Settings component.
- Used to inform failover decisions when providers don't expose usage via API.

---

## Dev Environment

### `shell.nix`
Provides a reproducible `nix-shell` with:
- JDK 17
- Android SDK (platform tools, build tools, Android platform 29)
- Gradle
- `adb`
- `ktlint`

Run `nix-shell` to enter. No flakes — compatible with SteamOS + Nix.

### `.envrc`
Uses `use nix` (direnv) to activate the shell automatically on `cd`.

### VSCode
- `.vscode/settings.json` points Kotlin/Java extensions at Nix-provided JDK and SDK paths.
- `.vscode/extensions.json` lists recommended extensions: Kotlin, Android tools, Nix language support.

---

## Distribution & Auto-Update

### Build & Release (GitHub Actions)
- On push to `main`: build signed debug APK, upload as GitHub Release asset.
- On version tag (e.g. `v1.0.0`): same, tagged as a formal release.

### Sideload Landing Page (GitHub Pages)
- Minimal page with a "Download Latest APK" button.
- Always links to the latest GitHub Release asset.
- This is the URL visited on the Magic Box browser to sideload.

### In-App Auto-Update
- On launch: silently calls GitHub Releases API, compares latest tag to running `versionName`.
- If newer: shows non-intrusive banner — "Update available — tap to install."
- Tapping downloads APK via Android `DownloadManager`, then launches system package installer.
- User taps "Install" once to confirm. Standard sideloaded update flow.
- Requires `REQUEST_INSTALL_PACKAGES` permission in manifest.

---

## Permissions Required
| Permission | Purpose |
|---|---|
| `ACCESS_FINE_LOCATION` | GPS coordinates, speed, heading |
| `ACCESS_WIFI_STATE`, `CHANGE_WIFI_STATE` | WiFi scanning |
| `BLUETOOTH`, `BLUETOOTH_ADMIN`, `ACCESS_FINE_LOCATION` | Bluetooth scanning |
| `RECORD_AUDIO` | Microphone, voice assistant |
| `INTERNET` | News feeds, LLM APIs, YouTube, GitHub update check |
| `REQUEST_INSTALL_PACKAGES` | Auto-update APK install |

---

## Out of Scope (v1)
- Video AirPlay / screen mirroring (stretch goal, post-v1)
- OBD-II integration
- Offline LLM inference
- Multi-user profiles
- CarPlay integration (this is explicitly not a CarPlay app)
