package com.heartsyncradio.hrv

import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ArtifactDetectorTest {

    @Test
    fun cleanDataPassesThroughUnchanged() {
        val rr = List(50) { 800 }
        val result = ArtifactDetector.clean(rr)
        assertEquals(0, result.artifactsRemoved)
        assertEquals(50, result.cleanedRR.size)
        result.cleanedRR.forEach { assertEquals(800.0, it, 1e-10) }
    }

    @Test
    fun outOfRangeValuesAreInterpolated() {
        // Insert a physiologically impossible value (100ms = 600bpm)
        val rr = MutableList(50) { 800 }
        rr[25] = 100  // artifact
        val result = ArtifactDetector.clean(rr)
        assertTrue(result.artifactsRemoved >= 1)
        // The interpolated value should be close to 800 (neighbors are all 800)
        assertTrue(abs(result.cleanedRR[25] - 800.0) < 50.0,
            "Interpolated value should be near neighbors, got ${result.cleanedRR[25]}")
        // Output length preserved (not deleted)
        assertEquals(50, result.cleanedRR.size)
    }

    @Test
    fun highValueArtifactsAreInterpolated() {
        val rr = MutableList(50) { 800 }
        rr[25] = 2500  // too slow, >2000ms
        val result = ArtifactDetector.clean(rr)
        assertTrue(result.artifactsRemoved >= 1)
        assertEquals(50, result.cleanedRR.size)
        // Should be clamped to MAX_RR_MS at most
        assertTrue(result.cleanedRR[25] <= 2000.0)
    }

    @Test
    fun deviationArtifactsAreInterpolated() {
        // A sudden jump of >25% from local median
        val rr = MutableList(50) { 800 }
        rr[25] = 1200  // 50% above 800, well over 25% threshold
        val result = ArtifactDetector.clean(rr)
        assertTrue(result.artifactsRemoved >= 1)
        assertEquals(50, result.cleanedRR.size)
        // Interpolated value should be much closer to 800 than to 1200
        assertTrue(abs(result.cleanedRR[25] - 800.0) < 100.0,
            "Spline-interpolated value should be near 800, got ${result.cleanedRR[25]}")
    }

    @Test
    fun outputLengthAlwaysMatchesInput() {
        // Multiple artifacts scattered throughout
        val rr = MutableList(100) { 800 }
        rr[10] = 100
        rr[30] = 2500
        rr[50] = 1500  // 87.5% above 800
        rr[70] = 200
        val result = ArtifactDetector.clean(rr)
        assertEquals(100, result.cleanedRR.size,
            "Output length must equal input length (no deletions per Quigley spec)")
    }

    @Test
    fun splineInterpolationIsSmoothAcrossMultipleArtifacts() {
        // Linearly increasing RR with two artifacts
        val rr = MutableList(50) { 700 + it * 4 }  // 700 to 896
        rr[20] = 100  // artifact in rising sequence
        rr[21] = 100  // consecutive artifact
        val result = ArtifactDetector.clean(rr)
        assertEquals(50, result.cleanedRR.size)
        // Interpolated values at 20,21 should be roughly 780, 784
        val expected20 = 700.0 + 20 * 4
        val expected21 = 700.0 + 21 * 4
        assertTrue(abs(result.cleanedRR[20] - expected20) < 30.0,
            "Spline at index 20 should be near $expected20, got ${result.cleanedRR[20]}")
        assertTrue(abs(result.cleanedRR[21] - expected21) < 30.0,
            "Spline at index 21 should be near $expected21, got ${result.cleanedRR[21]}")
    }

    @Test
    fun tooFewIntervalsReturnAsIs() {
        val rr = listOf(800, 900)
        val result = ArtifactDetector.clean(rr)
        assertEquals(0, result.artifactsRemoved)
        assertEquals(2, result.cleanedRR.size)
    }

    @Test
    fun cubicSplineReproducesLinearData() {
        // If good points form a line, spline should exactly reproduce it
        val x = doubleArrayOf(0.0, 1.0, 2.0, 3.0, 4.0)
        val y = doubleArrayOf(100.0, 200.0, 300.0, 400.0, 500.0)
        val spline = ArtifactDetector.naturalCubicSpline(x, y)

        // Evaluate at midpoints
        for (i in 0..3) {
            val midX = i + 0.5
            val expected = 100.0 + midX * 100.0
            val actual = ArtifactDetector.evaluateSpline(spline, x, midX)
            assertTrue(abs(actual - expected) < 0.01,
                "Spline should be exact for linear data at $midX: expected $expected, got $actual")
        }
    }

    @Test
    fun cubicSplineReproducesQuadraticData() {
        // Natural cubic spline should exactly reproduce quadratic polynomials
        val x = doubleArrayOf(0.0, 1.0, 2.0, 3.0, 4.0, 5.0)
        val y = DoubleArray(6) { i -> (i * i).toDouble() }  // y = x²
        val spline = ArtifactDetector.naturalCubicSpline(x, y)

        val testX = 2.5
        val expected = testX * testX  // 6.25
        val actual = ArtifactDetector.evaluateSpline(spline, x, testX)
        assertTrue(abs(actual - expected) < 0.1,
            "Spline should closely approximate quadratic at $testX: expected $expected, got $actual")
    }
}
