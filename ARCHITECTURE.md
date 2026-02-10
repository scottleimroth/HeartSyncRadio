# HrvXo - System Architecture

## Overview

HrvXo is a Kotlin Multiplatform (KMP) Android app that connects to a Polar H10 chest strap via BLE, performs real-time HRV analysis with coherence scoring (HeartMath algorithm), and integrates with a Python FastAPI backend for YouTube Music playlist generation based on physiological data.

---

## Architecture Diagram

```
┌──────────────────────────────────┐
│         Polar H10 Strap          │
│  (BLE: HR + RR intervals)       │
└──────────────┬───────────────────┘
               │ Bluetooth LE
               ▼
┌──────────────────────────────────┐
│       Android App (KMP)          │
│  ┌────────────────────────────┐  │
│  │ Polar BLE SDK              │  │
│  │ → RxJava3 → StateFlows    │  │
│  └─────────────┬──────────────┘  │
│                ▼                  │
│  ┌────────────────────────────┐  │
│  │ HRV Processing Pipeline    │  │
│  │ • Artifact correction      │  │
│  │ • RMSSD calculation        │  │
│  │ • FFT spectral analysis    │  │
│  │ • Coherence scoring        │  │
│  └─────────────┬──────────────┘  │
│                ▼                  │
│  ┌────────────────────────────┐  │
│  │ Compose Multiplatform UI   │  │
│  │ • Live BPM display         │  │
│  │ • Coherence % meter        │  │
│  │ • RR interval chart        │  │
│  └────────────────────────────┘  │
└──────────────┬───────────────────┘
               │ REST API
               ▼
┌──────────────────────────────────┐
│       Python FastAPI Backend     │
│       (Fly.io)                   │
│  ┌────────────────────────────┐  │
│  │ ytmusicapi                 │  │
│  │ → YouTube Music playlists  │  │
│  └────────────────────────────┘  │
└──────────────────────────────────┘
```

---

## Technology Stack

### Mobile App
- **Framework:** Kotlin Multiplatform + Compose Multiplatform
- **BLE:** Polar BLE SDK 6.14.0
- **Reactive:** RxJava3 bridged to Kotlin StateFlows
- **UI:** Material3 design system
- **Architecture:** Android ViewModel + StateFlow

### HRV Processing
- **Artifact correction:** Cubic spline interpolation
- **Time-domain:** RMSSD calculation
- **Frequency-domain:** FFT with Welch's method PSD estimation
- **Coherence:** HeartMath algorithm (peak power ratio in 0.04-0.26 Hz band)

### Backend
- **Framework:** Python FastAPI
- **Hosting:** Fly.io
- **Music API:** ytmusicapi (YouTube Music)

---

## Component Structure

```
hrvxo/
├── composeApp/src/
│   ├── commonMain/kotlin/com/hrvxo/
│   │   ├── hrv/              # HRV processing pipeline
│   │   │   ├── ArtifactDetector.kt
│   │   │   ├── FFTProcessor.kt
│   │   │   └── CoherenceCalculator.kt
│   │   ├── model/            # Data classes
│   │   └── ui/               # Compose UI screens
│   ├── androidMain/kotlin/com/hrvxo/
│   │   ├── polar/            # Polar BLE SDK integration
│   │   ├── viewmodel/        # Android ViewModels
│   │   └── permission/       # BLE permission handling
│   └── commonTest/           # 40 unit tests
├── backend/                  # Python FastAPI backend
├── gradle/
│   └── libs.versions.toml    # Version catalog
└── docs/                     # Design documentation
```

---

## Key Workflows

### BLE Connection & Streaming
1. User taps "Scan" — app discovers nearby Polar H10 devices
2. User selects device — BLE connection established
3. Polar SDK streams HR + RR intervals via RxJava3
4. Data bridged to Kotlin StateFlows for UI consumption

### HRV Processing Pipeline
1. Raw RR intervals received from Polar H10
2. Artifact detection identifies missed/extra beats
3. Cubic spline interpolation corrects artifacts
4. RMSSD computed from corrected RR intervals
5. FFT (Welch's method) computes power spectral density
6. Coherence score = peak power in 0.04-0.26 Hz / total power

### Playlist Generation (planned)
1. App records coherence scores during music playback
2. Song-coherence pairs sent to FastAPI backend
3. Backend uses ytmusicapi to build personalized playlists
4. High-coherence songs ranked and added to YouTube Music

---

## Security Considerations

- **BLE:** Local Bluetooth only, no data transmitted over internet during recording
- **API Keys:** YouTube Music credentials stored in CREDENTIALS.md (gitignored)
- **Backend:** FastAPI on Fly.io with HTTPS
- **No PII:** Only physiological metrics (HR, RR intervals) processed

---

## Testing

- 40 unit tests covering HRV processing, FFT accuracy (Parseval's theorem), coherence calculation
- Validated against PhysioNet real-world physiological data

---

**Last Updated:** 2026-02-10
