# HrvXo - Development Log & TODO

## Last Session

- **Date:** 2026-02-12
- **Summary:** Fixed session screen layout, versioned APK filenames, replaced broken YTM auto-return with notification + AlarmManager, added live movement warning
- **Key changes:**
  - Deleted old HeartSyncRadio logo.png, updated CREDENTIALS.md references
  - APK filename now includes version (HrvXo-v1.3.2.apk)
  - Made NotStartedContent scrollable; Start Session button hidden until notification access granted
  - Replaced broken moveTaskToFront/staggered retry auto-return with:
    - Persistent "Tap to return to HrvXo" notification (standard Android pattern)
    - AlarmManager PendingIntent auto-return (system-sent, exempt from background activity restrictions)
  - Added live red "Movement detected. Please remain still during data acquisition." warning during recording
  - Wired MovementDetector.isMoving StateFlow through ViewModel → Activity → App → SessionScreen
  - Removed REORDER_TASKS permission, added POST_NOTIFICATIONS
  - Version bump to v1.3.2 (versionCode 6)
- **Stopped at:** v1.3.2 released. All priority TODO items complete.
- **Blockers:** YTM auto-return may not work on Android 14+ (AlarmManager PendingIntent exemption removed); notification fallback covers this

---

## Current Status

### Working Features
- Compose Multiplatform project builds successfully (Android target)
- BLE permission request flow (Android 12+ and older)
- Device chooser UI: Polar H10 or Generic BLE HR Monitor selection
- Polar H10 device scanning (finds nearby Polar devices)
- Generic BLE HR sensor scanning and connection
- Device connection with auto HR streaming
- Live heart rate (BPM), RR intervals, coherence score, and RMSSD display
- Connection status + battery level display
- Error handling and disconnect
- HRV processing pipeline: artifact detection (cubic spline), RMSSD, coherence score, LF/HF band powers
- 40 unit tests all passing (PhysioNet spectral validation within 15% of scipy reference)
- App icon (generated from logo, adaptive icon support)
- HRV_METHODOLOGY.md scientific methodology document
- YouTube Music backend deployed at `https://hrvxo-music.fly.dev` (FastAPI + ytmusicapi)
- Ktor HTTP client wired to backend (search songs, create playlists)
- SQLDelight local database for song-coherence persistence
- Session screen UI with 6 phase states and song search bottom sheet
- Coherence-based playlist generation from user's actual leaderboard data

- Auto music detection via NotificationListenerService (track changes + play/pause)
- Session pause YTM on end, auto-return to app after YTM launch
- Early-end handling with user-friendly message
- Movement detection via accelerometer + HR anomaly flagging
- v1.1.0 released on GitHub with signed APK

### In Progress
- None — all priority items complete

### Known Bugs
- YTM ads play before songs — cannot be controlled externally (YTM Premium only)

---

## TODO - Priority

1. [x] **HRV & Coherence — Validation Complete:**
    - [x] Implement artifact removal with cubic spline interpolation (Quigley spec)
    - [x] Calculate coherence score based on PSD (HeartMath algorithm)
    - [x] Add LF and HF band powers (no ratio)
    - [x] Fix PSD normalization to match scipy (ms²/Hz units)
    - [x] Re-run all 40 tests after PSD normalization fix — all pass
    - [x] PhysioNet spectral powers within 15% of scipy reference (LF: 5.8% error, HF: 8.8% error)
    - [x] Tightened spectral power tolerances from 50% to 15%
    - [x] HRV metrics displayed in UI (coherence score + RMSSD)
2. [x] **YouTube Music Integration (replaced Spotify):**
    - [x] Build FastAPI backend with ytmusicapi (POST /search, POST /create-playlist, GET /health)
    - [x] Deploy to Fly.io Sydney region
    - [x] Wire Ktor HTTP client in Android app
    - [x] Song search and tagging via backend proxy
3. [x] **Music Session Mode:**
    - [x] Session screen with 5 phase states (NOT_STARTED, ACTIVE_NO_SONG, ACTIVE_SETTLING, ACTIVE_RECORDING, ENDED)
    - [x] 15-second settle-in exclusion at start of each new song
    - [x] Minimum 60 seconds per song for valid coherence reading
    - [x] Per-song coherence result cards with validity indicator
    - [x] Session summary on End: songs analysed, average coherence, best song
    - [x] Manual song tagging (YouTube Music has no "now playing" API)
    - [x] Auto music detection via NotificationListenerService (replaces manual-only tagging)
    - [x] ACTIVE_WAITING_PLAYBACK phase for pending song detection
4. [x] **Song-Coherence Database:**
    - [x] SQLDelight local database (song_coherence table)
    - [x] Store per-song coherence results after each valid reading
    - [x] Top coherence songs leaderboard (AVG grouped by video_id)
    - [x] Song count tracking for playlist generation threshold
5. [x] **Playlist Generation:**
    - [x] Progress indicator: "3/3 songs — ready for playlist" (3 song minimum for MVP)
    - [x] "Create Coherence Playlist" button: takes top songs from leaderboard, creates YouTube Music playlist
    - [x] Primary generation always from user's actual coherence leaderboard
6. [x] **Movement Detection:**
    - [x] Phone accelerometer monitoring via Android SensorManager (TYPE_LINEAR_ACCELERATION)
    - [x] Rolling movement score: flag when acceleration exceeds resting threshold
    - [x] HR anomaly detection: flag sudden HR spikes (>30 BPM above rolling average) not explained by music change
    - [x] Per-song movement badge: "Movement detected — score may be less accurate"
    - [x] Live red warning on recording screen when movement detected

---

## TODO - Nice to Have

- [ ] Cold-start suggestions for new users with zero session data (search "ambient meditation calm", etc.)
- [ ] Onboarding flow: Polar H10 pairing guide → YouTube Music setup → first session walkthrough
- [ ] Genre/tempo analysis: correlate audio features with coherence scores
- [ ] Re-listen mode: replay top coherence songs and see if scores hold across sessions
- [ ] iOS support (KMP structure ready, targets commented out)
- [ ] Dark mode / theme customization
- [ ] Export coherence session data (CSV/JSON)
- [ ] Historical session tracking and trends chart
- [x] App icon and branding assets (generated from logo)
- [ ] Session reminders / streak tracking ("Listen for 10 minutes daily")
- [ ] Insights: "Your coherence is X% higher with slower tempo songs" (after 20+ songs)

---

## Completed

- [x] Set up Compose Multiplatform project for Android and iOS (2026-02-03)
- [x] Rename the project to "HeartSyncRadio" (2026-02-03)
- [x] Integrate the Polar SDK for Android (2026-02-03)
- [x] Implement logic to search for, connect to, and receive data from H10 sensor (2026-02-03)
- [x] Design Home Screen as central hub for Polar H10 connection (2026-02-03)
- [x] Set up standard repo files (.gitignore, CREDENTIALS.md, SECURITY_AUDIT.md) (2026-02-03)
- [x] Implement HRV processing pipeline: ArtifactDetector, Rmssd, CoherenceCalculator, HrvProcessor, Fft (2026-02-03)
- [x] Rewrite ArtifactDetector with cubic spline interpolation per Quigley spec (2026-02-03)
- [x] Add LF/HF band powers to CoherenceCalculator and HrvMetrics (2026-02-03)
- [x] Fix frequency resolution bug in CoherenceCalculator (segment size vs total data) (2026-02-03)
- [x] Add PSD normalization for correct ms²/Hz units (2026-02-03)
- [x] Write 40 unit tests including PhysioNet real-data validation (2026-02-03)
- [x] Download PhysioNet Subject 000 RR data and compute scipy reference values (2026-02-03)
- [x] Device chooser UI: Polar H10 / Generic BLE HR Monitor selection (2026-02-04)
- [x] HrDeviceManager interface + GenericBleManager for multi-device support (2026-02-04)
- [x] HomeViewModel flatMapLatest for dynamic device manager switching (2026-02-04)
- [x] App icon from logo — all mipmap densities + adaptive icon (2026-02-04)
- [x] HRV_METHODOLOGY.md scientific methodology document (2026-02-04)
- [x] Switch spectral integration to trapezoidal rule (LF error 23%→5.8%, HF 16.5%→8.8%) (2026-02-04)
- [x] Extract CubicSpline.kt shared utility from ArtifactDetector (2026-02-04)
- [x] Tighten PhysioNet spectral test tolerances from 50% to 15% (2026-02-04)
- [x] Build YouTube Music backend (FastAPI + ytmusicapi) with search and playlist creation (2026-02-10)
- [x] Deploy backend to Fly.io Sydney region (2026-02-10)
- [x] Wire Ktor HTTP client to backend (2026-02-10)
- [x] Set up SQLDelight database for song-coherence tracking (2026-02-10)
- [x] Build SessionManager with 15s settle-in and 60s minimum recording (2026-02-10)
- [x] Build SessionViewModel with search, tag, end session, create playlist (2026-02-10)
- [x] Create SessionScreen UI with 5 session phases (2026-02-10)
- [x] Add enum-based navigation (HOME ↔ SESSION) (2026-02-10)
- [x] Wire MainActivity with SessionViewModel integration (2026-02-10)
- [x] Add auto music detection via NotificationListenerService (2026-02-11)
- [x] Fix release APK signing — add debug signingConfig to release build type (2026-02-11)
- [x] Bump version to v1.1.0 and create GitHub Release (2026-02-11)
- [x] Fix session auto-detecting already-playing songs — baseline snapshot + hasUserSelectedSong flag (2026-02-11)
- [x] Replace "Collecting data ~30s" card with subtle "Analysing heart rate..." text (2026-02-11)
- [x] Add 60s milestone notice and movement instructions to session screen (2026-02-11)
- [x] Add auto-return to HrvXo after opening YouTube Music (2026-02-11)
- [x] Add YTM pause on session end via MediaController (2026-02-11)
- [x] Add early-end message card for sessions ended before 60s (2026-02-11)

---

## Architecture & Decisions

| Decision | Reason | Date |
|----------|--------|------|
| Compose Multiplatform (KMP) | Cross-platform with shared UI, Android-first | 2026-02-03 |
| Single `composeApp` module | Simpler for greenfield; extract shared module later | 2026-02-03 |
| PolarManager with StateFlows | Clean bridge from RxJava (Polar SDK) to Compose | 2026-02-03 |
| minSdk 26 | Required by Polar BLE SDK 6.14.0 | 2026-02-03 |
| No expect/actual for ViewModel | Android-first; pass state as params to commonMain UI | 2026-02-03 |
| Manual DI (AppModule singleton) | No DI framework needed at this scale | 2026-02-03 |
| Cubic spline for artifact correction | Quigley spec: don't delete beats, interpolate to preserve time-series integrity | 2026-02-03 |
| No LF/HF ratio | Scientifically unsound; individual LF and HF band powers are valid | 2026-02-03 |
| Pure Kotlin FFT (Cooley-Tukey) | KMP commonMain can't use platform FFT libs; keeps code cross-platform | 2026-02-03 |
| HeartMath coherence algorithm | Peak power ratio in 0.04-0.26 Hz band; 64s sliding window | 2026-02-03 |
| Trapezoidal integration for band power | More accurate than rectangular; reduced spectral error by ~60% | 2026-02-04 |
| Linear resampling over cubic spline | Spline overshoots on irregular RR data; HF error 8.8% vs 78.9% with spline | 2026-02-04 |
| HrDeviceManager interface | Abstracts Polar SDK vs standard BLE; enables device chooser | 2026-02-04 |
| 60s minimum per song | Task Force HRV guidelines: 60s minimum for reliable short-term metrics; aligns with 64s coherence window | 2026-02-04 |
| 15s settle-in exclusion | Cardiac response to new auditory stimulus stabilises in ~10-15s; 30s too aggressive for session flow | 2026-02-04 |
| YouTube Music via backend proxy | Spotify registration frozen; ytmusicapi provides search + playlist creation via FastAPI backend | 2026-02-10 |
| Manual song tagging | YouTube Music has no "now playing" API (unlike Spotify); user searches and taps to tag | 2026-02-10 |
| Ktor 3.0.3 HTTP client | KMP-compatible; ContentNegotiation with kotlinx.serialization.json | 2026-02-10 |
| SQLDelight for local DB | KMP-compatible; type-safe SQL with coroutines Flow support | 2026-02-10 |
| Enum-based navigation | Simple state-driven screen switching; no nav library needed at this scale | 2026-02-10 |
| 3 song minimum for playlist | MVP threshold; lower than original 10 to let users experience playlist creation faster | 2026-02-10 |
| Leaderboard-based playlists | Primary generation always from user's actual coherence data, not genre assumptions | 2026-02-10 |
| NotificationListenerService for auto-detection | YTM has no "now playing" API; MediaSessionManager provides track metadata and playback state | 2026-02-11 |
| Baseline track snapshot on session start | Prevents auto-detecting already-playing song; only reacts to NEW tracks after user selects via search | 2026-02-11 |
| Debug signingConfig for release builds | Proper signing with release keystore deferred; debug keystore allows APK installation for testing | 2026-02-11 |
| Version bump per release | User requires GitHub Releases to be versioned — bump versionCode/versionName for each release | 2026-02-11 |

---

## Notes

- Polar SDK uses RxJava3; we bridge to Kotlin StateFlows in PolarManager
- Polar BLE SDK's `PolarBleApiCallback` requires implementing `disInformationReceived(String, DisInfo)` and `htsNotificationReceived` in v6.14.0
- `searchForDevice()` Flowable runs indefinitely until disposed -- always show Stop button
- PolarManager is a singleton tied to Activity lifecycle (shutDown in onDestroy when isFinishing)
- JetBrains lifecycle libraries use different versioning than AndroidX (2.8.4 not 2.8.7)
- PhysioNet Subject 000 (53M, healthy) scipy reference values for 5-min segment: RMSSD=52.62ms, MeanHR=61.23bpm, LF=273.53ms², HF=283.39ms²
- Welch PSD params matching scipy: nperseg=256, noverlap=128, window='hann', fs=4.0Hz
- JAVA_HOME needs to be set to Android Studio JBR for Gradle: `C:\Program Files\Android\Android Studio\jbr`
- **IMPORTANT: Always commit and push to the private GitHub repo before finishing a session**
- Remote configured: private GitHub repo HrvXo
- YouTube Music backend at `https://hrvxo-music.fly.dev` — uses base64-encoded OAuth (YTMUSIC_OAUTH_B64 env var)
- oauth.json is gitignored — never commit it
- SessionViewModel uses lambda provider `() -> HrDeviceManager` to get current device manager (avoids coupling to AppModule nullable state)
