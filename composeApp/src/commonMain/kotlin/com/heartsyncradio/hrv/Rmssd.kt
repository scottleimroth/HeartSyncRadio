package com.heartsyncradio.hrv

import kotlin.math.sqrt

/**
 * Calculates RMSSD (Root Mean Square of Successive Differences).
 * The most validated short-term parasympathetic HRV measure.
 * Requires only ~60 seconds of clean RR data.
 */
object Rmssd {

    fun calculate(rrIntervals: List<Double>): Double {
        if (rrIntervals.size < 2) return 0.0

        var sumSquaredDiffs = 0.0
        for (i in 0 until rrIntervals.size - 1) {
            val diff = rrIntervals[i + 1] - rrIntervals[i]
            sumSquaredDiffs += diff * diff
        }

        return sqrt(sumSquaredDiffs / (rrIntervals.size - 1))
    }
}
