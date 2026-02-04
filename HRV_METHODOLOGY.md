# HeartSync Radio - HRV Processing Methodology

> This document describes the heart rate variability (HRV) processing pipeline used by HeartSync Radio. It is intended for scientists, clinicians, and reviewers who want to understand exactly how raw sensor data is transformed into the metrics displayed in the app.

## Table of Contents

1. [Data Acquisition](#1-data-acquisition)
2. [Processing Pipeline Overview](#2-processing-pipeline-overview)
3. [Artifact Detection and Correction](#3-artifact-detection-and-correction)
4. [Time-Domain Analysis (RMSSD)](#4-time-domain-analysis-rmssd)
5. [Frequency-Domain Analysis](#5-frequency-domain-analysis)
6. [Cardiac Coherence Score](#6-cardiac-coherence-score)
7. [Output Metrics](#7-output-metrics)
8. [Validation Against Reference Data](#8-validation-against-reference-data)
9. [Limitations and Known Differences](#9-limitations-and-known-differences)
10. [References](#10-references)

---

## 1. Data Acquisition

### Sensor Input

HeartSync Radio supports two BLE input modes:

- **Polar H10** via the Polar BLE SDK, which provides RR intervals in milliseconds through the SDK's streaming API.
- **Generic BLE Heart Rate Profile** (UUID `0x180D`), which parses the Heart Rate Measurement characteristic (`0x2A37`) per the Bluetooth specification. RR intervals are extracted from the characteristic payload, where they are encoded as unsigned 16-bit integers in units of 1/1024 seconds, then converted to milliseconds:

```
RR_ms = RR_raw * 1000 / 1024
```

In both cases, the pipeline receives RR intervals in milliseconds as `List<Int>`.

### Sliding Window Buffer

RR intervals are accumulated in a sliding window buffer managed by `HrvProcessor`. The buffer retains the most recent N seconds of data (default: **64 seconds**, per the HeartMath coherence protocol). Older data is trimmed from the front of the buffer.

Metrics are computed only when the buffer contains at least **30 seconds** of data and at least **30 RR intervals**.

---

## 2. Processing Pipeline Overview

Each time new RR intervals arrive from the sensor, the pipeline executes these steps in sequence:

```
Raw RR intervals (ms)
        |
        v
  [1] Artifact Detection & Cubic Spline Correction
        |
        v
  [2] RMSSD (time-domain)
        |
        v
  [3] Resample to uniform 4 Hz grid (linear interpolation)
        |
        v
  [4] Mean-center (detrend)
        |
        v
  [5] Welch PSD estimation (Hann window, 50% overlap)
        |
        v
  [6] Band power integration (LF, HF)
        |
        v
  [7] Coherence score (peak power ratio)
        |
        v
  [8] Mean HR from mean RR
        |
        v
  Output: HrvMetrics
```

Source code: [`HrvProcessor.kt`](composeApp/src/commonMain/kotlin/com/heartsyncradio/hrv/HrvProcessor.kt)

---

## 3. Artifact Detection and Correction

### Approach

We use a two-pass approach following Quigley's recommendation: artifacts are never deleted. Instead, they are replaced via cubic spline interpolation to preserve time-series integrity and avoid altering the temporal structure of the data.

### Pass 1: Detection

Each RR interval is flagged as an artifact if either condition is met:

1. **Physiological range check**: RR < 300 ms (~200 bpm) or RR > 2000 ms (~30 bpm)
2. **Local median deviation**: The interval deviates by more than **25%** from the local median. The local median is computed from a window of up to 20 surrounding intervals (10 on each side), excluding values that fail the physiological range check.

### Pass 2: Cubic Spline Interpolation

A **natural cubic spline** is fitted to the non-artifact (good) intervals, using their array indices as the x-coordinates and their RR values as the y-coordinates. Natural boundary conditions are applied (second derivative = 0 at both endpoints).

For each spline segment *i*, the interpolating polynomial is:

```
S_i(x) = a_i + b_i(x - x_i) + c_i(x - x_i)^2 + d_i(x - x_i)^3
```

where coefficients are computed by solving the standard tridiagonal system arising from continuity of the function, first derivative, and second derivative at interior knots.

Artifact positions are then evaluated against this spline. Interpolated values are clamped to the physiological range [300, 2000] ms.

**Key property**: The output array is always the same length as the input. No beats are deleted.

If fewer than 2 good intervals exist, the data is returned unmodified (interpolation is impossible).

Source code: [`ArtifactDetector.kt`](composeApp/src/commonMain/kotlin/com/heartsyncradio/hrv/ArtifactDetector.kt)

---

## 4. Time-Domain Analysis (RMSSD)

RMSSD (Root Mean Square of Successive Differences) is calculated from the cleaned RR intervals:

```
RMSSD = sqrt( (1 / (N-1)) * sum_{i=1}^{N-1} (RR_{i+1} - RR_i)^2 )
```

where N is the number of cleaned RR intervals.

RMSSD is the most validated short-term measure of parasympathetic (vagal) HRV. It does not require detrending because successive differences inherently remove slow trends.

Returns 0 if fewer than 2 intervals are available.

Source code: [`Rmssd.kt`](composeApp/src/commonMain/kotlin/com/heartsyncradio/hrv/Rmssd.kt)

---

## 5. Frequency-Domain Analysis

### 5.1 Resampling

RR intervals are an irregularly sampled time series (the intervals between samples are the RR intervals themselves). Before spectral analysis, the series is resampled to a uniform grid at **4 Hz** using linear interpolation.

A cumulative time axis is constructed:

```
t_0 = 0
t_i = t_{i-1} + RR_i / 1000    (seconds)
```

The uniform grid samples at intervals of 1/4 = 0.25 seconds. Each sample is linearly interpolated between the two nearest original RR values.

### 5.2 Detrending

The mean of the resampled series is subtracted (mean-centering). This removes the DC component before spectral estimation.

### 5.3 Power Spectral Density (Welch's Method)

PSD is estimated using **Welch's method** with the following parameters:

| Parameter | Value | Rationale |
|-----------|-------|-----------|
| Segment length | min(data length, 256) samples, rounded up to next power of 2 | At 4 Hz, 256 samples = 64 seconds, matching the HeartMath window |
| Overlap | 50% (segment_length / 2) | Standard for Welch's method |
| Window | Hann (raised cosine) | Reduces spectral leakage |
| Sampling rate | 4 Hz | Standard for HRV frequency analysis |

The Hann window is defined as:

```
w(n) = 0.5 * (1 - cos(2*pi*n / (N-1)))
```

For each segment, the windowed data is transformed via FFT, and the one-sided power spectrum is computed as:

```
P(k) = Re(X_k)^2 + Im(X_k)^2
```

Segments are averaged, then normalized to produce PSD in units of **ms^2/Hz**:

```
PSD(k) = (2 / (f_s * S_2)) * P_avg(k)
```

where:
- `f_s` = 4.0 Hz (sampling frequency)
- `S_2` = sum of squared window values (window power)
- The factor of 2 converts from two-sided to one-sided spectrum

This normalization matches `scipy.signal.welch(scaling='density')`.

### 5.4 FFT Implementation

A pure Kotlin **Cooley-Tukey radix-2 FFT** is used. Input length must be a power of 2 (zero-padded if necessary). The implementation uses:

- Bit-reversal permutation for input reordering
- In-place butterfly operations with complex twiddle factors
- O(N log N) time complexity

Source code: [`Fft.kt`](composeApp/src/commonMain/kotlin/com/heartsyncradio/hrv/Fft.kt)

### 5.5 Band Power Integration

Band powers are computed using the trapezoidal rule over PSD bins within each frequency band:

```
Power_band = df * sum_{k=k_low}^{k_high-1} (PSD(k) + PSD(k+1)) / 2
```

where `df = f_s / N_segment` is the frequency resolution, and bin indices are:

```
k = round(f / df)
```

Standard frequency bands (per Task Force of the European Society of Cardiology, 1996):

| Band | Frequency Range | Physiological Association |
|------|----------------|--------------------------|
| LF | 0.04 - 0.15 Hz | Mixed sympathetic and parasympathetic; includes baroreflex activity |
| HF | 0.15 - 0.40 Hz | Parasympathetic (vagal); respiratory sinus arrhythmia |

**Note**: We deliberately do not compute the LF/HF ratio. This ratio was historically used as an index of "sympathovagal balance" but is now considered scientifically unsound. LF power is not a pure sympathetic measure, and the ratio conflates independent physiological processes. Individual LF and HF band powers are reported separately.

Source code: [`CoherenceCalculator.kt`](composeApp/src/commonMain/kotlin/com/heartsyncradio/hrv/CoherenceCalculator.kt)

---

## 6. Cardiac Coherence Score

The coherence score follows the **HeartMath algorithm**:

1. Identify the **peak frequency** within the coherence band (0.04 - 0.26 Hz) from the PSD.
2. Integrate the PSD in a narrow window of +/- 0.015 Hz around the peak.
3. Integrate the total PSD across all frequencies.
4. Coherence = peak window power / total power.

```
Coherence = sum(PSD[peak-w : peak+w]) / sum(PSD[all])
```

where `w` is the number of bins corresponding to 0.015 Hz.

The coherence score ranges from 0 to 1. A high score indicates that heart rate oscillations are concentrated at a single dominant frequency (typically ~0.1 Hz during paced breathing or states of physiological coherence). A low score indicates broadly distributed spectral power.

The coherence band upper limit of 0.26 Hz (rather than the full 0.40 Hz HF limit) focuses on the range where coherent oscillations are physiologically meaningful, excluding high-frequency respiratory variation that would dilute the coherence measure.

---

## 7. Output Metrics

Each pipeline computation produces an `HrvMetrics` object:

| Metric | Unit | Description |
|--------|------|-------------|
| `coherenceScore` | 0-1 | HeartMath coherence (peak power concentration in 0.04-0.26 Hz) |
| `rmssd` | ms | Root mean square of successive RR differences |
| `meanHr` | bpm | Mean heart rate, calculated as `60000 / mean(RR)` |
| `lfPower` | ms^2 | Power in LF band (0.04-0.15 Hz) |
| `hfPower` | ms^2 | Power in HF band (0.15-0.40 Hz) |
| `rrCount` | count | Number of cleaned RR intervals in the window |
| `artifactsRemoved` | count | Number of intervals replaced by spline interpolation |
| `timestamp` | epoch ms | Time of computation |

Source code: [`HrvMetrics.kt`](composeApp/src/commonMain/kotlin/com/heartsyncradio/hrv/HrvMetrics.kt)

---

## 8. Validation Against Reference Data

### PhysioNet Cross-Validation

The pipeline is validated against real-world data from the PhysioNet "RR Interval Time Series from Healthy Subjects" database (DOI: [10.13026/51yd-d219](https://doi.org/10.13026/51yd-d219)).

**Test subject**: Subject 000, 53-year-old healthy male, first 5-minute segment (306 RR intervals).

**Reference values** computed independently using Python (numpy 2.4.2, scipy 1.17.0) with matching Welch parameters (`nperseg=256, noverlap=128, window='hann', fs=4.0`):

| Metric | Our Value | Scipy Reference | Error | Tolerance |
|--------|-----------|----------------|-------|-----------|
| RMSSD | 52.62 ms | 52.62 ms | < 0.001% | +/- 5% |
| Mean HR | 61.23 bpm | 61.23 bpm | < 0.001% | +/- 2% |
| LF Power | 289.3 ms^2 | 273.5 ms^2 | 5.8% | +/- 15% |
| HF Power | 308.5 ms^2 | 283.4 ms^2 | 8.8% | +/- 15% |

Time-domain metrics (RMSSD, Mean HR) match scipy to effectively zero error. Frequency-domain residual error (~6-9%) is attributable to:
- Linear vs. cubic spline resampling to the uniform 4 Hz grid
- Bin boundary rounding at band edges

### Accuracy Improvement History

The initial implementation used rectangular (Riemann sum) integration for band power calculation. Cross-validation against scipy revealed significant overestimation:

| Metric | Rectangular (initial) | Trapezoidal (current) | Scipy Reference |
|--------|----------------------|----------------------|----------------|
| LF Power | 336.5 ms^2 (23.0% error) | 289.3 ms^2 (5.8% error) | 273.5 ms^2 |
| HF Power | 330.2 ms^2 (16.5% error) | 308.5 ms^2 (8.8% error) | 283.4 ms^2 |

Rectangular integration systematically overestimates because it sums each PSD bin at its full height. Switching to the trapezoidal rule (averaging adjacent bins before summing) reduced LF error from 23% to 5.8% and HF error from 16.5% to 8.8%. Test tolerances were tightened from +/-50% to +/-15% accordingly.

We also tested cubic spline resampling (replacing the linear interpolation in Section 5.1) to see if it would reduce the remaining error:

| Metric | Linear resampling | Cubic spline resampling | Scipy Reference |
|--------|-------------------|------------------------|----------------|
| LF Power | 289.3 ms^2 (5.8% error) | 302.8 ms^2 (10.7% error) | 273.5 ms^2 |
| HF Power | 308.5 ms^2 (8.8% error) | 507.0 ms^2 (78.9% error) | 283.4 ms^2 |

Cubic spline resampling made accuracy significantly worse. Spline interpolation overshoots on irregular RR interval data, creating ringing artifacts that inflate high-frequency spectral power. Linear interpolation was retained as it produces more accurate results for this application.

### Test Suite

The full test suite comprises **40 unit tests** across 6 test classes:

| Test Class | Tests | Coverage |
|------------|-------|----------|
| `RmssdTest` | 5 | Constant intervals, alternating intervals, hand-calculated values, variability sensitivity, edge cases |
| `ArtifactDetectorTest` | 9 | Clean pass-through, out-of-range detection, deviation detection, spline accuracy (linear/quadratic data), output length preservation, physiological clamping |
| `FftTest` | 5 | DC signal, single frequency, multiple frequencies, Parseval's theorem, power spectrum symmetry |
| `CoherenceCalculatorTest` | 7 | High coherence (single sine), low coherence (noise), LF/HF band discrimination, resampling accuracy, band power scaling |
| `HrvProcessorTest` | 8 | Minimum data requirements, value ranges, artifact handling, buffer trimming, incremental feeding, sinusoidal coherence, reset behaviour |
| `PhysioNetValidationTest` | 6 | RMSSD vs. scipy, mean HR, spectral powers, LF/HF balance, physiological plausibility, incremental feeding consistency |

Source code: [`composeApp/src/commonTest/kotlin/com/heartsyncradio/hrv/`](composeApp/src/commonTest/kotlin/com/heartsyncradio/hrv/)

---

## 9. Limitations and Known Differences

### Compared to Clinical HRV Software

1. **Resampling method**: We use linear interpolation for resampling to the uniform grid. Some clinical tools use cubic spline interpolation at this stage. We tested cubic spline resampling and found it introduced overshoot artifacts that inflated HF power by 79% (see Accuracy Improvement History). Linear interpolation produces more accurate spectral estimates for irregular RR data.

2. **Detrending**: We apply mean-centering only (DC removal). Some implementations use higher-order polynomial detrending or smoothness priors. For short windows (64 seconds), mean-centering is generally sufficient.

3. **Integration method**: Band powers are computed using trapezoidal integration of PSD bins, matching scipy's approach.

4. **Window length**: The default 64-second window satisfies the Task Force minimum of 60 seconds for short-term HRV analysis. Longer recordings would improve frequency resolution but are not practical for real-time feedback.

5. **Artifact detection**: The 25% deviation threshold and +/-10 interval median window are heuristic choices. More sophisticated algorithms (e.g., Berntson's editing criteria, Lipponen & Tarvainen's method) exist but add complexity without clear benefit for chest-strap data, which has low artifact rates.

6. **Coherence algorithm**: The HeartMath coherence score is a proprietary metric. Our implementation follows their published algorithm (peak power ratio in the 0.04-0.26 Hz band) but has not been validated against HeartMath's commercial software.

### Not Implemented

- Very low frequency (VLF, < 0.04 Hz) analysis: requires recordings of at least 5 minutes and is not meaningful for 64-second windows.
- Non-linear HRV measures (sample entropy, DFA, Poincare plots): not required for the coherence-based analysis but could be added.
- LF/HF ratio: deliberately excluded as scientifically unsound.

---

## 10. References

1. **Task Force of the European Society of Cardiology and the North American Society of Pacing and Electrophysiology.** Heart rate variability: Standards of measurement, physiological interpretation, and clinical use. *Circulation*, 93(5), 1043-1065, 1996.

2. **McCraty, R., & Shaffer, F.** Heart rate variability: New perspectives on physiological mechanisms, assessment of self-regulatory capacity, and health risk. *Global Advances in Health and Medicine*, 4(1), 46-61, 2015. (HeartMath coherence algorithm)

3. **Quigley, K. S., et al.** Recommendations for RR interval data preprocessing. (Cubic spline interpolation for artifact correction)

4. **Welch, P. D.** The use of fast Fourier transform for the estimation of power spectra. *IEEE Transactions on Audio and Electroacoustics*, 15(2), 70-73, 1967.

5. **Goldberger, A. L., et al.** PhysioBank, PhysioToolkit, and PhysioNet: Components of a new research resource for complex physiologic signals. *Circulation*, 101(23), e215-e220, 2000.

6. **Billman, G. E.** The LF/HF ratio does not accurately measure cardiac sympatho-vagal balance. *Frontiers in Physiology*, 4, 26, 2013. (Rationale for excluding LF/HF ratio)

---

## Source Code

All processing code is open for inspection in the [`composeApp/src/commonMain/kotlin/com/heartsyncradio/hrv/`](composeApp/src/commonMain/kotlin/com/heartsyncradio/hrv/) directory:

| File | Purpose |
|------|---------|
| `HrvProcessor.kt` | Pipeline orchestrator with sliding window buffer |
| `ArtifactDetector.kt` | Two-pass artifact detection and cubic spline correction |
| `Rmssd.kt` | RMSSD calculation |
| `CoherenceCalculator.kt` | Resampling, Welch PSD, band powers, coherence score |
| `Fft.kt` | Cooley-Tukey radix-2 FFT |
| `HrvMetrics.kt` | Output data class |

Test suite: [`composeApp/src/commonTest/kotlin/com/heartsyncradio/hrv/`](composeApp/src/commonTest/kotlin/com/heartsyncradio/hrv/)
