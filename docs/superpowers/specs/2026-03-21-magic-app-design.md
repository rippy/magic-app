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
- **Two small tiles** to the right of the hero: WiFi nearby count, BT device count. Values reflect the most recent manual scan — no automatic background scanning occurs in Glance Mode. Each tile shows a "last scanned" timestamp.
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
| --- | --- |
| **GPS** | Live speed, coordinates, heading, altitude, accuracy, satellite count |
| **WiFi Scanner** | Nearby networks: SSID, signal strength, security type. Manual refresh only (see WiFi scanning notes below). |
| **Bluetooth Scanner** | Nearby BLE devices with RSSI/signal strength. Manual refresh only. Paired devices listed separately. |
| **Microphone** | Live audio level meter, record/stop control, mute toggle. Foundation for voice. |
| **System Info** | CPU usage, RAM, storage, battery level/charging state, network type |
| **Compass** | Visual compass from device magnetometer. Useful at low speed when GPS heading stalls. |
| **Clock / Trip Timer** | Current time + start/stop trip timer |
| **Car Button Events** | Real-time log of incoming key events, hardware button presses, and broadcast intents. Shows event type, keycode, timestamp. Used to discover which car controls (e.g. mic button) send receivable events, and as a foundation for mapping them to app actions. |

#### WiFi Scanning Notes (API 29)

On Android 10, `WifiManager.startScan()` is throttled by the OS to ~4 scans per 2 minutes in the foreground; results may be returned from cache rather than a fresh scan. Behaviour:

- Scanning is manual (tap to refresh) — never automatic in the background.
- Results display a "Last scanned: X seconds ago" timestamp.
- When results are served from OS cache, a visible warning is shown: "Results may be from cache."
- No error is thrown for cached results — they are treated as valid but flagged.

#### Bluetooth Scanner Notes (API 29)

- Uses BLE scanning (`BluetoothLeScanner`) rather than classic `BluetoothAdapter.startDiscovery()`. This provides reliable RSSI values via `ScanResult.getRssi()`.
- Requires `ACCESS_FINE_LOCATION` at runtime on API 29 for BLE scanning.
- **Runtime prerequisite:** BLE scanning silently returns no results if the device's system location toggle (Settings → Location) is off, even with the permission granted. The app must check `LocationManager.isProviderEnabled(GPS_PROVIDER)` before scanning and prompt the user to enable location services if it is off.
- Classic BT paired devices are listed separately via `BluetoothAdapter.getBondedDevices()` (no location permission required).

### Media / Content

| Component | Description |
| --- | --- |
| **Web Browser** | Fullscreen WebView with URL bar, back/forward/refresh controls |
| **YouTube** | WebView pointed at YouTube mobile |
| **News Reader** | Fetches Reddit (configurable subreddit, default r/worldnews) and Hacker News top posts. Scrollable headline list. Two read-aloud modes (see below). |

#### News Reader — Read Aloud Modes

A toggle switches between two modes:

- **Headlines only**: TTS reads headline + one-sentence post description. Fast, no LLM call.
- **Full summary**: fetches article body text using a Readability-style HTML extraction library (e.g., a Kotlin/Java port of Mozilla Readability or jsoup-based extraction), sends extracted text to LLM for a 2-3 sentence summary, TTS reads the result. Falls back to headline-only if extraction fails (paywall, JS-rendered page, bot block).

In both modes, auto-read plays continuously: TTS finishes one story, brief pause, advances to the next automatically. A pause button and story progress indicator (e.g. "3 / 15") are visible during playback.

### AI / Voice

| Component | Description |
| --- | --- |
| **Voice Assistant** | Push-to-talk or always-listening (when mic unmuted). Records audio → transcribes via Groq Whisper API → sends to LLM → reads response via Android TTS. Conversation history: last 10 exchanges, cleared on app restart or after 30 minutes of idle. Runs as a foreground service when always-listening is active. |
| **LLM Settings** | Configure API keys for Groq, Gemini, OpenRouter. View per-provider usage counters. Drag to reorder failover priority. |
| **AirPlay Audio Receiver** | Advertises device as AirPlay receiver on local network via Android `NsdManager`. Implements AirPlay 1 audio protocol. **Note: library selection is an open research question** — no well-maintained Android-native AirPlay 1 library is confirmed at spec time. This feature is treated as a spike: if a suitable library (native C/C++ wrapped via JNI, or a pure-Java implementation) is found during initial implementation, it proceeds to v1; otherwise it is deferred to v2. Shows connection status when active. Full video/screen mirroring is deferred to v2. |

#### Voice Assistant — Always-Listening Implementation

- When always-listening mode is active, the app runs a **foreground service** (required for persistent mic access on API 29) with a persistent notification showing mic state.
- `SpeechRecognizer` is restarted automatically on silence timeout — it does not run continuously but loops.
- **Important:** `SpeechRecognizer` must be instantiated and called on the main thread (via `Looper.getMainLooper()`), even when the restart loop is orchestrated from a service. The service posts recogniser calls to the main looper; it does not call them directly from the service thread.
- A visual indicator in the UI distinguishes: **Listening** (active, awaiting speech) vs **Processing** (speech received, awaiting LLM response) vs **Idle** (paused/muted).
- No wake word in v1 — always-listening means continuously restarting the recogniser. Wake word support is a v2 consideration.

---

## Architecture

### Build Configuration

- `minSdk`: 29 (Android 10)
- `targetSdk`: 29 (acceptable for sideloaded apps — not subject to Play Store targetSdk requirements). The Magic Box 2.0 runs API 29, so `targetSdk == runtimeSdk` — no compatibility mode gap in practice. If the device OS is ever updated, the app will continue to run under its declared `targetSdk 29` behaviour; no action needed unless features relying on newer API semantics are added.
- `compileSdk`: 34 (required for current Jetpack Compose releases)

### Code Structure

```text
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
    voice/        # SpeechRecognizer, audio recorder, mic state, foreground service
    llm/          # Provider abstraction + failover manager
    airplay/      # NsdManager advertisement + AirPlay 1 audio receiver (spike)
    updater/      # GitHub release checker + APK download/install
    events/       # Hardware key/button event capture
  data/
    prefs/        # API keys, settings (DataStore)
    usage/        # LLM usage tracking (Room DB)
```

### Key Principles

- Each feature exposes state as `StateFlow`. UI observes; features have no knowledge of UI.
- Single-module app. No feature-level modules needed at this stage.
- Language: Kotlin. UI: Jetpack Compose.

---

## LLM Provider Abstraction

### Interface

All methods are `suspend` functions — called within a `CoroutineScope` tied to the appropriate lifecycle (`viewModelScope` for UI-bound calls, the foreground service scope for voice assistant calls).

```kotlin
interface LLMProvider {
    suspend fun complete(prompt: String, history: List<Message>): String
    suspend fun transcribe(audioFile: File): String
}
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

### Conversation History

- Retained in memory for the last 10 exchanges (20 messages: 10 user + 10 assistant).
- Cleared on app restart or after 30 minutes of inactivity.
- Approximate token budget: ~2000 tokens of history maximum. Oldest exchanges dropped first when budget is exceeded. Token count approximated as `characters / 4` — no provider-specific tokenizer required.

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

Two distinct workflows:

1. **Push to `main`**: builds a release-signed APK, uploads as a GitHub **pre-release** (not a formal release). Used for development builds. Does not affect the "latest" release endpoint.
2. **Version tag** (e.g. `v1.0.0`): builds and uploads as a formal **GitHub Release**. This is what the auto-update checker and sideload landing page target.

Both workflows use a release keystore stored as a GitHub Actions secret. The same keystore must be used for all builds — Android rejects installs where the signing certificate changes between versions.

### Sideload Landing Page (GitHub Pages)

- Minimal page with a "Download Latest APK" button.
- Links to the latest formal GitHub Release asset via the `/releases/latest` API endpoint.
- This is the URL visited on the Magic Box browser to sideload.

### In-App Auto-Update

- On launch: silently calls GitHub `/releases/latest` API, compares tag to running `versionName`.
- If newer: shows non-intrusive banner — "Update available — tap to install."
- Tapping downloads APK via Android `DownloadManager`, then launches system package installer.
- The install intent uses `DownloadManager.getUriForDownloadedFile(downloadId)` to obtain a `content://` URI — **do not use a `file://` URI**, which causes `FileUriExposedException` on API 24+.
- User taps "Install" once to confirm. Standard sideloaded update flow.
- Requires `REQUEST_INSTALL_PACKAGES` permission in manifest.

---

## Permissions Required

| Permission | Purpose |
| --- | --- |
| `ACCESS_FINE_LOCATION` | GPS coordinates, speed, heading; also required for BLE scanning on API 29 |
| `ACCESS_WIFI_STATE`, `CHANGE_WIFI_STATE` | WiFi scanning |
| `BLUETOOTH`, `BLUETOOTH_ADMIN` | Bluetooth device discovery and BLE scanning |
| `RECORD_AUDIO` | Microphone, voice assistant |
| `INTERNET` | News feeds, LLM APIs, YouTube, GitHub update check |
| `REQUEST_INSTALL_PACKAGES` | Auto-update APK install |
| `FOREGROUND_SERVICE` | Always-listening voice assistant foreground service |

---

## Out of Scope (v1)

- AirPlay audio receiver (promoted to v1 if a suitable Android library is confirmed during initial spike; otherwise deferred to v2)
- Video AirPlay / screen mirroring (v2)
- Wake word detection for always-listening voice (v2)
- OBD-II integration
- Offline LLM inference
- Multi-user profiles
- CarPlay integration (this is explicitly not a CarPlay app)
