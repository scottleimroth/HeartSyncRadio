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

        val spline = naturalCubicSpline(goodIndices, goodValues)

        val result = DoubleArray(rrIntervals.size)
        for (i in rrIntervals.indices) {
            result[i] = if (isArtifact[i]) {
                // Clamp interpolated value to physiological range
                evaluateSpline(spline, goodIndices, i.toDouble())
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

    // --- Natural cubic spline ---

    /**
     * Coefficients for each spline segment: S_i(x) = a + b*(x-x_i) + c*(x-x_i)^2 + d*(x-x_i)^3
     */
    data class SplineCoeffs(
        val a: DoubleArray,
        val b: DoubleArray,
        val c: DoubleArray,
        val d: DoubleArray
    )

    /**
     * Computes natural cubic spline coefficients for the given knot points.
     * Natural boundary conditions: S''(x_0) = 0 and S''(x_n) = 0.
     */
    internal fun naturalCubicSpline(x: DoubleArray, y: DoubleArray): SplineCoeffs {
        val n = x.size - 1  // number of segments
        require(n >= 1) { "Need at least 2 points for spline" }

        val h = DoubleArray(n) { i -> x[i + 1] - x[i] }
        val a = y.copyOf()

        // Solve tridiagonal system for c coefficients
        val alpha = DoubleArray(n + 1)
        for (i in 1 until n) {
            alpha[i] = (3.0 / h[i]) * (a[i + 1] - a[i]) -
                    (3.0 / h[i - 1]) * (a[i] - a[i - 1])
        }

        // Forward sweep
        val l = DoubleArray(n + 1)
        val mu = DoubleArray(n + 1)
        val z = DoubleArray(n + 1)
        l[0] = 1.0

        for (i in 1 until n) {
            l[i] = 2.0 * (x[i + 1] - x[i - 1]) - h[i - 1] * mu[i - 1]
            mu[i] = h[i] / l[i]
            z[i] = (alpha[i] - h[i - 1] * z[i - 1]) / l[i]
        }

        l[n] = 1.0

        // Back substitution
        val c = DoubleArray(n + 1)
        val b = DoubleArray(n)
        val d = DoubleArray(n)

        for (j in n - 1 downTo 0) {
            c[j] = z[j] - mu[j] * c[j + 1]
            b[j] = (a[j + 1] - a[j]) / h[j] - h[j] * (c[j + 1] + 2.0 * c[j]) / 3.0
            d[j] = (c[j + 1] - c[j]) / (3.0 * h[j])
        }

        return SplineCoeffs(a, b, c, d)
    }

    /**
     * Evaluates the spline at a given x value.
     * For x outside the knot range, uses the nearest boundary segment (clamped extrapolation).
     */
    internal fun evaluateSpline(spline: SplineCoeffs, knots: DoubleArray, x: Double): Double {
        val n = knots.size - 1

        // Find the segment
        val i = when {
            x <= knots[0] -> 0
            x >= knots[n] -> n - 1
            else -> {
                var lo = 0
                var hi = n
                while (lo < hi - 1) {
                    val mid = (lo + hi) / 2
                    if (knots[mid] <= x) lo = mid else hi = mid
                }
                lo
            }
        }

        val dx = x - knots[i]
        return spline.a[i] + spline.b[i] * dx + spline.c[i] * dx * dx + spline.d[i] * dx * dx * dx
    }
}
