package com.heartsyncradio.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.heartsyncradio.hrv.HrvMetrics
import com.heartsyncradio.model.ConnectionState
import com.heartsyncradio.model.HeartRateData
import com.heartsyncradio.model.PolarDeviceInfo
import com.heartsyncradio.ble.HrDeviceManager
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModel(
    initialManager: HrDeviceManager
) : ViewModel() {

    private val _deviceManager = MutableStateFlow(initialManager)

    private val _selectedDeviceMode = MutableStateFlow<String?>(null)
    val selectedDeviceMode: StateFlow<String?> = _selectedDeviceMode.asStateFlow()

    val connectionState: StateFlow<ConnectionState> = _deviceManager
        .flatMapLatest { it.connectionState }
        .stateIn(viewModelScope, SharingStarted.Eagerly, ConnectionState.DISCONNECTED)

    val heartRateData: StateFlow<HeartRateData?> = _deviceManager
        .flatMapLatest { it.heartRateData }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val scannedDevices: StateFlow<List<PolarDeviceInfo>> = _deviceManager
        .flatMapLatest { it.scannedDevices }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val isScanning: StateFlow<Boolean> = _deviceManager
        .flatMapLatest { it.isScanning }
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    val batteryLevel: StateFlow<Int?> = _deviceManager
        .flatMapLatest { it.batteryLevel }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val error: StateFlow<String?> = _deviceManager
        .flatMapLatest { it.error }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val hrvMetrics: StateFlow<HrvMetrics?> = _deviceManager
        .flatMapLatest { it.hrvMetrics }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private val _permissionsGranted = MutableStateFlow(false)
    val permissionsGranted: StateFlow<Boolean> = _permissionsGranted.asStateFlow()

    fun switchManager(newManager: HrDeviceManager, modeName: String) {
        _deviceManager.value = newManager
        _selectedDeviceMode.value = modeName
    }

    fun clearDeviceSelection() {
        _deviceManager.value.stopScan()
        _selectedDeviceMode.value = null
    }

    fun startScan() {
        if (_permissionsGranted.value) {
            _deviceManager.value.startScan()
        }
    }

    fun stopScan() {
        _deviceManager.value.stopScan()
    }

    fun connectToDevice(deviceId: String) {
        _deviceManager.value.connectToDevice(deviceId)
    }

    fun disconnectFromDevice() {
        _deviceManager.value.disconnectFromDevice()
    }

    fun clearError() {
        _deviceManager.value.clearError()
    }

    fun onPermissionsResult(granted: Boolean) {
        _permissionsGranted.value = granted
    }
}
