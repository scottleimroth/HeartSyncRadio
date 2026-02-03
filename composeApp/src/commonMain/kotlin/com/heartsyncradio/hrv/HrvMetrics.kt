package com.heartsyncradio.hrv

data class HrvMetrics(
    val coherenceScore: Double,
    val rmssd: Double,
    val meanHr: Double,
    val lfPower: Double,   // LF band power (0.04-0.15 Hz), ms²
    val hfPower: Double,   // HF band power (0.15-0.40 Hz), ms²
    val rrCount: Int,
    val artifactsRemoved: Int,
    val timestamp: Long
)
