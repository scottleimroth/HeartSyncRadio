package com.heartsyncradio.hrv

/**
 * Orchestrates the HRV processing pipeline with a sliding window
 * for real-time coherence updates.
 *
 * Maintains a rolling buffer of RR intervals. When enough data is
 * available (minimum 30 seconds), computes HRV metrics on the current
 * window. Window size is configurable (default 64 seconds per HeartMath).
 */
class HrvProcessor(
    private val windowSeconds: Int = 64
) {

    private val rrBuffer = mutableListOf<Int>()
    private var lastMetrics: HrvMetrics? = null

    /**
     * Adds new RR intervals from the sensor and recomputes metrics
     * if enough data is available.
     * @return updated HRV metrics, or null if insufficient data
     */
    fun addRrIntervals(rrIntervals: List<Int>): HrvMetrics? {
        rrBuffer.addAll(rrIntervals)
        trimBuffer()
        return computeMetrics()
    }

    /**
     * Returns the latest computed metrics without adding new data.
     */
    fun currentMetrics(): HrvMetrics? = lastMetrics

    /**
     * Resets the processor, clearing all buffered data.
     */
    fun reset() {
        rrBuffer.clear()
        lastMetrics = null
    }

    /**
     * Returns the total duration of buffered RR data in seconds.
     */
    fun bufferDurationSeconds(): Double {
        return rrBuffer.sumOf { it.toLong() } / 1000.0
    }

    private fun trimBuffer() {
        // Keep only the last `windowSeconds` worth of RR data
        val maxDurationMs = windowSeconds * 1000L
        var totalMs = 0L
        var keepFrom = rrBuffer.size

        for (i in rrBuffer.indices.reversed()) {
            totalMs += rrBuffer[i]
            if (totalMs > maxDurationMs) {
                keepFrom = i + 1
                break
            }
            if (i == 0) keepFrom = 0
        }

        if (keepFrom > 0) {
            rrBuffer.subList(0, keepFrom).clear()
        }
    }

    private fun computeMetrics(): HrvMetrics? {
        // Need at least ~30 seconds of data for meaningful metrics
        val durationMs = rrBuffer.sumOf { it.toLong() }
        if (durationMs < 30_000 || rrBuffer.size < 30) return lastMetrics

        // Step 1: Artifact removal
        val cleanResult = ArtifactDetector.clean(rrBuffer)
        if (cleanResult.cleanedRR.size < 20) return lastMetrics

        // Step 2: RMSSD
        val rmssd = Rmssd.calculate(cleanResult.cleanedRR)

        // Step 3: Spectral analysis (coherence + LF/HF band powers)
        val spectral = CoherenceCalculator.calculate(cleanResult.cleanedRR)

        // Step 4: Mean HR
        val meanRR = cleanResult.cleanedRR.average()
        val meanHr = if (meanRR > 0) 60000.0 / meanRR else 0.0

        lastMetrics = HrvMetrics(
            coherenceScore = spectral.coherence,
            rmssd = rmssd,
            meanHr = meanHr,
            lfPower = spectral.lfPower,
            hfPower = spectral.hfPower,
            rrCount = cleanResult.cleanedRR.size,
            artifactsRemoved = cleanResult.artifactsRemoved,
            timestamp = currentTimeMillis()
        )

        return lastMetrics
    }
}

internal expect fun currentTimeMillis(): Long
