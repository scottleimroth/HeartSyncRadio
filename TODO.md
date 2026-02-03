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
    - [ ] Integrate the Spotify Web API
    - [ ] Implement OAuth for user authentication
    - [ ] Fetch user playlists and track information
    - [ ] Create new playlists
3. [ ] **Session Screen:**
    - [ ] Display the currently playing track
    - [ ] Show a real-time graph of heart rate and coherence score
4. [ ] **Backend/Data Storage:**
    - [ ] Set up SQLDelight local database for song-coherence score relationships
5. [ ] **Playlist Generation Logic:**
    - [ ] Analyze stored data to find songs with highest coherence scores
    - [ ] Use Spotify API to create new playlist with these songs

---

## TODO - Nice to Have

- [ ] Onboarding screen for Spotify connection
- [ ] Playlist Generation screen to display "Coherence Playlists"
- [ ] iOS support (KMP structure ready, targets commented out)
- [ ] Dark mode / theme customization
- [ ] Export coherence session data (CSV/JSON)
- [ ] Historical session tracking and trends
- [ ] App icon and branding assets

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
