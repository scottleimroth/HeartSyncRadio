package com.heartsyncradio.hrv

import kotlin.math.abs
import kotlin.math.sqrt
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RmssdTest {

    @Test
    fun constantRrIntervalsGiveZeroRmssd() {
        // All identical intervals -> successive differences are 0
        val rr = List(60) { 800.0 }
        val result = Rmssd.calculate(rr)
        assertEquals(0.0, result, 1e-10)
    }

    @Test
    fun alternatingIntervalsGiveKnownRmssd() {
        // Alternating 800, 820 ms -> all successive diffs = ±20
        // RMSSD = sqrt(mean(20²)) = 20.0
        val rr = List(100) { if (it % 2 == 0) 800.0 else 820.0 }
        val result = Rmssd.calculate(rr)
        assertEquals(20.0, result, 0.01)
    }

    @Test
    fun knownHandCalculation() {
        // RR = [800, 810, 790, 815, 805]
        // Diffs: 10, -20, 25, -10
        // Squared: 100, 400, 625, 100
        // Mean: 1225/4 = 306.25
        // RMSSD = sqrt(306.25) = 17.5
        val rr = listOf(800.0, 810.0, 790.0, 815.0, 805.0)
        val result = Rmssd.calculate(rr)
        assertEquals(sqrt(306.25), result, 0.001)
    }

    @Test
    fun tooFewIntervalsReturnsZero() {
        assertEquals(0.0, Rmssd.calculate(emptyList()))
        assertEquals(0.0, Rmssd.calculate(listOf(800.0)))
    }

    @Test
    fun rmssdIncreasesWithMoreVariability() {
        val stable = List(100) { 800.0 + (it % 2) * 5.0 }    // ±5ms alternation
        val variable = List(100) { 800.0 + (it % 2) * 50.0 }  // ±50ms alternation

        val rmssdStable = Rmssd.calculate(stable)
        val rmssdVariable = Rmssd.calculate(variable)

        assertTrue(rmssdVariable > rmssdStable * 5,
            "More variable series should have much higher RMSSD")
    }
}
