package com.heartsyncradio.hrv

/**
 * Detects and corrects artifacts in RR-interval data using cubic spline
 * interpolation, per Quigley's spec: "Do not delete artifact segments.
 * Use cubic spline interpolation to replace missing or deviant beats
 * to maintain time-series integrity."
 *
 * Two-pass approach:
 * 1. Identify artifact indices (out-of-range or >25% deviation from local median)
 * 2. Interpolate artifact values using a natural cubic spline fitted to good beats
 */
object ArtifactDetector {

    private const val MIN_RR_MS = 300   // ~200 bpm
    private const val MAX_RR_MS = 2000  // ~30 bpm
    private const val DEVIATION_THRESHOLD = 0.25 // 25% from local median

    data class CleanResult(
        val cleanedRR: List<Double>,
        val artifactsRemoved: Int
    )

    fun clean(rrIntervals: List<Int>): CleanResult {
        if (rrIntervals.size < 3) {
            return CleanResult(rrIntervals.map { it.toDouble() }, 0)
        }

        // Pass 1: Identify artifact indices
        val isArtifact = BooleanArray(rrIntervals.size)

        for (i in rrIntervals.indices) {
            val rr = rrIntervals[i]

            // Flag physiologically impossible values
            if (rr < MIN_RR_MS || rr > MAX_RR_MS) {
                isArtifact[i] = true
                continue
            }

            // Check against local median (window of up to 20 surrounding intervals)
            val windowStart = maxOf(0, i - 10)
            val windowEnd = minOf(rrIntervals.size, i + 10)
            val window = rrIntervals.subList(windowStart, windowEnd)
                .filter { it in MIN_RR_MS..MAX_RR_MS }

            if (window.size >= 3) {
                val med = median(window)
                val deviation = kotlin.math.abs(rr - med) / med
                if (deviation > DEVIATION_THRESHOLD) {
                    isArtifact[i] = true
                }
            }
        }

        val artifactCount = isArtifact.count { it }
        if (artifactCount == 0) {
            return CleanResult(rrIntervals.map { it.toDouble() }, 0)
        }

        // If all or nearly all values are artifacts, we can't interpolate
        val goodCount = rrIntervals.size - artifactCount
        if (goodCount < 2) {
            return CleanResult(rrIntervals.map { it.toDouble() }, 0)
        }

        // Pass 2: Cubic spline interpolation over artifact positions
        val goodIndices = DoubleArray(goodCount)
        val goodValues = DoubleArray(goodCount)
        var gi = 0
        for (i in rrIntervals.indices) {
            if (!isArtifact[i]) {
                goodIndices[gi] = i.toDouble()
                goodValues[gi] = rrIntervals[i].toDouble()
                gi++
            }
        }

        val spline = CubicSpline.fit(goodIndices, goodValues)

        val result = DoubleArray(rrIntervals.size)
        for (i in rrIntervals.indices) {
            result[i] = if (isArtifact[i]) {
                // Clamp interpolated value to physiological range
                CubicSpline.evaluate(spline, goodIndices, i.toDouble())
                    .coerceIn(MIN_RR_MS.toDouble(), MAX_RR_MS.toDouble())
            } else {
                rrIntervals[i].toDouble()
            }
        }

        return CleanResult(result.toList(), artifactCount)
    }

    private fun median(values: List<Int>): Double {
        val sorted = values.sorted()
        val mid = sorted.size / 2
        return if (sorted.size % 2 == 0) {
            (sorted[mid - 1] + sorted[mid]) / 2.0
        } else {
            sorted[mid].toDouble()
        }
    }
}
