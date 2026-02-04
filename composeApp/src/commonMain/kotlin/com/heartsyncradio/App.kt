package com.heartsyncradio

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import com.heartsyncradio.hrv.HrvMetrics
import com.heartsyncradio.model.ConnectionState
import com.heartsyncradio.model.HeartRateData
import com.heartsyncradio.model.PolarDeviceInfo
import com.heartsyncradio.ui.HomeScreen

@Composable
fun App(
    connectionState: ConnectionState,
    heartRateData: HeartRateData?,
    scannedDevices: List<PolarDeviceInfo>,
    isScanning: Boolean,
    batteryLevel: Int?,
    error: String?,
    permissionsGranted: Boolean,
    hrvMetrics: HrvMetrics?,
    selectedDeviceMode: String?,
    onSelectDeviceMode: (String) -> Unit,
    onChangeDeviceMode: () -> Unit,
    onStartScan: () -> Unit,
    onStopScan: () -> Unit,
    onConnectDevice: (String) -> Unit,
    onDisconnect: () -> Unit,
    onClearError: () -> Unit,
    onRequestPermissions: () -> Unit
) {
    MaterialTheme {
        HomeScreen(
            connectionState = connectionState,
            heartRateData = heartRateData,
            scannedDevices = scannedDevices,
            isScanning = isScanning,
            batteryLevel = batteryLevel,
            error = error,
            permissionsGranted = permissionsGranted,
            hrvMetrics = hrvMetrics,
            selectedDeviceMode = selectedDeviceMode,
            onSelectDeviceMode = onSelectDeviceMode,
            onChangeDeviceMode = onChangeDeviceMode,
            onStartScan = onStartScan,
            onStopScan = onStopScan,
            onConnectDevice = onConnectDevice,
            onDisconnect = onDisconnect,
            onClearError = onClearError,
            onRequestPermissions = onRequestPermissions
        )
    }
}
