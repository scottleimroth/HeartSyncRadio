package com.heartsyncradio.ble

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.os.ParcelUuid
import android.util.Log
import com.heartsyncradio.hrv.HrvMetrics
import com.heartsyncradio.hrv.HrvProcessor
import com.heartsyncradio.model.ConnectionState
import com.heartsyncradio.model.HeartRateData
import com.heartsyncradio.model.PolarDeviceInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

/**
 * Generic BLE Heart Rate Profile manager.
 * Connects to any device implementing the standard Bluetooth Heart Rate Service (0x180D).
 * Parses Heart Rate Measurement characteristic (0x2A37) including RR intervals.
 * Works with: Wahoo TICKR, CooSpo, Magene, WHOOP, and any BLE HR chest strap.
 */
@SuppressLint("MissingPermission")
class GenericBleManager(private val context: Context) : HrDeviceManager {

    companion object {
        private const val TAG = "GenericBleManager"

        // Standard Bluetooth Heart Rate Service and Characteristic UUIDs
        val HR_SERVICE_UUID: UUID = UUID.fromString("0000180d-0000-1000-8000-00805f9b34fb")
        val HR_MEASUREMENT_UUID: UUID = UUID.fromString("00002a37-0000-1000-8000-00805f9b34fb")
        val BATTERY_SERVICE_UUID: UUID = UUID.fromString("0000180f-0000-1000-8000-00805f9b34fb")
        val BATTERY_LEVEL_UUID: UUID = UUID.fromString("00002a19-0000-1000-8000-00805f9b34fb")
        val CCC_DESCRIPTOR_UUID: UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")
    }

    private val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    private val bluetoothAdapter: BluetoothAdapter? = bluetoothManager.adapter
    private var scanner: BluetoothLeScanner? = null
    private var gatt: BluetoothGatt? = null
    private val hrvProcessor = HrvProcessor()

    private val _connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
    override val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private val _connectedDeviceId = MutableStateFlow<String?>(null)
    override val connectedDeviceId: StateFlow<String?> = _connectedDeviceId.asStateFlow()

    private val _heartRateData = MutableStateFlow<HeartRateData?>(null)
    override val heartRateData: StateFlow<HeartRateData?> = _heartRateData.asStateFlow()

    private val _batteryLevel = MutableStateFlow<Int?>(null)
    override val batteryLevel: StateFlow<Int?> = _batteryLevel.asStateFlow()

    private val _scannedDevices = MutableStateFlow<List<PolarDeviceInfo>>(emptyList())
    override val scannedDevices: StateFlow<List<PolarDeviceInfo>> = _scannedDevices.asStateFlow()

    private val _isScanning = MutableStateFlow(false)
    override val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    override val error: StateFlow<String?> = _error.asStateFlow()

    private val _hrvMetrics = MutableStateFlow<HrvMetrics?>(null)
    override val hrvMetrics: StateFlow<HrvMetrics?> = _hrvMetrics.asStateFlow()

    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            val device = result.device
            val name = device.name ?: return // Skip unnamed devices
            val address = device.address

            val deviceInfo = PolarDeviceInfo(
                deviceId = address,
                name = name,
                rssi = result.rssi,
                isConnectable = true
            )

            val current = _scannedDevices.value.toMutableList()
            val existing = current.indexOfFirst { it.deviceId == address }
            if (existing >= 0) {
                current[existing] = deviceInfo // Update RSSI
            } else {
                current.add(deviceInfo)
            }
            _scannedDevices.value = current
        }

        override fun onScanFailed(errorCode: Int) {
            Log.e(TAG, "Scan failed with error code: $errorCode")
            _error.value = "BLE scan failed (error $errorCode)"
            _isScanning.value = false
        }
    }

    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            when (newState) {
                BluetoothProfile.STATE_CONNECTED -> {
                    Log.d(TAG, "Connected to ${gatt.device.address}")
                    _connectionState.value = ConnectionState.CONNECTED
                    _connectedDeviceId.value = gatt.device.address
                    gatt.discoverServices()
                }
                BluetoothProfile.STATE_DISCONNECTED -> {
                    Log.d(TAG, "Disconnected from ${gatt.device.address}")
                    _connectionState.value = ConnectionState.DISCONNECTED
                    _connectedDeviceId.value = null
                    _heartRateData.value = null
                    _batteryLevel.value = null
                    _hrvMetrics.value = null
                    hrvProcessor.reset()
                    this@GenericBleManager.gatt?.close()
                    this@GenericBleManager.gatt = null
                }
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            if (status != BluetoothGatt.GATT_SUCCESS) {
                Log.e(TAG, "Service discovery failed: $status")
                _error.value = "Failed to discover device services"
                return
            }

            // Subscribe to Heart Rate Measurement notifications
            val hrService = gatt.getService(HR_SERVICE_UUID)
            if (hrService != null) {
                val hrChar = hrService.getCharacteristic(HR_MEASUREMENT_UUID)
                if (hrChar != null) {
                    gatt.setCharacteristicNotification(hrChar, true)
                    val descriptor = hrChar.getDescriptor(CCC_DESCRIPTOR_UUID)
                    if (descriptor != null) {
                        descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                        gatt.writeDescriptor(descriptor)
                    }
                    Log.d(TAG, "Subscribed to HR notifications")
                }
            } else {
                Log.e(TAG, "Heart Rate Service not found on device")
                _error.value = "Device does not support Heart Rate Service"
            }

            // Read battery level if available
            val batteryService = gatt.getService(BATTERY_SERVICE_UUID)
            if (batteryService != null) {
                val batteryChar = batteryService.getCharacteristic(BATTERY_LEVEL_UUID)
                if (batteryChar != null) {
                    gatt.readCharacteristic(batteryChar)
                }
            }
        }

        @Deprecated("Deprecated in API 33")
        override fun onCharacteristicChanged(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
            if (characteristic.uuid == HR_MEASUREMENT_UUID) {
                parseHeartRateMeasurement(characteristic.value)
            }
        }

        @Deprecated("Deprecated in API 33")
        override fun onCharacteristicRead(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS && characteristic.uuid == BATTERY_LEVEL_UUID) {
                val level = characteristic.value[0].toInt() and 0xFF
                _batteryLevel.value = level
                Log.d(TAG, "Battery: $level%")
            }
        }
    }

    /**
     * Parse the Heart Rate Measurement characteristic per Bluetooth spec.
     * Byte 0: Flags
     *   Bit 0: HR format (0 = UINT8, 1 = UINT16)
     *   Bit 1-2: Sensor contact status
     *   Bit 3: Energy expended present
     *   Bit 4: RR intervals present
     * Byte 1+: HR value (1 or 2 bytes)
     * Then optional energy expended (2 bytes)
     * Then optional RR intervals (2 bytes each, in 1/1024 second units)
     */
    private fun parseHeartRateMeasurement(data: ByteArray) {
        if (data.isEmpty()) return

        val flags = data[0].toInt() and 0xFF
        val hrFormatUint16 = (flags and 0x01) != 0
        val contactDetected = (flags and 0x02) != 0
        val contactSupported = (flags and 0x04) != 0
        val energyExpendedPresent = (flags and 0x08) != 0
        val rrIntervalsPresent = (flags and 0x10) != 0

        var offset = 1

        // Parse heart rate value
        val hr: Int
        if (hrFormatUint16) {
            hr = (data[offset].toInt() and 0xFF) or ((data[offset + 1].toInt() and 0xFF) shl 8)
            offset += 2
        } else {
            hr = data[offset].toInt() and 0xFF
            offset += 1
        }

        // Skip energy expended if present
        if (energyExpendedPresent) {
            offset += 2
        }

        // Parse RR intervals (in 1/1024 second resolution)
        val rrIntervals = mutableListOf<Int>()
        if (rrIntervalsPresent) {
            while (offset + 1 < data.size) {
                val rrRaw = (data[offset].toInt() and 0xFF) or ((data[offset + 1].toInt() and 0xFF) shl 8)
                // Convert from 1/1024 seconds to milliseconds
                val rrMs = (rrRaw * 1000.0 / 1024.0).toInt()
                rrIntervals.add(rrMs)
                offset += 2
            }
        }

        // Determine contact status: if sensor doesn't support contact detection, assume contact
        val hasContact = if (contactSupported) contactDetected || hr > 0 else true

        _heartRateData.value = HeartRateData(
            hr = hr,
            rrIntervals = rrIntervals,
            contactStatus = hasContact,
            timestamp = System.currentTimeMillis()
        )

        // Feed RR intervals into HRV processor
        if (rrIntervals.isNotEmpty()) {
            val metrics = hrvProcessor.addRrIntervals(rrIntervals)
            if (metrics != null) {
                _hrvMetrics.value = metrics
            }
        }
    }

    override fun startScan() {
        val adapter = bluetoothAdapter
        if (adapter == null || !adapter.isEnabled) {
            _error.value = "Bluetooth is not available or disabled"
            return
        }

        scanner = adapter.bluetoothLeScanner
        if (scanner == null) {
            _error.value = "BLE scanner not available"
            return
        }

        _scannedDevices.value = emptyList()
        _isScanning.value = true
        _error.value = null

        // Filter for devices advertising Heart Rate Service
        val filter = ScanFilter.Builder()
            .setServiceUuid(ParcelUuid(HR_SERVICE_UUID))
            .build()

        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()

        try {
            scanner?.startScan(listOf(filter), settings, scanCallback)
            Log.d(TAG, "BLE scan started (filtering for HR Service)")
        } catch (e: Exception) {
            Log.e(TAG, "Scan start failed", e)
            _error.value = "Scan failed: ${e.message}"
            _isScanning.value = false
        }
    }

    override fun stopScan() {
        try {
            scanner?.stopScan(scanCallback)
        } catch (e: Exception) {
            Log.e(TAG, "Stop scan failed", e)
        }
        scanner = null
        _isScanning.value = false
    }

    override fun connectToDevice(deviceId: String) {
        stopScan()
        _connectionState.value = ConnectionState.CONNECTING

        val device = bluetoothAdapter?.getRemoteDevice(deviceId)
        if (device == null) {
            _error.value = "Device not found"
            _connectionState.value = ConnectionState.DISCONNECTED
            return
        }

        try {
            gatt = device.connectGatt(context, false, gattCallback)
        } catch (e: Exception) {
            Log.e(TAG, "Connect failed", e)
            _error.value = "Connection failed: ${e.message}"
            _connectionState.value = ConnectionState.DISCONNECTED
        }
    }

    override fun disconnectFromDevice() {
        _connectionState.value = ConnectionState.DISCONNECTING
        try {
            gatt?.disconnect()
        } catch (e: Exception) {
            Log.e(TAG, "Disconnect failed", e)
            gatt?.close()
            gatt = null
            _connectionState.value = ConnectionState.DISCONNECTED
        }
    }

    override fun clearError() {
        _error.value = null
    }

    override fun shutDown() {
        stopScan()
        gatt?.disconnect()
        gatt?.close()
        gatt = null
        hrvProcessor.reset()
    }
}
