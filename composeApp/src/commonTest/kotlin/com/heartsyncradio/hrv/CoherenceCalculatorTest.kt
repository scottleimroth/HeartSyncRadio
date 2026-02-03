package com.heartsyncradio.hrv

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin
import kotlin.test.Test
import kotlin.test.assertTrue

class CoherenceCalculatorTest {

    /**
     * Generates synthetic RR intervals with a sinusoidal modulation.
     * This simulates a heart with HRV concentrated at a single frequency.
     *
     * @param baseRR baseline RR interval in ms (e.g., 800 = 75 bpm)
     * @param modulationAmplitude amplitude of RR variation in ms
     * @param modulationFreqHz frequency of the modulation in Hz
     * @param durationSeconds how many seconds of data to generate
     */
    private fun generateSinusoidalRR(
        baseRR: Double = 800.0,
        modulationAmplitude: Double = 30.0,
        modulationFreqHz: Double = 0.1,
        durationSeconds: Double = 120.0
    ): List<Double> {
        val rrList = mutableListOf<Double>()
        var elapsedMs = 0.0
        val targetMs = durationSeconds * 1000.0

        while (elapsedMs < targetMs) {
            val timeSec = elapsedMs / 1000.0
            val rr = baseRR + modulationAmplitude * sin(2.0 * PI * modulationFreqHz * timeSec)
            rrList.add(rr)
            elapsedMs += rr
        }
        return rrList
    }

    @Test
    fun highCoherenceForSingleFrequencyModulation() {
        // A pure sine wave modulation at 0.1 Hz should produce high coherence
        // because all power is concentrated at one frequency
        val rr = generateSinusoidalRR(
            baseRR = 800.0,
            modulationAmplitude = 40.0,
            modulationFreqHz = 0.1,
            durationSeconds = 120.0
        )

        val result = CoherenceCalculator.calculate(rr)
        assertTrue(result.coherence > 0.3,
            "Single-frequency modulation should have high coherence, got ${result.coherence}")
    }

    @Test
    fun lowCoherenceForNoiseSignal() {
        // Random RR intervals should have low coherence (power spread across frequencies)
        val random = java.util.Random(42)
        val rr = List(200) { 700.0 + random.nextGaussian() * 50.0 }

        val result = CoherenceCalculator.calculate(rr)
        assertTrue(result.coherence < 0.3,
            "Random noise should have low coherence, got ${result.coherence}")
    }

    @Test
    fun lfModulationProducesLfPower() {
        // Modulation at 0.08 Hz (LF band: 0.04-0.15 Hz)
        val rr = generateSinusoidalRR(
            modulationFreqHz = 0.08,
            modulationAmplitude = 40.0,
            durationSeconds = 120.0
        )

        val result = CoherenceCalculator.calculate(rr)
        assertTrue(result.lfPower > result.hfPower,
            "LF modulation should produce more LF power (${result.lfPower}) than HF (${result.hfPower})")
        assertTrue(result.lfPower > 0.0, "LF power should be positive")
    }

    @Test
    fun hfModulationProducesHfPower() {
        // Modulation at 0.25 Hz (HF band: 0.15-0.40 Hz) — typical breathing rate
        val rr = generateSinusoidalRR(
            modulationFreqHz = 0.25,
            modulationAmplitude = 40.0,
            durationSeconds = 120.0
        )

        val result = CoherenceCalculator.calculate(rr)
        assertTrue(result.hfPower > result.lfPower,
            "HF modulation should produce more HF power (${result.hfPower}) than LF (${result.lfPower})")
        assertTrue(result.hfPower > 0.0, "HF power should be positive")
    }

    @Test
    fun constantRrGivesNearZeroPower() {
        // Perfectly constant HR -> no variability -> no spectral power
        val rr = List(200) { 800.0 }
        val result = CoherenceCalculator.calculate(rr)
        // With zero variability, detrended signal is all zeros
        assertTrue(result.lfPower < 1.0, "Constant RR should have near-zero LF power")
        assertTrue(result.hfPower < 1.0, "Constant RR should have near-zero HF power")
    }

    @Test
    fun insufficientDataReturnsZeros() {
        val rr = List(10) { 800.0 }
        val result = CoherenceCalculator.calculate(rr)
        assertTrue(result.coherence == 0.0)
        assertTrue(result.lfPower == 0.0)
        assertTrue(result.hfPower == 0.0)
    }

    @Test
    fun resamplingProducesCorrectLength() {
        // 75 RR intervals of 800ms. Resampling builds cumulative time from
        // index 1 onward: times[i] = times[i-1] + rr[i]/1000
        // So total duration = sum(rr[1..74]) / 1000 = 74 * 0.8 = 59.2s
        // At 4Hz that's ~236 samples
        val rr = List(75) { 800.0 }
        val resampled = CoherenceCalculator.resampleToUniform(rr)
        val totalDurationSec = rr.drop(1).sum() / 1000.0
        val expectedSamples = (totalDurationSec * 4).toInt()
        assertTrue(abs(resampled.size - expectedSamples) <= 2,
            "Expected ~$expectedSamples resampled points, got ${resampled.size}")
    }
}
