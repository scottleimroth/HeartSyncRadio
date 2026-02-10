# HrvXo

[![Download APK](https://img.shields.io/badge/Download-APK-green?style=for-the-badge)](https://github.com/scottleimroth/hrvxo/releases/latest/download/HrvXo.apk)

A biofeedback-driven music app that uses your Polar H10 heart rate monitor to generate music playlists based on cardiac coherence.

## Overview

HrvXo connects to a Polar H10 chest strap via Bluetooth LE, processes real-time heart rate variability (HRV) data, and calculates a coherence score using the HeartMath algorithm. The goal is to identify which songs produce the highest physiological coherence and build personalised playlists from that data.

## Features

- **Polar H10 BLE Integration** — Scan, connect, and stream live heart rate + RR intervals
- **HRV Processing Pipeline** — Artifact correction (cubic spline), RMSSD, FFT spectral analysis
- **Coherence Scoring** — HeartMath algorithm with Welch's method PSD estimation
- **Real-time Display** — Live BPM, coherence %, RMSSD, and RR intervals
- **Battery Monitoring** — Shows Polar H10 battery level while connected

## Getting Started

### Prerequisites

- Android 8.0+ (API 26)
- Polar H10 chest strap
- Bluetooth enabled on device

### Installation

Download the latest APK from the badge above, or from the [Releases page](https://github.com/scottleimroth/HeartSyncRadio/releases).

1. Open the APK on your Android device
2. Allow "Install from unknown sources" when prompted
3. Open the app and grant Bluetooth permissions
4. Wet your Polar H10 strap, put it on, and tap "Scan"

### Building from Source

```bash
git clone https://github.com/scottleimroth/hrvxo.git
cd hrvxo
./gradlew composeApp:assembleDebug
```

APK output: `composeApp/build/outputs/apk/debug/composeApp-debug.apk`

## Project Structure

```
hrvxo/
├── composeApp/src/
│   ├── commonMain/         # Cross-platform code
│   │   └── kotlin/com/hrvxo/
│   │       ├── hrv/        # HRV processing (artifact detection, FFT, coherence)
│   │       ├── model/      # Data classes (HeartRateData, ConnectionState)
│   │       └── ui/         # Compose UI (HomeScreen, HeartRateDisplay)
│   ├── androidMain/        # Android-specific code
│   │   └── kotlin/com/hrvxo/
│   │       ├── polar/      # Polar BLE SDK integration
│   │       ├── viewmodel/  # Android ViewModels
│   │       └── permission/ # BLE permission handling
│   └── commonTest/         # Cross-platform tests (40 tests)
├── gradle/
│   └── libs.versions.toml  # Dependency version catalog
└── docs/                   # Design documentation
```

## Testing

```bash
./gradlew composeApp:testDebugUnitTest
```

40 unit tests covering HRV processing, FFT accuracy (Parseval's theorem), coherence calculation, and PhysioNet real-world data validation.

## Tech Stack

- **Kotlin Multiplatform** with Compose Multiplatform
- **Polar BLE SDK** 6.14.0 for heart rate sensor communication
- **RxJava3** bridged to Kotlin StateFlows
- **AndroidX Lifecycle** for ViewModel integration
- **Material3** design system

## Roadmap

- [ ] Spotify OAuth integration and playlist creation
- [ ] Session screen with real-time HR/coherence graphs
- [ ] Local data storage (SQLDelight) for song-coherence tracking
- [ ] Coherence playlist generation algorithm
- [ ] iOS support (KMP structure in place)
