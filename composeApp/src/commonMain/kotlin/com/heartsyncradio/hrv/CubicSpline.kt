package com.heartsyncradio.hrv

/**
 * Natural cubic spline interpolation.
 * Used by ArtifactDetector (artifact correction) and CoherenceCalculator (resampling).
 */
object CubicSpline {

    /**
     * Coefficients for each spline segment: S_i(x) = a + b*(x-x_i) + c*(x-x_i)^2 + d*(x-x_i)^3
     */
    data class Coeffs(
        val a: DoubleArray,
        val b: DoubleArray,
        val c: DoubleArray,
        val d: DoubleArray
    )

    /**
     * Computes natural cubic spline coefficients for the given knot points.
     * Natural boundary conditions: S''(x_0) = 0 and S''(x_n) = 0.
     */
    fun fit(x: DoubleArray, y: DoubleArray): Coeffs {
        val n = x.size - 1
        require(n >= 1) { "Need at least 2 points for spline" }

        val h = DoubleArray(n) { i -> x[i + 1] - x[i] }
        val a = y.copyOf()

        val alpha = DoubleArray(n + 1)
        for (i in 1 until n) {
            alpha[i] = (3.0 / h[i]) * (a[i + 1] - a[i]) -
                    (3.0 / h[i - 1]) * (a[i] - a[i - 1])
        }

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

        val c = DoubleArray(n + 1)
        val b = DoubleArray(n)
        val d = DoubleArray(n)

        for (j in n - 1 downTo 0) {
            c[j] = z[j] - mu[j] * c[j + 1]
            b[j] = (a[j + 1] - a[j]) / h[j] - h[j] * (c[j + 1] + 2.0 * c[j]) / 3.0
            d[j] = (c[j + 1] - c[j]) / (3.0 * h[j])
        }

        return Coeffs(a, b, c, d)
    }

    /**
     * Evaluates the spline at a given x value.
     * For x outside the knot range, uses the nearest boundary segment (clamped extrapolation).
     */
    fun evaluate(spline: Coeffs, knots: DoubleArray, x: Double): Double {
        val n = knots.size - 1

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
