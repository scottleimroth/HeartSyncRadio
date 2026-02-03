package com.heartsyncradio.viewmodel

import androidx.lifecycle.ViewModel
import com.heartsyncradio.model.ConnectionState
import com.heartsyncradio.model.HeartRateData
import com.heartsyncradio.model.PolarDeviceInfo
import com.heartsyncradio.polar.PolarManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class HomeViewModel(
    private val polarManager: PolarManager
) : ViewModel() {

    val connectionState: StateFlow<ConnectionState> = polarManager.connectionState
    val heartRateData: StateFlow<HeartRateData?> = polarManager.heartRateData
    val scannedDevices: StateFlow<List<PolarDeviceInfo>> = polarManager.scannedDevices
    val isScanning: StateFlow<Boolean> = polarManager.isScanning
    val batteryLevel: StateFlow<Int?> = polarManager.batteryLevel
    val error: StateFlow<String?> = polarManager.error

    private val _permissionsGranted = MutableStateFlow(false)
    val permissionsGranted: StateFlow<Boolean> = _permissionsGranted.asStateFlow()

    fun startScan() {
        if (_permissionsGranted.value) {
            polarManager.startScan()
        }
    }

    fun stopScan() {
        polarManager.stopScan()
    }

    fun connectToDevice(deviceId: String) {
        polarManager.connectToDevice(deviceId)
    }

    fun disconnectFromDevice() {
        polarManager.disconnectFromDevice()
    }

    fun clearError() {
        polarManager.clearError()
    }

    fun onPermissionsResult(granted: Boolean) {
        _permissionsGranted.value = granted
    }
}
