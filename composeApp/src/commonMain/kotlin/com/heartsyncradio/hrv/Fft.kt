package com.heartsyncradio.hrv

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Pure Kotlin Cooley-Tukey FFT implementation.
 * Operates on arrays of real and imaginary parts (in-place).
 * Input length must be a power of 2.
 */
object Fft {

    /**
     * Computes the FFT in-place. Both arrays are modified.
     * @param real real parts of the input/output
     * @param imag imaginary parts of the input/output
     */
    fun transform(real: DoubleArray, imag: DoubleArray) {
        val n = real.size
        require(n == imag.size) { "Arrays must have same length" }
        require(n > 0 && (n and (n - 1)) == 0) { "Length must be a power of 2" }

        // Bit-reversal permutation
        var j = 0
        for (i in 1 until n) {
            var bit = n shr 1
            while (j and bit != 0) {
                j = j xor bit
                bit = bit shr 1
            }
            j = j xor bit
            if (i < j) {
                var temp = real[i]; real[i] = real[j]; real[j] = temp
                temp = imag[i]; imag[i] = imag[j]; imag[j] = temp
            }
        }

        // Cooley-Tukey butterfly operations
        var len = 2
        while (len <= n) {
            val halfLen = len / 2
            val angle = -2.0 * PI / len
            val wReal = cos(angle)
            val wImag = sin(angle)

            var i = 0
            while (i < n) {
                var curReal = 1.0
                var curImag = 0.0

                for (k in 0 until halfLen) {
                    val uReal = real[i + k]
                    val uImag = imag[i + k]
                    val vReal = real[i + k + halfLen] * curReal - imag[i + k + halfLen] * curImag
                    val vImag = real[i + k + halfLen] * curImag + imag[i + k + halfLen] * curReal

                    real[i + k] = uReal + vReal
                    imag[i + k] = uImag + vImag
                    real[i + k + halfLen] = uReal - vReal
                    imag[i + k + halfLen] = uImag - vImag

                    val newCurReal = curReal * wReal - curImag * wImag
                    curImag = curReal * wImag + curImag * wReal
                    curReal = newCurReal
                }

                i += len
            }
            len = len shl 1
        }
    }

    /**
     * Computes power spectrum (magnitude squared) from FFT output.
     * Returns only the first half (positive frequencies).
     */
    fun powerSpectrum(real: DoubleArray, imag: DoubleArray): DoubleArray {
        val n = real.size / 2
        return DoubleArray(n) { i ->
            real[i] * real[i] + imag[i] * imag[i]
        }
    }

    /**
     * Returns the next power of 2 >= n.
     */
    fun nextPowerOf2(n: Int): Int {
        var p = 1
        while (p < n) p = p shl 1
        return p
    }
}
