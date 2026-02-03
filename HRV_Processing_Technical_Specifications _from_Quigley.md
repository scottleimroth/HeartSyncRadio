# HRV Processing Technical Specifications

## 1. Data Acquisition

* **Sensor:** Prefer ECG over PPG for R-wave precision.
* **Sampling Rate:** 1000 Hz is ideal (1ms accuracy); 250 Hz is the minimum acceptable.
* **Data Type:** Use Heart Period (HP) in milliseconds for all calculations.

## 2. Preprocessing & Artifacts

* **Fiducial Point:** Use the peak of the R-wave.
* **Correction:** Do not delete artifact segments. Use cubic spline interpolation to replace missing or deviant beats to maintain time-series integrity.
* **Validation:** Automated detection must allow for manual visual inspection of the waveform.

## 3. Metrics & Windows

* **RMSSD:** Primary vagal index. Use 30–60 second windows.
* **SDNN:** Total variability index. Requires consistent window lengths for comparison.
* **HF (High Frequency):** 0.12–0.40 Hz. Reflects parasympathetic activity.
* **LF (Low Frequency):** 0.05–0.15 Hz. Mixture of sympathetic/parasympathetic.
* **Ratio:** Avoid using LF/HF as a direct measure of "balance."

## 4. Contextual Controls

* **Respiration:** Record or estimate breathing rate; it directly influences HF power.
* **Posture:** Standardize and log user position (supine, sitting, or standing).
* **Stability:** Ensure at least a 2-minute stabilization period before recording baseline metrics.
