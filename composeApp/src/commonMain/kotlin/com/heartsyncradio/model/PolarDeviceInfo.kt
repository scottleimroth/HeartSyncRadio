package com.heartsyncradio.model

data class PolarDeviceInfo(
    val deviceId: String,
    val name: String,
    val rssi: Int,
    val isConnectable: Boolean
)
