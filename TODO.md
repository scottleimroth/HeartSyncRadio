# HeartSync Radio - Development Log & TODO

## Last Session

- **Date:** 2026-02-03
- **Summary:** Set up project from scratch and implemented Polar H10 BLE integration
- **Key changes:**
  - Scaffolded Compose Multiplatform project (Kotlin 2.1.0, CMP 1.7.3, Gradle 8.9)
  - Integrated Polar BLE SDK 6.14.0 with PolarManager wrapper (RxJava -> StateFlows)
  - Built Home screen with BLE scanning, device connection, live HR + RR display
  - Added runtime BLE permission handling
  - Set up standard repo files (CREDENTIALS.md, SECURITY_AUDIT.md)
- **Stopped at:** Polar H10 integration complete. Next up: HRV & Coherence Calculation
- **Blockers:** None currently. Will need Spotify Developer credentials when reaching Spotify integration.

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

### In Progress
- HRV & Coherence Calculation (next)

### Known Bugs
- None identified yet (untested on physical device)

---

## TODO - Priority

1. [ ] **HRV & Coherence Calculation:**
    - [ ] Process raw RR-intervals from the Polar SDK
    - [ ] Implement artifact removal to clean the data
    - [ ] Calculate the coherence score based on power spectrum density
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

---

## Notes

- Polar SDK uses RxJava3; we bridge to Kotlin StateFlows in PolarManager
- Polar BLE SDK's `PolarBleApiCallback` requires implementing `disInformationReceived(String, DisInfo)` and `htsNotificationReceived` in v6.14.0
- `searchForDevice()` Flowable runs indefinitely until disposed -- always show Stop button
- PolarManager is a singleton tied to Activity lifecycle (shutDown in onDestroy when isFinishing)
- JetBrains lifecycle libraries use different versioning than AndroidX (2.8.4 not 2.8.7)
- Root folder still named `polar-H10-app` (VS Code lock) -- rename to HeartSyncRadio when convenient
