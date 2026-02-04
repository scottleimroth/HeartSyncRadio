# HeartSync Radio - Development Log & TODO

## Last Session

- **Date:** 2026-02-03
- **Summary:** Implemented HRV processing pipeline with cubic spline artifact correction, spectral analysis, and unit tests validated against PhysioNet data
- **Key changes:**
  - Rewrote ArtifactDetector to use cubic spline interpolation (per Quigley spec) instead of median substitution. Two-pass: detect artifacts, then interpolate. No deletions — output length always matches input.
  - Added LF (0.04-0.15 Hz) and HF (0.15-0.40 Hz) band power to CoherenceCalculator and HrvMetrics. No LF/HF ratio (scientifically unsound).
  - Fixed bug in CoherenceCalculator where frequency resolution was computed from total data length instead of Welch segment size (was causing wrong bin mappings).
  - Added proper Welch PSD normalization (2/(fs*S2) one-sided) so band powers output in ms²/Hz units matching scipy.signal.welch.
  - Created 40 unit tests: RMSSD, ArtifactDetector (including spline accuracy), FFT (Parseval's theorem), CoherenceCalculator (synthetic sine modulations), HrvProcessor (incremental feeding), PhysioNet validation (Subject 000 real RR data).
  - Downloaded PhysioNet "RR Interval Time Series from Healthy Subjects" (Subject 000, 5-min segment) and computed scipy reference values for cross-validation.
  - Added kotlin-test dependency to commonTest source set.
- **Stopped at:** PSD normalization fix applied but tests not yet re-run. PhysioNet validation test for spectral powers needs verification after the normalization fix.
- **Blockers:** None. Will need Spotify Developer credentials when reaching Spotify integration.

---

## Current Status

### Working Features
- Compose Multiplatform project builds successfully (Android target)
- BLE permission request flow (Android 12+ and older)
- Polar H10 device scanning (finds nearby Polar devices)
- Device connection with auto HR streaming
- Live heart rate (BPM) and RR interval display
- Connection status + battery level display
- Error handling and disconnect
- HRV processing pipeline: artifact detection (cubic spline), RMSSD, coherence score, LF/HF band powers
- 40 unit tests (34 passing pre-normalization fix, 6 PhysioNet validation tests added)

### In Progress
- HRV pipeline validation (PSD normalization fix needs test run)

### Known Bugs
- None identified yet (untested on physical device)

---

## TODO - Priority

1. [ ] **HRV & Coherence — Finish Validation:**
    - [x] Implement artifact removal with cubic spline interpolation (Quigley spec)
    - [x] Calculate coherence score based on PSD (HeartMath algorithm)
    - [x] Add LF and HF band powers (no ratio)
    - [x] Fix PSD normalization to match scipy (ms²/Hz units)
    - [ ] Re-run all 40 tests after PSD normalization fix
    - [ ] Verify PhysioNet validation test spectral powers are within tolerance of scipy reference (LF: 273.5 ms², HF: 283.4 ms²)
    - [ ] Consider tightening spectral power tolerances if results are close
    - [ ] Wire HRV metrics into the UI (HomeScreen / HeartRateDisplay)
2. [ ] **Spotify Integration:**
    - [ ] Register Spotify Developer app, get client ID + redirect URI
    - [ ] Implement Spotify OAuth (PKCE flow for mobile — no client secret needed)
    - [ ] Fetch user playlists and track information
    - [ ] Detect currently playing track (Spotify Web API `player/currently-playing`)
    - [ ] Create new playlists via API
3. [ ] **Music Session Mode:**
    - [ ] Session screen: Start/End Session button, real-time coherence graph, current track display
    - [ ] 60-second baseline period (no music) to establish resting coherence before songs play
    - [ ] 15-second settle-in exclusion at start of each new song (cardiac transition response)
    - [ ] Auto-detect song changes via Spotify API polling (no per-song button needed)
    - [ ] Minimum 60 seconds of listening per song for a valid coherence reading (Task Force HRV guideline)
    - [ ] If song skipped before 60s: toast "Skipped — need at least 1 min for a reading"
    - [ ] Per-song feedback card after each track: coherence score with colour rating (red/amber/green)
    - [ ] Session summary on End: songs analysed, average coherence, top song, movement warnings
4. [ ] **Movement Detection:**
    - [ ] Phone accelerometer monitoring via Android SensorManager (TYPE_LINEAR_ACCELERATION)
    - [ ] Rolling movement score: flag when acceleration exceeds resting threshold
    - [ ] HR anomaly detection: flag sudden HR spikes (>30 BPM above rolling average) not explained by music change
    - [ ] Combine both signals: accelerometer catches phone movement, HR catches body movement when phone is stationary
    - [ ] Per-song movement badge: "Movement detected — score may be less accurate" (data kept, weighted lower)
    - [ ] If excessive movement throughout song: "Too much movement — no reading recorded"
5. [ ] **Song-Coherence Database:**
    - [ ] Set up SQLDelight local database
    - [ ] Schema: track_id, track_name, artist, avg_coherence, peak_coherence, avg_hr, rmssd, duration_listened, baseline_coherence, movement_confidence, session_date
    - [ ] Store per-song coherence results after each valid reading
    - [ ] Track improvement over multiple sessions
6. [ ] **Playlist Generation:**
    - [ ] Progress indicator: "8/10 songs — almost ready for your first playlist!"
    - [ ] Minimum 10 songs with valid readings before first playlist can be generated
    - [ ] "Create Coherence Playlist" button: takes top-scoring songs, creates Spotify playlist
    - [ ] Running leaderboard: "Your Top 5 Coherence Songs" visible in app
    - [ ] Weight scores by movement confidence (clean readings count more)
    - [ ] Insights: "Your coherence is X% higher with slower tempo songs" (after 20+ songs)

---

## TODO - Nice to Have

- [ ] Onboarding flow: Polar H10 pairing guide → Spotify login → first session walkthrough
- [ ] Genre/tempo analysis: correlate Spotify audio features (tempo, energy, valence) with coherence
- [ ] Re-listen mode: replay top coherence songs and see if scores hold across sessions
- [ ] iOS support (KMP structure ready, targets commented out)
- [ ] Dark mode / theme customization
- [ ] Export coherence session data (CSV/JSON)
- [ ] Historical session tracking and trends chart
- [ ] App icon and branding assets (user creating)
- [ ] Session reminders / streak tracking ("Listen for 10 minutes daily")

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
| 60s minimum per song | Task Force HRV guidelines: 60s minimum for reliable short-term metrics; aligns with 64s coherence window | 2026-02-04 |
| 15s settle-in exclusion | Cardiac response to new auditory stimulus stabilises in ~10-15s; 30s too aggressive for session flow | 2026-02-04 |
| Dual movement detection | Phone accelerometer + HR anomaly; neither alone is sufficient (phone may not be on person) | 2026-02-04 |
| Spotify PKCE OAuth | Mobile app — no backend server to hold client secret; PKCE flow is standard for native apps | 2026-02-04 |
| 10 songs minimum for playlist | Fewer than 10 songs gives unreliable ranking; 10-15 is minimum viable, 20-30 is solid | 2026-02-04 |
| Soft movement warnings | Don't discard data on movement — flag it and weight lower; user shouldn't feel penalised | 2026-02-04 |

---

## Notes

- Polar SDK uses RxJava3; we bridge to Kotlin StateFlows in PolarManager
- Polar BLE SDK's `PolarBleApiCallback` requires implementing `disInformationReceived(String, DisInfo)` and `htsNotificationReceived` in v6.14.0
- `searchForDevice()` Flowable runs indefinitely until disposed -- always show Stop button
- PolarManager is a singleton tied to Activity lifecycle (shutDown in onDestroy when isFinishing)
- JetBrains lifecycle libraries use different versioning than AndroidX (2.8.4 not 2.8.7)
- Root folder still named `polar-H10-app` (VS Code lock) -- rename to HeartSyncRadio when convenient
- PhysioNet Subject 000 (53M, healthy) scipy reference values for 5-min segment: RMSSD=52.62ms, MeanHR=61.23bpm, LF=273.53ms², HF=283.39ms²
- Welch PSD params matching scipy: nperseg=256, noverlap=128, window='hann', fs=4.0Hz
- JAVA_HOME needs to be set to Android Studio JBR for Gradle: `C:\Program Files\Android\Android Studio\jbr`
- **IMPORTANT: Always commit and push to the private GitHub repo before finishing a session**
- Remote not yet configured — run: `git remote add origin <repo-url> && git push -u origin main`
