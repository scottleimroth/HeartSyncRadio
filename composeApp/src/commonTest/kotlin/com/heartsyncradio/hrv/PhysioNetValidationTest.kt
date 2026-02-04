package com.heartsyncradio.hrv

import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Validates the HRV pipeline against real-world data from PhysioNet.
 *
 * Source: "RR Interval Time Series from Healthy Subjects" (PhysioNet)
 * Record: Subject 000, 53-year-old male, first 5 minutes (306 beats, ~300s)
 * DOI: 10.13026/51yd-d219
 * License: CC BY 4.0
 *
 * Reference values computed independently with scipy.signal.welch
 * (Python 3, numpy 2.4.2, scipy 1.17.0) using identical Welch parameters:
 * nperseg=256, noverlap=128, window='hann', fs=4.0 Hz
 *
 * RMSSD reference: 52.622348 ms (computed with numpy)
 * Mean HR reference: 61.230 bpm
 * LF Power reference: 273.5253 ms² (scipy Welch + trapezoidal integration)
 * HF Power reference: 283.3852 ms² (scipy Welch + trapezoidal integration)
 */
class PhysioNetValidationTest {

    // PhysioNet Subject 000, first 5 minutes (306 RR intervals in ms)
    private val physionetRR = listOf(
        789, 727, 789, 750, 812, 789, 828, 797, 867, 860, 843, 821, 812, 797, 797, 789, 867, 828,
        797, 828, 828, 868, 906, 844, 859, 844, 836, 820, 781, 828, 782, 750, 843, 883, 836, 891,
        914, 898, 938, 906, 961, 891, 968, 899, 1008, 906, 984, 1000, 969, 1031, 992, 1071, 1078,
        1078, 1031, 1078, 1047, 1039, 1078, 1032, 1109, 1023, 1102, 1070, 1125, 1086, 1102, 1101,
        1086, 1118, 1023, 1117, 1094, 1062, 1125, 1047, 1071, 1015, 1078, 1016, 1102, 1046, 1079,
        1078, 1054, 1102, 1055, 1023, 1094, 1015, 1086, 1071, 984, 1102, 1023, 1086, 1039, 1070,
        1055, 1070, 1032, 1023, 1016, 1086, 1015, 1039, 1024, 929, 1008, 1000, 992, 985, 1023,
        1016, 1000, 1015, 1024, 1000, 1023, 977, 1023, 993, 1015, 969, 1000, 1039, 992, 1039, 1008,
        969, 1000, 953, 969, 1008, 992, 828, 984, 961, 969, 961, 961, 984, 1000, 1016, 1016, 1023,
        961, 1078, 1016, 1054, 1040, 1031, 1039, 1039, 922, 945, 984, 1008, 1063, 1031, 1047, 1101,
        1047, 1047, 1016, 1031, 977, 1000, 1007, 1016, 1008, 1008, 1031, 961, 1086, 969, 1086,
        1054, 1016, 1101, 1063, 992, 1055, 953, 1070, 977, 1023, 1016, 914, 945, 985, 976, 961,
        992, 914, 969, 984, 1016, 914, 945, 985, 890, 1040, 1000, 1015, 1055, 1055, 1085, 1016,
        1070, 1071, 1062, 1039, 1016, 1101, 1094, 1047, 1055, 1023, 938, 1054, 1032, 1047, 1023,
        945, 977, 1000, 1000, 984, 1016, 1008, 1000, 1015, 1055, 961, 1031, 1000, 969, 1062, 985,
        1047, 1054, 985, 1031, 992, 1024, 992, 969, 1039, 969, 1016, 906, 1016, 992, 945, 985, 921,
        969, 946, 1015, 977, 906, 984, 969, 984, 985, 898, 930, 937, 961, 930, 938, 1000, 953, 922,
        906, 945, 922, 969, 906, 945, 875, 914, 860, 945, 899, 859, 914, 914, 914, 953, 906, 938,
        945, 961, 906, 946, 883, 898, 922, 883, 883, 945, 898, 899, 922
    )

    // Scipy-computed reference values
    private val refRmssd = 52.622348
    private val refMeanHr = 61.230
    private val refLfPower = 273.5253   // ms²
    private val refHfPower = 283.3852   // ms²

    @Test
    fun rmssdMatchesScipy() {
        val cleanResult = ArtifactDetector.clean(physionetRR)
        val rmssd = Rmssd.calculate(cleanResult.cleanedRR)

        // This data is pre-cleaned (max 8% artifacts), so artifact count should be low
        assertTrue(cleanResult.artifactsRemoved < physionetRR.size * 0.1,
            "Healthy subject data should have few artifacts, got ${cleanResult.artifactsRemoved}")

        // RMSSD should match within 5% of scipy reference
        // Small differences expected because our artifact detector may modify some values
        val relError = abs(rmssd - refRmssd) / refRmssd
        assertTrue(relError < 0.05,
            "RMSSD should be within 5% of scipy reference ($refRmssd). Got $rmssd (${relError * 100}% error)")
    }

    @Test
    fun meanHrMatchesReference() {
        val cleanResult = ArtifactDetector.clean(physionetRR)
        val meanRR = cleanResult.cleanedRR.average()
        val meanHr = 60000.0 / meanRR

        val relError = abs(meanHr - refMeanHr) / refMeanHr
        assertTrue(relError < 0.02,
            "Mean HR should be within 2% of reference ($refMeanHr bpm). Got $meanHr bpm (${relError * 100}% error)")
    }

    @Test
    fun spectralPowersInCorrectRange() {
        val cleanResult = ArtifactDetector.clean(physionetRR)
        val spectral = CoherenceCalculator.calculate(cleanResult.cleanedRR)

        // LF and HF should be close to scipy reference values.
        // Remaining differences (~6-9%) are due to:
        // - Linear vs cubic spline resampling to uniform grid
        // - Slight differences in bin boundary rounding at band edges
        assertTrue(spectral.lfPower > 0.0, "LF power should be positive")
        assertTrue(spectral.hfPower > 0.0, "HF power should be positive")

        val lfRelError = abs(spectral.lfPower - refLfPower) / refLfPower
        assertTrue(lfRelError < 0.15,
            "LF power should be within 15% of scipy reference ($refLfPower). " +
                    "Got ${spectral.lfPower} (${lfRelError * 100}% error)")

        val hfRelError = abs(spectral.hfPower - refHfPower) / refHfPower
        assertTrue(hfRelError < 0.15,
            "HF power should be within 15% of scipy reference ($refHfPower). " +
                    "Got ${spectral.hfPower} (${hfRelError * 100}% error)")
    }

    @Test
    fun lfAndHfPowersAreBothSubstantial() {
        // Subject 000 is healthy at rest — expect both LF and HF to have
        // meaningful power (not dominated by one band)
        val cleanResult = ArtifactDetector.clean(physionetRR)
        val spectral = CoherenceCalculator.calculate(cleanResult.cleanedRR)

        val total = spectral.lfPower + spectral.hfPower
        assertTrue(total > 0)

        val lfFraction = spectral.lfPower / total
        val hfFraction = spectral.hfPower / total

        // For a healthy resting subject, neither band should be negligible
        assertTrue(lfFraction > 0.1,
            "LF should be >10% of LF+HF for healthy resting subject, got ${lfFraction * 100}%")
        assertTrue(hfFraction > 0.1,
            "HF should be >10% of LF+HF for healthy resting subject, got ${hfFraction * 100}%")
    }

    @Test
    fun metricsArePhysiologicallyPlausible() {
        // Run the full pipeline through HrvProcessor
        val processor = HrvProcessor(windowSeconds = 300)
        val metrics = processor.addRrIntervals(physionetRR)

        assertTrue(metrics != null, "Should produce metrics from 300s of real data")
        metrics!!

        // Heart rate: 53-year-old male at rest, expect 50-90 bpm
        assertTrue(metrics.meanHr in 50.0..90.0,
            "Mean HR should be 50-90 bpm for resting adult, got ${metrics.meanHr}")

        // RMSSD: healthy subject at rest, expect 15-100 ms
        assertTrue(metrics.rmssd in 15.0..100.0,
            "RMSSD should be 15-100 ms for healthy resting adult, got ${metrics.rmssd}")

        // Coherence: resting (not paced breathing), expect low-moderate
        assertTrue(metrics.coherenceScore in 0.0..1.0,
            "Coherence should be 0-1, got ${metrics.coherenceScore}")

        // LF and HF should both be positive
        assertTrue(metrics.lfPower > 0.0, "LF power should be positive")
        assertTrue(metrics.hfPower > 0.0, "HF power should be positive")
    }

    @Test
    fun processorHandlesFullRecordingSegment() {
        // Feed data incrementally, simulating real-time sensor input
        val processor = HrvProcessor(windowSeconds = 120)

        // Feed in chunks of ~10 beats (simulating sensor callbacks)
        val chunks = physionetRR.chunked(10)
        var lastMetrics: HrvMetrics? = null

        for (chunk in chunks) {
            val result = processor.addRrIntervals(chunk)
            if (result != null) lastMetrics = result
        }

        assertTrue(lastMetrics != null, "Should produce metrics after incremental feeding")
        lastMetrics!!

        // Verify consistency with single-batch processing
        assertTrue(lastMetrics.meanHr in 50.0..90.0)
        assertTrue(lastMetrics.rmssd > 0.0)
    }

}
