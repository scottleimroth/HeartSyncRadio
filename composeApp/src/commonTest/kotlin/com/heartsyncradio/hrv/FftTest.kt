package com.heartsyncradio.hrv

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sqrt
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FftTest {

    @Test
    fun dcSignalHasAllPowerInBinZero() {
        val n = 64
        val real = DoubleArray(n) { 5.0 }
        val imag = DoubleArray(n)

        Fft.transform(real, imag)
        val power = Fft.powerSpectrum(real, imag)

        // Bin 0 should have all the power (DC component = N*value)
        assertTrue(power[0] > 0)
        // All other bins should be ~0
        for (i in 1 until power.size) {
            assertTrue(power[i] < 1e-10 * power[0],
                "Non-DC bin $i should be near zero, got ${power[i]}")
        }
    }

    @Test
    fun singleFrequencyDetectedAtCorrectBin() {
        val n = 256
        val sampleRate = 4.0
        val signalFreq = 0.1  // Hz

        val real = DoubleArray(n) { i ->
            cos(2.0 * PI * signalFreq * i / sampleRate)
        }
        val imag = DoubleArray(n)

        Fft.transform(real, imag)
        val power = Fft.powerSpectrum(real, imag)

        val freqResolution = sampleRate / n
        val expectedBin = (signalFreq / freqResolution).toInt()

        // Find the bin with maximum power (excluding DC)
        var maxBin = 1
        for (i in 2 until power.size) {
            if (power[i] > power[maxBin]) maxBin = i
        }

        assertEquals(expectedBin, maxBin,
            "Peak should be at bin $expectedBin (${signalFreq}Hz), found at $maxBin")
    }

    @Test
    fun twoFrequenciesProduceTwoPeaks() {
        val n = 512
        val sampleRate = 4.0
        val freq1 = 0.1  // Hz
        val freq2 = 0.25 // Hz

        val real = DoubleArray(n) { i ->
            cos(2.0 * PI * freq1 * i / sampleRate) +
                    0.7 * cos(2.0 * PI * freq2 * i / sampleRate)
        }
        val imag = DoubleArray(n)

        Fft.transform(real, imag)
        val power = Fft.powerSpectrum(real, imag)

        val freqResolution = sampleRate / n
        val expectedBin1 = (freq1 / freqResolution).toInt()
        val expectedBin2 = (freq2 / freqResolution).toInt()

        // Find peak power near each expected bin (±1 bin tolerance for rounding)
        fun peakNear(bin: Int): Double {
            val lo = maxOf(0, bin - 1)
            val hi = minOf(power.size - 1, bin + 1)
            return (lo..hi).maxOf { power[it] }
        }

        val peak1 = peakNear(expectedBin1)
        val peak2 = peakNear(expectedBin2)

        // Both peaks should have significant power
        assertTrue(peak1 > 0.0, "Peak near freq1 should have power")
        assertTrue(peak2 > 0.0, "Peak near freq2 should have power")

        // Freq1 (amplitude 1.0) should have more power than freq2 (amplitude 0.7)
        assertTrue(peak1 > peak2,
            "Freq1 peak ($peak1) should exceed freq2 peak ($peak2)")
    }

    @Test
    fun nextPowerOf2Works() {
        assertEquals(1, Fft.nextPowerOf2(1))
        assertEquals(2, Fft.nextPowerOf2(2))
        assertEquals(4, Fft.nextPowerOf2(3))
        assertEquals(256, Fft.nextPowerOf2(200))
        assertEquals(1024, Fft.nextPowerOf2(1024))
    }

    @Test
    fun parsevalsTheoremHolds() {
        // Energy in time domain should equal energy in frequency domain
        val n = 128
        val real = DoubleArray(n) { i ->
            cos(2.0 * PI * 0.15 * i / 4.0) + 0.5 * cos(2.0 * PI * 0.3 * i / 4.0)
        }
        val imag = DoubleArray(n)

        val timeEnergy = real.sumOf { it * it }

        Fft.transform(real, imag)

        val freqEnergy = (real.indices).sumOf { i -> real[i] * real[i] + imag[i] * imag[i] } / n

        assertTrue(abs(timeEnergy - freqEnergy) / timeEnergy < 0.01,
            "Parseval's theorem: time energy ($timeEnergy) should equal freq energy ($freqEnergy)")
    }
}
