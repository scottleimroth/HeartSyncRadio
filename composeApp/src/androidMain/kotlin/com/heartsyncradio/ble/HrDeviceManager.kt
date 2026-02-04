package com.heartsyncradio.ble

import com.heartsyncradio.hrv.HrvMetrics
import com.heartsyncradio.model.ConnectionState
import com.heartsyncradio.model.HeartRateData
import com.heartsyncradio.model.PolarDeviceInfo
import kotlinx.coroutines.flow.StateFlow

/**
 * Common interface for heart rate device managers.
 * Implemented by PolarManager (Polar SDK) and GenericBleManager (standard BLE HR Profile).
 */
interface HrDeviceManager {
    val connectionState: StateFlow<ConnectionState>
    val connectedDeviceId: StateFlow<String?>
    val heartRateData: StateFlow<HeartRateData?>
    val batteryLevel: StateFlow<Int?>
    val scannedDevices: StateFlow<List<PolarDeviceInfo>>
    val isScanning: StateFlow<Boolean>
    val error: StateFlow<String?>
    val hrvMetrics: StateFlow<HrvMetrics?>

    fun startScan()
    fun stopScan()
    fun connectToDevice(deviceId: String)
    fun disconnectFromDevice()
    fun clearError()
    fun shutDown()
}
