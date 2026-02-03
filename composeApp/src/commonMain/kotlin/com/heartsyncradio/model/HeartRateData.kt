package com.heartsyncradio.model

data class HeartRateData(
    val hr: Int,
    val rrIntervals: List<Int>,
    val contactStatus: Boolean,
    val timestamp: Long
)
