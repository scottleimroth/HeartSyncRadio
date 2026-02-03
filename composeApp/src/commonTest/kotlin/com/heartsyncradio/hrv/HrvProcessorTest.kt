package com.heartsyncradio.hrv

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class HrvProcessorTest {

    @Test
    fun returnsNullWithInsufficientData() {
        val processor = HrvProcessor()
        // Add only 5 seconds of data
        val rr = List(6) { 800 }
        val result = processor.addRrIntervals(rr)
        assertNull(result, "Should return null with less than 30s of data")
    }

    @Test
    fun returnsMetricsWithSufficientData() {
        val processor = HrvProcessor()
        // ~40 seconds of data at 75 bpm
        val rr = List(50) { 800 }
        val result = processor.addRrIntervals(rr)
        assertNotNull(result, "Should return metrics with 40s of data")
    }

    @Test
    fun metricsHaveReasonableValues() {
        val processor = HrvProcessor()
        // Generate 60 seconds of slightly varying RR data
        val rr = List(75) { i -> 800 + (i % 5) * 4 }  // 800-816ms range
        val result = processor.addRrIntervals(rr)
        assertNotNull(result)

        assertTrue(result.meanHr > 60.0 && result.meanHr < 100.0,
            "Mean HR should be in normal range, got ${result.meanHr}")
        assertTrue(result.rmssd >= 0.0,
            "RMSSD should be non-negative, got ${result.rmssd}")
        assertTrue(result.coherenceScore >= 0.0 && result.coherenceScore <= 1.0,
            "Coherence should be 0-1, got ${result.coherenceScore}")
        assertTrue(result.lfPower >= 0.0,
            "LF power should be non-negative, got ${result.lfPower}")
        assertTrue(result.hfPower >= 0.0,
            "HF power should be non-negative, got ${result.hfPower}")
    }

    @Test
    fun artifactedDataStillProducesMetrics() {
        val processor = HrvProcessor()
        val rr = MutableList(75) { 800 }
        // Inject some artifacts
        rr[20] = 100   // too fast
        rr[40] = 2500  // too slow
        rr[60] = 1200  // big deviation

        val result = processor.addRrIntervals(rr)
        assertNotNull(result)
        assertTrue(result.artifactsRemoved >= 3,
            "Should detect at least 3 artifacts, got ${result.artifactsRemoved}")
        // Mean HR should still be reasonable (artifacts were interpolated, not removed)
        assertTrue(result.meanHr > 60.0 && result.meanHr < 100.0,
            "Mean HR should still be reasonable after artifact correction, got ${result.meanHr}")
    }

    @Test
    fun bufferTrimsToWindowSize() {
        val processor = HrvProcessor(windowSeconds = 30)
        // Add 60 seconds of data
        val rr = List(75) { 800 }
        processor.addRrIntervals(rr)

        // Buffer should have been trimmed to ~30 seconds
        assertTrue(processor.bufferDurationSeconds() <= 35.0,
            "Buffer should be trimmed to ~30s, got ${processor.bufferDurationSeconds()}s")
    }

    @Test
    fun resetClearsState() {
        val processor = HrvProcessor()
        val rr = List(75) { 800 }
        processor.addRrIntervals(rr)
        assertNotNull(processor.currentMetrics())

        processor.reset()
        assertNull(processor.currentMetrics())
        assertEquals(0.0, processor.bufferDurationSeconds())
    }

    @Test
    fun incrementalAdditionWorks() {
        val processor = HrvProcessor()

        // Add data in small batches (like real-time sensor data)
        for (i in 0 until 20) {
            processor.addRrIntervals(listOf(800, 810, 790, 805))
        }

        // Should have enough data now (~64s)
        val result = processor.currentMetrics()
        assertNotNull(result, "Should have metrics after incremental additions")
    }

    @Test
    fun sinusoidalInputProducesHighCoherence() {
        val processor = HrvProcessor(windowSeconds = 120)

        // Generate coherent HRV signal: sine wave at 0.1 Hz
        val rrList = mutableListOf<Int>()
        var elapsedMs = 0.0
        while (elapsedMs < 120_000) {
            val timeSec = elapsedMs / 1000.0
            val rr = 800.0 + 30.0 * sin(2.0 * PI * 0.1 * timeSec)
            rrList.add(rr.toInt())
            elapsedMs += rr
        }

        val result = processor.addRrIntervals(rrList)
        assertNotNull(result)
        assertTrue(result.coherenceScore > 0.2,
            "Sinusoidal input should produce meaningful coherence, got ${result.coherenceScore}")
    }
}
